package net.kodehawa.mantarobot.commands;

import com.google.common.base.Preconditions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.core.CommandProcessor;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.DBGuild;
import net.kodehawa.mantarobot.data.entities.helpers.GuildData;
import net.kodehawa.mantarobot.modules.Command;
import net.kodehawa.mantarobot.modules.CommandRegistry;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.commands.CommandPermission;
import net.kodehawa.mantarobot.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Map.Entry;
import static net.kodehawa.mantarobot.utils.Utils.mapObjects;

@Module
//TODO Maybe automate boolean ones? (x toggle)
public class OptsCmd {
	private static final Map<String, BiConsumer<GuildMessageReceivedEvent, String[]>> options = new HashMap<>();
	private static net.kodehawa.mantarobot.modules.commands.base.Command optsCmd;

	static {

		//region logs
		//region enable
		registerOption("logs:enable", (event, args) -> {
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

		registerOption("logs:exclude", (event, args) -> {
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
		registerOption("logs:disable", (event) -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			guildData.setGuildLogChannel(null);
			dbGuild.saveAsync();
			event.getChannel().sendMessage(EmoteReference.MEGA + "Message logging has been disabled.").queue();
		});//endregion
		// endregion

		//region prefix
		//region set
		registerOption("prefix:set", (event, args) -> {
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
		registerOption("prefix:clear", (event) -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			guildData.setGuildCustomPrefix(null);
			dbGuild.save();
			event.getChannel().sendMessage(EmoteReference.MEGA + "Your server's custom prefix has been disabled").queue();
		});//endregion
		// endregion

		//region autorole
		//region set
		registerOption("autorole:set", (event, args) -> {
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
		registerOption("autorole:unbind", (event) -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			guildData.setGuildAutoRole(null);
			dbGuild.saveAsync();
			event.getChannel().sendMessage(EmoteReference.OK + "The autorole for this server has been removed.").queue();
		});//endregion
		//endregion

		//region usermessage
		//region resetchannel
		registerOption("usermessage:resetchannel", (event) -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			guildData.setLogJoinLeaveChannel(null);
			dbGuild.saveAsync();
		});//endregion

		//region resetdata
		registerOption("usermessage:resetdata", (event) -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			guildData.setLeaveMessage(null);
			guildData.setJoinMessage(null);
			dbGuild.save();
		});
		//endregion

		//region channel
		registerOption("usermessage:channel", (event, args) -> {
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
		registerOption("usermessage:joinmessage", (event, args) -> {
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
		registerOption("usermessage:leavemessage", (event, args) -> {
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
		registerOption("server:channel:disallow", (event, args) -> {
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
		registerOption("server:channel:allow", (event, args) -> {
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
		registerOption("server:command:disallow", (event, args) -> {
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
		registerOption("server:command:allow", (event, args) -> {
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

		registerOption("server:command:specific:disallow", (event, args) -> {
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

		registerOption("server:command:specific:allow", ((event, args) -> {
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
		registerOption("autoroles:add", (event, args) -> {
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

		registerOption("muterole:set", (event, args) -> {
			if (args.length < 1) {
				onHelp(event);
				return;
			}

			String roleName = String.join(" ", args);
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();

			List<Role> roleList = event.getGuild().getRolesByName(roleName, true);
			if (roleList.size() == 0) {
				event.getChannel().sendMessage(EmoteReference.ERROR + "I didn't find a role with that name!").queue();
			} else if (roleList.size() == 1) {
				Role role = roleList.get(0);
				guildData.setMutedRole(role.getId());
				dbGuild.saveAsync();
				event.getChannel().sendMessage(EmoteReference.OK + "Set mute role to **" + roleName + "**").queue();
			} else {
				DiscordUtils.selectList(event, roleList, role -> String.format("%s (ID: %s)  | Position: %s", role.getName(),
						role.getId(), role.getPosition()), s -> ((SimpleCommand) optsCmd).baseEmbed(event, "Select the Mute Role:")
								.setDescription(s).build(),
						role -> {
							guildData.setMutedRole(role.getId());
							dbGuild.saveAsync();
							event.getChannel().sendMessage(EmoteReference.OK + "Set mute role to **" + roleName + "**").queue();
						});
			}
		});

		registerOption("muterole:unbind", (event, args) -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			guildData.setMutedRole(null);
			dbGuild.saveAsync();
			event.getChannel().sendMessage(EmoteReference.OK + "Correctly resetted mute role.").queue();
		});
		//endregion

		//region remove
		registerOption("autoroles:remove", (event, args) -> {
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

		registerOption("check:data", event -> {
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
		});


		registerOption("localblacklist:add", (event, args) -> {

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

		registerOption("localblacklist:remove", (event, args) -> {
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

		registerOption("category:disable", (event, args) -> {
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

		registerOption("category:enable", (event, args) -> {
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

		registerOption("musicspeedup:fix", event -> {
			MantaroAudioManager manager = MantaroBot.getInstance().getAudioManager();
			AudioManager audioManager = event.getGuild().getAudioManager();
			VoiceChannel previousVc = audioManager.getConnectedChannel();
			audioManager.closeAudioConnection();
			manager.getMusicManagers().remove(event.getGuild().getId());
			event.getChannel().sendMessage(EmoteReference.THINKING + "Sped up music should be fixed now,"
					+ " with debug:\n " +
			 		"```diff\n"
					+ "Audio Manager: " + manager + "\n"
					+ "VC to connect: " + previousVc.getName() + "\n"
					+ "Music Managers: " + manager.getMusicManagers().size() + "\n"
					+ "New MM reference: " + manager.getMusicManager(event.getGuild()) + "\n" //this recreates the MusicManager
					+ "Music Managers after fix: " + manager.getMusicManagers().size() + "\n"
					+ "Send Handler: " + manager.getMusicManager(event.getGuild()).getSendHandler() + "\n"
					+ "Guild ID: " + event.getGuild().getId() + "\n"
					+ "Owner ID: " + event.getGuild().getOwner().getUser().getId() + "\n"
					+ "```\n" +
					"If this didn't work please forward this information to polr.me/mantaroguild or just kick and re-add the bot.").queue();
			audioManager.openAudioConnection(previousVc);
		});
	}

	@Command
	public static void register(CommandRegistry registry) {
		registry.register("opts", optsCmd = new SimpleCommand(Category.MODERATION, CommandPermission.ADMIN) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (args.length < 2) {
					event.getChannel().sendMessage(help(event)).queue();
					return;
				}
				String name = "";
				for (int i = 0; i < args.length; i++) {
					String s = args[i];
					if (!name.isEmpty()) name += ":";
					name += s;
					BiConsumer<GuildMessageReceivedEvent, String[]> option = options.get(name);
					if (option != null) {
						try{
							String[] a;
							if (++i < args.length) a = Arrays.copyOfRange(args, i, args.length);
							else a = new String[0];
							option.accept(event, a);
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
		});
	}

	public static void onHelp(GuildMessageReceivedEvent event) {
		event.getChannel().sendMessage(optsCmd.help(event)).queue();
	}

	public static void registerOption(String name, Consumer<GuildMessageReceivedEvent> code) {
		Preconditions.checkNotNull(code, "code");
		registerOption(name, (event, ignored) -> code.accept(event));
	}

	public static SimpleCommand getOpts(){
		return (SimpleCommand) optsCmd;
	}

	public static void registerOption(String name, BiConsumer<GuildMessageReceivedEvent, String[]> code) {
		Preconditions.checkNotNull(name, "name");
		Preconditions.checkArgument(!name.isEmpty(), "Name is empty");
		Preconditions.checkNotNull(code, "code");
		options.putIfAbsent(name, code);
	}
}