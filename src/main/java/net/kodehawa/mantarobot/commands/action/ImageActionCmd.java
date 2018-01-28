/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.action;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.modules.commands.NoArgsCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.utils.cache.URLCache;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static br.com.brjdevs.java.utils.collections.CollectionUtils.random;

@Slf4j
public class ImageActionCmd extends NoArgsCommand {
    public static final URLCache CACHE = new URLCache(20);

    private final Color color;
    private final String desc;
    private final String format;
    private final String imageName;
    private final String lonelyLine;
    private final String name;
    private final WeebAPIRequester weebapi = new WeebAPIRequester();
    private List<String> images;
    private boolean swapNames = false;
    private String type;

    public ImageActionCmd(String name, String desc, Color color, String imageName, String format, List<String> images, String lonelyLine, boolean swap) {
        super(Category.ACTION);
        this.name = name;
        this.desc = desc;
        this.color = color;
        this.imageName = imageName;
        this.format = format;
        this.images = images;
        this.lonelyLine = lonelyLine;
        this.swapNames = swap;
    }

    public ImageActionCmd(String name, String desc, Color color, String imageName, String format, String type, String lonelyLine) {
        super(Category.ACTION);
        this.name = name;
        this.desc = desc;
        this.color = color;
        this.imageName = imageName;
        this.format = format;
        this.images = Collections.singletonList(weebapi.getRandomImageByType(type, false, "gif").getKey());
        this.lonelyLine = lonelyLine;
        this.type = type;
    }

    public ImageActionCmd(String name, String desc, Color color, String imageName, String format, String type, String lonelyLine, boolean swap) {
        super(Category.ACTION);
        this.name = name;
        this.desc = desc;
        this.color = color;
        this.imageName = imageName;
        this.format = format;
        this.images = Collections.singletonList(weebapi.getRandomImageByType(type, false, "gif").getKey());
        this.lonelyLine = lonelyLine;
        this.swapNames = swap;
        this.type = type;
    }

    @Override
    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
        String random = "";
        String id = "";
        if(images.size() == 1) {
            if(type != null) {
                Pair<String, String> result = weebapi.getRandomImageByType(type, false, "gif");
                String image = result.getKey();
                id = result.getValue();

                if(image == null) {
                    event.getChannel().sendMessage(EmoteReference.SAD + "We got an error while retrieving the next gif for this action...").queue();
                    return;
                }

                images = Collections.singletonList(image);
                random = images.get(0); //Guaranteed random selection :^).
            }
        } else {
            random = random(images);
        }

        try {
            if(event.getMessage().getMentionedUsers().isEmpty()) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention a user").queue();
                return;
            }

            MessageBuilder toSend = new MessageBuilder()
                    .append(String.format(format, "**" + noMentions(event) + "**", "**" + event.getMember().getEffectiveName() + "**"))
                    .stripMentions(event.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE);


            if(swapNames) {
                toSend = new MessageBuilder()
                        .append(String.format(format, "**" + event.getMember().getEffectiveName() + "**", "**" + noMentions(event) + "**")
                        ).stripMentions(event.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE);
            }

            if(isLonely(event)) {
                toSend = new MessageBuilder().append("**").append(lonelyLine).append("**");
            }

            event.getChannel().sendFile(
                    CACHE.getInput(random),
                    imageName + "-" + id + ".gif",
                    toSend.build()
            ).queue();
        } catch(Exception e) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "S-Sorry, but I dropped the image. Probably I don't have permissions to send it.").queue();
        }
    }

    @Override
    public MessageEmbed help(GuildMessageReceivedEvent event) {
        return helpEmbed(event, name)
                .setDescription(desc)
                .setColor(color)
                .build();
    }

    private boolean isLonely(GuildMessageReceivedEvent event) {
        return event.getMessage().getMentionedUsers().stream().anyMatch(user -> user.getId().equals(event.getAuthor().getId()));
    }

    private String noMentions(GuildMessageReceivedEvent event) {
        return event.getMessage().getMentionedUsers().stream().map(user -> event.getGuild().getMember(user).getEffectiveName()).collect(Collectors.joining(", ")).trim();
    }
}
