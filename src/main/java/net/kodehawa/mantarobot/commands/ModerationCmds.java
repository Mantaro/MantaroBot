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

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.moderation.ModLog;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;

import java.util.List;

@Module
@SuppressWarnings("unused")
public class ModerationCmds {
    
    private static final Logger log = org.slf4j.LoggerFactory.getLogger("Moderation");
    
    @Subscribe
    public void softban(CommandRegistry cr) {
        cr.register("softban", new SimpleCommand(Category.MODERATION) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                Guild guild = event.getGuild();
                User author = event.getAuthor();
                
                TextChannel channel = event.getChannel();
                Message receivedMessage = event.getMessage();
                String reason = content;
                
                if(event.getMessage().getMentionedUsers().isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.softban.no_users"), EmoteReference.ERROR).queue();
                    return;
                }
                
                String affected = args[0];
                
                if(!guild.getMember(author).hasPermission(Permission.BAN_MEMBERS)) {
                    channel.sendMessage(String.format(languageContext.get("commands.softban.no_permission"), EmoteReference.ERROR2)).queue();
                    return;
                }
                
                Member selfMember = guild.getSelfMember();
                
                if(!selfMember.hasPermission(Permission.BAN_MEMBERS)) {
                    channel.sendMessage(String.format(languageContext.get("commands.softban.no_permission_self"), EmoteReference.ERROR2)).queue();
                    return;
                }
                
                if(args.length > 1) {
                    reason = StringUtils.splitArgs(content, 2)[1];
                }
                
                if(reason.isEmpty()) {
                    reason = "Reason not specified";
                }
                
                final String finalReason = String.format("Softbanned by %#s: %s", author, reason);
                
                User user = event.getMessage().getMentionedUsers().get(0);
                Member member = guild.getMember(user);
                if(!guild.getMember(author).canInteract(member)) {
                    channel.sendMessage(String.format(languageContext.get("commands.softban.hierarchy_conflict"), EmoteReference.ERROR)).queue();
                    return;
                }
                
                if(author.getId().equals(user.getId())) {
                    channel.sendMessage(String.format(languageContext.get("commands.softban.yourself_note"), EmoteReference.ERROR)).queue();
                    return;
                }
                
                //If one of them is in a higher hierarchy than the bot, cannot ban.
                if(!selfMember.canInteract(member)) {
                    channel.sendMessage(String.format(languageContext.get("commands.softban.self_hierarchy_conflict"), EmoteReference.ERROR)).queue();
                    return;
                }
                final DBGuild db = MantaroData.db().getGuild(event.getGuild());
                
                guild.ban(member, 7).reason(finalReason).queue(
                        success -> {
                            user.openPrivateChannel().queue(privateChannel ->
                                                                    privateChannel.sendMessage(String.format("%sYou were **softbanned** by %s#%s for reason %s on server **%s**.",
                                                                            EmoteReference.MEGA, author.getName(), author.getDiscriminator(), finalReason, event.getGuild().getName())).queue());
                            db.getData().setCases(db.getData().getCases() + 1);
                            db.saveAsync();
                            
                            channel.sendMessage(String.format(languageContext.get("commands.softban.success"), EmoteReference.ZAP, languageContext.get("general.mod_quotes"), user.getName())).queue();
                            guild.unban(member.getUser()).reason(finalReason).queue(aVoid -> {
                            }, error -> {
                                if(error instanceof PermissionException) {
                                    channel.sendMessage(String.format(languageContext.get("commands.softban.error"), EmoteReference.ERROR,
                                            user.getName(), ((PermissionException) error).getPermission())).queue();
                                } else {
                                    channel.sendMessage(String.format(languageContext.get("commands.softban.unknown_error"), EmoteReference.ERROR,
                                            user.getName(), error.getClass().getSimpleName(), error.getMessage())).queue();
                                    log.warn("Unexpected error while unbanning someone.", error);
                                }
                            });
                            
                            ModLog.log(event.getMember(), user, finalReason, event.getChannel().getName(), ModLog.ModAction.KICK, db.getData().getCases());
                            TextChannelGround.of(event).dropItemWithChance(2, 2);
                        },
                        error -> {
                            if(error instanceof PermissionException) {
                                channel.sendMessage(String.format(languageContext.get("commands.softban.error"), EmoteReference.ERROR,
                                        user.getName(), ((PermissionException) error).getPermission())).queue();
                            } else {
                                channel.sendMessage(String.format(languageContext.get("commands.softban.unknown_error"), EmoteReference.ERROR,
                                        user.getName(), error.getClass().getSimpleName(), error.getMessage())).queue();
                                log.warn("Unexpected error while soft banning someone.", error);
                            }
                        });
                
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Softban the mentioned user and clears their messages from the past week. (You need the Ban Members permission, so does the bot)♥n" +
                                                       "A soft ban is a ban & instant unban, usually useful to kick and clear messages.")
                               .setUsage("`~>softban <@user> [reason]`")
                               .addParameter("@user", "The user to softban.")
                               .addParameter("reason", "The reason of the softban. This is optional.")
                               .build();
            }
        });
    }
    
    @Subscribe
    public void ban(CommandRegistry cr) {
        cr.register("ban", new SimpleCommand(Category.MODERATION) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                Guild guild = event.getGuild();
                User author = event.getAuthor();
                TextChannel channel = event.getChannel();
                Message receivedMessage = event.getMessage();
                String reason = content;
                
                if(event.getMessage().getMentionedUsers().isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.ban.no_users"), EmoteReference.ERROR).queue();
                    return;
                }
                
                String affected = args[0];
                
                if(!guild.getMember(author).hasPermission(Permission.BAN_MEMBERS)) {
                    channel.sendMessage(String.format(languageContext.get("commands.ban.no_permission"), EmoteReference.ERROR)).queue();
                    return;
                }
                
                Member selfMember = guild.getSelfMember();
                
                if(!selfMember.hasPermission(Permission.BAN_MEMBERS)) {
                    channel.sendMessage(String.format(languageContext.get("commands.ban.no_permission_self"), EmoteReference.ERROR)).queue();
                    return;
                }
                
                if(args.length > 1) {
                    reason = StringUtils.splitArgs(content, 2)[1];
                }
                
                if(reason.isEmpty()) {
                    reason = "Reason not specified";
                }
                
                final String finalReason = String.format("Banned by %#s: %s", author, reason);
                List<User> mentionedUsers = event.getMessage().getMentionedUsers();
                
                for(User user : mentionedUsers) {
                    Member member = guild.getMember(user);
                    
                    if(!event.getGuild().getMember(author).canInteract(member)) {
                        event.getChannel().sendMessage(String.format(languageContext.get("commands.ban.hierarchy_conflict"), EmoteReference.ERROR, EmoteReference.SMILE)).queue();
                        return;
                    }
                    
                    if(author.getId().equals(user.getId())) {
                        channel.sendMessage(String.format(languageContext.get("commands.ban.yourself_note"), EmoteReference.ERROR)).queue();
                        return;
                    }
                    
                    if(!guild.getSelfMember().canInteract(member)) {
                        new MessageBuilder().setContent(String.format(languageContext.get("commands.ban.self_hierarchy_conflict"), EmoteReference.ERROR, user.getName()))
                                .stripMentions(event.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE)
                                .sendTo(event.getChannel())
                                .queue();
                        
                        return;
                    }
                    
                    final DBGuild db = MantaroData.db().getGuild(event.getGuild());
                    
                    guild.ban(member, 7).reason(finalReason).queue(
                            success -> user.openPrivateChannel().queue(privateChannel -> {
                                if(!user.isBot()) {
                                    privateChannel.sendMessage(String.format("%sYou were **banned** by %s#%s on server **%s**. Reason: %s.",
                                            EmoteReference.MEGA, author.getName(), author.getDiscriminator(), event.getGuild().getName(), finalReason)).queue();
                                }
                                
                                db.getData().setCases(db.getData().getCases() + 1);
                                db.saveAsync();
                                
                                if(mentionedUsers.size() == 1)
                                    channel.sendMessage(String.format(languageContext.get("commands.ban.success"), EmoteReference.ZAP, languageContext.get("general.mod_quotes"), user.getName())).queue();
                                
                                ModLog.log(event.getMember(), user, finalReason, event.getChannel().getName(), ModLog.ModAction.BAN, db.getData().getCases());
                                TextChannelGround.of(event).dropItemWithChance(1, 2);
                            }),
                            error ->
                            {
                                if(error instanceof PermissionException) {
                                    channel.sendMessage(String.format(languageContext.get("commands.ban.error"), EmoteReference.ERROR, user.getName(), ((PermissionException) error).getPermission())).queue();
                                } else {
                                    channel.sendMessage(String.format(languageContext.get("commands.ban.unknown_error"), EmoteReference.ERROR, user.getName())).queue();
                                    log.warn("Encountered an unexpected error while trying to ban someone.", error);
                                }
                            });
                }
                
                if(mentionedUsers.size() > 1) {
                    channel.sendMessage(String.format(languageContext.get("commands.ban.success_multiple"), EmoteReference.ZAP, languageContext.get("general.mod_quotes"), mentionedUsers.size())).queue();
                }
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Bans the mentioned user.")
                               .setUsage("`~>ban <@user> [reason]`")
                               .addParameter("@user", "The user to ban.")
                               .addParameter("reason", "The reason of the ban. This is optional.")
                               .build();
            }
        });
    }
    
    @Subscribe
    public void kick(CommandRegistry cr) {
        cr.register("kick", new SimpleCommand(Category.MODERATION) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                Guild guild = event.getGuild();
                User author = event.getAuthor();
                TextChannel channel = event.getChannel();
                Message receivedMessage = event.getMessage();
                String reason = content;
                
                if(args.length == 0) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.kick.no_users"), EmoteReference.ERROR).queue();
                    return;
                }
                
                String affected = args[0];
                
                if(!guild.getMember(author).hasPermission(Permission.KICK_MEMBERS)) {
                    channel.sendMessage(String.format(languageContext.get("commands.kick.no_permission"), EmoteReference.ERROR2)).queue();
                    return;
                }
                
                Member selfMember = guild.getSelfMember();
                
                if(!selfMember.hasPermission(Permission.KICK_MEMBERS)) {
                    channel.sendMessage(String.format(languageContext.get("commands.kick.no_permission_self"), EmoteReference.ERROR2)).queue();
                    return;
                }
                
                if(args.length > 1) {
                    reason = StringUtils.splitArgs(content, 2)[1];
                }
                
                if(reason.isEmpty()) {
                    reason = "Reason not specified";
                }
                
                final String finalReason = String.format("Kicked by %#s: %s", event.getAuthor(), reason);
                Member member = Utils.findMember(event, event.getMember(), affected);
                if(member == null)
                    return;
                
                User user = member.getUser();
                
                if(!event.getGuild().getMember(event.getAuthor()).canInteract(member)) {
                    channel.sendMessage(String.format(languageContext.get("commands.kick.hierarchy_conflict"), EmoteReference.ERROR)).queue();
                    return;
                }
                
                if(event.getAuthor().getId().equals(user.getId())) {
                    channel.sendMessage(String.format(languageContext.get("commands.kick.yourself_note"), EmoteReference.ERROR)).queue();
                    return;
                }
                
                //If one of them is in a higher hierarchy than the bot, cannot kick.
                if(!selfMember.canInteract(member)) {
                    channel.sendMessage(String.format(languageContext.get("commands.kick.self_hierarchy_conflict"),
                            EmoteReference.ERROR2, user.getName())).queue();
                    return;
                }
                final DBGuild db = MantaroData.db().getGuild(event.getGuild());
                
                guild.kick(member).reason(finalReason).queue(
                        success -> {
                            if(!user.isBot()) {
                                user.openPrivateChannel().queue(privateChannel ->
                                                                        privateChannel.sendMessage(String.format("%sYou were **kicked** by %s#%s with reason: %s on server **%s**.",
                                                                                EmoteReference.MEGA, event.getAuthor().getName(), event.getAuthor().getDiscriminator(), finalReason, event.getGuild().getName())).queue());
                            }
                            db.getData().setCases(db.getData().getCases() + 1);
                            db.saveAsync();
                            channel.sendMessage(String.format(languageContext.get("commands.kick.success"), EmoteReference.ZAP, languageContext.get("general.mod_quotes"), user.getName())).queue();
                            ModLog.log(event.getMember(), user, finalReason, event.getChannel().getName(), ModLog.ModAction.KICK, db.getData().getCases());
                            TextChannelGround.of(event).dropItemWithChance(2, 2);
                        },
                        error -> {
                            if(error instanceof PermissionException) {
                                channel.sendMessage(String.format(languageContext.get("commands.kick.error"), EmoteReference.ERROR,
                                        user.getName(), ((PermissionException) error).getPermission().getName())
                                ).queue();
                            } else {
                                channel.sendMessage(String.format(languageContext.get("commands.kick.unknown_error"),
                                        EmoteReference.ERROR, user.getName())
                                ).queue();
                                
                                log.warn("Unexpected error while kicking someone.", error);
                            }
                        }
                );
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Kicks the mentioned user.")
                               .setUsage("`~>kick <@user> [reason]`")
                               .addParameter("@user", "The kick to ban.")
                               .addParameter("reason", "The reason of the kick. This is optional.")
                               .build();
            }
        });
    }
}
