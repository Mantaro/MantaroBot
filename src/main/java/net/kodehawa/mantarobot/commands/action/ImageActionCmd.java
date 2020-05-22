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
import net.dv8tion.jda.api.entities.Message;
import net.kodehawa.mantarobot.core.modules.commands.NoArgsCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.Utils.handleIncreasingRatelimit;

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

    public ImageActionCmd(String name, String desc, EmoteReference emoji, String format, List<String> images, String lonelyLine, String botLine, boolean swap) {
        super(Category.ACTION);
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

    public ImageActionCmd(String name, String desc, EmoteReference emoji, String format, String type, String lonelyLine, String botLine) {
        super(Category.ACTION);
        this.name = name;
        this.desc = desc;
        this.format = format;
        this.emoji = emoji;
        this.images = Collections.singletonList(weebapi.getRandomImageByType(type, false, "gif").getKey());
        this.lonelyLine = lonelyLine;
        this.type = type;
        this.botLine = botLine;
        this.rateLimiter = buildRatelimiter(name);
    }

    public ImageActionCmd(String name, String desc, EmoteReference emoji, String format, String type, String lonelyLine, String botLine, boolean swap) {
        super(Category.ACTION);
        this.name = name;
        this.desc = desc;
        this.format = format;
        this.emoji = emoji;
        this.images = Collections.singletonList(weebapi.getRandomImageByType(type, false, "gif").getKey());
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
        if (!Utils.handleIncreasingRatelimit(rateLimiter, ctx.getAuthor(), ctx.getEvent(), null))
            return;

        I18nContext languageContext = ctx.getGuildLanguageContext();
        String random = "";
        if (images.size() == 1) {
            if (type != null) {
                Pair<String, String> result = weebapi.getRandomImageByType(type, false, "gif");
                String image = result.getKey();

                if (image == null) {
                    ctx.sendLocalized("commands.action.error_retrieving", EmoteReference.SAD);
                    return;
                }

                images = Collections.singletonList(image);
                random = images.get(0); //Guaranteed random selection :^).
            }
        } else {
            random = images.get(rand.nextInt(images.size()));
        }

        try {
            if (ctx.getMentionedMembers().isEmpty()) {
                ctx.sendLocalized("commands.action.no_mention", EmoteReference.ERROR);
                return;
            }

            MessageBuilder toSend = new MessageBuilder()
                    .append(String.format(emoji + languageContext.get(format), "**" + noMentions(ctx)
                            + "**", "**" + ctx.getMember().getEffectiveName() + "**")
                    ).stripMentions(ctx.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE);


            if (swapNames) {
                toSend = new MessageBuilder()
                        .append(String.format(emoji + languageContext.get(format), "**" + ctx.getMember().getEffectiveName()
                                + "**", "**" + noMentions(ctx) + "**")
                        ).stripMentions(ctx.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE);
            }

            if (isLonely(ctx)) {
                toSend = new MessageBuilder().append("**").append(languageContext.get(lonelyLine)).append("**");
            }

            if (isMentioningBot(ctx)) {
                toSend = new MessageBuilder().append("**").append(languageContext.get(botLine)).append("**");
            }

            toSend.setEmbed(new EmbedBuilder().setColor(Color.DARK_GRAY).setImage(random).build());
            toSend.sendTo(ctx.getChannel()).queue();
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
