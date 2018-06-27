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
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.kodehawa.mantarobot.commands.moderation.ModLog;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Module
@SuppressWarnings("unused")
public class MessageCmds {
    @Subscribe
    public void prune(CommandRegistry cr) {
        cr.register("prune", new SimpleCommand(Category.MODERATION, CommandPermission.ADMIN) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                if(content.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.prune.no_messages_specified"), EmoteReference.ERROR).queue();
                    return;
                }

                if(!event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.prune.no_permissions"), EmoteReference.ERROR).queue();
                    return;
                }

                if(content.startsWith("bot")) {
                    channel.getHistory().retrievePast(100).queue(
                            messageHistory -> {
                                String prefix = MantaroData.db().getGuild(event.getGuild()).getData().getGuildCustomPrefix();
                                getMessageHistory(event, messageHistory, languageContext, "commands.prune.bots_no_messages",
                                        message -> message.getAuthor().isBot() || message.getContentRaw().startsWith(prefix == null ? "~>" : prefix));
                            },
                            error -> {
                                channel.sendMessage(String.format(languageContext.get("commands.prune.error_retrieving"),
                                        EmoteReference.ERROR, error.getClass().getSimpleName(), error.getMessage())).queue();
                                error.printStackTrace();
                            }
                    );
                    return;
                }

                if(content.startsWith("nopins")) {
                    int i = 100;
                    if(args.length > 1) {
                        try {
                            i = Integer.parseInt(args[1]);
                            if(i < 3) i = 3;
                        } catch(Exception e) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.prune.not_valid"), EmoteReference.ERROR).queue();
                            return;
                        }
                    }

                    channel.getHistory().retrievePast(Math.min(i, 100)).queue(
                            messageHistory -> getMessageHistory(event, messageHistory, languageContext, "commands.prune.no_pins_no_messages", message -> !message.isPinned()),
                            error -> {
                                channel.sendMessage(String.format(languageContext.get("commands.prune.error_retrieving"),
                                        EmoteReference.ERROR, error.getClass().getSimpleName(), error.getMessage())).queue();
                                error.printStackTrace();
                            }
                    );

                    return;
                }

                if(!event.getMessage().getMentionedUsers().isEmpty()) {
                    List<Long> users = new ArrayList<>();
                    for(User user : event.getMessage().getMentionedUsers()) {
                        users.add(user.getIdLong());
                    }

                    int i = 5;

                    if(args.length > 1) {
                        try {
                            i = Integer.parseInt(args[1]);
                            if(i < 3) i = 3;
                        } catch(Exception e) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.prune.not_valid"), EmoteReference.ERROR).queue();
                        }
                    }

                    channel.getHistory().retrievePast(Math.min(i, 100)).queue(
                            messageHistory -> getMessageHistory(event, messageHistory, languageContext, "commands.prune.mention_no_messages", message -> users.contains(message.getAuthor().getIdLong())),
                            error -> {
                                channel.sendMessage(String.format(languageContext.get("commands.prune.error_retrieving"),
                                        EmoteReference.ERROR, error.getClass().getSimpleName(), error.getMessage())).queue();
                                error.printStackTrace();
                            });

                    return;
                }

                int i;
                try {
                    i = Integer.parseInt(content);
                } catch(Exception e) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.prune.invalid_number"), EmoteReference.ERROR).queue();
                    return;
                }

                channel.getHistory().retrievePast(Math.min(i, 100)).queue(
                        messageHistory -> prune(event, languageContext, messageHistory),
                        error -> {
                            channel.sendMessage(String.format(languageContext.get("commands.prune.error_retrieving"),
                                    EmoteReference.ERROR, error.getClass().getSimpleName(), error.getMessage())).queue();
                            error.printStackTrace();
                        }
                );
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Prune command")
                        .setDescription("**Prunes a specific amount of messages.**")
                        .addField("Usage", "`~>prune <x>/<@user>` - **Prunes messages**", false)
                        .addField("Parameters", "x = **number of messages to delete**", false)
                        .addField("Important", "You need to provide *at least* 3 messages. I'd say better 10 or more.\n" +
                                "You can use `~>prune bot` to remove all bot messages and bot calls.\n" +
                                "You can use `~>prune nopins` to avoid pruning pinned messages.", false)
                        .build();
            }

        });
    }

    private void getMessageHistory(GuildMessageReceivedEvent event, List<Message> messageHistory, I18nContext languageContext, String i18n, Predicate<Message> predicate) {
        messageHistory = messageHistory.stream().filter(predicate).collect(Collectors.toList());

        if(messageHistory.isEmpty()) {
            event.getChannel().sendMessageFormat(languageContext.get(i18n), EmoteReference.ERROR).queue();
            return;
        }

        if(messageHistory.size() < 3) {
            event.getChannel().sendMessageFormat(languageContext.get("commands.prune.too_few_messages"), EmoteReference.ERROR).queue();
            return;
        }

        prune(event, languageContext, messageHistory);
    }

    private void prune(GuildMessageReceivedEvent event, I18nContext languageContext, List<Message> messageHistory) {
        messageHistory = messageHistory.stream().filter(message -> !message.getCreationTime()
                .isBefore(OffsetDateTime.now().minusWeeks(2)))
                .collect(Collectors.toList());

        TextChannel channel = event.getChannel();

        if(messageHistory.isEmpty()) {
            channel.sendMessageFormat(languageContext.get("commands.prune.messages_too_old"), EmoteReference.ERROR).queue();
            return;
        }

        final int size = messageHistory.size();

        if(messageHistory.size() < 3) {
            channel.sendMessageFormat(languageContext.get("commands.prune.too_few_messages"), EmoteReference.ERROR).queue();
            return;
        }

        channel.deleteMessages(messageHistory).queue(
                success -> {
                    channel.sendMessageFormat(languageContext.get("commands.prune.success"), EmoteReference.PENCIL, size)
                            .queue(message -> message.delete().queueAfter(15, TimeUnit.SECONDS));
                    DBGuild db = MantaroData.db().getGuild(event.getGuild());
                    db.getData().setCases(db.getData().getCases() + 1);
                    db.saveAsync();
                    ModLog.log(event.getMember(), null, "Pruned Messages", ModLog.ModAction.PRUNE, db.getData().getCases());
                },
                error -> {
                    if(error instanceof PermissionException) {
                        PermissionException pe = (PermissionException) error;
                        channel.sendMessage(String.format(languageContext.get("commands.prune.lack_perms"),
                                EmoteReference.ERROR, pe.getPermission())).queue();
                    } else {
                        channel.sendMessage(String.format(languageContext.get("commands.prune.error_deleting"),
                                EmoteReference.ERROR, error.getClass().getSimpleName(), error.getMessage())).queue();
                        error.printStackTrace();
                    }
                }
        );
    }
}
