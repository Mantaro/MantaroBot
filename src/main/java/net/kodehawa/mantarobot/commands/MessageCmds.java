/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.kodehawa.mantarobot.commands.moderation.ModLog;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
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
            public Command defaultTrigger(Context context, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, String content) {
                        var args = ctx.getArguments();

                        if (content.isEmpty()) {
                            ctx.sendLocalized("commands.prune.no_messages_specified", EmoteReference.ERROR);
                            return;
                        }

                        List<User> mentionedUsers = ctx.getMentionedUsers();

                        int i = 5;
                        if (args.length >= 1) {
                            try {
                                i = Integer.parseInt(args.length == 1 ? content : args[0]);
                                if (i < 3) i = 3;
                            } catch (Exception e) {
                                ctx.sendLocalized("commands.prune.not_valid", EmoteReference.ERROR);
                                return;
                            }
                        }


                        if (!mentionedUsers.isEmpty()) {
                            List<Long> users = mentionedUsers.stream().map(User::getIdLong).collect(Collectors.toList());
                            ctx.getChannel().getHistory().retrievePast(Math.min(i, 100)).queue(
                                    messageHistory -> getMessageHistory(
                                            ctx, ctx.getChannel(), messageHistory, "commands.prune.mention_no_messages",
                                            message -> users.contains(message.getAuthor().getIdLong())
                                    ), error -> {
                                        ctx.sendLocalized("commands.prune.error_retrieving",
                                                EmoteReference.ERROR, error.getClass().getSimpleName(), error.getMessage()
                                        );

                                        error.printStackTrace();
                                    });

                            return;
                        }

                        ctx.getChannel().getHistory().retrievePast(Math.min(i, 100)).queue(
                                messageHistory -> prune(ctx, messageHistory),
                                error -> {
                                    ctx.sendLocalized("commands.prune.error_retrieving",
                                            EmoteReference.ERROR, error.getClass().getSimpleName(), error.getMessage()
                                    );

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
                        .setUsage("`~>prune <messages> [@user...]`")
                        .addParameter("messages", "Number of messages from 4 to 100.")
                        .addParameterOptional("@user...", "Prunes messages only from mentioned users.")
                        .build();
            }
        });

        pruneCmd.addSubCommand("bot", new SubCommand() {
            @Override
            public String description() {
                return "Prune bot messages. It takes the number of messages as an argument.";
            }

            @Override
            protected void call(Context ctx, String content) {
                var args = ctx.getArguments();

                int i = 100;
                if (args.length >= 1) {
                    try {
                        i = Integer.parseInt(args[0]);
                        if (i < 3) i = 3;
                    } catch (Exception e) {
                        ctx.sendLocalized("commands.prune.not_valid", EmoteReference.ERROR);
                        return;
                    }
                }

                ctx.getChannel().getHistory().retrievePast(Math.min(i, 100)).queue(
                        messageHistory -> {
                            String prefix = MantaroData.db().getGuild(ctx.getGuild()).getData().getGuildCustomPrefix();
                            getMessageHistory(ctx, ctx.getChannel(), messageHistory, "commands.prune.bots_no_messages",
                                    message -> message.getAuthor().isBot() || message.getContentRaw().startsWith(prefix == null ? "~>" : prefix));
                        }, error -> {
                            ctx.sendLocalized("commands.prune.error_retrieving",
                                    EmoteReference.ERROR, error.getClass().getSimpleName(), error.getMessage()
                            );

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
            protected void call(Context ctx, String content) {
                var args = ctx.getArguments();

                int i = 100;
                if (args.length >= 1) {
                    try {
                        i = Integer.parseInt(args[0]);
                        if (i < 3) i = 3;
                    } catch (Exception e) {
                        ctx.sendLocalized("commands.prune.not_valid", EmoteReference.ERROR);
                        return;
                    }
                }

                ctx.getChannel().getHistory().retrievePast(Math.min(i, 100)).queue(
                        messageHistory -> getMessageHistory(ctx, ctx.getChannel(), messageHistory,
                                "commands.prune.no_pins_no_messages", message -> !message.isPinned()
                        ), error -> {
                            ctx.sendLocalized("commands.prune.error_retrieving",
                                    EmoteReference.ERROR, error.getClass().getSimpleName(), error.getMessage()
                            );

                            error.printStackTrace();
                        }
                );

            }
        });

        pruneCmd.setPredicate(ctx -> {
            if (!ctx.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                ctx.sendLocalized("commands.prune.no_permissions", EmoteReference.ERROR);
                return false;
            }

            return true;
        });
    }

    private void getMessageHistory(Context ctx, TextChannel channel, List<Message> messageHistory, String i18n, Predicate<Message> predicate) {
        messageHistory = messageHistory.stream().filter(predicate).collect(Collectors.toList());

        if (messageHistory.isEmpty()) {
            ctx.sendLocalized(i18n, EmoteReference.ERROR);
            return;
        }

        if (messageHistory.size() < 3) {
            ctx.sendLocalized("commands.prune.too_few_messages", EmoteReference.ERROR);
            return;
        }

        prune(ctx, messageHistory);
    }

    private void prune(Context ctx, List<Message> messageHistory) {
        messageHistory = messageHistory.stream().filter(message -> !message.getTimeCreated()
                .isBefore(OffsetDateTime.now().minusWeeks(2)))
                .collect(Collectors.toList());

        if (messageHistory.isEmpty()) {
            ctx.sendLocalized("commands.prune.messages_too_old", EmoteReference.ERROR);
            return;
        }

        final int size = messageHistory.size();

        if (messageHistory.size() < 3) {
            ctx.sendLocalized("commands.prune.too_few_messages", EmoteReference.ERROR);
            return;
        }

        ctx.getChannel().deleteMessages(messageHistory).queue(
                success -> {
                    ctx.sendLocalized("commands.prune.success", EmoteReference.PENCIL, size);

                    DBGuild db = ctx.getDBGuild();
                    db.getData().setCases(db.getData().getCases() + 1);
                    db.saveAsync();
                    ModLog.log(ctx.getMember(), null, "Pruned Messages",
                            ctx.getChannel().getName(), ModLog.ModAction.PRUNE, db.getData().getCases(), size
                    );
                },
                error -> {
                    if (error instanceof PermissionException) {
                        PermissionException pe = (PermissionException) error;
                        ctx.sendLocalized("commands.prune.lack_perms", EmoteReference.ERROR, pe.getPermission());
                    } else {
                        ctx.sendLocalized("commands.prune.error_deleting", EmoteReference.ERROR, error.getClass().getSimpleName(), error.getMessage());
                        error.printStackTrace();
                    }
                }
        );
    }
}
