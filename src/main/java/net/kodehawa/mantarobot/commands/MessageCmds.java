package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.kodehawa.mantarobot.commands.moderation.ModLog;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Module
public class MessageCmds {
    @Subscribe
    public void prune(CommandRegistry cr) {
        cr.register("prune", new SimpleCommand(Category.MODERATION) {
            @Override
            public CommandPermission permission() {
                return CommandPermission.ADMIN;
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                TextChannel channel = event.getChannel();
                if (content.isEmpty()) {
                    channel.sendMessage(EmoteReference.ERROR + "You specified no messages to prune.").queue();
                    return;
                }

                if (!event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "I cannot prune on this server since I don't have the permission: " +
                            "Manage Messages").queue();
                    return;
                }

                if (content.startsWith("bot")) {
                    channel.getHistory().retrievePast(100).queue(
                            messageHistory -> {
                                String prefix = MantaroData.db().getGuild(event.getGuild()).getData().getGuildCustomPrefix();
                                messageHistory = messageHistory.stream().filter(message -> message.getAuthor().isBot() ||
                                        message.getContent().startsWith(prefix == null ? "~>" : prefix)).collect(Collectors.toList());

                                if (messageHistory.isEmpty()) {
                                    event.getChannel().sendMessage(EmoteReference.ERROR + "There are no messages from bots or bot calls " +
                                            "here.").queue();
                                    return;
                                }

                                final int size = messageHistory.size();

                                if(messageHistory.size() < 3) {
                                    event.getChannel().sendMessage(EmoteReference.ERROR + "Too few messages to prune!").queue();
                                    return;
                                }

                                channel.deleteMessages(messageHistory).queue(
                                        success -> {
                                            channel.sendMessage(EmoteReference.PENCIL + "Successfully pruned " + size + " bot " +
                                                    "messages").queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
                                            DBGuild db = MantaroData.db().getGuild(event.getGuild());
                                            db.getData().setCases(db.getData().getCases() + 1);
                                            db.save();
                                            ModLog.log(event.getMember(), null, "Prune action", ModLog.ModAction.PRUNE, db.getData().getCases());
                                        },
                                        error -> {
                                            if (error instanceof PermissionException) {
                                                PermissionException pe = (PermissionException) error;
                                                channel.sendMessage(EmoteReference.ERROR + "Lack of permission while pruning messages" +
                                                        "(No permission provided: " + pe.getPermission() + ")").queue();
                                            }
                                            else {
                                                channel.sendMessage(EmoteReference.ERROR + "Unknown error while pruning messages" + "<"
                                                        + error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();
                                                error.printStackTrace();
                                            }
                                        });

                            },
                            error -> {
                                channel.sendMessage(EmoteReference.ERROR + "Unknown error while retrieving the history to prune the " +
                                        "messages" + "<"
                                        + error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();
                                error.printStackTrace();
                            }
                    );
                    return;
                }

                if (!event.getMessage().getMentionedUsers().isEmpty()) {
                    List<Long> users = new ArrayList<>();
                    for (User user : event.getMessage().getMentionedUsers()) {
                        users.add(user.getIdLong());
                    }

                    int i = 5;

                    if(args.length > 1) {
                        try{
                            i = Integer.parseInt(args[1]);
                            if(i < 3) i = 3;
                        } catch (Exception e) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "That's not a number!").queue();
                        }
                    }

                    channel.getHistory().retrievePast(Math.min(i, 100)).queue(
                            messageHistory -> {
                                messageHistory = messageHistory.stream().filter(message -> users.contains(message.getAuthor().getIdLong())).collect(Collectors.toList());

                                if (messageHistory.isEmpty()) {
                                    event.getChannel().sendMessage(EmoteReference.ERROR + "There are no messages from users which you mentioned " +
                                            "here.").queue();
                                    return;
                                }

                                final int size = messageHistory.size();

                                if(messageHistory.size() < 3) {
                                    event.getChannel().sendMessage(EmoteReference.ERROR + "Too few messages to prune!").queue();
                                    return;
                                }

                                channel.deleteMessages(messageHistory).queue(
                                        success -> {
                                            channel.sendMessage(EmoteReference.PENCIL + "Successfully pruned " + size + " messages from **"
                                                    + event.getMessage().getMentionedUsers().get(0).getName() +
                                                    "**").queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
                                            DBGuild db = MantaroData.db().getGuild(event.getGuild());
                                            db.getData().setCases(db.getData().getCases() + 1);
                                            db.save();
                                            ModLog.log(event.getMember(), null, "Prune action", ModLog.ModAction.PRUNE, db.getData().getCases());
                                        },
                                        error -> {
                                            if (error instanceof PermissionException) {
                                                PermissionException pe = (PermissionException) error;
                                                channel.sendMessage(EmoteReference.ERROR + "Lack of permission while pruning messages" +
                                                        "(No permission provided: " + pe.getPermission() + ")").queue();
                                            }
                                            else {
                                                channel.sendMessage(EmoteReference.ERROR + "Unknown error while pruning messages" + "<"
                                                        + error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();
                                                error.printStackTrace();
                                            }
                                        });
                            });
                    return;
                }

                int i;
                try {
                    i = Integer.parseInt(content);
                }
                catch (Exception e) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Please specify a valid number.").queue();
                    return;
                }

                if (i < 5) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to provide at least 5 messages.").queue();
                    return;
                }

                channel.getHistory().retrievePast(Math.min(i, 100)).queue(
                        messageHistory -> {
                            messageHistory = messageHistory.stream().filter(message -> !message.getCreationTime()
                                    .isBefore(OffsetDateTime.now().minusWeeks(2)))
                                    .collect(Collectors.toList());

                            if (messageHistory.isEmpty()) {
                                event.getChannel().sendMessage(EmoteReference.ERROR + "There are no messages newer than 2 weeks old, " +
                                        "discord won't let me delete them.").queue();
                                return;
                            }

                            final int size = messageHistory.size();

                            if(messageHistory.size() < 3) {
                                event.getChannel().sendMessage(EmoteReference.ERROR + "Too few messages to prune!").queue();
                                return;
                            }

                            channel.deleteMessages(messageHistory).queue(
                                    success -> {
                                        channel.sendMessage(EmoteReference.PENCIL + "Successfully pruned " + size + " messages")
                                                .queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
                                        DBGuild db = MantaroData.db().getGuild(event.getGuild());
                                        db.getData().setCases(db.getData().getCases() + 1);
                                        db.save();
                                        ModLog.log(event.getMember(), null, "Prune action", ModLog.ModAction.PRUNE, db.getData().getCases());
                                    },
                                    error -> {
                                        if (error instanceof PermissionException) {
                                            PermissionException pe = (PermissionException) error;
                                            channel.sendMessage(EmoteReference.ERROR + "Lack of permission while pruning messages" +
                                                    "(No permission provided: " + pe.getPermission() + ")").queue();
                                        }
                                        else {
                                            channel.sendMessage(EmoteReference.ERROR + "Unknown error while pruning messages" + "<"
                                                    + error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();
                                            error.printStackTrace();
                                        }
                                    });
                        },
                        error -> {
                            channel.sendMessage(EmoteReference.ERROR + "Unknown error while retrieving the history to prune the messages"
                                    + "<"
                                    + error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();
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
                        .addField("Important", "You need to provide *at least* 5 messages. I'd say better 10 or more.\n" +
                                "You can use `~>prune bot` to remove all bot messages and bot calls.", false)
                        .build();
            }

        });
    }
}
