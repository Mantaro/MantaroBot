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

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.modules.commands.NoArgsCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.utils.cache.URLCache;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ImageCmd extends NoArgsCommand {
    public static final URLCache CACHE = new URLCache(10);
    
    private final String desc;
    private final String imageName;
    private final String name;
    private final String toSend;
    private final WeebAPIRequester weebapi = new WeebAPIRequester();
    private final Random rand = new Random();
    private List<String> images;
    private boolean noMentions = false;
    private String type;
    
    public ImageCmd(String name, String desc, String imageName, List<String> images, String toSend) {
        super(Category.ACTION);
        this.name = name;
        this.desc = desc;
        this.imageName = imageName;
        this.images = images;
        this.toSend = toSend;
    }
    
    public ImageCmd(String name, String desc, String imageName, List<String> images, String toSend, boolean noMentions) {
        super(Category.ACTION);
        this.name = name;
        this.desc = desc;
        this.imageName = imageName;
        this.images = images;
        this.toSend = toSend;
        this.noMentions = noMentions;
    }
    
    public ImageCmd(String name, String desc, String imageName, String type, String toSend) {
        super(Category.ACTION);
        this.name = name;
        this.desc = desc;
        this.imageName = imageName;
        this.images = Collections.singletonList(weebapi.getRandomImageByType(type, false, null).getKey());
        this.toSend = toSend;
        this.type = type;
    }
    
    public ImageCmd(String name, String desc, String imageName, String type, String toSend, boolean noMentions) {
        super(Category.ACTION);
        this.name = name;
        this.desc = desc;
        this.imageName = imageName;
        this.images = Collections.singletonList(weebapi.getRandomImageByType(type, false, null).getKey());
        this.toSend = toSend;
        this.noMentions = noMentions;
        this.type = type;
    }
    
    @Override
    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
        String random;
        String id = "";
        if(images.size() == 1) {
            if(type != null) {
                Pair<String, String> result = weebapi.getRandomImageByType(type, false, null);
                images = Collections.singletonList(result.getKey());
                id = result.getValue();
            }
            
            random = images.get(0); //Guaranteed random selection :^).
        } else {
            random = images.get(rand.nextInt(images.size()));
        }
        
        String extension = random.substring(random.lastIndexOf("."));
        MessageBuilder builder = new MessageBuilder();
        builder.append(EmoteReference.TALKING);
        
        if(!noMentions) {
            List<User> users = event.getMessage().getMentionedUsers();
            String names = "";
            names = users.stream().distinct().map(user -> {
                if(event.getGuild().getMember(user) == null) {
                    return "unknown";
                }
                
                return event.getGuild().getMember(user).getEffectiveName();
            }).collect(Collectors.joining(", "));
            if(!names.isEmpty())
                builder.append("**").append(names).append("**, ");
        }
        
        builder.append(languageContext.get(toSend));
        event.getChannel().sendMessage(builder.build()).addFile(CACHE.getInput(random), imageName + "-" + id + "." + extension).queue();
    }
    
    @Override
    public HelpContent help() {
        return new HelpContent.Builder()
                       .setDescription(desc)
                       .build();
    }
}
