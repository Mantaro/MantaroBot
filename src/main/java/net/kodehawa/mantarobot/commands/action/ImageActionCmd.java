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

package net.kodehawa.mantarobot.commands.action;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.kodehawa.mantarobot.core.modules.commands.NoArgsCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.awt.Color;
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
    private final WeebAPIRequester weebapi = new WeebAPIRequester();
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
                var result = weebapi.getRandomImageByType(type, false, "gif");
                var image = result.getKey();

                if (image == null) {
                    ctx.sendLocalized("commands.action.error_retrieving", EmoteReference.SAD);
                    return;
                }

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
            if (ctx.getMentionedMembers().isEmpty()) {
                ctx.sendLocalized("commands.action.no_mention", EmoteReference.ERROR);
                return;
            }

            var toSend = new MessageBuilder()
                    .append(emoji)
                    .append(String.format(languageContext.get(format),
                            "**%s**".formatted(noMentions(ctx)),
                            "**%s**".formatted(ctx.getMember().getEffectiveName()))
                    );


            if (swapNames) {
                toSend = new MessageBuilder()
                        .append(emoji)
                        .append(String.format(
                                languageContext.get(format),
                                "**%s**".formatted(ctx.getMember().getEffectiveName()),
                                "**%s**".formatted(noMentions(ctx))
                        ));
            }

            if (isLonely(ctx)) {
                toSend = new MessageBuilder()
                        .append("**")
                        .append(languageContext.get(lonelyLine))
                        .append("**");
            }

            if (isMentioningBot(ctx)) {
                toSend = new MessageBuilder()
                        .append("**")
                        .append(languageContext.get(botLine))
                        .append("**");
            }

            var member = ctx.getMember();
            toSend.setEmbed(new EmbedBuilder()
                    .setColor(member.getColor() == null ? Color.PINK : member.getColor())
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

    private String noMentions(Context ctx) {
        return ctx.getMentionedMembers().stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")).trim();
    }
}
