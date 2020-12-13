/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
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
    public static void investigate(GuildMessageReceivedEvent event, Type type, String id, boolean file) {
        switch (type) {
            case GUILD -> investigateGuild(event, MantaroBot.getInstance().getShardManager().getGuildById(id), file);
            case USER -> investigateUser(event, MantaroBot.getInstance().getShardManager().getUserById(id), file);
            case CHANNEL -> investigateChannel(event, MantaroBot.getInstance().getShardManager().getTextChannelById(id), file);
            default -> throw new AssertionError();
        }
    }

    private static void investigateGuild(GuildMessageReceivedEvent event, Guild guild, boolean file) {
        if (guild == null) {
            event.getChannel().sendMessage("Unknown guild").queue();
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
                .thenRun(() -> investigation.result(guild, event))
                .exceptionally(e -> {
                    e.printStackTrace();
                    event.getChannel().sendMessage("Unable to execute: " + e).queue();
                    return null;
                });
    }

    private static void investigateUser(GuildMessageReceivedEvent event, User user, boolean file) {
        if (user == null) {
            event.getChannel().sendMessage("Unknown user").queue();
            return;
        }

        var eb = new EmbedBuilder()
                .setTitle("Please pick a guild")
                .setColor(Color.PINK);

        DiscordUtils.selectList(event, user.getMutualGuilds(), Guild::toString, s -> eb.setDescription(s).build(), g -> investigateGuild(event, g, file));
    }

    private static void investigateChannel(GuildMessageReceivedEvent event, TextChannel channel, boolean file) {
        if (channel == null) {
            event.getChannel().sendMessage("Unknown channel").queue();
            return;
        }

        var investigation = new Investigation(file);
        channel.getIterableHistory().takeAsync(200)
                .thenAccept(messages -> {
                    List<InvestigatedMessage> res = investigation.get(channel);
                    messages.forEach(m -> res.add(0, InvestigatedMessage.from(m)));
                })
                .thenRun(() -> investigation.result(channel.getGuild(), event))
                .exceptionally(e -> {
                    e.printStackTrace();
                    event.getChannel().sendMessage("Unable to execute: " + e).queue();
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

                investigate(ctx.getEvent(), type, id, file);
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

        public void result(Guild target, GuildMessageReceivedEvent event) {
            if (file) {
                var channels = new JSONObject();
                parts.forEach((channelId, channel) -> channels.put(channelId, channel.toJson()));

                var object = new JSONObject()
                        .put("name", target.getName())
                        .put("id", target.getId())
                        .put("channels", channels);

                var bytes = object.toString().getBytes(StandardCharsets.UTF_8);
                if (bytes.length > 7_800_000) {
                    event.getChannel().sendMessage("Result too big!").queue();
                } else {
                    event.getChannel().sendFile(bytes, "result.json").queue();
                }
            } else {
                if (parts.size() == 1) {
                    event.getChannel().sendMessage(Utils.paste(
                            parts.entrySet().iterator().next().getValue().messages.stream()
                                    .map(InvestigatedMessage::format)
                                    .collect(Collectors.joining("\n"))
                    )).queue();
                } else {
                    event.getChannel().sendMessage(parts.entrySet().stream().map(entry ->
                            entry.getKey() + ": " + Utils.paste(
                                    entry.getValue().messages.stream()
                                            .map(InvestigatedMessage::format)
                                            .collect(Collectors.joining("\n"))
                            )
                    ).collect(Collectors.joining("\n"))).queue();
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

            messages.stream().collect(Collectors.groupingBy(InvestigatedMessage::getAuthorId))
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

    private static class InvestigatedMessage {
        private final String id;
        private final String authorName;
        private final String authorDiscriminator;
        private final String authorId;
        private final boolean bot;
        private final String raw;
        private final String content;

        InvestigatedMessage(String id, String authorName, String authorDiscriminator, String authorId, boolean bot, String raw, String content) {
            this.id = id;
            this.authorName = authorName;
            this.authorDiscriminator = authorDiscriminator;
            this.authorId = authorId;
            this.bot = bot;
            this.raw = raw;
            this.content = content;
        }

        static InvestigatedMessage from(Message message) {
            return new InvestigatedMessage(message.getId(), message.getAuthor().getName(),
                    message.getAuthor().getDiscriminator(), message.getAuthor().getId(),
                    message.getAuthor().isBot(), message.getContentRaw(), message.getContentStripped());
        }

        OffsetDateTime timestamp() {
            return TimeUtil.getTimeCreated(MiscUtil.parseSnowflake(id));
        }

        String format() {
            return "%s - %s - %-37s (%-20s bot = %5s): %s".formatted(
                    timestamp(),
                    id,
                    authorName + "#" + authorDiscriminator,
                    authorId + ",",
                    bot,
                    content
            );
        }

        JSONObject toJson() {
            return new JSONObject()
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
        }

        public String getId() {
            return this.id;
        }

        public String getAuthorName() {
            return this.authorName;
        }

        public String getAuthorDiscriminator() {
            return this.authorDiscriminator;
        }

        public String getAuthorId() {
            return this.authorId;
        }

        public boolean isBot() {
            return this.bot;
        }

        public String getRaw() {
            return this.raw;
        }

        public String getContent() {
            return this.content;
        }
    }
}
