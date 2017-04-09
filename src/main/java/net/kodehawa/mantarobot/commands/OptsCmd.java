package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.DBGuild;
import net.kodehawa.mantarobot.data.entities.helpers.GuildData;
import net.kodehawa.mantarobot.modules.*;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class OptsCmd extends Module {
    private static final Logger LOGGER = LoggerFactory.getLogger("Options");

    public OptsCmd() {
        super(Category.MODERATION);
        opts();
    }

    private void opts() {
        super.register("opts", new SimpleCommand() {
            @Override
            protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
                if (args.length < 1) {
                    onHelp(event);
                    return;
                }

                String option = args[0];
                DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                GuildData guildData = dbGuild.getData();

                if (option.equals("resetmoney")) {
                    //TODO guildData.users.clear();
                    dbGuild.save();
                    event.getChannel().sendMessage(EmoteReference.CORRECT + " This server's local money was cleared.").queue();
                    return;
                }

                if (args.length < 2) {
                    onHelp(event);
                    return;
                }

                String action = args[1];

                if (option.equals("logs")) {
                    if (action.equals("enable")) {
                        if (args.length < 3) {
                            onHelp(event);
                            return;
                        }

                        String logChannel = args[2];
                        boolean isId = args[2].matches("^[0-9]*$");
                        String id = isId ? logChannel : event.getGuild().getTextChannelsByName(logChannel, true).get(0).getId();
                        guildData.setGuildLogChannel(id);
                        dbGuild.saveAsync();
                        event.getChannel().sendMessage(String.format(EmoteReference.MEGA + "Message logging has been enabled with " +
                                        "parameters -> ``Channel #%s (%s)``",
                                logChannel, id)).queue();
                        return;
                    }

                    if (action.equals("disable")) {
                        guildData.setGuildLogChannel(null);
                        dbGuild.saveAsync();
                        event.getChannel().sendMessage(EmoteReference.MEGA + "Message logging has been disabled.").queue();
                        return;
                    }

                    onHelp(event);
                    return;
                }

                if (option.equals("prefix")) {
                    if (action.equals("set")) {
                        if (args.length < 3) {
                            onHelp(event);
                            return;
                        }

                        String prefix = args[2];
                        guildData.setGuildCustomPrefix(prefix);
                        dbGuild.save();
                        event.getChannel().sendMessage(EmoteReference.MEGA + "Your server's custom prefix has been set to " + prefix)
                                .queue();
                        return;
                    }

                    if (action.equals("clear")) {
                        guildData.setGuildCustomPrefix(null);
                        dbGuild.save();
                        event.getChannel().sendMessage(EmoteReference.MEGA + "Your server's custom prefix has been disabled").queue();
                        return;
                    }
                    onHelp(event);
                    return;
                }

                if (option.equals("nsfw")) {
                    if (action.equals("toggle")) {
                        if (guildData.getGuildUnsafeChannels().contains(event.getChannel().getId())) {
                            guildData.getGuildUnsafeChannels().remove(event.getChannel().getId());
                            event.getChannel().sendMessage(EmoteReference.CORRECT + "NSFW in this channel has been disabled").queue();
                            dbGuild.saveAsync();
                            return;
                        }

                        guildData.getGuildUnsafeChannels().add(event.getChannel().getId());
                        dbGuild.saveAsync();
                        event.getChannel().sendMessage(EmoteReference.CORRECT + "NSFW in this channel has been enabled.").queue();
                        return;
                    }

                    onHelp(event);
                    return;
                }

                if (option.equals("devaluation")) {
                    if (args.length < 1) {
                        onHelp(event);
                        return;
                    }

                    if (action.equals("enable")) {
                        guildData.setRpgDevaluation(true);
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Enabled currency devaluation on this server.").queue();
                        return;
                    }

                    if (action.equals("disable")) {
                        guildData.setRpgDevaluation(true);
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Disabled currency devaluation on this server.").queue();
                        return;
                    }
                    dbGuild.saveAsync();
                    return;
                }

                if (option.equals("birthday")) {
                    if (action.equals("enable")) {
                        if (args.length < 4) {
                            onHelp(event);
                            return;
                        }
                        try {
                            String channel = args[2];
                            String role = args[3];

                            boolean isId = channel.matches("^[0-9]*$");
                            String channelId = isId ? channel : event.getGuild().getTextChannelsByName(channel, true).get(0).getId();
                            String roleId = event.getGuild().getRolesByName(role.replace(channelId, ""), true).get(0).getId();
                            guildData.setBirthdayChannel(channelId);
                            guildData.setBirthdayRole(roleId);
                            dbGuild.save();
                            event.getChannel().sendMessage(
                                    String.format(EmoteReference.MEGA + "Birthday logging enabled on this server with parameters -> " +
                                                    "Channel: ``#%s (%s)`` and role: ``%s (%s)``",
                                            channel, channelId, role, roleId)).queue();
                            return;
                        }
                        catch (Exception e) {
                            if (e instanceof IndexOutOfBoundsException) {
                                event.getChannel().sendMessage(EmoteReference.ERROR + "I didn't find a channel or role!\n " +
                                        "**Remember, you don't have to mention neither the role or the channel, rather just type its " +
                                        "name, order is <channel> <role>, without the leading \"<>\".**")
                                        .queue();
                                return;
                            }
                            event.getChannel().sendMessage(EmoteReference.ERROR + "You supplied invalid arguments for this command " +
                                    EmoteReference.SAD).queue();
                            onHelp(event);
                            return;
                        }
                    }

                    if (action.equals("disable")) {
                        guildData.setBirthdayChannel(null);
                        guildData.setBirthdayRole(null);
                        dbGuild.save();
                        event.getChannel().sendMessage(EmoteReference.MEGA + "Birthday logging has been disabled on this server").queue();
                        return;
                    }

                    onHelp(event);
                    return;
                }

                if (option.equals("music")) {
                    if (action.equals("limit")) {
                        boolean isNumber = args[2].matches("^[0-9]*$");
                        if (!isNumber) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "That's not a valid number!").queue();
                            return;
                        }

                        try {
                            guildData.setMusicSongDurationLimit(Long.parseLong(args[2]));
                            dbGuild.save();
                            event.getChannel().sendMessage(String.format(EmoteReference.MEGA + "Song duration limit (in milliseconds) on " +
                                    "this server is now: %sms.", args[2])).queue();
                            return;
                        }
                        catch (NumberFormatException e) {
                            event.getChannel().sendMessage(EmoteReference.WARNING + "You're trying to set a huge number, silly! How cute " +
                                    ":-)").queue();
                        }
                        return;
                    }

                    if (action.equals("queuelimit")) {
                        boolean isNumber = args[2].matches("^[0-9]*$");
                        if (!isNumber) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "That's not a valid number!").queue();
                            return;
                        }
                        try {
                            int finalSize = Integer.parseInt(args[2]);
                            int applySize = finalSize >= 300 ? 300 : finalSize;
                            guildData.setMusicQueueSizeLimit((long) applySize);
                            dbGuild.save();
                            event.getChannel().sendMessage(String.format(EmoteReference.MEGA + "The queue limit on this server is now " +
                                    "**%d** songs.", applySize)).queue();
                            return;
                        }
                        catch (NumberFormatException e) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "You're trying to set too high of a number (which won't" +
                                    " be applied anyway), silly").queue();
                        }
                        return;
                    }

                    if (action.equals("channel")) {
                        if (args.length < 3) {
                            onHelp(event);
                            return;
                        }

                        String channelName = splitArgs(content)[2];

                        VoiceChannel channel = event.getGuild().getVoiceChannelById(channelName);

                        if (channel == null) {
                            try {
                                List<VoiceChannel> voiceChannels = event.getGuild().getVoiceChannels().stream()
                                        .filter(voiceChannel -> voiceChannel.getName().contains(channelName))
                                        .collect(Collectors.toList());

                                if (voiceChannels.size() == 0) {
                                    event.getChannel().sendMessage(EmoteReference.ERROR + "I couldn't found a voice channel matching that" +
                                            " name or id").queue();
                                    return;
                                }
                                else if (voiceChannels.size() == 1) {
                                    channel = voiceChannels.get(0);
                                    guildData.setMusicChannel(channel.getId());
                                    dbGuild.save();
                                    event.getChannel().sendMessage(EmoteReference.OK + "Music Channel set to: " + channel.getName())
                                            .queue();
                                }
                                else {
                                    DiscordUtils.selectList(event, voiceChannels,
                                            voiceChannel -> String.format("%s (ID: %s)", voiceChannel.getName(), voiceChannel.getId()),
                                            s -> baseEmbed(event, "Select the Channel:").setDescription(s).build(),
                                            voiceChannel -> {
                                                guildData.setMusicChannel(voiceChannel.getId());
                                                dbGuild.save();
                                                event.getChannel().sendMessage(EmoteReference.OK + "Music Channel set to: " +
                                                        voiceChannel.getName()).queue();
                                            }
                                    );
                                }
                            }
                            catch (Exception e) {
                                LOGGER.warn("Error while setting voice channel", e);
                                event.getChannel().sendMessage("I couldn't set the voice channel " + EmoteReference.SAD + " - try again " +
                                        "in a few minutes " +
                                        "-> " + e.getClass().getSimpleName()).queue();
                            }
                        }

                        return;
                    }

                    if (action.equals("clear")) {
                        guildData.setMusicSongDurationLimit(null);
                        guildData.setMusicChannel(null);
                        dbGuild.save();
                        event.getChannel().sendMessage(EmoteReference.CORRECT + "I can play music on all channels now").queue();
                        return;
                    }

                    onHelp(event);
                    return;
                }

                if (option.equals("admincustom")) {
                    try {
                        guildData.setCustomAdminLock(Boolean.parseBoolean(action));
                        dbGuild.save();
                        String toSend = EmoteReference.CORRECT + (Boolean.parseBoolean(action) ? "``Permission -> User command creation " +
                                "is now admin only.``" : "``Permission -> User command creation can be done by anyone.``");
                        event.getChannel().sendMessage(toSend).queue();
                        return;
                    }
                    catch (Exception e) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Silly, that's not a boolean value!").queue();
                        return;
                    }
                }

                if (option.equals("localmoney")) {
                    try {
                        guildData.setRpgLocalMode(Boolean.parseBoolean(action));
                        dbGuild.save();
                        String toSend = EmoteReference.CORRECT + (guildData.isRpgLocalMode() ? "``Money -> Money for this server is now " +
                                "localized.``" : "``Permission -> Money on this guild will be shared with the global database.``");
                        event.getChannel().sendMessage(toSend).queue();
                        return;
                    }
                    catch (Exception e) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Not a boolean value, silly!").queue();
                        return;
                    }
                }

                if (option.equals("autorole")) {
                    if (action.equals("set")) {
                        String name = content.replace(option + " " + action + " ", "");
                        List<Role> roles = event.getGuild().getRolesByName(name, true);
                        StringBuilder b = new StringBuilder();

                        if (roles.isEmpty()) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "I couldn't find a role with that name").queue();
                            return;
                        }

                        if (roles.size() <= 1) {
                            guildData.setGuildAutoRole(roles.get(0).getId());
                            event.getMessage().addReaction("\ud83d\udc4c").queue();
                            dbGuild.save();
                            event.getChannel().sendMessage(EmoteReference.CORRECT + "The server autorole is now set to: **" + roles.get
                                    (0).getName() + "** (Position: " + roles.get(0).getPosition() + ")").queue();
                            return;
                        }

                        event.getChannel().sendMessage(new EmbedBuilder().setTitle("Selection", null).setDescription(b.toString()).build
                                ()).queue();

                        DiscordUtils.selectList(event, roles,
                                role -> String.format("%s (ID: %s)  | Position: %s", role.getName(), role.getId(), role.getPosition()),
                                s -> baseEmbed(event, "Select the Role:").setDescription(s).build(),
                                role -> {
                                    guildData.setGuildAutoRole(role.getId());
                                    dbGuild.save();
                                    event.getChannel().sendMessage(EmoteReference.OK + "The server autorole is now set to role: **" +
                                            role.getName() + "** (Position: " + role.getPosition() + ")").queue();
                                }
                        );

                        return;

                    }
                    else if (action.equals("unbind")) {
                        guildData.setGuildAutoRole(null);
                        event.getChannel().sendMessage(EmoteReference.OK + "The autorole for this server has been removed.").queue();
                        return;
                    }
                }

                if (option.equals("usermessage")) {
                    if (action.equals("resetchannel")) {
                        guildData.setLogJoinLeaveChannel(null);
                        dbGuild.save();
                        return;
                    }

                    if (action.equals("resetdata")) {
                        guildData.setLeaveMessage(null);
                        guildData.setJoinMessage(null);
                        dbGuild.save();
                        return;
                    }

                    if (action.equals("channel")) {
                        String channelName = splitArgs(content)[2];
                        List<TextChannel> textChannels = event.getGuild().getTextChannels().stream()
                                .filter(textChannel -> textChannel.getName().contains(channelName))
                                .collect(Collectors.toList());

                        if (textChannels.isEmpty()) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "There were no channels matching your search.").queue();
                        }

                        if (textChannels.size() <= 1) {
                            guildData.setLogJoinLeaveChannel(textChannels.get(0).getId());
                            dbGuild.save();
                            event.getChannel().sendMessage(EmoteReference.CORRECT + "The logging Join/Leave channel is set to: **" +
                                    textChannels.get(0).getAsMention()).queue();
                            return;
                        }

                        DiscordUtils.selectList(event, textChannels,
                                textChannel -> String.format("%s (ID: %s)", textChannel.getName(), textChannel.getId()),
                                s -> baseEmbed(event, "Select the Channel:").setDescription(s).build(),
                                textChannel -> {
                                    guildData.setLogJoinLeaveChannel(textChannel.getId());
                                    dbGuild.save();
                                    event.getChannel().sendMessage(EmoteReference.OK + "The logging Join/Leave channel is set to: " +
                                            textChannel.getAsMention()).queue();
                                }
                        );
                        return;
                    }

                    if (action.equals("joinmessage")) {
                        String joinMessage = content.replace(args[0] + " " + args[1] + " ", "");
                        guildData.setJoinMessage(joinMessage);
                        dbGuild.save();
                        event.getChannel().sendMessage(EmoteReference.CORRECT + "Server join message set to: " + joinMessage).queue();
                        return;
                    }

                    if (action.equals("leavemessage")) {
                        String leaveMessage = content.replace(args[0] + " " + args[1] + " ", "");
                        guildData.setLeaveMessage(leaveMessage);
                        dbGuild.save();
                        event.getChannel().sendMessage(EmoteReference.CORRECT + "Server leave message set to: " + leaveMessage).queue();
                        return;
                    }
                }

                if (option.equals("server")) {
                    if (action.equals("channel")) {
                        String channelName = splitArgs(content)[3];
                        List<TextChannel> textChannels = event.getGuild().getTextChannels().stream()
                                .filter(textChannel -> textChannel.getName().contains(channelName))
                                .collect(Collectors.toList());
                        String op = splitArgs(content)[2];

                        if (op.equals("disallow")) {
                            if ((guildData.getDisabledChannels().size() + 1) >= event.getGuild().getTextChannels().size()) {
                                event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot disable more channels since the bot " +
                                        "wouldn't be able to talk otherwise.").queue();
                                return;
                            }

                            DiscordUtils.selectList(event, textChannels,
                                    textChannel -> String.format("%s (ID: %s)", textChannel.getName(), textChannel.getId()),
                                    s -> baseEmbed(event, "Select the Channel:").setDescription(s).build(),
                                    textChannel -> {
                                        guildData.getDisabledChannels().add(textChannel.getId());
                                        dbGuild.save();
                                        event.getChannel().sendMessage(EmoteReference.OK + "Channel " + textChannel.getAsMention() + " " +
                                                "will not longer listen to commands").queue();
                                    }
                            );
                            return;
                        }

                        if (op.equals("allow")) {
                            DiscordUtils.selectList(event, textChannels,
                                    textChannel -> String.format("%s (ID: %s)", textChannel.getName(), textChannel.getId()),
                                    s -> baseEmbed(event, "Select the Channel:").setDescription(s).build(),
                                    textChannel -> {
                                        guildData.getDisabledChannels().remove(textChannel.getId());
                                        dbGuild.save();
                                        event.getChannel().sendMessage(EmoteReference.OK + "Channel " + textChannel.getAsMention() + " " +
                                                "will now listen to commands").queue();
                                    }
                            );
                            return;
                        }
                    }

                    if (action.equals("command")) {
                        String commandName = splitArgs(content)[3];
                        Command command = Manager.commands.get(commandName).getLeft();

                        if (command == null) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "No command called " + commandName).queue();
                            return;
                        }

                        if (commandName.equals("opts") || commandName.equals("help")) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot disable the options or the help command.")
                                    .queue();
                            return;
                        }

                        String op = args[2];
                        if (op.equals("disallow")) {
                            guildData.getDisabledCommands().add(commandName);
                            event.getChannel().sendMessage(EmoteReference.MEGA + "Disabled " + commandName + " on this server.").queue();
                            dbGuild.save();
                            return;
                        }

                        if (op.equals("allow")) {
                            guildData.getDisabledCommands().remove(commandName);
                            event.getChannel().sendMessage(EmoteReference.MEGA + "Enabled " + commandName + " on this server.").queue();
                            dbGuild.save();
                            return;
                        }
                        return;
                    }
                }

                if (option.equals("autorole")) {
                    if (args.length < 3) {
                        onHelp(event);
                        return;
                    }
                    HashMap<String, String> autoroles = guildData.getAutoroles();
                    if (action.equals("add")) {
                        if (args.length < 4) {
                            onHelp(event);
                            return;
                        }
                        String roleName = contentFrom(content, 3);

                        List<Role> roleList = event.getGuild().getRolesByName(roleName, true);
                        if (roleList.size() == 0) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "I didn't find a role with that name!").queue();
                            return;
                        }
                        else if (roleList.size() == 1) {
                            Role role = roleList.get(0);
                            guildData.getAutoroles().put(option, role.getId());
                            event.getChannel().sendMessage(EmoteReference.OK + "Added autorole **" + option + "**, which gives the role " +
                                    "**" +
                                    role.getName() + "**").queue();
                            return;
                        }
                        else {
                            DiscordUtils.selectList(event, roleList, role -> String.format("%s (ID: %s)  | Position: %s", role.getName(),
                                    role.getId(), role.getPosition()), s -> baseEmbed(event, "Select the Role:").setDescription(s).build
                                    (), role -> {
                                guildData.getAutoroles().put(option, role.getId());
                                event.getChannel().sendMessage(EmoteReference.OK + "Added autorole **" + option + "**, which gives the " +
                                        "role " +
                                        "**" +
                                        role.getName() + "**").queue();
                            });
                            return;
                        }
                    }
                    else if (action.equals("remove")) {
                        if (autoroles.containsKey(option)) {
                            autoroles.remove(option);
                            event.getChannel().sendMessage(EmoteReference.OK + "Removed autorole " + option).queue();
                            return;
                        }
                        else {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "I couldn't find an autorole with that name").queue();
                            return;
                        }
                    }
                }
                onHelp(event);
            }

            @Override
            public CommandPermission permissionRequired() {
                return CommandPermission.ADMIN;
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Bot options")
                        .addField("Description", "This command allows you to change Mantaro settings for this server.\n" +
                                "All values set are local rather than global, meaning that they will only effect this server.", false)
                        .setDescription("Usage\n" +
                                "~>opts logs enable <channel> - Send logs to the specified channel (use its name).\n" +
                                "~>opts logs disable - Disable server-wide logs.\n" +
                                "~>opts prefix set <prefix> - Set a custom prefix for your server.\n" +
                                "~>opts prefix clear - Remove your server's custom prefix.\n" +
                                "~>opts nsfw toggle - Toggle NSFW usage for this channel to allow usage with explicit images in yandere " +
                                "and other commands.\n" +
                                "~>opts birthday enable <channel> <role> - Enable birthday alerts in your server. Arguments such as " +
                                "channel and role don't accept spaces.\n" +
                                "~>opts birthday disable - Disable birthday alerts.\n" +
                                "~>opts music limit <ms> - Changes the music length limit.\n" +
                                "~>opts music queuelimit <number> - Changes the queue song limit (max is 300 for non-donors).\n" +
                                "~>opts autorole set <role> - Set an autorole that will be assigned to users when they join.\n" +
                                "~>opts autorole unbind - Clear the autorole config.\n" +
                                "~>opts resetmoney - Reset local money.\n" +
                                "~>opts localmoney <true/false> - Toggle server local mode (currency stats only for this server).\n" +
                                "~>opts music channel <channel> - If set, I will connect only to the specified channel. You can specify a" +
                                " channel name or ID.\n" +
                                "~>opts music clear - If set, I will connect to any voice channel when called.\n" +
                                "~>opts admincustom <true/false> - If set to true, custom command creation will be available solely for " +
                                "administrators; Otherwise, everyone can create them. Setting defaults to false.\n" +
                                "~>opts usermessage channel <channel name> - Set a channel to send join/leave messages.\n" +
                                "~>opts usermessage joinmessage <message> - Set the join message.\n" +
                                "~>opts usermessage leavemessage <message> - Set the leave message.\n" +
                                "~>opts usermessage resetchannel - Resets the channel to use for join/leave messsages.\n" +
                                "~>opts usermessage resetdata - Resets the join/leave message.")
                        .addField("Command settings",
                                "~>opts server channel disallow <channel name> - Makes a channel deaf to commands\n" +
                                        "~>opts server channel allow <channel name> - Makes a channel able to hear commands again.\n" +
                                        "~>opts server command disable <command name> - Disables the specified command. (Doing enable " +
                                        "with the same syntax will enable it again)\n"
                                , false)
                        .build();
            }
        });
    }
}