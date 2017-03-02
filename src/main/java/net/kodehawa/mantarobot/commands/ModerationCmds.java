package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.kodehawa.mantarobot.commands.currency.inventory.TextChannelGround;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.data.GuildData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.IntConsumer;
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
					channel.sendMessage(EmoteReference.ERROR + "Cannot ban: You have no Ban Members permission.").queue();
					return;
				}

				if (receivedMessage.getMentionedUsers().isEmpty()) {
					channel.sendMessage(EmoteReference.ERROR + "You need to mention at least one user to ban.").queue();
					return;
				}

				//For all mentioned members..
				receivedMessage.getMentionedUsers().forEach(user -> {
					Member member = guild.getMember(user);
					if (member == null) return;
					//If one of them is in a higher hierarchy than the bot, I cannot ban them.
					if (!guild.getSelfMember().canInteract(member)) {
						channel.sendMessage(EmoteReference.ERROR + "Cannot ban member " + member.getEffectiveName() + ", they are higher or the same " + "hierachy than I am!").queue();
						return;
					}

					//If I cannot ban, well..
					if (!guild.getSelfMember().hasPermission(net.dv8tion.jda.core.Permission.BAN_MEMBERS)) {
						channel.sendMessage(EmoteReference.ERROR + "Sorry! I don't have permission to ban members in this server!").queue();
						return;
					}

					//Proceed to ban them. Again, using queue so I don't get rate limited.
					//Also delete all messages from past 7 days.
					guild.getController().ban(member, 7).queue(
						success -> {
							channel.sendMessage(EmoteReference.ZAP + "You will be missed... or not " + member.getEffectiveName()).queue();
							TextChannelGround.of(event).dropItemWithChance(1, 2);
						},
						error ->
						{
							if (error instanceof PermissionException) {
								channel.sendMessage(EmoteReference.ERROR + "Error banning " + member.getEffectiveName()
									+ ": " + "(No permission provided: " + ((PermissionException) error).getPermission() + ")").queue();
							} else {
								channel.sendMessage(EmoteReference.ERROR + "Unknown error while banning " + member.getEffectiveName()
									+ ": " + "<" + error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();

								//I need more information in the case of an unexpected error.

								LOGGER.warn("Unexpected error while banning someone.", error);
							}
						});
				});
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Ban")
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
					channel.sendMessage(EmoteReference.ERROR2 + "Cannot kick: You have no Kick Members permission.").queue();
					return;
				}

				//If they mentioned a user this gets passed, if they didn't it just doesn't.
				if (receivedMessage.getMentionedUsers().isEmpty()) {
					channel.sendMessage(EmoteReference.ERROR + "You must mention 1 or more users to be kicked!").queue();
					return;
				}

				Member selfMember = guild.getSelfMember();

				//Do I have permissions to kick members, if yes continue, if no end command.
				if (!selfMember.hasPermission(net.dv8tion.jda.core.Permission.KICK_MEMBERS)) {
					channel.sendMessage(EmoteReference.ERROR2 + "Sorry! I don't have permission to kick members in this server!").queue();
					return;
				}

				//For all mentioned users in the command.
				receivedMessage.getMentionedUsers().forEach(user -> {
					Member member = guild.getMember(user);
					if (member == null) return;

					//If one of them is in a higher hierarchy than the bot, cannot kick.
					if (!selfMember.canInteract(member)) {
						channel.sendMessage(EmoteReference.ERROR2 + "Cannot kick member: " + member.getEffectiveName() + ", they are higher or the same " + "hierachy than I am!").queue();
						return;
					}

					//Proceed to kick them. Again, using queue so I don't get rate limited.
					guild.getController().kick(member).queue(
						success -> {
							channel.sendMessage(EmoteReference.ZAP + "You will be missed... or not " + member.getEffectiveName()).queue(); //Quite funny, I think.
							TextChannelGround.of(event).dropItemWithChance(2, 2);
						},
						error -> {
							if (error instanceof PermissionException) {
								channel.sendMessage(String.format(EmoteReference.ERROR + "Error kicking [%s]: (No permission provided: %s)", member.getEffectiveName(), ((PermissionException) error).getPermission())).queue();
							} else {
								channel.sendMessage(String.format(EmoteReference.ERROR + "Unknown error while kicking [%s]: <%s>: %s", member.getEffectiveName(), error.getClass().getSimpleName(), error.getMessage())).queue();

								//Just so I get more info in the case of an unexpected error.
								LOGGER.warn("Unexpected error while kicking someone.", error);
							}
						});
				});
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Kick")
					.setDescription("Kicks the mentioned users.")
					.build();
			}
		});
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
				GuildData guildData = MantaroData.getData().get().getGuild(event.getGuild(), true);

				if (option.equals("resetmoney")) { //TODO DOCUMENT ON HELP
					guildData.users.clear();
					MantaroData.getData().save();
					event.getChannel().sendMessage(EmoteReference.CORRECT + " Local Guild Money cleared.").queue();
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
						guildData.logChannel = id;
						MantaroData.getData().save();
						event.getChannel().sendMessage(String.format(EmoteReference.MEGA + "Message logging enabled on this server with parameters -> ``Channel #%s (%s)``",
							logChannel, id)).queue();
						return;
					}

					if (action.equals("disable")) {
						guildData.logChannel = null;
						MantaroData.getData().save();
						event.getChannel().sendMessage(EmoteReference.MEGA + "Message logging disabled on this server.").queue();
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
						MantaroData.getData().save();
						event.getChannel().sendMessage(EmoteReference.MEGA + "Guild custom prefix set to " + prefix).queue();
						return;
					}

					if (action.equals("clear")) {
						guildData.prefix = null;
						MantaroData.getData().save();
						event.getChannel().sendMessage(EmoteReference.MEGA + "Guild custom prefix disabled	").queue();
						return;
					}
					onHelp(event);
					return;
				}

				if (option.equals("nsfw")) {
					if (action.equals("toggle")) {
						if(guildData.unsafeChannels.contains(event.getChannel().getId())){
							guildData.unsafeChannels.remove(event.getChannel().getId());
							event.getChannel().sendMessage(EmoteReference.CORRECT + "NSFW in this channel has been disabled").queue();
							MantaroData.getData().save();
							return;
						}

						guildData.unsafeChannels.add(event.getChannel().getId());
						MantaroData.getData().save();
						event.getChannel().sendMessage(EmoteReference.CORRECT + "NSFW in this channel has been enabled.").queue();
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
						try {
							String channel = args[2];
							String role = args[3];

							boolean isId = channel.matches("^[0-9]*$");
							String channelId = isId ? channel : event.getGuild().getTextChannelsByName(channel, true).get(0).getId();
							String roleId = event.getGuild().getRolesByName(role.replace(channelId, ""), true).get(0).getId();
							guildData.birthdayChannel = channelId;
							guildData.birthdayRole = roleId;
							MantaroData.getData().save();
							event.getChannel().sendMessage(
								String.format(EmoteReference.MEGA + "Birthday logging enabled on this server with parameters -> Channel: ``#%s (%s)`` and role: ``%s (%s)``",
									channel, channelId, role, roleId)).queue();
							return;
						} catch (Exception e) {
							if (e instanceof IndexOutOfBoundsException) {
								event.getChannel().sendMessage(EmoteReference.ERROR + "Nothing found on channel or role.\n " +
									"**Remember, you don't have to mention neither the role or the channel, rather just type its name, order is <channel> <role>, without the leading \"<>\".**")
									.queue();
								return;
							}
							event.getChannel().sendMessage(EmoteReference.ERROR + "Wrong command arguments.").queue();
							onHelp(event);
							return;
						}
					}

					if (action.equals("disable")) {
						guildData.birthdayChannel = null;
						guildData.birthdayRole = null;
						MantaroData.getData().save();
						event.getChannel().sendMessage(EmoteReference.MEGA + "Birthday logging disabled on this server").queue();
						return;
					}

					onHelp(event);
					return;
				}

				if (option.equals("music")) {
					if (action.equals("limit")) {
						boolean isNumber = args[2].matches("^[0-9]*$");
						if (!isNumber) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "That's not a valid number.").queue();
							return;
						}

						try {
							guildData.songDurationLimit = Integer.parseInt(args[2]);
							MantaroData.getData().save();
							event.getChannel().sendMessage(String.format(EmoteReference.MEGA + "Song duration limit (on ms) on this server is now: %sms.", args[2])).queue();
							return;
						} catch (NumberFormatException e) {
							event.getChannel().sendMessage(EmoteReference.WARNING + "You're trying to set a big af number, silly").queue();
						}
					}

					if (action.equals("queuelimit")) {
						boolean isNumber = args[2].matches("^[0-9]*$");
						if (!isNumber) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "That's not a valid number.").queue();
							return;
						}
						try {
							int finalSize = Integer.parseInt(args[2]);
							int applySize = finalSize >= 300 ? 300 : finalSize;
							guildData.queueSizeLimit = applySize;
							MantaroData.getData().save();
							event.getChannel().sendMessage(String.format(EmoteReference.MEGA + "Queue limit on this server is now **%d** songs.", applySize)).queue();
							return;
						} catch (NumberFormatException e) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "You're trying to set a big af number (which won't be applied anyway), silly").queue();
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
									event.getChannel().sendMessage(EmoteReference.ERROR + "I couldn't found any Voice Channel with that Name or Id").queue();
									return;
								} else if (voiceChannels.size() == 1) {
									channel = voiceChannels.get(0);
									guildData.musicChannel = channel.getId();
									MantaroData.getData().save();
									event.getChannel().sendMessage(EmoteReference.OK + "Music Channel set to: " + channel.getName()).queue();
								} else {
									DiscordUtils.selectList(event, voiceChannels,
										voiceChannel -> String.format("%s (ID: %s)", voiceChannel.getName(), voiceChannel.getId()),
										s -> baseEmbed(event, "Select the Channel:").setDescription(s).build(),
										voiceChannel -> {
											guildData.musicChannel = voiceChannel.getId();
											MantaroData.getData().save();
											event.getChannel().sendMessage(EmoteReference.OK + "Music Channel set to: " + voiceChannel.getName()).queue();
										}
									);
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
						MantaroData.getData().save();
						event.getChannel().sendMessage(EmoteReference.CORRECT + "Now I can play music on all channels!").queue();
						return;
					}

					onHelp(event);
					return;
				}

				if (option.equals("admincustom")) {
					try {
						guildData.customCommandsAdminOnly = Boolean.parseBoolean(action);
						MantaroData.getData().save();
						String toSend = EmoteReference.CORRECT + (Boolean.parseBoolean(action) ? "``Permission -> Now user command creation is admin only.``" : "``Permission -> Now user command creation can be done by users.``");
						event.getChannel().sendMessage(toSend).queue();
						return;
					} catch (Exception e) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "Not a boolean value.").queue();
						return;
					}
				}

				if (option.equals("localmoney")) { //TODO DOCUMENT ON HELP
					try {
						guildData.localMode = Boolean.parseBoolean(action);
						MantaroData.getData().save();
						String toSend = EmoteReference.CORRECT + (guildData.localMode ? "``Money -> Now money on this guild is localized.``" : "``Permission -> Now money on this guild is shared with global.``");
						event.getChannel().sendMessage(toSend).queue();
						return;
					} catch (Exception e) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "Not a boolean value.").queue();
						return;
					}
				}

				if (option.equals("autorole")) {
					if (action.equals("set")) {
						String name = content.replace(option + " " + action + " ", "");
						List<Role> roles = event.getGuild().getRolesByName(name, true);
						StringBuilder b = new StringBuilder();

						if (roles.isEmpty()) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "We didn't find any roles with that name").queue();
							return;
						}

						if(roles.size() <= 1){
							MantaroData.getData().get().getGuild(event.getGuild(), true).autoRole = roles.get(0).getId();
							event.getMessage().addReaction("\ud83d\udc4c").queue();
							MantaroData.getData().save();
							event.getChannel().sendMessage(EmoteReference.CORRECT + "Autorole now set to role: **" + roles.get(0).getName() + "** (Position: " + roles.get(0).getPosition() + ")").queue();
							return;
						}

						for (int i = 0; i < 5 && i < roles.size(); i++) {
							Role role = roles.get(i);
							if (role != null)
								b.append('[').append(i + 1).append("] ").append(role.getName()).append(" | Position: ").append(role.getPosition()).append("\n");
						}

						event.getChannel().sendMessage(new EmbedBuilder().setTitle("Selection", null).setDescription(b.toString()).build()).queue();

						IntConsumer roleSelector = (c) -> {
							MantaroData.getData().get().getGuild(event.getGuild(), true).autoRole = roles.get(c - 1).getId();
							event.getMessage().addReaction("\ud83d\udc4c").queue();
							MantaroData.getData().save();
							event.getChannel().sendMessage(EmoteReference.OK + "Autorole now set to role: **" + roles.get(c - 1).getName() + "** (Position: " + roles.get(c - 1).getPosition() + ")").queue();
						};

						DiscordUtils.selectInt(event, roles.size() + 1, roleSelector);
						return;

					} else if (action.equals("unbind")) {
						MantaroData.getData().get().getGuild(event.getGuild(), true).autoRole = null;
						event.getChannel().sendMessage(EmoteReference.OK + "Autorole resetted.").queue();
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
					.addField("Description", "This command allows you to set different customizable options for your guild instance of the bot.\n" +
						"All values set here are local, that means, they only take effect on your server and not on other " +
						"servers the bot might be on.", false)
					.setDescription("Usage\n" +
						"~>opts logs enable <channel> - Enables logs to the specified channel (use the name).\n" +
						"~>opts logs disable - Disables server-wide logs.\n" +
						"~>opts prefix set <prefix> - Sets a custom prefix for your server.\n" +
						"~>opts prefix clear - Resets your server custom prefix.\n" +
						"~>opts nsfw toggle - Toggles NSFW usage for this channel to allow usage with explicit images in yandere and other commands.\n" +
						"~>opts birthday enable <channel> <role> - Enables birthday monitoring in your server. Arguments such as channel and role don't accept spaces.\n" +
						"~>opts birthday disable - Disables birthday monitoring.\n" +
						"~>opts music limit <ms> - Changes the music lenght limit.\n" +
						"~>opts music queuelimit <number> - Changes the queue song limit (max is 300 regardless).\n" +
						"~>opts autorole set <role> - Sets the new autorole which will be assigned to users on user join.\n" +
						"~>opts autorole unbind - Clears the autorole config.\n" +
						"~>opts music channel <channel> - If set, mantaro will connect only to the specified channel. It might be the name or the ID.\n" +
						"~>opts music clear - If set, mantaro will connect to any music channel the user who called the bot is on if nobody did it already.\n" +
						"~>opts admincustom <true/false> - If set to true, custom commands will only be avaliable for admin creation, otherwise everyone can do it. It defaults to false.")
					.build();
			}
		});
	}

	private void prune() {
		super.register("prune", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				TextChannel channel = event.getChannel();

				if (content.isEmpty()) {
					channel.sendMessage(EmoteReference.ERROR + "No messages to prune.").queue();
					return;
				}

				channel.getHistory().retrievePast(Math.min(Integer.parseInt(content), 100)).queue(
					messageHistory -> channel.deleteMessages(messageHistory).queue(
						success -> channel.sendMessage(EmoteReference.PENCIL + "Successfully pruned " + messageHistory.size() + " messages").queue(),
						error -> {
							if (error instanceof PermissionException) {
								PermissionException pe = (PermissionException) error;
								channel.sendMessage(EmoteReference.ERROR + "Lack of permission while pruning messages" +
									"(No permission provided: " + pe.getPermission() + ")").queue();
							} else {
								channel.sendMessage(EmoteReference.ERROR + "Unknown error while pruning messages" + "<"
									+ error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();
								error.printStackTrace();
							}
						}),
					error -> {
						channel.sendMessage(EmoteReference.ERROR + "Unknown error while retrieving the history to prune the messages" + "<"
							+ error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();
						error.printStackTrace();
					}
				);
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.ADMIN;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Prune command")
					.setDescription("Prunes a specific amount of messages.")
					.addField("Usage", "~>prune <x> - Prunes messages", false)
					.addField("Parameters", "x = number of messages to delete", false)
					.build();
			}
		});
	}
}
