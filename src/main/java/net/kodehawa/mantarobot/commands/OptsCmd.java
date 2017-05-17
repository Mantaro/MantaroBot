package net.kodehawa.mantarobot.commands;

import com.google.common.base.Preconditions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Map.Entry;

@Module
public class OptsCmd {
	private static final Logger LOGGER = LoggerFactory.getLogger("Options");
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

		//region nsfw
		registerOption("nsfw:toggle", (event) -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			if (guildData.getGuildUnsafeChannels().contains(event.getChannel().getId())) {
				guildData.getGuildUnsafeChannels().remove(event.getChannel().getId());
				event.getChannel().sendMessage(EmoteReference.CORRECT + "NSFW in this channel has been disabled").queue();
				dbGuild.saveAsync();
				return;
			}

			guildData.getGuildUnsafeChannels().add(event.getChannel().getId());
			dbGuild.saveAsync();
			event.getChannel().sendMessage(EmoteReference.CORRECT + "NSFW in this channel has been enabled.").queue();
		});//endregion

		//region birthday
		//region enable
		registerOption("birthday:enable", (event, args) -> {
			if (args.length < 2) {
				onHelp(event);
				return;
			}

			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();

			try {
				String channel = args[0];
				String role = args[1];

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
			} catch (Exception e) {
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
			}
		});//endregion

		//region disable
		registerOption("birthday:disable", (event) -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			guildData.setBirthdayChannel(null);
			guildData.setBirthdayRole(null);
			dbGuild.save();
			event.getChannel().sendMessage(EmoteReference.MEGA + "Birthday logging has been disabled on this server").queue();
		});//endregion
		//endregion

		//region music
		//region channel
		registerOption("music:channel", (event, args) -> {
			if (args.length == 0) {
				onHelp(event);
				return;
			}

			String channelName = String.join(" ", args);

			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();

			VoiceChannel channel = null;

			try {
				channel = event.getGuild().getVoiceChannelById(channelName);
			} catch (Exception ignored) {}

			if (channel == null) {
				try {
					List<VoiceChannel> voiceChannels = event.getGuild().getVoiceChannels().stream()
						.filter(voiceChannel -> voiceChannel.getName().contains(channelName))
						.collect(Collectors.toList());

					if (voiceChannels.size() == 0) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "I couldn't found a voice channel matching that" +
							" name or id").queue();
						return;
					} else if (voiceChannels.size() == 1) {
						channel = voiceChannels.get(0);
						guildData.setMusicChannel(channel.getId());
						dbGuild.save();
						event.getChannel().sendMessage(EmoteReference.OK + "Music Channel set to: " + channel.getName())
							.queue();
					} else {
						DiscordUtils.selectList(event, voiceChannels,
							voiceChannel -> String.format("%s (ID: %s)", voiceChannel.getName(), voiceChannel.getId()),
							s -> ((SimpleCommand) optsCmd).baseEmbed(event, "Select the Channel:").setDescription(s).build(),
							voiceChannel -> {
								guildData.setMusicChannel(voiceChannel.getId());
								dbGuild.save();
								event.getChannel().sendMessage(EmoteReference.OK + "Music Channel set to: " +
									voiceChannel.getName()).queue();
							}
						);
					}
				} catch (Exception e) {
					LOGGER.warn("Error while setting voice channel", e);
					event.getChannel().sendMessage("I couldn't set the voice channel " + EmoteReference.SAD + " - try again " +
						"in a few minutes " +
						"-> " + e.getClass().getSimpleName()).queue();
				}
			}
		});//endregion

		//region queuelimit
		registerOption("music:queuelimit", (event, args) -> {
			if (args.length == 0) {
				onHelp(event);
				return;
			}

			boolean isNumber = args[0].matches("^[0-9]*$");
			if (!isNumber) {
				event.getChannel().sendMessage(EmoteReference.ERROR + "That's not a valid number!").queue();
				return;
			}

			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			try {
				int finalSize = Integer.parseInt(args[0]);
				int applySize = finalSize >= 300 ? 300 : finalSize;
				guildData.setMusicQueueSizeLimit((long) applySize);
				dbGuild.save();
				event.getChannel().sendMessage(String.format(EmoteReference.MEGA + "The queue limit on this server is now " +
					"**%d** songs.", applySize)).queue();
				return;
			} catch (NumberFormatException e) {
				event.getChannel().sendMessage(EmoteReference.ERROR + "You're trying to set too high of a number (which won't" +
					" be applied anyway), silly").queue();
			}
		});
		//endregion

		//region clear
		registerOption("music:clear", (event) -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			guildData.setMusicSongDurationLimit(null);
			guildData.setMusicChannel(null);
			dbGuild.save();
			event.getChannel().sendMessage(EmoteReference.CORRECT + "I can play music on all channels now").queue();
		});//endregion
		//endregion

		//region admincustom
		registerOption("admincustom", (event, args) -> {
			if (args.length == 0) {
				onHelp(event);
				return;
			}

			String action = args[0];
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();

			try {
				guildData.setCustomAdminLock(Boolean.parseBoolean(action));
				dbGuild.save();
				String toSend = EmoteReference.CORRECT + (Boolean.parseBoolean(action) ? "``Permission -> User command creation " +
					"is now admin only.``" : "``Permission -> User command creation can be done by anyone.``");
				event.getChannel().sendMessage(toSend).queue();
			} catch (Exception e) {
				event.getChannel().sendMessage(EmoteReference.ERROR + "Silly, that's not a boolean value!").queue();
			}
		});//endregion

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
				s -> ((SimpleCommand) optsCmd).baseEmbed(event, "Select the Channel:").setDescription(s).build(),
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
			System.out.println(roleName);
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

		registerOption("lobby:reset", event -> {
			GameLobby.LOBBYS.remove(event.getChannel());
			event.getChannel().sendMessage(EmoteReference.CORRECT + "Reset the lobby correctly.").queue();
		});

		//region moderation
		registerOption("linkprotection:toggle", event -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			boolean toggler = guildData.isLinkProtection();

			guildData.setLinkProtection(!toggler);
			event.getChannel().sendMessage(EmoteReference.CORRECT + "Set link protection to " + "**" + !toggler + "**").queue();
			dbGuild.save();
		});

		registerOption("slowmode:toggle", event -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			boolean toggler = guildData.isSlowMode();

			guildData.setSlowMode(!toggler);
			event.getChannel().sendMessage(EmoteReference.CORRECT + "Set slowmode chat to " + "**" + !toggler + "**").queue();
			dbGuild.save();
		});
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

			for(Entry e : fieldMap.entrySet()){

				show.append("Option `")
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
			String blacklisted = mentioned.stream().map(user -> user.getName() + "#" + user.getId()).collect(Collectors.joining(","));

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
			String unBlackListed = mentioned.stream().map(user -> user.getName() + "#" + user.getId()).collect(Collectors.joining(","));

			guildData.getDisabledUsers().removeAll(toUnBlackList);
			dbGuild.save();

			event.getChannel().sendMessage(EmoteReference.CORRECT + "Locally unblacklisted users: **" + unBlackListed + "**").queue();
		});

		registerOption("actionmention:toggle", event -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			boolean toggler = guildData.isNoMentionsAction();

			guildData.setNoMentionsAction(!toggler);
			event.getChannel().sendMessage(EmoteReference.CORRECT + "Set no action mentions in chat to " + "**" + !toggler + "**").queue();
			dbGuild.save();
		});

		registerOption("musicannounce:toggle", event -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			boolean toggler = guildData.isMusicAnnounce();

			guildData.setMusicAnnounce(!toggler);
			event.getChannel().sendMessage(EmoteReference.CORRECT + "Set no music announce to " + "**" + !toggler + "**").queue();
			dbGuild.save();
		});

		registerOption("timedisplay:set", (event, args) -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();

			if(args.length == 0){
				event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify a mode (12h or 24h)").queue();
				return;
			}

			String mode = args[0];

			switch (mode){
				case "12h":
					event.getChannel().sendMessage(EmoteReference.CORRECT + "Set time display mode to 12h").queue();
					guildData.setTimeDisplay(1);
					dbGuild.save();
					break;
				case "24h":
					event.getChannel().sendMessage(EmoteReference.CORRECT + "Set time display mode to 24h").queue();
					guildData.setTimeDisplay(0);
					dbGuild.save();
					break;
				default:
					event.getChannel().sendMessage(EmoteReference.ERROR + "Not a valid choice. Valid choices: **24h**, **12h**").queue();
					break;
			}
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

	private static void onHelp(GuildMessageReceivedEvent event) {
		event.getChannel().sendMessage(optsCmd.help(event)).queue();
	}

	private static void registerOption(String name, Consumer<GuildMessageReceivedEvent> code) {
		Preconditions.checkNotNull(code, "code");
		registerOption(name, (event, ignored) -> code.accept(event));
	}

	private static void registerOption(String name, BiConsumer<GuildMessageReceivedEvent, String[]> code) {
		Preconditions.checkNotNull(name, "name");
		Preconditions.checkArgument(!name.isEmpty(), "Name is empty");
		Preconditions.checkNotNull(code, "code");
		options.putIfAbsent(name, code);
	}

	/**
	 *
	 * Retrieves a map of objects in a class and its respective values.
	 * Yes, I'm too lazy to do it manually and it would make absolutely no sense to either.
	 *
	 * Modified it a bit. (Original: https://narendrakadali.wordpress.com/2011/08/27/41/)
	 *
	 * @since Aug 27, 2011 5:27:19 AM
	 * @author Narendra
	 */
	private static HashMap<String, Object> mapObjects(Object valueObj)
	{
		try{
			Class c1 = valueObj.getClass();
			HashMap<String, Object> fieldMap = new HashMap();
			Field[] valueObjFields = c1.getDeclaredFields();

			for (int i = 0; i < valueObjFields.length; i++)
			{
				String fieldName = valueObjFields[i].getName();
				valueObjFields[i].setAccessible(true);
				Object newObj = valueObjFields[i].get(valueObj);

				fieldMap.put(fieldName, newObj);
			}

			return fieldMap;
		} catch (IllegalAccessException e){
			return null;
		}
	}
}