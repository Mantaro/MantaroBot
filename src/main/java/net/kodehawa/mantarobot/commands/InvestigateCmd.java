/*
 * Copyright (C) 2016 Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.api.utils.TimeUtil;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Module
public class InvestigateCmd {
    public static void investigate(Context ctx, Type type, String id, boolean file) {
        switch (type) {
            case GUILD -> investigateGuild(ctx, MantaroBot.getInstance().getShardManager().getGuildById(id), file);
            case USER -> investigateUser(ctx, MantaroBot.getInstance().getShardManager().getUserById(id), file);
            case CHANNEL -> investigateChannel(ctx, MantaroBot.getInstance().getShardManager().getTextChannelById(id), file);
            default -> throw new AssertionError();
        }
    }

    private static void investigateGuild(Context ctx, Guild guild, boolean file) {
        if (guild == null) {
            ctx.send("Unknown guild");
            return;
        }

        var investigation = new Investigation(file);
        CompletableFuture.allOf(guild.getTextChannels().stream().filter(tc ->
                guild.getSelfMember().hasPermission(tc, Permission.MESSAGE_HISTORY)
        ).map(tc -> {
            var f = new CompletableFuture<>();
            tc.getIterableHistory().takeAsync(200)
                    .thenAccept(messages -> {
                        List<InvestigatedMessage> res = investigation.get(tc);
                        messages.forEach(m -> res.add(0, InvestigatedMessage.from(m)));
                        f.complete(null);
                    })
                    .exceptionally(e -> {
                        f.completeExceptionally(e);
                        return null;
                    });
            return f;
        }).toArray(CompletableFuture[]::new))
                .thenRun(() -> investigation.result(guild, ctx))
                .exceptionally(e -> {
                    e.printStackTrace();
                    ctx.send("Unable to execute: " + e);
                    return null;
                });
    }

    private static void investigateUser(Context ctx, User user, boolean file) {
        if (user == null) {
            ctx.send("Unknown user");
            return;
        }

        var eb = new EmbedBuilder()
                .setTitle("Please pick a guild")
                .setColor(Color.PINK);

        DiscordUtils.selectListButton(ctx, user.getMutualGuilds(), Guild::toString, s -> eb.setDescription(s).build(), g -> investigateGuild(ctx, g, file));
    }

    private static void investigateChannel(Context ctx, TextChannel channel, boolean file) {
        if (channel == null) {
            ctx.send("Unknown channel");
            return;
        }

        var investigation = new Investigation(file);
        channel.getIterableHistory().takeAsync(200)
                .thenAccept(messages -> {
                    List<InvestigatedMessage> res = investigation.get(channel);
                    messages.forEach(m -> res.add(0, InvestigatedMessage.from(m)));
                })
                .thenRun(() -> investigation.result(channel.getGuild(), ctx))
                .exceptionally(e -> {
                    e.printStackTrace();
                    ctx.send("Unable to execute: " + e);
                    return null;
                });
    }

    @Subscribe
    public void investigate(CommandRegistry cr) {
        cr.register("investigate", new SimpleCommand(CommandCategory.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (args.length == 0) {
                    ctx.send("You need to provide an id!");
                    return;
                }

                String id;
                try {
                    //noinspection ResultOfMethodCallIgnored
                    Long.parseUnsignedLong(id = args[0]);
                } catch (NumberFormatException e) {
                    ctx.send("That's not a valid id!");
                    return;
                }

                Type type;
                boolean file;

                if (args.length > 1) {
                    String s = args[1];
                    if (s.equalsIgnoreCase("file")) {
                        type = Type.GUILD;
                        file = true;
                    } else {
                        try {
                            type = Type.valueOf(s.toUpperCase());
                            file = args.length > 2 && args[2].equalsIgnoreCase("file");
                        } catch (Exception e) {
                            type = Type.GUILD;
                            file = false;
                        }
                    }
                } else {
                    type = Type.GUILD;
                    file = false;
                }

                investigate(ctx, type, id, file);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Investigate suspicious users, guilds or channels.")
                        .setUsage("~>investigate <id> [type]\n~>investigate <id> [type] file")
                        .addParameter("id", "The guild, user or channel id")
                        .addParameter("type", "guild, user or channel, defaults to guild")
                        .build();
            }
        });
    }

    public enum Type {
        GUILD, USER, CHANNEL
    }

    private static class Investigation {
        private final Map<String, ChannelData> parts = new ConcurrentHashMap<>();
        private final boolean file;

        private Investigation(boolean file) {
            this.file = file;
        }

        public List<InvestigatedMessage> get(TextChannel key) {
            return parts.computeIfAbsent(key.getId(), __ -> new ChannelData(key)).messages;
        }

        public void result(Guild target, Context ctx) {
            if (file) {
                var channels = new JSONObject();
                parts.forEach((channelId, channel) -> channels.put(channelId, channel.toJson()));

                var object = new JSONObject()
                        .put("name", target.getName())
                        .put("id", target.getId())
                        .put("channels", channels);

                var bytes = object.toString().getBytes(StandardCharsets.UTF_8);
                if (bytes.length > 7_800_000) {
                    ctx.send("Result too big!");
                } else {
                    ctx.sendFile(bytes, "result.json");
                }
            } else {
                if (parts.size() == 1) {
                    ctx.send(Utils.paste(
                            parts.entrySet().iterator().next().getValue().messages.stream()
                                    .map(InvestigatedMessage::format)
                                    .collect(Collectors.joining("\n"))
                    ));
                } else {
                    ctx.send(parts.entrySet().stream().map(entry ->
                            entry.getKey() + ": " + Utils.paste(
                                    entry.getValue().messages.stream()
                                            .map(InvestigatedMessage::format)
                                            .collect(Collectors.joining("\n"))
                            )
                    ).collect(Collectors.joining("\n")));
                }
            }
        }
    }

    private static class ChannelData {
        private final List<InvestigatedMessage> messages = new LinkedList<>();
        private final String name;

        private ChannelData(TextChannel channel) {
            this.name = channel.getName();
        }

        public JSONObject toJson() {
            var array = new JSONArray();
            messages.forEach(m -> array.put(m.toJson()));

            var stats = new JSONObject();
            var delays = new JSONObject();

            messages.stream().collect(Collectors.groupingBy(InvestigatedMessage::authorId))
                    .forEach((author, m) -> {
                        var d = new JSONArray();
                        var it = m.iterator();
                        var time = it.next().timestamp().toInstant().toEpochMilli();

                        while (it.hasNext()) {
                            var msg = it.next();
                            var creation = msg.timestamp().toInstant().toEpochMilli();
                            var delta = creation - time;
                            time = creation;
                            d.put(delta);
                        }
                        delays.put(author, d);
                    });
            stats.put("delays", delays);
            return new JSONObject()
                    .put("name", name)
                    .put("messages", array)
                    .put("stats", stats);
        }
    }

    private record InvestigatedMessage(String id, String authorName, String authorDiscriminator, String authorId, boolean bot, String raw, String content, Message.Interaction command) {
        static InvestigatedMessage from(Message message) {
                return new InvestigatedMessage(message.getId(), message.getAuthor().getName(),
                        message.getAuthor().getDiscriminator(), message.getAuthor().getId(),
                        message.getAuthor().isBot(), message.getContentRaw(), message.getContentStripped(), message.getInteraction());
            }

            OffsetDateTime timestamp() {
                return TimeUtil.getTimeCreated(MiscUtil.parseSnowflake(id));
            }

            String format() {
                return "%s - %s - %-37s (%-20s bot = %5s): %s for command %s".formatted(
                        timestamp(),
                        id,
                        authorName + "#" + authorDiscriminator,
                        authorId + ",",
                        bot,
                        content, command == null ? "" : "%s ran by %d (%s)".formatted(command.getName(), command.getUser().getIdLong(), command.getUser().getAsTag())
                );
            }

            JSONObject toJson() {
                var object = new JSONObject()
                        .put("id", id)
                        .put("timestamp", timestamp())
                        .put("author", new JSONObject()
                                .put("name", authorName)
                                .put("discriminator", authorDiscriminator)
                                .put("id", authorId)
                                .put("bot", bot)
                        )
                        .put("content", content)
                        .put("raw", raw);

                if (command != null) {
                    object.put("command", new JSONObject()
                            .put("name", command.getName())
                            .put("type", command.getType())
                            .put("username", command.getUser().getName())
                            .put("userdiscriminator", command.getUser().getDiscriminator())
                            .put("userid", command.getUser().getIdLong())
                    );
                }

                return object;
            }
        }
}
