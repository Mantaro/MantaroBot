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
import net.kodehawa.mantarobot.commands.action.cache.ImageCache;
import net.kodehawa.mantarobot.commands.action.cache.ImageCacheType;
import net.kodehawa.mantarobot.core.modules.commands.NoArgsCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;
import net.kodehawa.mantarobot.utils.data.JsonDataManager;
import redis.clients.jedis.args.ExpiryOption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ImageActionCmd extends NoArgsCommand {
    private final String desc;
    private final String format;
    private final String lonelyLine;
    private final String name;
    private final WeebAPIRequester weebAPI = new WeebAPIRequester();
    private final Random rand = new Random();
    private final IncreasingRateLimiter rateLimiter;
    private List<String> images;
    private boolean swapNames = false;
    private String type;
    private final EmoteReference emoji;
    private final String botLine;

    public ImageActionCmd(String name, String desc, EmoteReference emoji,
                          String format, List<String> images, String lonelyLine, String botLine, boolean swap) {
        super(CommandCategory.ACTION);
        this.name = name;
        this.desc = desc;
        this.format = format;
        this.emoji = emoji;
        this.images = images;
        this.lonelyLine = lonelyLine;
        this.swapNames = swap;
        this.botLine = botLine;
        this.rateLimiter = buildRatelimiter(name);
    }

    public ImageActionCmd(String name, String desc, EmoteReference emoji,
                          String format, String type, String lonelyLine, String botLine) {
        super(CommandCategory.ACTION);
        this.name = name;
        this.desc = desc;
        this.format = format;
        this.emoji = emoji;
        this.images = Collections.emptyList();
        this.lonelyLine = lonelyLine;
        this.type = type;
        this.botLine = botLine;
        this.rateLimiter = buildRatelimiter(name);
    }

    public ImageActionCmd(String name, String desc, EmoteReference emoji,
                          String format, String type, String lonelyLine, String botLine, boolean swap) {
        super(CommandCategory.ACTION);
        this.name = name;
        this.desc = desc;
        this.format = format;
        this.emoji = emoji;
        this.images = Collections.emptyList();
        this.lonelyLine = lonelyLine;
        this.swapNames = swap;
        this.type = type;
        this.botLine = botLine;
        this.rateLimiter = buildRatelimiter(name);
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
    protected void call(Context ctx, String content) {
        if (!RatelimitUtils.ratelimit(rateLimiter, ctx, null)) {
            return;
        }

        var languageContext = ctx.getGuildLanguageContext();
        var random = "";
        try {
            if (type != null) {
                var result = ImageCache.getImage(weebAPI.getRandomImageByType(type, false, "gif"), type);
                var image = result.url();
                images = Collections.singletonList(image);
                random = images.get(0); //Guaranteed random selection :^).
            } else {
                if (images.isEmpty()) {
                    ctx.sendLocalized("commands.action.no_type", EmoteReference.ERROR);
                    return;
                }

                random = images.get(rand.nextInt(images.size()));
            }
        } catch (Exception e) {
            ctx.sendLocalized("commands.action.error_retrieving", EmoteReference.ERROR);
            return;
        }

        try {
            var mentionedMembers = ctx.getMentionedMembers();
            if (mentionedMembers.isEmpty()) {
                ctx.sendLocalized("commands.action.no_mention", EmoteReference.ERROR);
                return;
            }

            boolean filtered = false;
            if (mentionedMembers.size() == 1) {
                final var dbUser = ctx.getDBUser(mentionedMembers.get(0).getId());
                if (dbUser.getData().isActionsDisabled()) {
                    ctx.sendLocalized("commands.action.actions_disabled", EmoteReference.ERROR);
                    return;
                }
            } else {
                var filter = mentionedMembers.stream()
                        .limit(10)
                        .filter(member -> ctx.getDBUser(member).getData().isActionsDisabled()).toList();

                // Needs to be mutable.
                mentionedMembers = new ArrayList<>(mentionedMembers);
                if (mentionedMembers.removeAll(filter)) {
                    filtered = true;
                }

                if (mentionedMembers.isEmpty()) {
                    ctx.sendLocalized("commands.action.no_mention_disabled", EmoteReference.ERROR);
                    return;
                }
            }

            var toSend = new MessageCreateBuilder()
                    .addContent(emoji.toHeaderString())
                    .addContent(String.format(languageContext.get(format),
                            "**%s**".formatted(noMentions(mentionedMembers)),
                            "**%s**".formatted(ctx.getMember().getEffectiveName()))
                    );


            if (swapNames) {
                toSend = new MessageCreateBuilder()
                        .addContent(emoji.toHeaderString())
                        .addContent(String.format(
                                languageContext.get(format),
                                "**%s**".formatted(ctx.getMember().getEffectiveName()),
                                "**%s**".formatted(noMentions(mentionedMembers))
                        ));
            }

            if (isLonely(ctx)) {
                toSend = new MessageCreateBuilder()
                        .addContent("**")
                        .addContent(languageContext.get(lonelyLine))
                        .addContent("**");
            }

            if (isMentioningBot(ctx)) {
                toSend = new MessageCreateBuilder()
                        .addContent("**")
                        .addContent(languageContext.get(botLine))
                        .addContent("**");
            }

            if (filtered) {
                toSend.addContent("\n").addContent(
                        String.format(languageContext.get("commands.action.filtered"), EmoteReference.WARNING)
                );
            }

            var member = ctx.getMember();
            toSend.setEmbeds(new EmbedBuilder()
                    .setColor(ctx.getMemberColor())
                    .setImage(random)
                    .build()
            );

            ctx.getChannel().sendMessage(toSend.build()).queue();
        } catch (Exception e) {
            e.printStackTrace();
            ctx.sendLocalized("commands.action.permission_or_unexpected_error", EmoteReference.ERROR);
        }
    }

    @Override
    public HelpContent help() {
        return new HelpContent.Builder()
                .setDescription(desc)
                .setUsagePrefixed(name + " @user")
                .build();
    }

    private boolean isMentioningBot(Context ctx) {
        return ctx.getMentionedUsers().stream().anyMatch(user -> user.getIdLong() == ctx.getSelfUser().getIdLong());
    }

    private boolean isLonely(Context ctx) {
        return ctx.getMentionedUsers().stream().anyMatch(user -> user.getId().equals(ctx.getAuthor().getId()));
    }

    private String noMentions(List<Member> mentions) {
        return mentions.stream()
                .map(Member::getEffectiveName)
                .collect(Collectors.joining(", "))
                .trim();
    }
}
