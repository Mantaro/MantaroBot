package net.kodehawa.mantarobot.commands;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.moderation.ModLog;
import net.kodehawa.mantarobot.commands.music.AudioCmdUtils;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.db.ManagedDatabase;
import net.kodehawa.mantarobot.data.entities.DBGuild;
import net.kodehawa.mantarobot.data.entities.helpers.GuildData;
import net.kodehawa.mantarobot.modules.CommandRegistry;
import net.kodehawa.mantarobot.modules.Command;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.commands.CommandPermission;
import net.kodehawa.mantarobot.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.modules.events.PostLoadEvent;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j(topic = "Moderation")
@Module
public class ModerationCmds {
    @Command
    public static void softban(CommandRegistry cr) {
        cr.register("softban", new SimpleCommand(Category.MODERATION) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                Guild guild = event.getGuild();
                User author = event.getAuthor();
                TextChannel channel = event.getChannel();
                Message receivedMessage = event.getMessage();
                String reason = content;

                if (!guild.getMember(author).hasPermission(Permission.BAN_MEMBERS)) {
                    channel.sendMessage(EmoteReference.ERROR2 + "Cannot softban: You don't have the Ban Members permission.").queue();
                    return;
                }

                if (receivedMessage.getMentionedUsers().isEmpty()) {
                    channel.sendMessage(EmoteReference.ERROR + "You must mention 1 or more users to be softbanned!").queue();
                    return;
                }

                Member selfMember = guild.getSelfMember();

                if (!selfMember.hasPermission(Permission.BAN_MEMBERS)) {
                    channel.sendMessage(EmoteReference.ERROR2 + "Sorry! I don't have permission to ban members in this server!").queue();
                    return;
                }

                for (User user : event.getMessage().getMentionedUsers()) {
                    reason = reason.replaceAll("(\\s+)?<@!?" + user.getId() + ">(\\s+)?", "");
                }

                if (reason.isEmpty()) {
                    reason = "Not specified";
                }

                final String finalReason = reason;

                receivedMessage.getMentionedUsers().forEach(user -> {
                    if (!event.getGuild().getMember(event.getAuthor()).canInteract(event.getGuild().getMember(user))) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot softban an user in a higher hierarchy than you")
                                .queue();
                        return;
                    }

                    if (event.getAuthor().getId().equals(user.getId())) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Why are you trying to softban yourself?").queue();
                        return;
                    }

                    Member member = guild.getMember(user);
                    if (member == null) return;

                    //If one of them is in a higher hierarchy than the bot, cannot kick.
                    if (!selfMember.canInteract(member)) {
                        channel.sendMessage(EmoteReference.ERROR2 + "Cannot softban member: " + member.getEffectiveName() + ", they are " +
                                "higher or the same " + "hierachy than I am!").queue();
                        return;
                    }
                    final DBGuild db = MantaroData.db().getGuild(event.getGuild());

                    //Proceed to kick them. Again, using queue so I don't get rate limited.
                    guild.getController().ban(member, 7).queue(
                            success -> {
                                user.openPrivateChannel().complete().sendMessage(EmoteReference.MEGA + "You were **softbanned** by " + event
                                        .getAuthor().getName() + "#"
                                        + event.getAuthor().getDiscriminator() + " for reason " + finalReason + ".").queue();
                                db.getData().setCases(db.getData().getCases() + 1);
                                db.saveAsync();
                                channel.sendMessage(EmoteReference.ZAP + "You'll be missed... haha just kidding " + member.getEffectiveName())
                                        .queue(); //Quite funny, I think.
                                guild.getController().unban(member.getUser()).queue(aVoid -> {}, error -> {
                                    if (error instanceof PermissionException) {
                                        channel.sendMessage(String.format(EmoteReference.ERROR + "Error unbanning [%s]: (No permission " +
                                                "provided: %s)", member.getEffectiveName(), ((PermissionException) error).getPermission()))
                                                .queue();
                                    }
                                    else {
                                        channel.sendMessage(String.format(EmoteReference.ERROR + "Unknown error while unbanning [%s]: <%s>: " +
                                                "%s", member.getEffectiveName(), error.getClass().getSimpleName(), error.getMessage()))
                                                .queue();
                                        log.warn("Unexpected error while unbanning someone.", error);
                                    }
                                });


                                ModLog.log(event.getMember(), user, finalReason, ModLog.ModAction.KICK, db.getData().getCases());
                                TextChannelGround.of(event).dropItemWithChance(2, 2);
                            },
                            error -> {
                                if (error instanceof PermissionException) {
                                    channel.sendMessage(String.format(EmoteReference.ERROR + "Error softbanning [%s]: (No permission " +
                                            "provided: %s)", member.getEffectiveName(), ((PermissionException) error).getPermission()))
                                            .queue();
                                }
                                else {
                                    channel.sendMessage(String.format(EmoteReference.ERROR + "Unknown error while softbanning [%s]: <%s>: " +
                                            "%s", member.getEffectiveName(), error.getClass().getSimpleName(), error.getMessage()))
                                            .queue();
                                    log.warn("Unexpected error while softbanning someone.", error);
                                }
                            });
                });
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Softban")
                        .setDescription("**Softban the mentioned user and clears their messages from the past week. (You need Ban " +
                                "Members)**")
                        .addField("Summarizing", "A softban is a ban & instant unban, normally used to clear " +
                                "the user's messages but **without banning the person permanently**.", false)
                        .build();
            }

        });
    }

    @Command
    public static void ban(CommandRegistry cr) {
        cr.register("ban", new SimpleCommand(Category.MODERATION) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                Guild guild = event.getGuild();
                User author = event.getAuthor();
                TextChannel channel = event.getChannel();
                Message receivedMessage = event.getMessage();
                String reason = content;

                if (!guild.getMember(author).hasPermission(net.dv8tion.jda.core.Permission.BAN_MEMBERS)) {
                    channel.sendMessage(EmoteReference.ERROR + "You can't ban: You need the `Ban Users` permission.").queue();
                    return;
                }

                if (receivedMessage.getMentionedUsers().isEmpty()) {
                    channel.sendMessage(EmoteReference.ERROR + "You need to mention at least one user!").queue();
                    return;
                }

                for (User user : event.getMessage().getMentionedUsers()) {
                    reason = reason.replaceAll("(\\s+)?<@!?" + user.getId() + ">(\\s+)?", "");
                }

                if (reason.isEmpty()) {
                    reason = "Not specified";
                }

                final String finalReason = reason;

                receivedMessage.getMentionedUsers().forEach(user -> {
                    if (!event.getGuild().getMember(event.getAuthor()).canInteract(event.getGuild().getMember(user))) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot ban an user who's higher than you in the " +
                                "server hierarchy! Nice try " + EmoteReference.SMILE).queue();
                        return;
                    }

                    if (event.getAuthor().getId().equals(user.getId())) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Why're trying to ban yourself, silly?").queue();
                        return;
                    }

                    Member member = guild.getMember(user);
                    if (member == null) return;
                    if (!guild.getSelfMember().canInteract(member)) {
                        channel.sendMessage(EmoteReference.ERROR + "I can't ban " + member.getEffectiveName() + "; they're higher in the " +
                                "server hierarchy than me!").queue();
                        return;
                    }

                    if (!guild.getSelfMember().hasPermission(net.dv8tion.jda.core.Permission.BAN_MEMBERS)) {
                        channel.sendMessage(EmoteReference.ERROR + "Sorry! I don't have permission to ban members in this server!").queue();
                        return;
                    }
                    final DBGuild db = MantaroData.db().getGuild(event.getGuild());

                    guild.getController().ban(member, 7).queue(
                            success -> {
                                user.openPrivateChannel().complete().sendMessage(EmoteReference.MEGA + "You were **banned** by " + event
                                        .getAuthor().getName() + "#"
                                        + event.getAuthor().getDiscriminator() + ". Reason: " + finalReason + ".").queue();
                                db.getData().setCases(db.getData().getCases() + 1);
                                db.saveAsync();
                                channel.sendMessage(EmoteReference.ZAP + "You'll be missed " + user.getName() + "... or not!")
                                        .queue();
                                ModLog.log(event.getMember(), user, finalReason, ModLog.ModAction.BAN, db.getData().getCases());
                                TextChannelGround.of(event).dropItemWithChance(1, 2);
                            },
                            error ->
                            {
                                if (error instanceof PermissionException) {
                                    channel.sendMessage(EmoteReference.ERROR + "Error banning " + user.getName()
                                            + ": " + "(I need the permission " + ((PermissionException) error).getPermission() + ")")
                                            .queue();
                                }
                                else {
                                    channel.sendMessage(EmoteReference.ERROR + "I encountered an unknown error while banning " + member
                                            .getEffectiveName()
                                            + ": " + "<" + error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();

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

    @Command
    public static void kick(CommandRegistry cr) {
        cr.register("kick", new SimpleCommand(Category.MODERATION) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                Guild guild = event.getGuild();
                User author = event.getAuthor();
                TextChannel channel = event.getChannel();
                Message receivedMessage = event.getMessage();
                String reason = content;

                if (!guild.getMember(author).hasPermission(net.dv8tion.jda.core.Permission.KICK_MEMBERS)) {
                    channel.sendMessage(EmoteReference.ERROR2 + "Cannot kick: You have no Kick Members permission.").queue();
                    return;
                }

                if (receivedMessage.getMentionedUsers().isEmpty()) {
                    channel.sendMessage(EmoteReference.ERROR + "You must mention 1 or more users to be kicked!").queue();
                    return;
                }

                Member selfMember = guild.getSelfMember();

                if (!selfMember.hasPermission(net.dv8tion.jda.core.Permission.KICK_MEMBERS)) {
                    channel.sendMessage(EmoteReference.ERROR2 + "Sorry! I don't have permission to kick members in this server!").queue();
                    return;
                }

                for (User user : event.getMessage().getMentionedUsers()) {
                    reason = reason.replaceAll("(\\s+)?<@!?" + user.getId() + ">(\\s+)?", "");
                }

                if (reason.isEmpty()) {
                    reason = "Not specified";
                }

                final String finalReason = reason;

                receivedMessage.getMentionedUsers().forEach(user -> {
                    if (!event.getGuild().getMember(event.getAuthor()).canInteract(event.getGuild().getMember(user))) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot kick an user in a higher hierarchy than you")
                                .queue();
                        return;
                    }

                    if (event.getAuthor().getId().equals(user.getId())) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Why are you trying to kick yourself?").queue();
                        return;
                    }

                    Member member = guild.getMember(user);
                    if (member == null) return;

                    //If one of them is in a higher hierarchy than the bot, cannot kick.
                    if (!selfMember.canInteract(member)) {
                        channel.sendMessage(EmoteReference.ERROR2 + "Cannot kick member: " + member.getEffectiveName() + ", they are " +
                                "higher or the same " + "hierachy than I am!").queue();
                        return;
                    }
                    final DBGuild db = MantaroData.db().getGuild(event.getGuild());

                    //Proceed to kick them. Again, using queue so I don't get rate limited.
                    guild.getController().kick(member).queue(
                            success -> {
                                user.openPrivateChannel().complete().sendMessage(EmoteReference.MEGA + "You were **kicked** by " + event
                                        .getAuthor().getName() + "#"
                                        + event.getAuthor().getDiscriminator() + " with reason: " + finalReason + ".").queue();
                                db.getData().setCases(db.getData().getCases() + 1);
                                db.saveAsync();
                                channel.sendMessage(EmoteReference.ZAP + "You will be missed... or not " + user.getName())
                                        .queue(); //Quite funny, I think.
                                ModLog.log(event.getMember(), user, finalReason, ModLog.ModAction.KICK, db.getData().getCases());
                                TextChannelGround.of(event).dropItemWithChance(2, 2);
                            },
                            error -> {
                                if (error instanceof PermissionException) {
                                    channel.sendMessage(String.format(EmoteReference.ERROR + "Error kicking [%s]: (No permission " +
                                            "provided: %s)", member.getEffectiveName(), ((PermissionException) error).getPermission()))
                                            .queue();
                                }
                                else {
                                    channel.sendMessage(String.format(EmoteReference.ERROR + "Unknown error while kicking [%s]: <%s>: " +
                                            "%s", member.getEffectiveName(), error.getClass().getSimpleName(), error.getMessage()))
                                            .queue();
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

    @Command
    public static void prune(CommandRegistry cr) {
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
                    event.getChannel().sendMessage(EmoteReference.ERROR + "I cannot prune on this server since I don't have permission: " +
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

                                channel.deleteMessages(messageHistory).queue(
                                        success -> {
                                            channel.sendMessage(EmoteReference.PENCIL + "Successfully pruned " + size + " bot " +
                                                    "messages").queue();
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
                    channel.getHistory().retrievePast(100).queue(
                            messageHistory -> {
                                messageHistory = messageHistory.stream().filter(message -> users.contains(message.getAuthor().getIdLong())).collect(Collectors.toList());

                                if (messageHistory.isEmpty()) {
                                    event.getChannel().sendMessage(EmoteReference.ERROR + "There are no messages from users which you mentioned " +
                                            "here.").queue();
                                    return;
                                }

                                final int size = messageHistory.size();

                                channel.deleteMessages(messageHistory).queue(
                                        success -> {
                                            channel.sendMessage(EmoteReference.PENCIL + "Successfully pruned " + size + " users " +
                                                    "messages").queue();
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

                            channel.deleteMessages(messageHistory).queue(
                                    success -> {
                                        channel.sendMessage(EmoteReference.PENCIL + "Successfully pruned " + size + " messages")
                                                .queue();
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

    @Command
    public static void tempban(CommandRegistry cr) {
        cr.register("tempban", new SimpleCommand(Category.MODERATION) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                String reason = content;
                Guild guild = event.getGuild();
                User author = event.getAuthor();
                TextChannel channel = event.getChannel();
                Message receivedMessage = event.getMessage();

                if (!guild.getMember(author).hasPermission(net.dv8tion.jda.core.Permission.BAN_MEMBERS)) {
                    channel.sendMessage(EmoteReference.ERROR + "Cannot ban: You have no Ban Members permission.").queue();
                    return;
                }

                if (event.getMessage().getMentionedUsers().isEmpty()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention an user!").queue();
                    return;
                }

                for (User user : event.getMessage().getMentionedUsers()) {
                    reason = reason.replaceAll("(\\s+)?<@!?" + user.getId() + ">(\\s+)?", "");
                }
                int index = reason.indexOf("time:");
                if (index < 0) {
                    event.getChannel().sendMessage(EmoteReference.ERROR +
                            "You cannot temp ban an user without giving me the time!").queue();
                    return;
                }
                String time = reason.substring(index);
                reason = reason.replace(time, "").trim();
                time = time.replaceAll("time:(\\s+)?", "");
                if (reason.isEmpty()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot temp ban someone without a reason.!").queue();
                    return;
                }

                if (time.isEmpty()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot temp ban someone without giving me the time!")
                            .queue();
                    return;
                }

                final DBGuild db = MantaroData.db().getGuild(event.getGuild());
                long l = AudioCmdUtils.parseTime(time);
                String finalReason = reason;
                String sTime = StringUtils.parseTime(l);
                receivedMessage.getMentionedUsers().forEach(user ->
                    guild.getController().ban(user, 7).queue(
                            success -> {
                                user.openPrivateChannel().complete().sendMessage(EmoteReference.MEGA + "You were **temporarly banned** by " + event
                                        .getAuthor().getName() + "#"
                                        + event.getAuthor().getDiscriminator() + " with reason: " + finalReason + ".").queue();
                                db.getData().setCases(db.getData().getCases() + 1);
                                db.saveAsync();
                                channel.sendMessage(EmoteReference.ZAP + "You will be missed... or not " + user.getName())
                                        .queue();
                                ModLog.log(event.getMember(), user, finalReason, ModLog.ModAction.TEMP_BAN, db.getData().getCases(), sTime);
                                MantaroBot.getTempBanManager().addTempban(
                                        guild.getId() + ":" + user.getId(), l + System.currentTimeMillis());
                                TextChannelGround.of(event).dropItemWithChance(1, 2);
                            },
                            error ->
                            {
                                if (error instanceof PermissionException) {
                                    channel.sendMessage(EmoteReference.ERROR + "Error banning " + user.getName()
                                            + ": " + "(I need the permission " + ((PermissionException) error).getPermission() + ")")
                                            .queue();
                                }
                                else {
                                    channel.sendMessage(EmoteReference.ERROR + "I encountered an unknown error while banning " + user.getName() + ": " + "<" + error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();

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

    @Command
    public static void mute(CommandRegistry registry){
        registry.register("mute", new SimpleCommand(Category.MODERATION, CommandPermission.ADMIN) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                ManagedDatabase db = MantaroData.db();
                DBGuild dbGuild = db.getGuild(event.getGuild());
                GuildData guildData = dbGuild.getData();
                String reason = "Not specified";
                Map<String, Optional<String>> opts = br.com.brjdevs.java.utils.texts.StringUtils.parse(args);

                if(guildData.getMutedRole() == null) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "The mute role is not set in this server, you can set it by doing `~>opts muterole set <role>`").queue();
                    return;
                }

                Role mutedRole = event.getGuild().getRoleById(guildData.getMutedRole());

                if(args.length > 1){
                    reason = StringUtils.splitArgs(content, 2)[1];
                }

                if(event.getMessage().getMentionedUsers().isEmpty()){
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention at least one user to mute.").queue();
                    return;
                }

                if(!event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MANAGE_ROLES)){
                    event.getChannel().sendMessage(EmoteReference.ERROR + "I don't have permissions to administrate roles on this server!").queue();
                    return;
                }

                //Regex from: Fabricio20
                final String finalReason = reason.replaceAll("-time (\\d+)((?:h(?:our(?:s)?)?)|(?:m(?:in(?:ute(?:s)?)?)?)|(?:s(?:ec(?:ond(?:s)?)?)?))", "");

                event.getMessage().getMentionedUsers().forEach(user -> {
                    Member m = event.getGuild().getMember(user);
                    long time = System.currentTimeMillis() + guildData.getSetModTimeout();

                    if(time > 0){
                        guildData.getMutedTimelyUsers().put(user.getIdLong(), time);
                        dbGuild.save();
                    }

                    if(opts.containsKey("time")){
                        if(opts.get("time").get().isEmpty()){
                            event.getChannel().sendMessage(EmoteReference.WARNING + "You wanted time but didn't specify for how long!").queue();
                            return;
                        }

                        time = System.currentTimeMillis() + AudioCmdUtils.parseTime(opts.get("time").get());
                        guildData.getMutedTimelyUsers().put(user.getIdLong(), time);
                        dbGuild.save();
                    }


                    if(m.getRoles().contains(mutedRole)){
                        event.getChannel().sendMessage(EmoteReference.WARNING + "This user already has a mute role assigned. Please do `~>unmute` to unmute them.").queue();
                        return;
                    }

                    if(!event.getGuild().getSelfMember().canInteract(m)){
                        event.getChannel().sendMessage(EmoteReference.ERROR + "I cannot assign the mute role to this user because they're in a higher hierarchy than me, or the role is in a higher hierarchy!").queue();
                        return;
                    }

                    if(!event.getMember().canInteract(m)){
                        event.getChannel().sendMessage(EmoteReference.ERROR + "I cannot assign the mute role to this user because they're in a higher hierarchy than me, or the role is in a higher hierarchy than you!").queue();
                        return;
                    }

                    final DBGuild dbg = db.getGuild(event.getGuild());
                    event.getGuild().getController().addRolesToMember(m, mutedRole).queue();
                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Added mute role to **" +
                            m.getEffectiveName() + (time > 0 ? "** for around " + Utils.getVerboseTime(time - System.currentTimeMillis()) : "**")).queue();
                    dbg.getData().setCases(dbg.getData().getCases() + 1);
                    dbg.saveAsync();
                    ModLog.log(event.getMember(), user, finalReason, ModLog.ModAction.MUTE, dbg.getData().getCases());
                });
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Mute")
                        .setDescription("**Mutes the specified users**")
                        .addField("Usage", "`~>mute <user> <reason> [-time <time>]` - Mutes the specified users.", false)
                        .addField("Parameters", "`users` - The users to mute. Needs to be mentions.\n" +
                                "`[-time <time>]` - The time to mute an user for. For example `~>mute @Natan#1289 wew, nice -time 1m20s` will mute Natan for 1 minute and 20 seconds.", false)
                        .addField("Considerations", "To unmute an user, do `~>unmute`.", false)
                        .addField("Extended usage", "`time` - can be used with the following parameters: " +
                                "d (days), s (second), m (minutes), h (hour). **For example time:1d1h will give a day and an hour.**", false)
                        .build();
            }
        });
    }

    @Command
    public static void unmute(CommandRegistry commandRegistry){
        commandRegistry.register("unmute", new SimpleCommand(Category.MODERATION, CommandPermission.ADMIN) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                ManagedDatabase db = MantaroData.db();
                DBGuild dbGuild = db.getGuild(event.getGuild());
                GuildData guildData = dbGuild.getData();
                String reason = "Not specified";

                if(guildData.getMutedRole() == null) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "The mute role is not set in this server, you can set it by doing `~>opts muterole set <role>`").queue();
                    return;
                }

                Role mutedRole = event.getGuild().getRoleById(guildData.getMutedRole());

                if(args.length > 1){
                    reason = StringUtils.splitArgs(content, 2)[1];
                }

                if(event.getMessage().getMentionedUsers().isEmpty()){
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention at least one user to un-mute.").queue();
                    return;
                }

                if(!event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MANAGE_ROLES)){
                    event.getChannel().sendMessage(EmoteReference.ERROR + "I don't have permissions to administrate roles on this server!").queue();
                    return;
                }

                final String finalReason = reason;
                final DBGuild dbg = db.getGuild(event.getGuild());

                event.getMessage().getMentionedUsers().forEach(user -> {
                    Member m = event.getGuild().getMember(user);

                    guildData.getMutedTimelyUsers().remove(user.getIdLong());
                    if(!event.getGuild().getSelfMember().canInteract(m)){
                        event.getChannel().sendMessage(EmoteReference.ERROR + "I cannot remove a mute role to this user because they're in a higher hierarchy than me, or the role is in a higher hierarchy!").queue();
                        return;
                    }

                    if(!event.getMember().canInteract(m)){
                        event.getChannel().sendMessage(EmoteReference.ERROR + "I cannot remove a mute role to this user because they're in a higher hierarchy than me, or the role is in a higher hierarchy than you!").queue();
                        return;
                    }

                    if(m.getRoles().contains(mutedRole)){
                        event.getGuild().getController().removeRolesFromMember(m, mutedRole).queue();
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Removed mute role from **" + m.getEffectiveName() + "**").queue();
                        dbg.getData().setCases(dbg.getData().getCases() + 1 );
                        dbg.saveAsync();
                        ModLog.log(event.getMember(), user, finalReason, ModLog.ModAction.UNMUTE, db.getGuild(event.getGuild()).getData().getCases());
                        return;
                    } else {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "This user doesn't have the mute role assigned to them.").queue();
                    }
                });
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Un-mute")
                        .setDescription("**Un-mutes the specified users**")
                        .addField("Usage", "`~>unmute <user> <reason>` - Un-mutes the specified users.", false)
                        .addField("Parameters", "`users` - The users to un-mute. Needs to be mentions.", false)
                        .addField("Considerations", "To mute an user, do `~>mute`.", false)
                        .build();
            }
        });
    }

    @Command
    public static void onPostLoad(PostLoadEvent e){

        OptsCmd.registerOption("modlog:blacklist", event -> {
            List<User> mentioned = event.getMessage().getMentionedUsers();
            if(mentioned.isEmpty()){
                event.getChannel().sendMessage(EmoteReference.ERROR + "**You need to specify the users to locally blacklist from mod logs.**").queue();
                return;
            }

            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();

            List<String> toBlackList = mentioned.stream().map(ISnowflake::getId).collect(Collectors.toList());
            String blacklisted = mentioned.stream().map(user -> user.getName() + "#" + user.getDiscriminator()).collect(Collectors.joining(","));

            guildData.getModlogBlacklistedPeople().addAll(toBlackList);
            dbGuild.save();

            event.getChannel().sendMessage(EmoteReference.CORRECT + "Locally blacklisted users from mod-log: **" + blacklisted + "**").queue();
        });

        OptsCmd.registerOption("modlog:whitelist", event -> {
            List<User> mentioned = event.getMessage().getMentionedUsers();
            if(mentioned.isEmpty()){
                event.getChannel().sendMessage(EmoteReference.ERROR + "**You need to specify the users to locally un-blacklist from mod logs.**").queue();
                return;
            }

            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();

            List<String> toUnBlacklist = mentioned.stream().map(ISnowflake::getId).collect(Collectors.toList());
            String unBlacklisted = mentioned.stream().map(user -> user.getName() + "#" + user.getDiscriminator()).collect(Collectors.joining(","));

            guildData.getModlogBlacklistedPeople().removeAll(toUnBlacklist);
            dbGuild.save();

            event.getChannel().sendMessage(EmoteReference.CORRECT + "Locally un-blacklisted users from mod-log: **" + unBlacklisted + "**").queue();
        });

        OptsCmd.registerOption("linkprotection:toggle", event -> {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            boolean toggler = guildData.isLinkProtection();

            guildData.setLinkProtection(!toggler);
            event.getChannel().sendMessage(EmoteReference.CORRECT + "Set link protection to " + "`" + !toggler + "`").queue();
            dbGuild.save();
        });

        OptsCmd.registerOption("slowmode:toggle", event -> {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            boolean toggler = guildData.isSlowMode();

            guildData.setSlowMode(!toggler);
            event.getChannel().sendMessage(EmoteReference.CORRECT + "Set slowmode chat to " + "`" + !toggler + "`").queue();
            dbGuild.save();
        });

        OptsCmd.registerOption("antispam:toggle", event -> {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            boolean toggler = guildData.isAntiSpam();

            guildData.setAntiSpam(!toggler);
            event.getChannel().sendMessage(EmoteReference.CORRECT + "Set anti-spam chat mode to " + "`" + !toggler + "`").queue();
            dbGuild.save();
        });

        OptsCmd.registerOption("linkprotection:channel:allow", (event, args) -> {
            if (args.length == 0) {
                OptsCmd.onHelp(event);
                return;
            }

            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            String channelName = args[0];
            List<TextChannel> textChannels = event.getGuild().getTextChannels().stream()
                    .filter(textChannel -> textChannel.getName().contains(channelName))
                    .collect(Collectors.toList());

            if (textChannels.isEmpty()) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "There were no channels matching your search.").queue();
            }

            if (textChannels.size() <= 1) {
                guildData.getLinkProtectionAllowedChannels().add(textChannels.get(0).getId());
                dbGuild.save();
                event.getChannel().sendMessage(EmoteReference.CORRECT + textChannels.get(0).getAsMention() + " can now be used to post discord invites.").queue();
                return;
            }

            DiscordUtils.selectList(event, textChannels,
                    textChannel -> String.format("%s (ID: %s)", textChannel.getName(), textChannel.getId()),
                    s -> OptsCmd.getOpts().baseEmbed(event, "Select the Channel:").setDescription(s).build(),
                    textChannel -> {
                        guildData.getLinkProtectionAllowedChannels().add(textChannel.getId());
                        dbGuild.save();
                        event.getChannel().sendMessage(EmoteReference.OK + textChannel.getAsMention() + " can now be used to send discord invites.").queue();
                    }
            );
        });

        OptsCmd.registerOption("linkprotection:channel:disallow", (event, args) -> {
            if (args.length == 0) {
                OptsCmd.onHelp(event);
                return;
            }

            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            String channelName = args[0];
            List<TextChannel> textChannels = event.getGuild().getTextChannels().stream()
                    .filter(textChannel -> textChannel.getName().contains(channelName))
                    .collect(Collectors.toList());

            if (textChannels.isEmpty()) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "There were no channels matching your search.").queue();
            }

            if (textChannels.size() <= 1) {
                guildData.getLinkProtectionAllowedChannels().remove(textChannels.get(0).getId());
                dbGuild.save();
                event.getChannel().sendMessage(EmoteReference.CORRECT + textChannels.get(0).getAsMention() + " cannot longer be used to post discord invites.").queue();
                return;
            }

            DiscordUtils.selectList(event, textChannels,
                    textChannel -> String.format("%s (ID: %s)", textChannel.getName(), textChannel.getId()),
                    s -> OptsCmd.getOpts().baseEmbed(event, "Select the Channel:").setDescription(s).build(),
                    textChannel -> {
                        guildData.getLinkProtectionAllowedChannels().remove(textChannel.getId());
                        dbGuild.save();
                        event.getChannel().sendMessage(EmoteReference.OK + textChannel.getAsMention() + " cannot longer be used to send discord invites.").queue();
                    }
            );
        });
    }
}
