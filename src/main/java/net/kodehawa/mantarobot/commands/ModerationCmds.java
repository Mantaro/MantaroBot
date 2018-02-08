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

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.moderation.ModLog;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Random;

@Slf4j(topic = "Moderation")
@Module
public class ModerationCmds {

    private final String[] modActionQuotes = {
            "Uh-oh, seems like someone just got hit hard!", "Just wholesome admin work happening over here...", "The boot has been thrown!",
            "You'll be missed... not really", "I hope I did the right thing...", "Woah there, mods have spoken!", "U-Uh... w-well, someone just went through the door."
    };

    private final Random r = new Random();

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

                if(!guild.getMember(author).hasPermission(Permission.BAN_MEMBERS)) {
                    channel.sendMessage(String.format("%sCannot soft ban: You don't have the Ban Members permission.", EmoteReference.ERROR2)).queue();
                    return;
                }

                Member selfMember = guild.getSelfMember();

                if(!selfMember.hasPermission(Permission.BAN_MEMBERS)) {
                    channel.sendMessage(String.format("%sSorry! I don't have permission to ban members in this server!", EmoteReference.ERROR2)).queue();
                    return;
                }

                if(receivedMessage.getMentionedUsers().isEmpty()) {
                    channel.sendMessage(String.format("%sYou must mention 1 or more users to be soft-banned!", EmoteReference.ERROR)).queue();
                    return;
                }

                for(User user : event.getMessage().getMentionedUsers()) {
                    reason = reason.replaceAll("(\\s+)?<@!?" + user.getId() + ">(\\s+)?", "");
                }

                if(reason.isEmpty()) {
                    reason = "Reason not specified";
                }

                final String finalReason = String.format("Softbanned by %#s: %s", event.getAuthor(), reason);

                receivedMessage.getMentionedUsers().forEach(user -> {
                    if(!event.getGuild().getMember(event.getAuthor()).canInteract(event.getGuild().getMember(user))) {
                        event.getChannel().sendMessage(String.format("%sYou cannot softban an user in a higher hierarchy than you", EmoteReference.ERROR))
                                .queue();
                        return;
                    }

                    if(event.getAuthor().getId().equals(user.getId())) {
                        event.getChannel().sendMessage(String.format("%sWhy are you trying to soft-ban yourself?", EmoteReference.ERROR)).queue();
                        return;
                    }

                    Member member = guild.getMember(user);
                    if(member == null) return;

                    //If one of them is in a higher hierarchy than the bot, cannot ban.
                    if(!selfMember.canInteract(member)) {
                        channel.sendMessage(String.format("%sCannot softban member: %s, they are higher or the same hierarchy than I am!", EmoteReference.ERROR2, member.getEffectiveName())).queue();
                        return;
                    }
                    final DBGuild db = MantaroData.db().getGuild(event.getGuild());

                    //Proceed to ban them. Again, using queue so I don't get rate limited.
                    guild.getController().ban(member, 7).reason(finalReason).queue(
                            success -> {
                                user.openPrivateChannel().complete().sendMessage(String.format("%sYou were **softbanned** by %s#%s for reason %s on server **%s**.",
                                        EmoteReference.MEGA, event.getAuthor().getName(), event.getAuthor().getDiscriminator(), finalReason, event.getGuild().getName())).queue();
                                db.getData().setCases(db.getData().getCases() + 1);
                                db.saveAsync();
                                channel.sendMessage(String.format("%s%s. **(%s got softbanned)**", EmoteReference.ZAP, modActionQuotes[r.nextInt(modActionQuotes.length)], member.getEffectiveName())).queue();
                                guild.getController().unban(member.getUser()).reason(finalReason).queue(aVoid -> {
                                }, error -> {
                                    if(error instanceof PermissionException) {
                                        channel.sendMessage(String.format(EmoteReference.ERROR + "Error unbanning [%s]: (No permission provided: %s)",
                                                member.getEffectiveName(), ((PermissionException) error).getPermission())).queue();
                                    } else {
                                        channel.sendMessage(String.format(EmoteReference.ERROR + "Unknown error while unbanning [%s]: <%s>: %s",
                                                member.getEffectiveName(), error.getClass().getSimpleName(), error.getMessage())).queue();
                                        log.warn("Unexpected error while unbanning someone.", error);
                                    }
                                });


                                ModLog.log(event.getMember(), user, finalReason, ModLog.ModAction.KICK, db.getData().getCases());
                                TextChannelGround.of(event).dropItemWithChance(2, 2);
                            },
                            error -> {
                                if(error instanceof PermissionException) {
                                    channel.sendMessage(String.format(EmoteReference.ERROR + "Error softbanning %s: (No permission provided: %s)",
                                            member.getEffectiveName(), ((PermissionException) error).getPermission())).queue();
                                } else {
                                    channel.sendMessage(String.format(EmoteReference.ERROR + "Unknown error while softbanning %s",
                                            member.getEffectiveName())).queue();
                                    log.warn("Unexpected error while soft banning someone.", error);
                                }
                            });
                });
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Softban")
                        .setDescription("**Softban the mentioned user and clears their messages from the past week. (You need Ban " +
                                "Members)**")
                        .addField("Summarizing", "A soft ban is a ban & instant unban, normally used to clear " +
                                "the user's messages but **without banning the person permanently**.", false)
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

                if(!guild.getMember(author).hasPermission(net.dv8tion.jda.core.Permission.BAN_MEMBERS)) {
                    channel.sendMessage(EmoteReference.ERROR + "You can't ban: You need the `Ban Users` permission.").queue();
                    return;
                }

                if(receivedMessage.getMentionedUsers().isEmpty()) {
                    channel.sendMessage(EmoteReference.ERROR + "You need to mention at least one user!").queue();
                    return;
                }

                for(User user : event.getMessage().getMentionedUsers()) {
                    reason = reason.replaceAll("(\\s+)?<@!?" + user.getId() + ">(\\s+)?", "");
                }

                if(reason.isEmpty()) {
                    reason = "Reason not specified";
                }

                final String finalReason = String.format("Banned by %#s: %s", event.getAuthor(), reason);

                receivedMessage.getMentionedUsers().forEach(user -> {
                    if(!event.getGuild().getMember(event.getAuthor()).canInteract(event.getGuild().getMember(user))) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot ban an user who's higher than you in the " +
                                "server hierarchy! Nice try " + EmoteReference.SMILE).queue();
                        return;
                    }

                    if(event.getAuthor().getId().equals(user.getId())) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Why are you trying to ban yourself, silly?").queue();
                        return;
                    }

                    Member member = guild.getMember(user);
                    if(member == null) return;
                    if(!guild.getSelfMember().canInteract(member)) {
                        channel.sendMessage(String.format("%sI can't ban %s; they're higher in the server hierarchy than me!", EmoteReference.ERROR, member.getEffectiveName())).queue();
                        return;
                    }

                    if(!guild.getSelfMember().hasPermission(net.dv8tion.jda.core.Permission.BAN_MEMBERS)) {
                        channel.sendMessage(EmoteReference.ERROR + "Sorry! I don't have permission to ban members in this server!").queue();
                        return;
                    }
                    final DBGuild db = MantaroData.db().getGuild(event.getGuild());

                    guild.getController().ban(member, 7).reason(finalReason).queue(
                            success -> {
                                user.openPrivateChannel().complete().sendMessage(String.format("%sYou were **banned** by %s#%s on server **%s**. Reason: %s.",
                                        EmoteReference.MEGA, event.getAuthor().getName(), event.getAuthor().getDiscriminator(), event.getGuild().getName(), finalReason)).queue();
                                db.getData().setCases(db.getData().getCases() + 1);
                                db.saveAsync();
                                channel.sendMessage(String.format("%s%s (%s got banned)", EmoteReference.ZAP, modActionQuotes[r.nextInt(modActionQuotes.length)], user.getName())).queue();
                                ModLog.log(event.getMember(), user, finalReason, ModLog.ModAction.BAN, db.getData().getCases());
                                TextChannelGround.of(event).dropItemWithChance(1, 2);
                            },
                            error ->
                            {
                                if(error instanceof PermissionException) {
                                    channel.sendMessage(String.format("%sError banning %s: (I need the permission %s)",
                                            EmoteReference.ERROR, user.getName(), ((PermissionException) error).getPermission())).queue();
                                } else {
                                    channel.sendMessage(String.format("%sI encountered an unknown error while banning %s",
                                            EmoteReference.ERROR, member.getEffectiveName())).queue();
                                    log.warn("Encountered an unexpected error while trying to ban someone.", error);
                                }
                            });
                });
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Ban")
                        .setDescription("**Bans the mentioned users. (You need Ban Members)**")
                        .addField("Usage", "`~>ban <@user> <reason>` - **Bans the specified user**", false)
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

                if(!guild.getMember(author).hasPermission(net.dv8tion.jda.core.Permission.KICK_MEMBERS)) {
                    channel.sendMessage(EmoteReference.ERROR2 + "Cannot kick: You have no Kick Members permission.").queue();
                    return;
                }

                if(receivedMessage.getMentionedUsers().isEmpty()) {
                    channel.sendMessage(EmoteReference.ERROR + "You must mention 1 or more users to be kicked!").queue();
                    return;
                }

                Member selfMember = guild.getSelfMember();

                if(!selfMember.hasPermission(net.dv8tion.jda.core.Permission.KICK_MEMBERS)) {
                    channel.sendMessage(EmoteReference.ERROR2 + "Sorry! I don't have permission to kick members in this server!").queue();
                    return;
                }

                for(User user : event.getMessage().getMentionedUsers()) {
                    reason = reason.replaceAll("(\\s+)?<@!?" + user.getId() + ">(\\s+)?", "");
                }

                if(reason.isEmpty()) {
                    reason = "Reason not specified";
                }

                final String finalReason = String.format("Kicked by %#s: %s", event.getAuthor(), reason);

                receivedMessage.getMentionedUsers().forEach(user -> {
                    if(!event.getGuild().getMember(event.getAuthor()).canInteract(event.getGuild().getMember(user))) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot kick an user in a higher hierarchy than you")
                                .queue();
                        return;
                    }

                    if(event.getAuthor().getId().equals(user.getId())) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Why are you trying to kick yourself?").queue();
                        return;
                    }

                    Member member = guild.getMember(user);
                    if(member == null) return;

                    //If one of them is in a higher hierarchy than the bot, cannot kick.
                    if(!selfMember.canInteract(member)) {
                        channel.sendMessage(String.format("%sCannot kick member: %s, they are higher or the same hierarchy than I am!",
                                EmoteReference.ERROR2, member.getEffectiveName())).queue();
                        return;
                    }
                    final DBGuild db = MantaroData.db().getGuild(event.getGuild());

                    //Proceed to kick them. Again, using queue so I don't get rate limited.
                    guild.getController().kick(member).reason(finalReason).queue(
                            success -> {
                                if(!user.isBot()) {
                                    user.openPrivateChannel().complete().sendMessage(String.format("%sYou were **kicked** by %s#%s with reason: %s on server **%s**.",
                                            EmoteReference.MEGA, event.getAuthor().getName(), event.getAuthor().getDiscriminator(), finalReason, event.getGuild().getName())).queue();
                                }
                                db.getData().setCases(db.getData().getCases() + 1);
                                db.saveAsync();
                                channel.sendMessage(String.format("%s%s (%s got kicked)", EmoteReference.ZAP, modActionQuotes[r.nextInt(modActionQuotes.length)], user.getName())).queue();
                                ModLog.log(event.getMember(), user, finalReason, ModLog.ModAction.KICK, db.getData().getCases());
                                TextChannelGround.of(event).dropItemWithChance(2, 2);
                            },
                            error -> {
                                if(error instanceof PermissionException) {
                                    channel.sendMessage(String.format(EmoteReference.ERROR + "Error kicking %s: (No permission provided: %s)",
                                            member.getEffectiveName(), ((PermissionException) error).getPermission().getName())).queue();
                                } else {
                                    channel.sendMessage(String.format(EmoteReference.ERROR + "Unknown error while attempting to kick %s", member.getEffectiveName())).queue();
                                    log.warn("Unexpected error while kicking someone.", error);
                                }
                            });
                });
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Kick")
                        .setDescription("**Kicks the mentioned users. (You need Kick Members)**")
                        .addField("Usage", "`~>kick <@user> <reason> - **Kicks the mentioned user   **", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void tempban(CommandRegistry cr) {
        cr.register("tempban", new SimpleCommand(Category.MODERATION) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                String reason = content;
                Guild guild = event.getGuild();
                User author = event.getAuthor();
                TextChannel channel = event.getChannel();
                Message receivedMessage = event.getMessage();

                if(!guild.getMember(author).hasPermission(net.dv8tion.jda.core.Permission.BAN_MEMBERS)) {
                    channel.sendMessage(EmoteReference.ERROR + "Cannot ban: You have no Ban Members permission.").queue();
                    return;
                }

                if(event.getMessage().getMentionedUsers().isEmpty()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention an user!").queue();
                    return;
                }

                for(User user : event.getMessage().getMentionedUsers()) {
                    reason = reason.replaceAll("(\\s+)?<@!?" + user.getId() + ">(\\s+)?", "");
                }
                int index = reason.indexOf("time:");
                if(index < 0) {
                    event.getChannel().sendMessage(EmoteReference.ERROR +
                            "You cannot temp ban an user without giving me the time!").queue();
                    return;
                }
                String time = reason.substring(index);
                reason = reason.replace(time, "").trim();
                time = time.replaceAll("time:(\\s+)?", "");
                if(reason.isEmpty()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot temp ban someone without a reason.!").queue();
                    return;
                }

                if(time.isEmpty()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot temp ban someone without giving me the time!")
                            .queue();
                    return;
                }

                final DBGuild db = MantaroData.db().getGuild(event.getGuild());
                long l = Utils.parseTime(time);
                String finalReason = String.format("Temporally banned by %#s: %s", event.getAuthor(), reason);
                String sTime = StringUtils.parseTime(l);
                receivedMessage.getMentionedUsers().forEach(user ->
                        guild.getController().ban(user, 7).queue(
                                success -> {
                                    user.openPrivateChannel().complete().sendMessage(String.format("%sYou were **temporarily banned** by %s#%s with reason: %s on server **%s**.",
                                            EmoteReference.MEGA, event.getAuthor().getName(), event.getAuthor().getDiscriminator(), finalReason, event.getGuild().getName())).queue();

                                    db.getData().setCases(db.getData().getCases() + 1);
                                    db.saveAsync();

                                    channel.sendMessage(String.format("%s%s (%s got temporarly banned)", EmoteReference.ZAP, modActionQuotes[r.nextInt(modActionQuotes.length)], user.getName())).queue();

                                    ModLog.log(event.getMember(), user, finalReason, ModLog.ModAction.TEMP_BAN, db.getData().getCases(), sTime);
                                    MantaroBot.getTempBanManager().addTempban(guild.getId() + ":" + user.getId(), l + System.currentTimeMillis());
                                    TextChannelGround.of(event).dropItemWithChance(1, 2);
                                },
                                error ->
                                {
                                    if(error instanceof PermissionException) {
                                        channel.sendMessage(String.format("%sError banning %s: (I need the permission %s)",
                                                EmoteReference.ERROR, user.getName(), ((PermissionException) error).getPermission())).queue();
                                    } else {
                                        channel.sendMessage(String.format("%sI encountered an unknown error while banning %s", EmoteReference.ERROR, user.getName())).queue();
                                        log.warn("Encountered an unexpected error while trying to ban someone.", error);
                                    }
                                })
                );
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Tempban Command")
                        .setDescription("**Temporarily bans an user**")
                        .addField("Usage", "`~>tempban <user> <reason> time:<time>`", false)
                        .addField("Example", "`~>tempban @Kodehawa example time:1d`", false)
                        .addField("Extended usage", "`time` - can be used with the following parameters: " +
                                "d (days), s (second), m (minutes), h (hour). **For example time:1d1h will give a day and an hour.**", false)
                        .build();
            }
        });
    }
}
