/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.action;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.modules.commands.NoArgsCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.cache.URLCache;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.Utils.handleDefaultIncreasingRatelimit;

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
    protected void call(GuildMessageReceivedEvent event, I18nContext lang, String content) {
        if (!handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event, null))
            return;

        TextChannel channel = event.getChannel();

        I18nContext languageContext = new I18nContext(MantaroData.db().getGuild(event.getGuild()).getData(), null);
        String random = "";
        if (images.size() == 1) {
            if (type != null) {
                Pair<String, String> result = weebapi.getRandomImageByType(type, false, "gif");
                String image = result.getKey();

                if (image == null) {
                    channel.sendMessageFormat(languageContext.get("commands.action.error_retrieving"), EmoteReference.SAD).queue();
                    return;
                }

                images = Collections.singletonList(image);
                random = images.get(0); //Guaranteed random selection :^).
            }
        } else {
            random = images.get(rand.nextInt(images.size()));
        }

        try {
            if (event.getMessage().getMentionedMembers().isEmpty()) {
                channel.sendMessageFormat(languageContext.get("commands.action.no_mention"), EmoteReference.ERROR).queue();
                return;
            }

            MessageBuilder toSend = new MessageBuilder()
                    .append(String.format(emoji + languageContext.get(format), "**" + noMentions(event.getMessage())
                            + "**", "**" + event.getMember().getEffectiveName() + "**")
                    ).stripMentions(event.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE);


            if (swapNames) {
                toSend = new MessageBuilder()
                        .append(String.format(emoji + languageContext.get(format), "**" + event.getMember().getEffectiveName()
                                + "**", "**" + noMentions(event.getMessage()) + "**")
                        ).stripMentions(event.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE);
            }

            if (isLonely(event)) {
                toSend = new MessageBuilder().append("**").append(languageContext.get(lonelyLine)).append("**");
            }

            if (isMentioningBot(event)) {
                toSend = new MessageBuilder().append("**").append(languageContext.get(botLine)).append("**");
            }

            toSend.setEmbed(new EmbedBuilder().setColor(Color.DARK_GRAY).setImage(random).build());
            toSend.sendTo(channel).queue();
        } catch (Exception e) {
            e.printStackTrace();
            channel.sendMessageFormat(languageContext.get("commands.action.permission_or_unexpected_error"), EmoteReference.ERROR).queue();
        }
    }

    @Override
    public HelpContent help() {
        return new HelpContent.Builder()
                .setDescription(desc)
                .setUsagePrefixed(name + " @user")
                .build();
    }

    private boolean isMentioningBot(GuildMessageReceivedEvent event) {
        return event.getMessage().getMentionedUsers().stream().anyMatch(user -> user.getIdLong() == event.getGuild().getSelfMember().getUser().getIdLong());
    }

    private boolean isLonely(GuildMessageReceivedEvent event) {
        return event.getMessage().getMentionedUsers().stream().anyMatch(user -> user.getId().equals(event.getAuthor().getId()));
    }

    private String noMentions(Message message) {
        return message.getMentionedMembers().stream().map(Member::getEffectiveName).collect(Collectors.joining(", ")).trim();
    }
}
