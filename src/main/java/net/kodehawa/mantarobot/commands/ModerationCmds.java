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
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.moderation.ModLog;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Random;

@Slf4j(topic = "Moderation")
@Module
@SuppressWarnings("unused")
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
                    channel.sendMessage(String.format(languageContext.get("commands.softban.no_permission"), EmoteReference.ERROR2)).queue();
                    return;
                }

                Member selfMember = guild.getSelfMember();

                if(!selfMember.hasPermission(Permission.BAN_MEMBERS)) {
                    channel.sendMessage(String.format(languageContext.get("commands.softban.no_permission_self"), EmoteReference.ERROR2)).queue();
                    return;
                }

                if(receivedMessage.getMentionedUsers().isEmpty()) {
                    channel.sendMessage(String.format(languageContext.get("commands.softban.no_mention"), EmoteReference.ERROR)).queue();
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
                        channel.sendMessage(String.format(languageContext.get("commands.softban.hierarchy_conflict"), EmoteReference.ERROR)).queue();
                        return;
                    }

                    if(event.getAuthor().getId().equals(user.getId())) {
                        channel.sendMessage(String.format(languageContext.get("commands.softban.yourself_note"), EmoteReference.ERROR)).queue();
                        return;
                    }

                    Member member = guild.getMember(user);
                    if(member == null) return;

                    //If one of them is in a higher hierarchy than the bot, cannot ban.
                    if(!selfMember.canInteract(member)) {
                        channel.sendMessage(String.format(languageContext.get("commands.softban.self_hierarchy_conflict"), EmoteReference.ERROR)).queue();
                        return;
                    }
                    final DBGuild db = MantaroData.db().getGuild(event.getGuild());

                    guild.getController().ban(member, 7).reason(finalReason).queue(
                            success -> {
                                user.openPrivateChannel().queue(privateChannel ->
                                        privateChannel.sendMessage(String.format("%sYou were **softbanned** by %s#%s for reason %s on server **%s**.",
                                                EmoteReference.MEGA, event.getAuthor().getName(), event.getAuthor().getDiscriminator(), finalReason, event.getGuild().getName())).queue());
                                db.getData().setCases(db.getData().getCases() + 1);
                                db.saveAsync();
                                channel.sendMessage(String.format(languageContext.get("commands.softban.success"), EmoteReference.ZAP, modActionQuotes[r.nextInt(modActionQuotes.length)], member.getEffectiveName())).queue();
                                guild.getController().unban(member.getUser()).reason(finalReason).queue(aVoid -> {
                                }, error -> {
                                    if(error instanceof PermissionException) {
                                        channel.sendMessage(String.format(languageContext.get("commands.softban.error"), EmoteReference.ERROR,
                                                member.getEffectiveName(), ((PermissionException) error).getPermission())).queue();
                                    } else {
                                        channel.sendMessage(String.format(languageContext.get("commands.softban.unknown_error"), EmoteReference.ERROR,
                                                member.getEffectiveName(), error.getClass().getSimpleName(), error.getMessage())).queue();
                                        log.warn("Unexpected error while unbanning someone.", error);
                                    }
                                });

                                ModLog.log(event.getMember(), user, finalReason, ModLog.ModAction.KICK, db.getData().getCases());
                                TextChannelGround.of(event).dropItemWithChance(2, 2);
                            },
                            error -> {
                                if(error instanceof PermissionException) {
                                    channel.sendMessage(String.format(languageContext.get("commands.softban.error"), EmoteReference.ERROR,
                                            member.getEffectiveName(), ((PermissionException) error).getPermission())).queue();
                                } else {
                                    channel.sendMessage(String.format(languageContext.get("commands.softban.unknown_error"), EmoteReference.ERROR,
                                            member.getEffectiveName(), error.getClass().getSimpleName(), error.getMessage())).queue();
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
                    channel.sendMessage(String.format(languageContext.get("commands.ban.no_permission"), EmoteReference.ERROR)).queue();
                    return;
                }

                Member selfMember = guild.getSelfMember();

                if(!selfMember.hasPermission(Permission.BAN_MEMBERS)) {
                    channel.sendMessage(String.format(languageContext.get("commands.ban.no_permission_self"), EmoteReference.ERROR)).queue();
                    return;
                }

                if(receivedMessage.getMentionedUsers().isEmpty()) {
                    channel.sendMessage(String.format(languageContext.get("commands.ban.no_mention"), EmoteReference.ERROR)).queue();
                    return;
                }

                for(User user : event.getMessage().getMentionedUsers()) {
                    reason = reason.replaceAll("(\\s+)?<@!?" + user.getId() + ">(\\s+)?", "");
                }

                if(reason.isEmpty()) {
                    reason = "Reason not specified";
                }

                final String finalReason = String.format("Banned by %#s: %s", event.getAuthor(), reason);

                receivedMessage.getMentionedUsers().forEach((User user) -> {
                    if(!event.getGuild().getMember(event.getAuthor()).canInteract(event.getGuild().getMember(user))) {
                        event.getChannel().sendMessage(String.format(languageContext.get("commands.ban.hierarchy_conflict"), EmoteReference.ERROR, EmoteReference.SMILE)).queue();
                        return;
                    }

                    if(event.getAuthor().getId().equals(user.getId())) {
                        channel.sendMessage(String.format(languageContext.get("commands.ban.yourself_note"), EmoteReference.ERROR)).queue();
                        return;
                    }

                    Member member = guild.getMember(user);
                    if(member == null) return;
                    if(!guild.getSelfMember().canInteract(member)) {
                        channel.sendMessage(String.format(languageContext.get("commands.ban.self_hierarchy_conflict"), EmoteReference.ERROR, member.getEffectiveName())).queue();
                        return;
                    }

                    final DBGuild db = MantaroData.db().getGuild(event.getGuild());

                    guild.getController().ban(member, 7).reason(finalReason).queue(
                            success -> user.openPrivateChannel().queue(privateChannel -> {
                                if(!user.isBot()) {
                                    privateChannel.sendMessage(String.format("%sYou were **banned** by %s#%s on server **%s**. Reason: %s.",
                                            EmoteReference.MEGA, event.getAuthor().getName(), event.getAuthor().getDiscriminator(), event.getGuild().getName(), finalReason)).queue();
                                }
                                db.getData().setCases(db.getData().getCases() + 1);
                                db.saveAsync();
                                channel.sendMessage(String.format(languageContext.get("commands.ban.success"), EmoteReference.ZAP, modActionQuotes[r.nextInt(modActionQuotes.length)], user.getName())).queue();
                                ModLog.log(event.getMember(), user, finalReason, ModLog.ModAction.BAN, db.getData().getCases());
                                TextChannelGround.of(event).dropItemWithChance(1, 2);
                            }),
                            error ->
                            {
                                if(error instanceof PermissionException) {
                                    channel.sendMessage(String.format(languageContext.get("commands.ban.error"),
                                            EmoteReference.ERROR, user.getName(), ((PermissionException) error).getPermission())).queue();
                                } else {
                                    channel.sendMessage(String.format(languageContext.get("commands.ban.unknown_error"),
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
                    channel.sendMessage(String.format(languageContext.get("commands.kick.no_permission"), EmoteReference.ERROR2)).queue();
                    return;
                }

                if(receivedMessage.getMentionedUsers().isEmpty()) {
                    channel.sendMessage(String.format(languageContext.get("commands.kick.no_mention"), EmoteReference.ERROR)).queue();
                    return;
                }

                Member selfMember = guild.getSelfMember();

                if(!selfMember.hasPermission(net.dv8tion.jda.core.Permission.KICK_MEMBERS)) {
                    channel.sendMessage(String.format(languageContext.get("commands.kick.no_permission_self"), EmoteReference.ERROR2)).queue();
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
                        channel.sendMessage(String.format(languageContext.get("commands.kick.hierarchy_conflict"), EmoteReference.ERROR)).queue();
                        return;
                    }

                    if(event.getAuthor().getId().equals(user.getId())) {
                        channel.sendMessage(String.format(languageContext.get("commands.kick.yourself_note"), EmoteReference.ERROR)).queue();
                        return;
                    }

                    Member member = guild.getMember(user);
                    if(member == null) return;

                    //If one of them is in a higher hierarchy than the bot, cannot kick.
                    if(!selfMember.canInteract(member)) {
                        channel.sendMessage(String.format(languageContext.get("commands.kick.self_hierarchy_conflict"),
                                EmoteReference.ERROR2, member.getEffectiveName())).queue();
                        return;
                    }
                    final DBGuild db = MantaroData.db().getGuild(event.getGuild());

                    guild.getController().kick(member).reason(finalReason).queue(
                            success -> {
                                if(!user.isBot()) {
                                    user.openPrivateChannel().queue(privateChannel ->
                                            privateChannel.sendMessage(String.format("%sYou were **kicked** by %s#%s with reason: %s on server **%s**.",
                                                    EmoteReference.MEGA, event.getAuthor().getName(), event.getAuthor().getDiscriminator(), finalReason, event.getGuild().getName())).queue());
                                }
                                db.getData().setCases(db.getData().getCases() + 1);
                                db.saveAsync();
                                channel.sendMessage(String.format(languageContext.get("commands.kick.success"), EmoteReference.ZAP, modActionQuotes[r.nextInt(modActionQuotes.length)], user.getName())).queue();
                                ModLog.log(event.getMember(), user, finalReason, ModLog.ModAction.KICK, db.getData().getCases());
                                TextChannelGround.of(event).dropItemWithChance(2, 2);
                            },
                            error -> {
                                if(error instanceof PermissionException) {
                                    channel.sendMessage(String.format(languageContext.get("commands.kick.error"), EmoteReference.ERROR,
                                            member.getEffectiveName(), ((PermissionException) error).getPermission().getName())
                                    ).queue();
                                } else {
                                    channel.sendMessage(String.format(languageContext.get("commands.kick.unknown_error"),
                                            EmoteReference.ERROR, member.getEffectiveName())
                                    ).queue();

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
}
