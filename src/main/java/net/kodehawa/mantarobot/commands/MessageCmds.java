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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.kodehawa.mantarobot.commands.moderation.ModLog;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Module
@SuppressWarnings("unused")
public class MessageCmds {
    @Subscribe
    public void prune(CommandRegistry cr) {
        var pruneCmd = (TreeCommand) cr.register("prune", new TreeCommand(Category.MODERATION, CommandPermission.ADMIN) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                        var args = StringUtils.advancedSplitArgs(content, 0);
                        TextChannel channel = event.getChannel();

                        if (content.isEmpty()) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.prune.no_messages_specified"), EmoteReference.ERROR).queue();
                            return;
                        }

                        int i = 5;
                        if (args.length > 1) {
                            try {
                                i = Integer.parseInt(event.getMessage().getMentionedUsers().isEmpty() ? content : args[1]);
                                if (i < 3) i = 3;
                            } catch (Exception e) {
                                event.getChannel().sendMessageFormat(languageContext.get("commands.prune.not_valid"), EmoteReference.ERROR).queue();
                                return;
                            }
                        }


                        if (!event.getMessage().getMentionedUsers().isEmpty()) {
                            List<Long> users = event.getMessage().getMentionedUsers().stream().map(User::getIdLong).collect(Collectors.toList());

                            channel.getHistory().retrievePast(Math.min(i, 100)).queue(
                                    messageHistory -> getMessageHistory(event, channel, messageHistory, languageContext, "commands.prune.mention_no_messages", message -> users.contains(message.getAuthor().getIdLong())),
                                    error -> {
                                        channel.sendMessage(String.format(languageContext.get("commands.prune.error_retrieving"),
                                                EmoteReference.ERROR, error.getClass().getSimpleName(), error.getMessage())).queue();
                                        error.printStackTrace();
                                    });

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
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Prunes X amount of messages from a channel.")
                        .setUsage("`~>prune <messages>`")
                        .addParameter("messages", "Number of messages from 4 to 100.")
                        .build();
            }
        });

        pruneCmd.addSubCommand("bot", new SubCommand() {
            @Override
            public String description() {
                return "Prune bot messages. It takes the number of messages as an argument.";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                var args = StringUtils.advancedSplitArgs(content, 0);
                TextChannel channel = event.getChannel();

                int i = 100;
                if (args.length > 1) {
                    try {
                        i = Integer.parseInt(args[1]);
                        if (i < 3) i = 3;
                    } catch (Exception e) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.prune.not_valid"), EmoteReference.ERROR).queue();
                        return;
                    }
                }

                channel.getHistory().retrievePast(Math.min(i, 100)).queue(
                        messageHistory -> {
                            String prefix = MantaroData.db().getGuild(event.getGuild()).getData().getGuildCustomPrefix();
                            getMessageHistory(event, channel, messageHistory, languageContext, "commands.prune.bots_no_messages",
                                    message -> message.getAuthor().isBot() || message.getContentRaw().startsWith(prefix == null ? "~>" : prefix));
                        },
                        error -> {
                            channel.sendMessage(String.format(languageContext.get("commands.prune.error_retrieving"),
                                    EmoteReference.ERROR, error.getClass().getSimpleName(), error.getMessage())).queue();
                            error.printStackTrace();
                        }
                );
            }
        });

        pruneCmd.addSubCommand("nopins", new SubCommand() {
            @Override
            public String description() {
                return "Prune messages that aren't pinned. It takes the number of messages as an argument.";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                var args = StringUtils.advancedSplitArgs(content, 0);
                TextChannel channel = event.getChannel();

                int i = 100;
                if (args.length > 1) {
                    try {
                        i = Integer.parseInt(args[1]);
                        if (i < 3) i = 3;
                    } catch (Exception e) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.prune.not_valid"), EmoteReference.ERROR).queue();
                        return;
                    }
                }

                channel.getHistory().retrievePast(Math.min(i, 100)).queue(
                        messageHistory -> getMessageHistory(event, channel, messageHistory, languageContext, "commands.prune.no_pins_no_messages", message -> !message.isPinned()),
                        error -> {
                            channel.sendMessage(String.format(languageContext.get("commands.prune.error_retrieving"),
                                    EmoteReference.ERROR, error.getClass().getSimpleName(), error.getMessage())).queue();
                            error.printStackTrace();
                        }
                );

            }
        });

        pruneCmd.setPredicate((event, languageContext, content) -> {
            if (!event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                event.getChannel().sendMessageFormat(languageContext.get("commands.prune.no_permissions"), EmoteReference.ERROR).queue();
                return false;
            }

            return true;
        });
    }

    private void getMessageHistory(GuildMessageReceivedEvent event, TextChannel channel, List<Message> messageHistory, I18nContext languageContext, String i18n, Predicate<Message> predicate) {
        messageHistory = messageHistory.stream().filter(predicate).collect(Collectors.toList());

        if (messageHistory.isEmpty()) {
            channel.sendMessageFormat(languageContext.get(i18n), EmoteReference.ERROR).queue();
            return;
        }

        if (messageHistory.size() < 3) {
            channel.sendMessageFormat(languageContext.get("commands.prune.too_few_messages"), EmoteReference.ERROR).queue();
            return;
        }

        prune(event, languageContext, messageHistory);
    }

    private void prune(GuildMessageReceivedEvent event, I18nContext languageContext, List<Message> messageHistory) {
        messageHistory = messageHistory.stream().filter(message -> !message.getTimeCreated()
                .isBefore(OffsetDateTime.now().minusWeeks(2)))
                .collect(Collectors.toList());

        TextChannel channel = event.getChannel();

        if (messageHistory.isEmpty()) {
            channel.sendMessageFormat(languageContext.get("commands.prune.messages_too_old"), EmoteReference.ERROR).queue();
            return;
        }

        final int size = messageHistory.size();

        if (messageHistory.size() < 3) {
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
                    ModLog.log(event.getMember(), null, "Pruned Messages", event.getChannel().getName(), ModLog.ModAction.PRUNE, db.getData().getCases(), size);
                },
                error -> {
                    if (error instanceof PermissionException) {
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
