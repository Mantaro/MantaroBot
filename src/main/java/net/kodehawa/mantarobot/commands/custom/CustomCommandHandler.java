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

package net.kodehawa.mantarobot.commands.custom;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.MiscCmds;
import net.kodehawa.mantarobot.commands.custom.legacy.ConditionalCustoms;
import net.kodehawa.mantarobot.commands.custom.legacy.DynamicModifiers;
import net.kodehawa.mantarobot.commands.custom.v3.CCv3;
import net.kodehawa.mantarobot.commands.custom.v3.Parser;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class CustomCommandHandler {
    private static final Map<String, Func> specialHandlers = new LinkedHashMap<>();
    //there's no way in hell this would work but ok
    //actually p sure this is just to make me feel safer and serves no purpose whatsoever.
    private static final Pattern filtered1 = Pattern.compile("([a-zA-Z0-9]{24}\\.[a-zA-Z0-9]{6}\\.[a-zA-Z0-9_\\-])\\w+");
    private final String args;
    private final GuildMessageReceivedEvent event;
    private final I18nContext langContext;
    private String response;
    private String prefixUsed;

    {
        specialHandlers.put("text", (event, lang, value, args) -> event.getChannel().sendMessage(value).queue());
        
        specialHandlers.put("play", (event, lang, value, args) -> {
            GuildData data = MantaroData.db().getGuild(event.getGuild()).getData();
            if(data.getDisabledCommands().contains("play")) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "The play command is disabled on this server. Cannot run this custom command.").queue();
                return;
            }
            
            List<String> channelDisabledCommands = data.getChannelSpecificDisabledCommands().get(event.getChannel().getId());
            if(channelDisabledCommands != null && channelDisabledCommands.contains("play")) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "The play command is disabled on this channel. Cannot run this custom command.").queue();
                return;
            }
            
            try {
                new URL(value);
            } catch(Exception ignored) {
                value = "ytsearch: " + value;
            }
            
            MantaroBot.getInstance().getAudioManager().loadAndPlay(event, value, false, false, lang);
        });
        
        specialHandlers.put("embed", (event, lang, value, args) -> {
            try {
                EmbedJSON embed = GsonDataManager.gson(false)
                                          .fromJson('{' + value + '}', EmbedJSON.class);
                event.getChannel().sendMessage(embed.gen(event.getMember())).queue();
            } catch(IllegalArgumentException invalid) {
                if(invalid.getMessage().contains("URL must be a valid http or https url")) {
                    event.getChannel().sendMessageFormat(lang.get("commands.custom.invalid_image"), EmoteReference.ERROR2).queue();
                } else {
                    event.getChannel().sendMessageFormat(lang.get("commands.custom.invalid_string"), EmoteReference.ERROR2, value).queue();
                }
            } catch(Exception ignored) {
                event.getChannel().sendMessageFormat(lang.get("commands.custom.invalid_json"), EmoteReference.ERROR2, value).queue();
            }
        });
        
        specialHandlers.put("img", (event, lang, value, args) -> {
            try {
                if(!EmbedBuilder.URL_PATTERN.asPredicate().test(value)) {
                    event.getChannel().sendMessageFormat(lang.get("commands.custom.invalid_link"), EmoteReference.ERROR2, value).queue();
                    return;
                }
                
                event.getChannel().sendMessage(new EmbedBuilder().setImage(value).setColor(event.getMember().getColor()).build()).queue();
                
            } catch(IllegalArgumentException invalid) {
                event.getChannel().sendMessageFormat(lang.get("commands.custom.invalid_image"), EmoteReference.ERROR2).queue();
            }
        });
        
        specialHandlers.put("image", specialHandlers.get("img"));
        specialHandlers.put("imgembed", specialHandlers.get("img"));
        specialHandlers.put("iam", (event, lang, value, args) -> MiscCmds.iamFunction(value.trim().replace("\"", ""), event, lang));
        specialHandlers.put("iamnot", (event, lang, value, args) -> MiscCmds.iamnotFunction(value.trim().replace("\"", ""), event, lang));
        
        specialHandlers.put("iamcustom", (event, lang, value, args) -> {
            String[] arg = StringUtils.advancedSplitArgs(value, 2);
            String iam = arg[0];
            String message = this.processText(arg[1]);
            
            MiscCmds.iamFunction(iam.trim().replace("\"", ""), event, lang, message);
        });
        
        specialHandlers.put("iamnotcustom", (event, lang, value, args) -> {
            String[] arg = StringUtils.advancedSplitArgs(value, 2);
            String iam = arg[0];
            String message = this.processText(arg[1]);
            
            MiscCmds.iamnotFunction(iam.trim().replace("\"", ""), event, lang, message);
        });
    }
    public CustomCommandHandler(String prefixUsed, GuildMessageReceivedEvent event, I18nContext lang, String response) {
        this(prefixUsed, event, lang, response, "");
    }
    
    public CustomCommandHandler(String prefixUsed, GuildMessageReceivedEvent event, I18nContext lang, String response, String args) {
        this.event = event;
        this.response = response;
        this.langContext = lang;
        this.args = args;
        this.prefixUsed = prefixUsed;
    }
    
    public void handle(boolean preview) {
        if(!processResponse())
            return;
        if(specialHandling())
            return;
        
        if(response.startsWith("v3:")) {
            CCv3.process(prefixUsed, event, new Parser(response.substring(3)).parse(), preview);
            return;
        }
        
        MessageBuilder builder = new MessageBuilder().setContent(filtered1.matcher(response).replaceAll("-filtered regex-"));
        if(preview) {
            builder.append("\n\n")
                    .append(EmoteReference.WARNING)
                    .append("**This is a preview of how a CC with this content would look like, ALL MENTIONS ARE DISABLED ON THIS MODE.**\n")
                    .append("`Command Preview Requested By: ")
                    .append(event.getAuthor().getName())
                    .append("#")
                    .append(event.getAuthor().getDiscriminator())
                    .append("`")
                    .stripMentions(event.getJDA());
        }
        
        builder.stripMentions(event.getJDA(), Message.MentionType.HERE, Message.MentionType.EVERYONE).sendTo(event.getChannel()).queue();
    }
    
    public void handle() {
        this.handle(false);
    }
    
    private boolean processResponse() {
        if(response.startsWith("v3:")) {
            return true;
        }
        
        if(response.startsWith("text:")) {
            return true;
        }
        
        response = processText(response);
        
        return true;
    }
    
    private String processText(String text) {
        if(text.contains("$(")) {
            text = new DynamicModifiers()
                           .mapEvent(prefixUsed, "event", event)
                           .resolve(text);
        }
        
        return ConditionalCustoms.resolve(text, 0);
    }
    
    private boolean specialHandling() {
        int c = response.indexOf(':');
        if(c == -1) return false;
        
        String prefix = response.substring(0, c);
        String value = response.substring(c + 1);
        
        Func func = specialHandlers.get(prefix);
        if(func == null)
            return false;
        
        func.handle(event, langContext, value, args);
        
        return true;
    }
    
    private interface Func {
        void handle(GuildMessageReceivedEvent event, I18nContext lang, String value, String args);
    }
}
