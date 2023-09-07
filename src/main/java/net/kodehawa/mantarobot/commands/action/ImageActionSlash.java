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

package net.kodehawa.mantarobot.commands.action;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.cache.ImageCache;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ImageActionSlash extends SlashCommand {
    private final String format;
    private final String lonelyLine;
    private final Random rand = new Random();
    private final IncreasingRateLimiter rateLimiter;
    private List<String> images;
    private boolean swapNames = false;
    private String type;
    private final EmoteReference emoji;
    private final String botLine;

    public ImageActionSlash(String name, String desc, EmoteReference emoji,
                          String format, List<String> images, String lonelyLine, String botLine, boolean swap, boolean isSub) {
        super.setCategory(CommandCategory.ACTION);
        this.format = format;
        this.emoji = emoji;
        this.images = images;
        this.lonelyLine = lonelyLine;
        this.swapNames = swap;
        this.botLine = botLine;
        this.rateLimiter = buildRatelimiter(name);
        super.setHelp(new HelpContent.Builder()
                .setDescription(desc)
                .setUsage("`/" + (isSub ? "action " : "") + name.toLowerCase() + " user:[user] extra:[@mentions]`")
                .build()
        );
    }

    public ImageActionSlash(String name, String desc, EmoteReference emoji,
                          String format, String type, String lonelyLine, String botLine) {
        super.setCategory(CommandCategory.ACTION);
        this.format = format;
        this.emoji = emoji;
        this.images = Collections.emptyList();
        this.lonelyLine = lonelyLine;
        this.type = type;
        this.botLine = botLine;
        this.rateLimiter = buildRatelimiter(name);
        super.setHelp(new HelpContent.Builder()
                .setDescription(desc)
                .setUsage("`/" + name.toLowerCase() + " user:[user] extra:[@mentions]`")
                .build()
        );
    }

    public ImageActionSlash(String name, String desc, EmoteReference emoji,
                          String format, String type, String lonelyLine, String botLine, boolean swap, boolean isSub) {
        super.setCategory(CommandCategory.ACTION);
        this.format = format;
        this.emoji = emoji;
        this.images = Collections.emptyList();
        this.lonelyLine = lonelyLine;
        this.swapNames = swap;
        this.type = type;
        this.botLine = botLine;
        this.rateLimiter = buildRatelimiter(name);
        super.setHelp(new HelpContent.Builder()
                .setDescription(desc)
                .setUsage("`/" + (isSub ? "action " : "") + name.toLowerCase() + " user:[user] extra:[@mentions]`")
                .build()
        );
    }

    private IncreasingRateLimiter buildRatelimiter(String name) {
        return new IncreasingRateLimiter.Builder()
                .limit(1)
                .spamTolerance(2)
                .cooldown(4, TimeUnit.SECONDS)
                .maxCooldown(4, TimeUnit.MINUTES)
                .randomIncrement(true)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix(name)
                .build();
    }

    @Override
    public void process(SlashContext ctx) {
        if (!RatelimitUtils.ratelimit(rateLimiter, ctx, null)) {
            return;
        }

        var languageContext = ctx.getGuildLanguageContext();
        var random = "";
        try {
            if (type != null) {
                var result = ImageCache.getImage(type);
                var image = result.url();
                images = Collections.singletonList(image);
                random = images.get(0); //Guaranteed random selection :^).
            } else {
                if (images.isEmpty()) {
                    ctx.reply("commands.action.no_type", EmoteReference.ERROR);
                    return;
                }

                random = images.get(rand.nextInt(images.size()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.reply("commands.action.error_retrieving", EmoteReference.ERROR);
            return;
        }

        try {
            var user = ctx.getOptionAsMember("user");
            if (user == null) { // Shouldn't be possible, but just in case.
                ctx.reply("commands.action.no_mention", EmoteReference.ERROR);
                return;
            }

            var extra = ctx.getOption("extra");
            List<Member> mentions = new ArrayList<>();
            if (extra != null) {
                mentions = extra.getMentions().getMembers();
            }

            var member = ctx.getGuild().getMember(user);
            final var dbUser = ctx.getDBUser(user.getId());
            if (dbUser.isActionsDisabled()) {
                ctx.reply("commands.action.actions_disabled", EmoteReference.ERROR);
                return;
            }

            var mentioned = member.getEffectiveName();
            boolean filtered = false;
            if (!mentions.isEmpty()) {
                var filter = mentions.stream()
                        .limit(10)
                        .filter(m -> ctx.getDBUser(m).isActionsDisabled()).toList();

                // Need it to be mutable.
                mentions = new ArrayList<>(mentions);
                if (mentions.removeAll(filter)) {
                    filtered = true;
                }

                if (!mentions.isEmpty()) {
                    mentioned += ", " + mentions.stream().map(Member::getEffectiveName).collect(Collectors.joining(", "));
                }
            }

            var toSend = new MessageCreateBuilder()
                    .addContent(emoji.toHeaderString())
                    .addContent(String.format(languageContext.get(format),
                            "**%s**".formatted(mentioned),
                            "**%s**".formatted(ctx.getMember().getEffectiveName()))
                    );

            if (filtered) {
                toSend.addContent("\n").addContent(
                        String.format(languageContext.get("commands.action.filtered"), EmoteReference.WARNING)
                );
            }

            if (swapNames) {
                toSend = new MessageCreateBuilder()
                        .addContent(emoji.toHeaderString())
                        .addContent(String.format(
                                languageContext.get(format),
                                "**%s**".formatted(ctx.getMember().getEffectiveName()),
                                "**%s**".formatted(mentioned))
                        );
            }

            if (isLonely(ctx, user)) {
                toSend = new MessageCreateBuilder()
                        .addContent("**")
                        .addContent(languageContext.get(lonelyLine))
                        .addContent("**");
            }

            if (isMentioningBot(ctx, user)) {
                toSend = new MessageCreateBuilder()
                        .addContent("**")
                        .addContent(languageContext.get(botLine))
                        .addContent("**");
            }

            toSend.setEmbeds(new EmbedBuilder()
                    .setColor(ctx.getMemberColor())
                    .setImage(random)
                    .build()
            );

            if (ctx.getOption("extra") != null) {
                toSend.addContent(" ").addContent(languageContext.get("commands.action.extra"));
            }

            ctx.getEvent().getHook().sendMessage(toSend.build()).queue();
        } catch (Exception e) {
            e.printStackTrace();
            ctx.reply("commands.action.permission_or_unexpected_error", EmoteReference.ERROR);
        }
    }

    private boolean isMentioningBot(SlashContext ctx, Member user) {
        return user.getIdLong() == ctx.getSelfUser().getIdLong();
    }

    private boolean isLonely(SlashContext ctx, Member user) {
        return user.getIdLong() == ctx.getAuthor().getIdLong();
    }
}
