/*
 * Copyright (C) 2016-2021 David Rubio Escares / Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.action;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ImageActionSlash extends SlashCommand {
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

    public ImageActionSlash(String name, String desc, EmoteReference emoji,
                          String format, List<String> images, String lonelyLine, String botLine, boolean swap) {
        super.setCategory(CommandCategory.ACTION);
        this.name = name;
        this.desc = desc;
        this.format = format;
        this.emoji = emoji;
        this.images = images;
        this.lonelyLine = lonelyLine;
        this.swapNames = swap;
        this.botLine = botLine;
        this.rateLimiter = buildRatelimiter(name);
        super.setHelp(new HelpContent.Builder()
                .setDescription(desc)
                .setUsagePrefixed(name + " @user")
                .build()
        );
    }

    public ImageActionSlash(String name, String desc, EmoteReference emoji,
                          String format, String type, String lonelyLine, String botLine) {
        super.setCategory(CommandCategory.ACTION);
        this.name = name;
        this.desc = desc;
        this.format = format;
        this.emoji = emoji;
        this.images = Collections.emptyList();
        this.lonelyLine = lonelyLine;
        this.type = type;
        this.botLine = botLine;
        this.rateLimiter = buildRatelimiter(name);
        super.setHelp(new HelpContent.Builder()
                .setDescription(desc)
                .setUsage("`/" + name.toLowerCase() + " [@user]`")
                .build()
        );
    }

    public ImageActionSlash(String name, String desc, EmoteReference emoji,
                          String format, String type, String lonelyLine, String botLine, boolean swap) {
        super.setCategory(CommandCategory.ACTION);
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
        super.setHelp(new HelpContent.Builder()
                .setDescription(desc)
                .setUsage("`/" + name.toLowerCase() + " [@user]`")
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
                var result = weebapi.getRandomImageByType(type, false, "gif");
                var image = result.url();

                if (image == null) {
                    ctx.reply("commands.action.error_retrieving", EmoteReference.SAD);
                    return;
                }

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
            ctx.reply("commands.action.error_retrieving", EmoteReference.ERROR);
            return;
        }

        try {
            var user = ctx.getOptionAsUser("user");
            if (user == null) { // Shouldn't be possible, but just in case.
                ctx.reply("commands.action.no_mention", EmoteReference.ERROR);
                return;
            }

            var member = ctx.getGuild().getMember(user);
            final var dbUser = ctx.getDBUser(user.getId());
            if (dbUser.getData().isActionsDisabled()) {
                ctx.reply("commands.action.actions_disabled", EmoteReference.ERROR);
                return;
            }

            var toSend = new MessageBuilder()
                    .append(emoji)
                    .append(String.format(languageContext.get(format),
                            "**%s**".formatted(member.getEffectiveName()),
                            "**%s**".formatted(ctx.getMember().getEffectiveName()))
                    );


            if (swapNames) {
                toSend = new MessageBuilder()
                        .append(emoji)
                        .append(String.format(
                                languageContext.get(format),
                                "**%s**".formatted(ctx.getMember().getEffectiveName()),
                                "**%s**".formatted(member.getEffectiveName()))
                        );
            }

            if (isLonely(ctx, user)) {
                toSend = new MessageBuilder()
                        .append("**")
                        .append(languageContext.get(lonelyLine))
                        .append("**");
            }

            if (isMentioningBot(ctx, user)) {
                toSend = new MessageBuilder()
                        .append("**")
                        .append(languageContext.get(botLine))
                        .append("**");
            }

            toSend.setEmbeds(new EmbedBuilder()
                    .setColor(ctx.getMemberColor())
                    .setImage(random)
                    .build()
            );

            ctx.getEvent().getHook().sendMessage(toSend.build()).queue();
        } catch (Exception e) {
            e.printStackTrace();
            ctx.reply("commands.action.permission_or_unexpected_error", EmoteReference.ERROR);
        }
    }

    private boolean isMentioningBot(SlashContext ctx, User user) {
        return user.getIdLong() == ctx.getSelfUser().getIdLong();
    }

    private boolean isLonely(SlashContext ctx, User user) {
        return user.getIdLong() == ctx.getAuthor().getIdLong();
    }
}
