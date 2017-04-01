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

public class OptsCmd extends Module {
	private static final Logger LOGGER = LoggerFactory.getLogger("Options");

	public OptsCmd(){
		super(Category.MODERATION);
		opts();
	}

	private void opts(){
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
						guildData.setGuildLogChannel(id);
						dbGuild.saveAsync();
						event.getChannel().sendMessage(String.format(EmoteReference.MEGA + "Message logging enabled on this server with parameters -> ``Channel #%s (%s)``",
								logChannel, id)).queue();
						return;
					}

					if (action.equals("disable")) {
						guildData.setGuildLogChannel(null);
						dbGuild.saveAsync();
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
						guildData.setGuildCustomPrefix(prefix);
						dbGuild.save();
						event.getChannel().sendMessage(EmoteReference.MEGA + "Guild custom prefix set to " + prefix).queue();
						return;
					}

					if (action.equals("clear")) {
						guildData.setGuildCustomPrefix(null);
						dbGuild.save();
						event.getChannel().sendMessage(EmoteReference.MEGA + "Guild custom prefix disabled	").queue();
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

				if(option.equals("devaluation")){
					if (args.length < 1) {
						onHelp(event);
						return;
					}

					if(action.equals("enable")){
						guildData.setRpgDevaluation(true);
						event.getChannel().sendMessage(EmoteReference.ERROR + "Enabled currency devaluation on this server.").queue();
						return;
					}

					if(action.equals("disable")){
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
						guildData.setBirthdayChannel(null);
						guildData.setBirthdayRole(null);
						dbGuild.save();
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
							guildData.setMusicSongDurationLimit(Long.parseLong(args[2]));
							dbGuild.save();
							event.getChannel().sendMessage(String.format(EmoteReference.MEGA + "Song duration limit (on ms) on this server is now: %sms.", args[2])).queue();
							return;
						} catch (NumberFormatException e) {
							event.getChannel().sendMessage(EmoteReference.WARNING + "You're trying to set a big af number, silly").queue();
						}
						return;
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
							guildData.setMusicQueueSizeLimit((long) applySize);
							dbGuild.save();
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
									guildData.setMusicChannel(channel.getId());
									dbGuild.save();
									event.getChannel().sendMessage(EmoteReference.OK + "Music Channel set to: " + channel.getName()).queue();
								} else {
									DiscordUtils.selectList(event, voiceChannels,
											voiceChannel -> String.format("%s (ID: %s)", voiceChannel.getName(), voiceChannel.getId()),
											s -> baseEmbed(event, "Select the Channel:").setDescription(s).build(),
											voiceChannel -> {
												guildData.setMusicChannel(voiceChannel.getId());
												dbGuild.save();
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
						guildData.setMusicSongDurationLimit(null);
						guildData.setMusicChannel(null);
						dbGuild.save();
						event.getChannel().sendMessage(EmoteReference.CORRECT + "Now I can play music on all channels!").queue();
						return;
					}

					onHelp(event);
					return;
				}

				if (option.equals("admincustom")) {
					try {
						guildData.setCustomAdminLock(Boolean.parseBoolean(action));
						dbGuild.save();
						String toSend = EmoteReference.CORRECT + (Boolean.parseBoolean(action) ? "``Permission -> Now user command creation is admin only.``" : "``Permission -> Now user command creation can be done by users.``");
						event.getChannel().sendMessage(toSend).queue();
						return;
					} catch (Exception e) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "Not a boolean value.").queue();
						return;
					}
				}

				if (option.equals("localmoney")) {
					try {
						guildData.setRpgLocalMode(Boolean.parseBoolean(action));
						dbGuild.save();
						String toSend = EmoteReference.CORRECT + (guildData.isRpgLocalMode() ? "``Money -> Now money on this guild is localized.``" : "``Permission -> Now money on this guild is shared with global.``");
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

						if (roles.size() <= 1) {
							guildData.setGuildAutoRole(roles.get(0).getId());
							event.getMessage().addReaction("\ud83d\udc4c").queue();
							dbGuild.save();
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
							guildData.setGuildAutoRole(roles.get(c - 1).getId());
							event.getMessage().addReaction("\ud83d\udc4c").queue();
							dbGuild.save();
							event.getChannel().sendMessage(EmoteReference.OK + "Autorole now set to role: **" + roles.get(c - 1).getName() + "** (Position: " + roles.get(c - 1).getPosition() + ")").queue();
						};

						DiscordUtils.selectInt(event, roles.size() + 1, roleSelector);
						return;

					} else if (action.equals("unbind")) {
						guildData.setGuildAutoRole(null);
						event.getChannel().sendMessage(EmoteReference.OK + "Autorole resetted.").queue();
						return;
					}
				}

				if(option.equals("usermessage")){
					if(action.equals("channel")){
						String channelName = splitArgs(content)[2];
						List<TextChannel> textChannels = event.getGuild().getTextChannels().stream()
								.filter(textChannel -> textChannel.getName().contains(channelName))
								.collect(Collectors.toList());

						if(textChannels.isEmpty()){
							event.getChannel().sendMessage(EmoteReference.ERROR + "There are no channels matching your search query.").queue();
						}

						if (textChannels.size() <= 1) {
							guildData.setLogJoinLeaveChannel(textChannels.get(0).getId());
							dbGuild.save();
							event.getChannel().sendMessage(EmoteReference.CORRECT + "Log Join/Leave Channel set to: **" + textChannels.get(0).getAsMention()).queue();
							return;
						}

						DiscordUtils.selectList(event, textChannels,
								textChannel -> String.format("%s (ID: %s)", textChannel.getName(), textChannel.getId()),
								s -> baseEmbed(event, "Select the Channel:").setDescription(s).build(),
								textChannel -> {
									guildData.setLogJoinLeaveChannel(textChannel.getId());
									dbGuild.save();
									event.getChannel().sendMessage(EmoteReference.OK + "Log Join/Leave Channel set to: " + textChannel.getAsMention()).queue();
								}
						);
						return;
					}

					if(action.equals("joinmessage")){
						String joinMessage = content.replace(args[0] + " " + args[1] + " ", "");
						guildData.setJoinMessage(joinMessage);
						dbGuild.save();
						event.getChannel().sendMessage(EmoteReference.CORRECT + "Server join message set to: " + joinMessage).queue();
						return;
					}

					if(action.equals("leavemessage")){
						String leaveMessage = content.replace(args[0] + " " + args[1] + " ", "");
						guildData.setLeaveMessage(leaveMessage);
						dbGuild.save();
						event.getChannel().sendMessage(EmoteReference.CORRECT + "Server leave message set to: " + leaveMessage).queue();
						return;
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
								"~>opts resetmoney - Resets local money.\n" +
							"~>opts localmoney <true/false> - Toggles guild local mode (currency stats only for your guild).\n" +
								"~>opts music channel <channel> - If set, mantaro will connect only to the specified channel. It might be the name or the ID.\n" +
								"~>opts music clear - If set, mantaro will connect to any music channel the user who called the bot is on if nobody did it already.\n" +
								"~>opts admincustom <true/false> - If set to true, custom commands will only be avaliable for admin creation, otherwise everyone can do it. It defaults to false.\n" +
								"~>opts usermessage channel <channel name> - Sets a channel to send join/leave messages.\n" +
								"~>opts usermessage joinmessage <message> - Sets the join message.\n" +
								"~>opts usermessage leavemessage <message> - Sets the leave message.")
						.build();
			}
		});
	}

}
