package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.options.Option;
import net.kodehawa.mantarobot.commands.options.OptionType;
import net.kodehawa.mantarobot.core.CommandProcessor;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.modules.CommandRegistry;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.commands.CommandPermission;
import net.kodehawa.mantarobot.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Map.Entry;
import static net.kodehawa.mantarobot.utils.Utils.centerString;
import static net.kodehawa.mantarobot.utils.Utils.mapObjects;

@Module
//TODO Maybe automate boolean ones? (x toggle)
public class OptsCmd {
	private static net.kodehawa.mantarobot.modules.commands.base.Command optsCmd;

	static {

		//region logs
		//region enable
		registerOption("logs:enable",  "Enable logs",
				"Enables logs. You need to use the channel name, *not* the mention.\n" +
						"**Example:** `~>opts logs enable mod-logs`",
				"Enables logs.", (event, args) -> {
			if (args.length < 1) {
				onHelp(event);
				return;
			}

			String logChannel = args[0];
			boolean isId = args[0].matches("^[0-9]*$");
			String id = isId ? logChannel : event.getGuild().getTextChannelsByName(logChannel, true).get(0).getId();
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			guildData.setGuildLogChannel(id);
			dbGuild.saveAsync();
			event.getChannel().sendMessage(String.format(EmoteReference.MEGA + "Message logging has been enabled with " +
					"parameters -> ``Channel #%s (%s)``",
				logChannel, id)).queue();
		});

		registerOption("logs:exclude", "Exclude log channel.",
				"Excludes a channel from logging. You need to use the channel name, *not* the mention.\n" +
						"**Example:** `~>opts logs exclude staff`",
				"Excludes a channel from logging.", (event, args) -> {
			if (args.length == 0) {
				onHelp(event);
				return;
			}
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();

			if(args[0].equals("clearchannels")){
				guildData.getLogExcludedChannels().clear();
				dbGuild.saveAsync();
				event.getChannel().sendMessage(EmoteReference.OK + "Cleared log exceptions!").queue();
				return;
			}

			if(args[0].equals("remove")){
				if(args.length < 2){
					event.getChannel().sendMessage(EmoteReference.ERROR + "Incorrect argument lenght.").queue();
					return;
				}
				String channel = args[1];
				List<TextChannel> channels = event.getGuild().getTextChannelsByName(channel, true);
				if (channels.size() == 0) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "I didn't find a channel with that name!").queue();
				} else if (channels.size() == 1) {
					TextChannel ch = channels.get(0);
					guildData.getLogExcludedChannels().remove(ch.getId());
					dbGuild.saveAsync();
					event.getChannel().sendMessage(EmoteReference.OK + "Removed logs exception on channel: " + ch.getAsMention()).queue();
				} else {
					DiscordUtils.selectList(event, channels, ch -> String.format("%s (ID: %s)", ch.getName(), ch.getId()),
							s -> ((SimpleCommand) optsCmd).baseEmbed(event, "Select the Channel:")
									.setDescription(s).build(),
							ch -> {
								guildData.getLogExcludedChannels().remove(ch.getId());
								dbGuild.saveAsync();
								event.getChannel().sendMessage(EmoteReference.OK + "Removed logs exception on channel: " + ch.getAsMention()).queue();
							});
				}
				return;
			}

			String channel = args[0];
			List<TextChannel> channels = event.getGuild().getTextChannelsByName(channel, true);
			if (channels.size() == 0) {
				event.getChannel().sendMessage(EmoteReference.ERROR + "I didn't find a channel with that name!").queue();
			} else if (channels.size() == 1) {
				TextChannel ch = channels.get(0);
				guildData.getLogExcludedChannels().add(ch.getId());
				dbGuild.saveAsync();
				event.getChannel().sendMessage(EmoteReference.OK + "Added logs exception on channel: " + ch.getAsMention()).queue();
			} else {
				DiscordUtils.selectList(event, channels, ch -> String.format("%s (ID: %s)", ch.getName(), ch.getId()),
						s -> ((SimpleCommand) optsCmd).baseEmbed(event, "Select the Channel:")
								.setDescription(s).build(),
						ch -> {
							guildData.getLogExcludedChannels().add(ch.getId());
							dbGuild.saveAsync();
							event.getChannel().sendMessage(EmoteReference.OK + "Added logs exception on channel: " + ch.getAsMention()).queue();
						});
			}
		});//endregion

		//region disable
		registerOption("logs:disable", "Disable logs",
				"Disables logs.\n" +
						"**Example:** `~>opts logs disable`",
				"Disables logs.",  (event) -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			guildData.setGuildLogChannel(null);
			dbGuild.saveAsync();
			event.getChannel().sendMessage(EmoteReference.MEGA + "Message logging has been disabled.").queue();
		});//endregion
		// endregion

		//region prefix
		//region set
		registerOption("prefix:set", "Prefix set",
				"Sets the server prefix.\n" +
						"**Example:** `~>opts prefix set .`",
				"Sets the server prefix.", (event, args) -> {
			if (args.length < 1) {
				onHelp(event);
				return;
			}
			String prefix = args[0];

			if(prefix.length() > 200){
				event.getChannel().sendMessage(EmoteReference.ERROR + "Don't you think that's a bit too long?").queue();
				return;
			}

			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			guildData.setGuildCustomPrefix(prefix);
			dbGuild.save();
			event.getChannel().sendMessage(EmoteReference.MEGA + "Your server's custom prefix has been set to " + prefix)
				.queue();
		});//endregion

		//region clear
		registerOption("prefix:clear", "Prefix clear",
				"Clear the server prefix.\n" +
						"**Example:** `~>opts prefix clear`",
				"Resets the server prefix.", (event) -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			guildData.setGuildCustomPrefix(null);
			dbGuild.save();
			event.getChannel().sendMessage(EmoteReference.MEGA + "Your server's custom prefix has been disabled").queue();
		});//endregion
		// endregion

		//region autorole
		//region set
		registerOption("autorole:set", "Autorole set",
				"Sets the server autorole. This means every user who joins will get this role. **You need to use the role name, if it contains spaces" +
						" you need to wrap it in quotation marks**\n" +
						"**Example:** `~>opts autorole set Member`, `~>opts autorole set \"Magic Role\"`",
				"Sets the server prefix.", (event, args) -> {
			if (args.length == 0) {
				onHelp(event);
				return;
			}

			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();

			String name = args[0];
			List<Role> roles = event.getGuild().getRolesByName(name, true);
			StringBuilder b = new StringBuilder();

			if (roles.isEmpty()) {
				event.getChannel().sendMessage(EmoteReference.ERROR + "I couldn't find a role with that name").queue();
				return;
			}

			if (roles.size() <= 1) {
				guildData.setGuildAutoRole(roles.get(0).getId());
				event.getMessage().addReaction("\ud83d\udc4c").queue();
				dbGuild.saveAsync();
				event.getChannel().sendMessage(EmoteReference.CORRECT + "The server autorole is now set to: **" + roles.get
					(0).getName() + "** (Position: " + roles.get(0).getPosition() + ")").queue();
				return;
			}

			event.getChannel().sendMessage(new EmbedBuilder().setTitle("Selection", null).setDescription(b.toString()).build
				()).queue();

			DiscordUtils.selectList(event, roles,
				role -> String.format("%s (ID: %s)  | Position: %s", role.getName(), role.getId(), role.getPosition()),
				s -> ((SimpleCommand) optsCmd).baseEmbed(event, "Select the Role:").setDescription(s).build(),
				role -> {
					guildData.setGuildAutoRole(role.getId());
					dbGuild.saveAsync();
					event.getChannel().sendMessage(EmoteReference.OK + "The server autorole is now set to role: **" +
						role.getName() + "** (Position: " + role.getPosition() + ")").queue();
				}
			);
		});//endregion

		//region unbind
		registerOption("autorole:unbind", "Autorole clear",
				"Clear the server autorole.\n" +
						"**Example:** `~>opts autorole unbind`",
				"Resets the servers autorole.", (event) -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			guildData.setGuildAutoRole(null);
			dbGuild.saveAsync();
			event.getChannel().sendMessage(EmoteReference.OK + "The autorole for this server has been removed.").queue();
		});//endregion
		//endregion

		//region usermessage
		//region resetchannel
		registerOption("usermessage:resetchannel", "Reset message channel",
				"Clears the join/leave message channel.\n" +
						"**Example:** `~>opts usermessage resetchannel`",
				"Clears the join/leave message channel.", (event) -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			guildData.setLogJoinLeaveChannel(null);
			dbGuild.saveAsync();
		});//endregion

		//region resetdata
		registerOption("usermessage:resetdata", "Reset join/leave message data",
				"Resets the join/leave message data.\n" +
						"**Example:** `~>opts usermessage resetdata`",
				"Resets the join/leave message data.", (event) -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			guildData.setLeaveMessage(null);
			guildData.setJoinMessage(null);
			dbGuild.save();
		});
		//endregion

		//region channel
		registerOption("usermessage:channel", "Set message channel",
				"Sets the join/leave message channel. You need the channel **name**\n" +
						"**Example:** `~>opts usermessage channel join-magic`",
				"Sets the join/leave message channel.", (event, args) -> {
			if (args.length == 0) {
				onHelp(event);
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
				guildData.setLogJoinLeaveChannel(textChannels.get(0).getId());
				dbGuild.save();
				event.getChannel().sendMessage(EmoteReference.CORRECT + "The logging Join/Leave channel is set to: " +
					textChannels.get(0).getAsMention()).queue();
				return;
			}

			DiscordUtils.selectList(event, textChannels,
				textChannel -> String.format("%s (ID: %s)", textChannel.getName(), textChannel.getId()),
				s -> ((SimpleCommand) optsCmd).baseEmbed(event, "Select the Channel:").setDescription(s).build(),
				textChannel -> {
					guildData.setLogJoinLeaveChannel(textChannel.getId());
					dbGuild.save();
					event.getChannel().sendMessage(EmoteReference.OK + "The logging Join/Leave channel is set to: " +
						textChannel.getAsMention()).queue();
				}
			);
		});//endregion

		//region joinmessage
		registerOption("usermessage:joinmessage", "User join message",
				"Sets the join message.\n" +
						"**Example:** `~>opts usermessage joinmessage Welcome $(event.user.name) to $(event.guild.name) server! Hope you have a great time`",
				"Sets the join message.", (event, args) -> {
			if (args.length == 0) {
				onHelp(event);
				return;
			}

			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();

			String joinMessage = String.join(" ", args);
			guildData.setJoinMessage(joinMessage);
			dbGuild.save();
			event.getChannel().sendMessage(EmoteReference.CORRECT + "Server join message set to: " + joinMessage).queue();
		});//endregion

		//region leavemessage
		registerOption("usermessage:leavemessage", "User leave message",
				"Sets the leave message.\n" +
						"**Example:** `~>opts usermessage leavemessage Sad to see you depart, $(event.user.name)`",
				"Sets the leave message.", (event, args) -> {
			if (args.length == 0) {
				onHelp(event);
				return;
			}

			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();

			String leaveMessage = String.join(" ", args);
			guildData.setLeaveMessage(leaveMessage);
			dbGuild.save();
			event.getChannel().sendMessage(EmoteReference.CORRECT + "Server leave message set to: " + leaveMessage).queue();
		});//endregion
		//endregion

		//region server
		//region channel
		//region disallow
		registerOption("server:channel:disallow", "Channel disallow",
				"Disallows a channel from commands. Use the channel **name**\n" +
						"**Example:** `~>opts server channel disallow general`",
				"Disallows a channel from commands.", (event, args) -> {
			if (args.length == 0) {
				onHelp(event);
				return;
			}

			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();

			if (args[0].equals("*")) {
				Set<String> allChannelsMinusCurrent = event.getGuild().getTextChannels().
						stream().filter(textChannel -> textChannel.getId().equals(event.getChannel().getId())).map(ISnowflake::getId).collect(Collectors.toSet());
				guildData.getDisabledChannels().addAll(allChannelsMinusCurrent);
				dbGuild.save();
				event.getChannel().sendMessage(EmoteReference.CORRECT + "Disallowed all channels except the current one. " +
						"You can start allowing channels one by one again with `opts server channel allow` from **this** channel. " +
						"You can disallow this channel later if you so desire.").queue();
				return;
			}

			if ((guildData.getDisabledChannels().size() + 1) >= event.getGuild().getTextChannels().size()) {
				event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot disable more channels since the bot " +
					"wouldn't be able to talk otherwise :<").queue();
				return;
			}
			List<TextChannel> textChannels = event.getGuild().getTextChannels().stream()
				.filter(textChannel -> textChannel.getName().contains(args[0]))
				.collect(Collectors.toList());
			DiscordUtils.selectList(event, textChannels,
				textChannel -> String.format("%s (ID: %s)", textChannel.getName(), textChannel.getId()),
				s -> getOpts().baseEmbed(event, "Select the Channel:").setDescription(s).build(),
				textChannel -> {
					guildData.getDisabledChannels().add(textChannel.getId());
					dbGuild.save();
					event.getChannel().sendMessage(EmoteReference.OK + "Channel " + textChannel.getAsMention() + " " +
						"will not longer listen to commands").queue();
				}
			);
		});//endregion

		//region allow
		registerOption("server:channel:allow", "Channel allow",
				"Allows a channel from commands. Use the channel **name**\n" +
						"**Example:** `~>opts server channel allow general`",
				"Re-allows a channel from commands.", (event, args) -> {
			if (args.length == 0) {
				onHelp(event);
				return;
			}

			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();

			if (args[0].equals("*")) {
				guildData.getDisabledChannels().clear();
				dbGuild.save();
				event.getChannel().sendMessage(EmoteReference.CORRECT + "All channels are allowed now.").queue();
				return;
			}

			List<TextChannel> textChannels = event.getGuild().getTextChannels().stream()
				.filter(textChannel -> textChannel.getName().contains(args[0]))
				.collect(Collectors.toList());
			DiscordUtils.selectList(event, textChannels,
				textChannel -> String.format("%s (ID: %s)", textChannel.getName(), textChannel.getId()),
				s -> ((SimpleCommand) optsCmd).baseEmbed(event, "Select the Channel:").setDescription(s).build(),
				textChannel -> {
					guildData.getDisabledChannels().remove(textChannel.getId());
					dbGuild.save();
					event.getChannel().sendMessage(EmoteReference.OK + "Channel " + textChannel.getAsMention() + " " +
						"will now listen to commands").queue();
				}
			);
		});//endregion
		//endregion

		//region command
		//region disallow
		registerOption("server:command:disallow", "Command disallow",
				"Disallows a command from being triggered at all. Use the command name\n" +
						"**Example:** `~>opts server command disallow 8ball`",
				"Disallows a command from being triggered at all.", (event, args) -> {
			if (args.length == 0) {
				onHelp(event);
				return;
			}
			String commandName = args[0];
			if (CommandProcessor.REGISTRY.commands().get(commandName) == null) {
				event.getChannel().sendMessage(EmoteReference.ERROR + "No command called " + commandName).queue();
				return;
			}
			if (commandName.equals("opts") || commandName.equals("help")) {
				event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot disable the options or the help command.")
					.queue();
				return;
			}
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			guildData.getDisabledCommands().add(commandName);
			event.getChannel().sendMessage(EmoteReference.MEGA + "Disabled " + commandName + " on this server.").queue();
			dbGuild.saveAsync();
		});
		//endregion

		//region allow
		registerOption("server:command:allow", "Command allow",
				"Allows a command from being triggered. Use the command name\n" +
						"**Example:** `~>opts server command allow 8ball`",
				"Allows a command from being triggered.", (event, args) -> {
			if (args.length == 0) {
				onHelp(event);
				return;
			}
			String commandName = args[0];
			if (CommandProcessor.REGISTRY.commands().get(commandName) == null) {
				event.getChannel().sendMessage(EmoteReference.ERROR + "No command called " + commandName).queue();
				return;
			}
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			guildData.getDisabledCommands().remove(commandName);
			event.getChannel().sendMessage(EmoteReference.MEGA + "Enabled " + commandName + " on this server.").queue();
			dbGuild.saveAsync();
		});
		//endregion
		//endregion

		registerOption("server:command:specific:disallow", "Specific command disallow",
				"Disallows a command from being triggered at all in a specific channel. Use the channel **name** and command name\n" +
						"**Example:** `~>opts server command specific disallow general 8ball`",
				"Disallows a command from being triggered at all in a specific channel.", (event, args) -> {
			if (args.length == 0) {
				onHelp(event);
				return;
			}

			if(args.length < 2){
				event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify the channel name and the command to disalllow!").queue();
				return;
			}

			String channelName = args[0];
			String commandName = args[1];

			if (CommandProcessor.REGISTRY.commands().get(commandName) == null) {
				event.getChannel().sendMessage(EmoteReference.ERROR + "No command called " + commandName).queue();
				return;
			}

			if(event.getGuild().getTextChannelsByName(channelName, true).isEmpty()){
				event.getChannel().sendMessage(EmoteReference.ERROR + "No channel called " + channelName + " was found. Try again with the correct name.").queue();
				return;
			}

			if (commandName.equals("opts") || commandName.equals("help")) {
				event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot disable the options or the help command.")
						.queue();
				return;
			}

			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();

			String id = event.getGuild().getTextChannelsByName(channelName, true).get(0).getId();
			guildData.getChannelSpecificDisabledCommands().computeIfAbsent(id, k -> new ArrayList<>());

			guildData.getChannelSpecificDisabledCommands().get(id).add(commandName);

			event.getChannel().sendMessage(EmoteReference.MEGA + "Disabled " + commandName + " on channel #" + channelName + ".").queue();
			dbGuild.saveAsync();

		});

		registerOption("server:command:specific:allow", "Specific command allow",
				"Re-allows a command from being triggered in a specific channel. Use the channel **name** and command name\n" +
						"**Example:** `~>opts server command specific allow general 8ball`",
				"Re-allows a command from being triggered in a specific channel.", ((event, args) -> {
			if (args.length == 0) {
				onHelp(event);
				return;
			}

			if(args.length < 2){
				event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify the channel name and the command to disalllow!").queue();
				return;
			}

			String channelName = args[0];
			String commandName = args[1];

			if (CommandProcessor.REGISTRY.commands().get(commandName) == null) {
				event.getChannel().sendMessage(EmoteReference.ERROR + "No command called " + commandName).queue();
				return;
			}

			if(event.getGuild().getTextChannelsByName(channelName, true).isEmpty()){
				event.getChannel().sendMessage(EmoteReference.ERROR + "No channel called " + channelName + " was found. Try again with the correct name.").queue();
				return;
			}

			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			String id = event.getGuild().getTextChannelsByName(channelName, true).get(0).getId();

			guildData.getChannelSpecificDisabledCommands().computeIfAbsent(id, k -> new ArrayList<>());

			guildData.getChannelSpecificDisabledCommands().get(id).remove(commandName);

			event.getChannel().sendMessage(EmoteReference.MEGA + "Enabled " + commandName + " on channel #" + channelName + ".").queue();
			dbGuild.saveAsync();
		}));
		//endregion

		//region autoroles
		//region add
		registerOption("autoroles:add", "Autoroles add",
				"Adds a role to the `~>iam` list.\n" +
						"You need the name of the iam and the name of the role. If the role contains spaces wrap it in quotation marks.\n" +
						"**Example:** `~>opts autoroles add member Member`, `~>opts autoroles add wew \"A role with spaces on its name\"`",
				"Adds an auto-assignable role to the iam lists.", (event, args) -> {
			if (args.length < 2) {
				onHelp(event);
				return;
			}

			String roleName = args[1];

			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();

			List<Role> roleList = event.getGuild().getRolesByName(roleName, true);
			if (roleList.size() == 0) {
				event.getChannel().sendMessage(EmoteReference.ERROR + "I didn't find a role with that name!").queue();
			} else if (roleList.size() == 1) {
				Role role = roleList.get(0);
				guildData.getAutoroles().put(args[0], role.getId());
				dbGuild.saveAsync();
				event.getChannel().sendMessage(EmoteReference.OK + "Added autorole **" + args[0] + "**, which gives the role " +
					"**" +
					role.getName() + "**").queue();
			} else {
				DiscordUtils.selectList(event, roleList, role -> String.format("%s (ID: %s)  | Position: %s", role.getName(),
					role.getId(), role.getPosition()), s -> ((SimpleCommand) optsCmd).baseEmbed(event, "Select the Role:")
						.setDescription(s).build(),
					role -> {
						guildData.getAutoroles().put(args[0], role.getId());
						dbGuild.saveAsync();
						event.getChannel().sendMessage(EmoteReference.OK + "Added autorole **" + args[0] + "**, which gives the " +
							"role " +
							"**" +
							role.getName() + "**").queue();
					});
			}
		});

		//region remove
		registerOption("autoroles:remove","Autoroles remove",
				"Removes a role from the `~>iam` list.\n" +
						"You need the name of the iam.\n" +
						"**Example:** `~>opts autoroles remove iamname`",
				"Removes an auto-assignable role from iam.", (event, args) -> {
			if (args.length == 0) {
				onHelp(event);
				return;
			}

			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			HashMap<String, String> autoroles = guildData.getAutoroles();
			if (autoroles.containsKey(args[0])) {
				autoroles.remove(args[0]);
				dbGuild.saveAsync();
				event.getChannel().sendMessage(EmoteReference.OK + "Removed autorole " + args[0]).queue();
			} else {
				event.getChannel().sendMessage(EmoteReference.ERROR + "I couldn't find an autorole with that name").queue();
			}
		});//endregion
		//endregion

		registerOption("localblacklist:add", "Local Blacklist add",
				"Adds someone to the local blacklist.\n" +
						"You need to mention the user. You can mention multiple users.\n" +
						"**Example:** `~>opts localblacklist add @user1 @user2`",
				"Adds someone to the local blacklist.", (event, args) -> {

			List<User> mentioned = event.getMessage().getMentionedUsers();

			if(mentioned.isEmpty()){
				event.getChannel().sendMessage(EmoteReference.ERROR + "**You need to specify the users to locally blacklist.**").queue();
				return;
			}

			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();

			List<String> toBlackList = mentioned.stream().map(ISnowflake::getId).collect(Collectors.toList());
			String blacklisted = mentioned.stream().map(user -> user.getName() + "#" + user.getDiscriminator()).collect(Collectors.joining(","));

			guildData.getDisabledUsers().addAll(toBlackList);
			dbGuild.save();

			event.getChannel().sendMessage(EmoteReference.CORRECT + "Locally blacklisted users: **" + blacklisted + "**").queue();
		});

		registerOption("localblacklist:remove", "Local Blacklist remove",
				"Removes someone from the local blacklist.\n" +
						"You need to mention the user. You can mention multiple users.\n" +
						"**Example:** `~>opts localblacklist remove @user1 @user2`",
				"Removes someone from the local blacklist.", (event, args) -> {
			List<User> mentioned = event.getMessage().getMentionedUsers();

			if(mentioned.isEmpty()){
				event.getChannel().sendMessage(EmoteReference.ERROR + "**You need to specify the users to locally blacklist.**").queue();
				return;
			}

			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();

			List<String> toUnBlackList = mentioned.stream().map(ISnowflake::getId).collect(Collectors.toList());
			String unBlackListed = mentioned.stream().map(user -> user.getName() + "#" + user.getDiscriminator()).collect(Collectors.joining(","));

			guildData.getDisabledUsers().removeAll(toUnBlackList);
			dbGuild.save();

			event.getChannel().sendMessage(EmoteReference.CORRECT + "Locally unblacklisted users: **" + unBlackListed + "**").queue();
		});

		registerOption("category:disable", "Disable categories",
				"Disables a specified category.\n" +
						"If a non-valid category it's specified, it will display a list of valid categories",
				"Disables a specified category", (event, args) -> {
			if(args.length == 0){
				event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify a category to disable.").queue();
				return;
			}

			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			Category toDisable = Category.lookupFromString(args[0]);

			if(toDisable == null){
				AtomicInteger at = new AtomicInteger();
				event.getChannel().sendMessage(EmoteReference.ERROR + "You entered a invalid category. A list of valid categories to disable (case-insensitive) will be shown below"
								+ "```md\n" + Category.getAllNames().stream().map(name -> "#" +  at.incrementAndGet() + ". " + name).collect(Collectors.joining("\n")) + "```").queue();
				return;
			}

			if (guildData.getDisabledCategories().contains(toDisable)) {
				event.getChannel().sendMessage(EmoteReference.WARNING + "This category is already disabled.").queue();
				return;
			}

			if(toDisable.toString().equals("Moderation")){
				event.getChannel().sendMessage(EmoteReference.WARNING + "You cannot disable moderation since it contains this command.").queue();
				return;
			}

			guildData.getDisabledCategories().add(toDisable);
			dbGuild.save();
			event.getChannel().sendMessage(EmoteReference.CORRECT + "Disabled category `" + toDisable.toString() + "`").queue();
		});

		registerOption("category:enable", "Enable categories",
				"Enables a specified category.\n" +
						"If a non-valid category it's specified, it will display a list of valid categories",
				"Enables a specified category", (event, args) -> {
			if(args.length == 0){
				event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify a category to disable.").queue();
				return;
			}

			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			Category toEnable = Category.lookupFromString(args[0]);

			if(toEnable == null){
				AtomicInteger at = new AtomicInteger();
				event.getChannel().sendMessage(EmoteReference.ERROR + "You entered a invalid category. A list of valid categories to disable (case-insensitive) will be shown below"
						+ "```md\n" + Category.getAllNames().stream().map(name -> "#" +  at.incrementAndGet() + ". " + name).collect(Collectors.joining("\n")) + "```").queue();
				return;
			}

			guildData.getDisabledCategories().remove(toEnable);
			dbGuild.save();
			event.getChannel().sendMessage(EmoteReference.CORRECT + "Enabled category `" + toEnable.toString() + "`").queue();
		});
	}

	@Subscribe
	public static void register(CommandRegistry registry) {
		registry.register("opts", optsCmd = new SimpleCommand(Category.MODERATION, CommandPermission.ADMIN) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (args.length == 1 && args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("ls")) {
					Queue<Message> toSend = new MessageBuilder()
							.append("```prologn```") //Trick to make it count the end and start of the formatting
							.append(String.format("%-34s | %s \n", centerString("Name", 34), centerString("Description", 60)))
							.append(centerString("** ------------------- **", 75))
							.append("\n")
							.append(Option.getAvaliableOptions().stream().collect(Collectors.joining("\n")))
							.buildAll(MessageBuilder.SplitPolicy.NEWLINE);

					toSend.forEach(message -> event.getChannel().sendMessage("```prolog\n" +
							message.getContent().replace("```prologn```", "")
							+ "```").queue());

					return;
				}

				if (args.length < 2) {
					event.getChannel().sendMessage(help(event)).queue();
					return;
				}

                StringBuilder name = new StringBuilder();

                for (int i = 1; i < args.length; i++) {
                    String s = args[i];
                    if (name.length() > 0) name.append(":");
                    name.append(s);
                    Option option = Option.getOptionMap().get(name.toString().replace("help ", ""));

                    if (option != null) {
                        try{
                            EmbedBuilder builder = new EmbedBuilder()
                                    .setAuthor("Help for " +
                                            option.getOptionName(), null, event.getAuthor().getEffectiveAvatarUrl())
                                    .setDescription(option.getDescription())
                                    .addField("Type", option.getType().toString(), false);

                            event.getChannel().sendMessage(builder.build()).queue();
                            return;
                        } catch (IndexOutOfBoundsException ignored){}
                        return;
                    }
                }

                for (int i = 0; i < args.length; i++) {
					String s = args[i];
					if (name.length() > 0) name.append(":");
					name.append(s);
					Option option = Option.getOptionMap().get(name.toString());

					if (option != null) {
						BiConsumer<GuildMessageReceivedEvent, String[]> callable = Option.getOptionMap().get(name.toString()).getEventConsumer();
						try{
							String[] a;
							if (++i < args.length) a = Arrays.copyOfRange(args  , i, args.length);
							else a = new String[0];
							if(content.contains("-help") || content.contains("-h")){
								EmbedBuilder builder = new EmbedBuilder()
										.setAuthor("Help for " +
												option.getOptionName(), null, event.getAuthor().getEffectiveAvatarUrl())
										.setDescription(option.getDescription())
										.addField("Type", option.getType().toString(), false);

								event.getChannel().sendMessage(builder.build()).queue();
								return;
							}
							callable.accept(event, a);
						} catch (IndexOutOfBoundsException ignored){}
						return;
					}
				}
				event.getChannel().sendMessage(help(event)).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Options and Configurations Command")
					.setDescription("**This command allows you to change Mantaro settings for this server.**\n" +
						"All values set are local rather than global, meaning that they will only effect this server.")
					.addField("Usage", "The command is so big that we moved the description to the wiki. [Click here](https://github.com/Mantaro/MantaroBot/wiki/Configuration) to go to the Wiki Article.", false)
					.build();
			}
		}).addOption("check:data", new Option("Data check.",
				"Checks the data values you have set on this server. **THIS IS NOT USER-FRIENDLY**", OptionType.GENERAL)
		.setAction(event -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			//Map as follows: name, value
			Map<String, Object> fieldMap = mapObjects(guildData);

			if(fieldMap == null){
				event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot retrieve values. Weird thing...").queue();
				return;
			}

			StringBuilder show = new StringBuilder();
			show.append("Options set for server **")
					.append(event.getGuild().getName())
					.append("**\n\n");

			AtomicInteger ai = new AtomicInteger();

			for(Entry e : fieldMap.entrySet()){
				show.append(ai.incrementAndGet())
						.append(".- `")
						.append(e.getKey())
						.append("`");

				if(e.getValue() == null){
					show.append(" **is not set to anything.")
							.append("**\n");
				} else {
					show.append(" is set to: **")
							.append(e.getValue())
							.append("**\n");
				}
			}

			Queue<Message> toSend = new MessageBuilder().append(show.toString()).buildAll(MessageBuilder.SplitPolicy.NEWLINE);
			toSend.forEach(message -> event.getChannel().sendMessage(message).queue());
		}).setShortDescription("Checks the data values you have set on this server."));
	}

	public static void onHelp(GuildMessageReceivedEvent event) {
		event.getChannel().sendMessage(optsCmd.help(event)).queue();
	}

	public static void registerOption(String name, Consumer<GuildMessageReceivedEvent> code) {
		Option.addOption(name, new Option("Default name", "Default description", OptionType.GENERAL).setAction(code).setShortDescription("Not set."));
	}

	public static void registerOption(String name, String displayName, String description, Consumer<GuildMessageReceivedEvent> code) {
		Option.addOption(name,
				new Option(displayName, description, OptionType.GENERAL).setAction(code).setShortDescription(description));
	}

	public static void registerOption(String name, String displayName, String description, String shortDescription, Consumer<GuildMessageReceivedEvent> code) {
		Option.addOption(name,
				new Option(displayName, description, OptionType.GENERAL).setAction(code).setShortDescription(shortDescription));
	}

	public static void registerOption(String name, String displayName, String description, String shortDescription,
									  OptionType type, Consumer<GuildMessageReceivedEvent> code) {
		Option.addOption(name,
				new Option(displayName, description, type).setAction(code).setShortDescription(shortDescription));
	}

	public static void registerOption(String name, BiConsumer<GuildMessageReceivedEvent, String[]> code) {
		Option.addOption(name, new Option("Default name", "Default description", OptionType.GENERAL).setAction(code));
	}

	public static void registerOption(String name, String displayName, String description, BiConsumer<GuildMessageReceivedEvent, String[]> code) {
		Option.addOption(name,
				new Option(displayName, description, OptionType.GENERAL).setAction(code).setShortDescription(description));
	}

	public static void registerOption(String name, String displayName, String description, String shortDescription, BiConsumer<GuildMessageReceivedEvent, String[]> code) {
		Option.addOption(name,
				new Option(displayName, description, OptionType.GENERAL).setAction(code).setShortDescription(shortDescription));
	}

	public static void registerOption(String name, String displayName, String description, String shortDescription,
									  OptionType type, BiConsumer<GuildMessageReceivedEvent, String[]> code) {
		Option.addOption(name,
				new Option(displayName, description, type).setAction(code).setShortDescription(shortDescription));
	}

	public static SimpleCommand getOpts(){
		return (SimpleCommand) optsCmd;
	}
}