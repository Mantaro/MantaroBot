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

package net.kodehawa.mantarobot.options;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.commands.interaction.polls.Poll;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.core.OptionHandler;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Option
public class GeneralOptions extends OptionHandler {
    
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GeneralOptions.class);
    
    @Subscribe
    public void onRegistry(OptionRegistryEvent e) {
        registerOption("lobby:reset", "Lobby reset", "Fixes stuck game/poll/operations session.", (event, lang) -> {
            GameLobby.LOBBYS.remove(event.getChannel().getIdLong());
            Poll.getRunningPolls().remove(event.getChannel().getId());
            
            List<Future<Void>> stuck = InteractiveOperations.get(event.getChannel());
            if(stuck.size() > 0)
                stuck.forEach(f -> f.cancel(true));
            
            event.getChannel().sendMessageFormat(lang.get("options.lobby_reset.success"), EmoteReference.CORRECT).queue();
        });
        
        registerOption("modlog:blacklist", "Prevents an user from appearing in modlogs",
                "Prevents an user from appearing in modlogs.\n" +
                        "You need the user mention.\n" +
                        "Example: ~>opts modlog blacklist @user", (event, lang) -> {
                    List<User> mentioned = event.getMessage().getMentionedUsers();
                    if(mentioned.isEmpty()) {
                        event.getChannel().sendMessageFormat(lang.get("options.modlog_blacklist.no_mentions"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    List<String> toBlackList = mentioned.stream().map(ISnowflake::getId).collect(Collectors.toList());
                    String blacklisted = mentioned.stream().map(user -> user.getName() + "#" + user.getDiscriminator()).collect(Collectors.joining(","));
                    
                    guildData.getModlogBlacklistedPeople().addAll(toBlackList);
                    dbGuild.save();
                    
                    event.getChannel().sendMessageFormat(lang.get("options.modlog_blacklist.success"), EmoteReference.CORRECT, blacklisted).queue();
                });
        
        registerOption("modlog:whitelist", "Allows an user from appearing in modlogs (everyone by default)",
                "Allows an user from appearing in modlogs.\n" +
                        "You need the user mention.\n" +
                        "Example: ~>opts modlog whitelist @user", (event, lang) -> {
                    List<User> mentioned = event.getMessage().getMentionedUsers();
                    if(mentioned.isEmpty()) {
                        event.getChannel().sendMessageFormat(lang.get("options.modlog_whitelist.no_mentions"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    List<String> toUnBlacklist = mentioned.stream().map(ISnowflake::getId).collect(Collectors.toList());
                    String unBlacklisted = mentioned.stream().map(user -> user.getName() + "#" + user.getDiscriminator()).collect(Collectors.joining(","));
                    
                    guildData.getModlogBlacklistedPeople().removeAll(toUnBlacklist);
                    dbGuild.save();
                    
                    event.getChannel().sendMessageFormat(lang.get("options.modlog_whitelist.success"), EmoteReference.CORRECT, unBlacklisted).queue();
                });
        
        registerOption("linkprotection:toggle", "Link-protection toggle", "Toggles anti-link protection.", (event, lang) -> {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            boolean toggler = guildData.isLinkProtection();
            
            guildData.setLinkProtection(!toggler);
            event.getChannel().sendMessageFormat(lang.get("options.linkprotection_toggle.success"), EmoteReference.CORRECT, !toggler).queue();
            dbGuild.save();
        });
        
        registerOption("linkprotection:channel:allow", "Link-protection channel allow",
                "Allows the posting of invites on a channel.\n" +
                        "You need the channel name.\n" +
                        "Example: ~>opts linkprotection channel allow promote-here",
                "Allows the posting of invites on a channel.", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.linkprotection_channel_allow.no_channel"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    String channelName = args[0];
                    
                    Consumer<TextChannel> consumer = tc -> {
                        guildData.getLinkProtectionAllowedChannels().add(tc.getId());
                        dbGuild.save();
                        event.getChannel().sendMessageFormat(lang.get("options.linkprotection_channel_allow.success"), EmoteReference.OK, tc.getAsMention()).queue();
                    };
                    
                    TextChannel channel = Utils.findChannelSelect(event, channelName, consumer);
                    
                    if(channel != null) {
                        consumer.accept(channel);
                    }
                });
        addOptionAlias("linkprotection:channel:allow", "linkprotection:channel:enable");
        
        
        registerOption("linkprotection:channel:disallow", "Link-protection channel disallow",
                "Disallows the posting of invites on a channel.\n" +
                        "You need the channel name.\n" +
                        "Example: ~>opts linkprotection channel disallow general",
                "Disallows the posting of invites on a channel (every channel by default)", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.linkprotection_channel_disallow.no_channel"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    String channelName = args[0];
                    
                    Consumer<TextChannel> consumer = tc -> {
                        guildData.getLinkProtectionAllowedChannels().remove(tc.getId());
                        dbGuild.save();
                        event.getChannel().sendMessageFormat(lang.get("options.linkprotection_channel_disallow.success"), EmoteReference.OK, tc.getAsMention()).queue();
                    };
                    
                    TextChannel channel = Utils.findChannelSelect(event, channelName, consumer);
                    
                    if(channel != null) {
                        consumer.accept(channel);
                    }
                });
        addOptionAlias("linkprotection:channel:disallow", "linkprotection:channel:disable");
        
        registerOption("linkprotection:user:allow", "Link-protection user whitelist", "Allows an user to post invites.\n" +
                                                                                              "You need to mention the user.", "Allows an user to post invites.", (event, args, lang) -> {
            if(args.length == 0) {
                event.getChannel().sendMessageFormat(lang.get("options.linkprotection_user_allow.no_user"), EmoteReference.ERROR).queue();
                return;
            }
            
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            
            if(event.getMessage().getMentionedUsers().isEmpty()) {
                event.getChannel().sendMessageFormat(lang.get("options.linkprotection_user_allow.no_mentions"), EmoteReference.ERROR).queue();
                return;
            }
            
            User toWhiteList = event.getMessage().getMentionedUsers().get(0);
            guildData.getLinkProtectionAllowedUsers().add(toWhiteList.getId());
            dbGuild.save();
            event.getChannel().sendMessageFormat(lang.get("options.linkprotection_user_allow.success"),
                    EmoteReference.CORRECT, toWhiteList.getName(), toWhiteList.getDiscriminator()
            ).queue();
        });
        addOptionAlias("linkprotection:user:allow", "linkprotection:user:enable");
        
        registerOption("linkprotection:user:disallow", "Link-protection user blacklist", "Disallows an user to post invites.\n" +
                                                                                                 "You need to mention the user. (This is the default behaviour)", "Allows an user to post invites (This is the default behaviour)", (event, args, lang) -> {
            if(args.length == 0) {
                event.getChannel().sendMessageFormat(lang.get("options.linkprotection_user_disallow.no_user"), EmoteReference.ERROR).queue();
                return;
            }
            
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            
            if(event.getMessage().getMentionedUsers().isEmpty()) {
                event.getChannel().sendMessageFormat(lang.get("options.linkprotection_user_disallow.no_mentions"), EmoteReference.ERROR).queue();
                return;
            }
            
            User toBlackList = event.getMessage().getMentionedUsers().get(0);
            
            if(!guildData.getLinkProtectionAllowedUsers().contains(toBlackList.getId())) {
                event.getChannel().sendMessageFormat(lang.get("options.linkprotection_user_disallow.not_whitelisted"), EmoteReference.ERROR).queue();
                return;
            }
            
            guildData.getLinkProtectionAllowedUsers().remove(toBlackList.getId());
            dbGuild.save();
            event.getChannel().sendMessageFormat(lang.get("options.linkprotection_user_disallow.success"),
                    EmoteReference.CORRECT, toBlackList.getName(), toBlackList.getDiscriminator()
            ).queue();
        });
        addOptionAlias("linkprotection:user:disallow", "linkprotection:user:disable");
        
        registerOption("imageboard:tags:blacklist:add", "Blacklist imageboard tags", "Blacklists the specified imageboard tag from being looked up.",
                "Blacklist imageboard tags", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.imageboard_tags_blacklist_add.no_tag"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    for(String tag : args) {
                        guildData.getBlackListedImageTags().add(tag.toLowerCase());
                    }
                    
                    dbGuild.saveAsync();
                    event.getChannel().sendMessageFormat(lang.get("options.imageboard_tags_blacklist_add.success"),
                            EmoteReference.CORRECT, String.join(" ,", args)
                    ).queue();
                });
        
        registerOption("imageboard:tags:blacklist:remove", "Un-blacklist imageboard tags", "Un-blacklist the specified imageboard tag from being looked up.",
                "Un-blacklist imageboard tags", (event, args, lang) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.imageboard_tags_blacklist_remove.no_tag"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    
                    for(String tag : args) {
                        guildData.getBlackListedImageTags().remove(tag.toLowerCase());
                    }
                    
                    dbGuild.saveAsync();
                    event.getChannel().sendMessageFormat(lang.get("options.imageboard_tags_blacklist_remove.success"),
                            EmoteReference.CORRECT, String.join(" ,", args)
                    ).queue();
                });
    }
    
    @Override
    public String description() {
        return "Everything that doesn't fit anywhere else.";
    }
}
