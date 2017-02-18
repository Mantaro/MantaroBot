package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.kodehawa.mantarobot.data.Data.GuildData;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class ModerationCmds extends Module {
	private static final Logger LOGGER = LoggerFactory.getLogger("osu!");

	public ModerationCmds() {
		super(Category.MODERATION);
		ban();
		kick();
		opts();
		prune();
	}

	private void ban() {
		super.register("ban", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				//Initialize the variables I'll need.
				Guild guild = event.getGuild();
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				Message receivedMessage = event.getMessage();

				//We need to check if this is in a guild AND if the member trying to kick the person has KICK_MEMBERS permission.
				if (!guild.getMember(author).hasPermission(net.dv8tion.jda.core.Permission.BAN_MEMBERS)) {
					channel.sendMessage("\u274C " + "Cannot ban: You have no Ban Members permission.").queue();
					return;
				}

				if (receivedMessage.getMentionedUsers().isEmpty()) {
					channel.sendMessage("\u274C" + "You need to mention at least one user to ban.").queue();
					return;
				}

				//For all mentioned members..
				receivedMessage.getMentionedUsers().forEach(user -> {
					Member member = guild.getMember(user);
					if (member == null) return;
					//If one of them is in a higher hierarchy than the bot, I cannot ban them.
					if (!guild.getSelfMember().canInteract(member)) {
						channel.sendMessage("\u274C" + "Cannot ban member " + member.getEffectiveName() + ", they are higher or the same " + "hierachy than I am!").queue();
						return;
					}

					//If I cannot ban, well..
					if (!guild.getSelfMember().hasPermission(net.dv8tion.jda.core.Permission.BAN_MEMBERS)) {
						channel.sendMessage("\u274C" + "Sorry! I don't have permission to ban members in this server!").queue();
						return;
					}

					//Proceed to ban them. Again, using queue so I don't get rate limited.
					//Also delete all messages from past 7 days.
					guild.getController().ban(member, 7).queue(
						success -> channel.sendMessage(":zap: You will be missed... or not " + member.getEffectiveName()).queue(),
						error ->
						{
							if (error instanceof PermissionException) {
								channel.sendMessage("\u274C" + "Error banning " + member.getEffectiveName()
									+ ": " + "(No permission provided: " + ((PermissionException) error).getPermission() + ")").queue();
							} else {
								channel.sendMessage("\u274C" + "Unknown error while banning " + member.getEffectiveName()
									+ ": " + "<" + error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();

								//I need more information in the case of an unexpected error.

								LOGGER.warn("Unexpected error while banning someone.", error);
							}
						});
				});
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Ban")
					.setDescription("Bans the mentioned users.")
					.build();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

		});
	}

	private void kick() {
		super.register("kick", new SimpleCommand() {
			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				Guild guild = event.getGuild();
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				Message receivedMessage = event.getMessage();

				//We need to check if the member trying to kick the person has KICK_MEMBERS permission.
				if (!guild.getMember(author).hasPermission(net.dv8tion.jda.core.Permission.KICK_MEMBERS)) {
					channel.sendMessage("\u274C Cannot kick: You have no Kick Members permission.").queue();
					return;
				}

				//If they mentioned a user this gets passed, if they didn't it just doesn't.
				if (receivedMessage.getMentionedUsers().isEmpty()) {
					channel.sendMessage("\u274C" + "You must mention 1 or more users to be kicked!").queue();
					return;
				}

				Member selfMember = guild.getSelfMember();

				//Do I have permissions to kick members, if yes continue, if no end command.
				if (!selfMember.hasPermission(net.dv8tion.jda.core.Permission.KICK_MEMBERS)) {
					channel.sendMessage("\u274C" + "Sorry! I don't have permission to kick members in this server!").queue();
					return;
				}

				//For all mentioned users in the command.
				receivedMessage.getMentionedUsers().forEach(user -> {
					Member member = guild.getMember(user);
					if (member == null) return;

					//If one of them is in a higher hierarchy than the bot, cannot kick.
					if (!selfMember.canInteract(member)) {
						channel.sendMessage("\u274C" + "Cannot kick member: " + member.getEffectiveName() + ", they are higher or the same " + "hierachy than I am!").queue();
						return;
					}

					//Proceed to kick them. Again, using queue so I don't get rate limited.
					guild.getController().kick(member).queue(
						success -> channel.sendMessage(":zap: You will be missed... or not " + member.getEffectiveName()).queue(), //Quite funny, I think.
						error -> {
							if (error instanceof PermissionException) {
								channel.sendMessage(String.format("\u274C Error kicking [%s]: (No permission provided: %s)", member.getEffectiveName(), ((PermissionException) error).getPermission())).queue();
							} else {
								channel.sendMessage(String.format("\u274C Unknown error while kicking [%s]: <%s>: %s", member.getEffectiveName(), error.getClass().getSimpleName(), error.getMessage())).queue();

								//Just so I get more info in the case of an unexpected error.
								LOGGER.warn("Unexpected error while kicking someone.", error);
							}
						});
				});
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Kick")
					.setDescription("Kicks the mentioned users.")
					.build();
			}
		});
	}

	private void opts() {
		super.register("opts", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (args.length < 2) {
					onHelp(event);
					return;
				}

				String option = args[0];
				String action = args[1];

				GuildData guildData = MantaroData.getData().get().getGuild(event.getGuild(), true);

				if (option.equals("logs")) {
					if (action.equals("enable")) {
						if (args.length < 3) {
							onHelp(event);
							return;
						}

						String logChannel = args[2];
						boolean isId = args[2].matches("^[0-9]*$");
						String id = isId ? logChannel : event.getGuild().getTextChannelsByName(logChannel, true).get(0).getId();
						guildData.logChannel = id;
						MantaroData.getData().update();
						event.getChannel().sendMessage(String.format(":mega: Message logging enabled on this server with parameters -> ``Channel #%s (%s)``",
							logChannel, id)).queue();
						return;
					}

					if (action.equals("disable")) {
						guildData.logChannel = null;
						MantaroData.getData().update();
						event.getChannel().sendMessage(":mega: Message logging disabled on this server.").queue();
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
						guildData.prefix = prefix;
						MantaroData.getData().update();
						event.getChannel().sendMessage(":mega: Guild custom prefix set to " + prefix).queue();
						return;
					}

					if (action.equals("clear")) {
						guildData.prefix = null;
						MantaroData.getData().update();
						event.getChannel().sendMessage(":mega: Guild custom prefix disabled	").queue();
						return;
					}
					onHelp(event);
					return;
				}

				if (option.equals("nsfw")) {
					if (action.equals("setchannel")) {
						if (args.length < 3) {
							onHelp(event);
							return;
						}

						String channel = args[2];
						boolean isId = args[2].matches("^[0-9]*$");
						String channelId = isId ? args[2] : event.getGuild().getTextChannelsByName(channel, true).get(0).getId();
						guildData.nsfwChannel = channelId;
						MantaroData.getData().update();
						event.getChannel().sendMessage(String.format(":mega: NSFW channel set to %s (%s)", args[2], channelId)).queue();
						return;
					}

					if (action.equals("disable")) {
						guildData.nsfwChannel = null;
						MantaroData.getData().update();
						event.getChannel().sendMessage(String.format(":mega: NSFW channel set to %s", "null")).queue();
						return;
					}
					onHelp(event);
					return;
				}

				if (option.equals("birthday")) {
					if (action.equals("enable")) {
						if (args.length < 4) {
							onHelp(event);
							return;
						}

						String channel = args[2];
						String role = args[3];

						boolean isId = channel.matches("^[0-9]*$");
						String channelId = isId ? channel : event.getGuild().getTextChannelsByName(channel, true).get(0).getId();
						String roleId = event.getGuild().getRolesByName(role.replace(channelId, ""), true).get(0).getId();
						guildData.birthdayChannel = channelId;
						guildData.birthdayRole = roleId;
						MantaroData.getData().update();
						event.getChannel().sendMessage(
							String.format(":mega: Birthday logging enabled on this server with parameters -> Channel: ``#%s (%s)`` and role: ``%s (%s)``",
								channel, channelId, role, roleId)).queue();
						return;
					}

					if (action.equals("disable")) {
						guildData.birthdayChannel = null;
						guildData.birthdayRole = null;
						MantaroData.getData().update();
						event.getChannel().sendMessage(":mega: Birthday logging disabled on this server").queue();
						return;
					}

					onHelp(event);
					return;
				}

				if (option.equals("music")) {
					if (action.equals("limit")) {
						boolean isNumber = args[2].matches("^[0-9]*$");
						if (!isNumber) return;
						guildData.songDurationLimit = Integer.parseInt(args[2]);
						MantaroData.getData().update();
						event.getChannel().sendMessage(String.format(":mega: Song duration limit (on ms) on this server is now: %sms.", args[2])).queue();
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
											event.getChannel().sendMessage("\u274C I couldn't found any Voice Channel with that Name or Id").queue();
											return;
										} else if (voiceChannels.size() == 1) {
											channel = voiceChannels.get(0);
											guildData.musicChannel = channel.getId();
											MantaroData.getData().update();
											event.getChannel().sendMessage("Music Channel set to: " + channel.getName()).queue();
										} else {
											DiscordUtils.selectList(event, voiceChannels,
													voiceChannel -> String.format("%s (ID: %s)", voiceChannel.getName(), voiceChannel.getId()),
													s -> baseEmbed(event, "Select the Channel:").setDescription(s).build(),
											voiceChannel -> {
												guildData.musicChannel = voiceChannel.getId();
												MantaroData.getData().update();
												event.getChannel().sendMessage("Music Channel set to: " + voiceChannel.getName()).queue();
											}
									);
									guildData.musicChannel = channel.getId();
									MantaroData.getData().update();
									event.getChannel().sendMessage("Music Channel set to: " + channel.getName()).queue();
								}
							} catch (Exception e) {
								LOGGER.warn("Error while setting voice channel", e);
								event.getChannel().sendMessage("There has been an error while trying to set the voice channel, maybe try again? " +
										"-> " + e.getClass().getSimpleName()).queue();
							}
						}

						return;
					}

					if (action.equals("clear")) {
						guildData.songDurationLimit = null;
						guildData.musicChannel = null;
						MantaroData.getData().update();
						event.getChannel().sendMessage("Now I can play music on all channels!").queue();
						return;
					}
					onHelp(event);
					return;
				}

				if (option.equals("admincustom")) {
					guildData.customCommandsAdminOnly = Boolean.parseBoolean(action);
					MantaroData.getData().update();
					String toSend = Boolean.parseBoolean(action) ? "``Permission -> Now user command creation is admin only.``" : "``Permission -> Now user command creation can be done by users.``";
					event.getChannel().sendMessage(toSend).queue();
					return;
				}

				onHelp(event);
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.ADMIN;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Bot options")
						.addField("Description", "This command allows you to set different customizable options for your guild instance of the bot.\n" +
								"All values set here are local, that means, they only take effect on your server and not on other " +
								"servers the bot might be on.", false)
						.setDescription("Usage\n" +
										"~>opts logs enable <channel> - Enables logs in the specified channel (use the name).\n" +
										"~>opts logs disable - Disables server-wide logs.\n" +
										"~>opts prefix set <prefix> - Sets a custom prefix for your server.\n" +
										"~>opts prefix clear - Resets your server custom prefix.\n" +
										"~>opts nsfw setchannel <channel> - Sets the NSFW channel for usage with explicit images in yandere.\n" +
										"~>opts nsfw disable - Clears the NSFW channel.\n" +
										"~>opts birthday enable <channel> <role> - Enables birthday monitoring in your server. Arguments such as channel and role don't accept spaces.\n" +
										"~>opts birthday disable - Disables birthday monitoring.\n" +
										"~>opts music limit <ms> - Changes the music lenght limit.\n" +
										"~>opts music channel <channel> - If set, mantaro will connect only to the specified channel. It might be the name or the ID.\n" +
										"~>opts music clear - If set, mantaro will connect to any music channel the user who called the bot is on if nobody did it already.\n" +
										"~>opts admincustom <true/false> - If set to true, custom commands will only be avaliable for admin creation, otherwise everyone can do it. It defaults to false.")
						.build();
			}
		});
	}

	private void prune(){
		super.register("prune", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				Guild guild = event.getGuild();
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				Message receivedMessage = event.getMessage();

				if(!receivedMessage.isFromType(ChannelType.TEXT) || !guild.getMember(author).hasPermission(Permission.MESSAGE_MANAGE)){
					channel.sendMessage(":heavy_multiplication_x: " + "Cannot prune. Possible errors: You have no Manage Messages permission or this was triggered outside of a guild.").queue();
					return;
				}

				if (content.isEmpty()) {
					channel.sendMessage(":heavy_multiplication_x: No messages to prune.").queue();
					return;
				}

				int container = Integer.parseInt(content);
				if (container > 100) container = 100;
				final int i = container;
				TextChannel channel2 = event.getGuild().getTextChannelById(channel.getId());
				List<Message> messageHistory = channel2.getHistory().retrievePast(i).complete();
				channel2.deleteMessages(messageHistory).queue(
						success -> channel.sendMessage(":pencil: Successfully pruned " + i + " messages").queue(),
						error -> {
							if (error instanceof PermissionException) {
								PermissionException pe = (PermissionException) error;
								channel.sendMessage(":heavy_multiplication_x: " + "Lack of permission while pruning messages" +
										"(No permission provided: " + pe.getPermission() + ")").queue();
							} else {
								channel.sendMessage(":heavy_multiplication_x: " + "Unknown error while pruning messages" + "<"
										+ error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();
								error.printStackTrace();
							}
						});
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Prune command")
						.setDescription("Prunes a specific amount of messages.")
						.addField("Usage", "~>prune <x> - Prunes messages", false)
						.addField("Parameters", "x = number of messages to delete", false)
						.build();
			}
		});
	}
}
