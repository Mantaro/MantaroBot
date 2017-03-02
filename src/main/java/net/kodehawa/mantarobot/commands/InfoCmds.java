package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.commands.currency.CurrencyManager;
import net.kodehawa.mantarobot.commands.currency.inventory.TextChannelGround;
import net.kodehawa.mantarobot.commands.info.StatsHelper.CalculatedDoubleValues;
import net.kodehawa.mantarobot.commands.info.StatsHelper.CalculatedIntValues;
import net.kodehawa.mantarobot.core.listeners.MantaroListener;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.*;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.lang.management.ManagementFactory;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static net.kodehawa.mantarobot.commands.info.AsyncInfoMonitor.*;
import static net.kodehawa.mantarobot.commands.info.CommandStatsManager.*;
import static net.kodehawa.mantarobot.commands.info.HelpUtils.forType;
import static net.kodehawa.mantarobot.commands.info.StatsHelper.calculateDouble;
import static net.kodehawa.mantarobot.commands.info.StatsHelper.calculateInt;

public class InfoCmds extends Module {
	public static Logger LOGGER = LoggerFactory.getLogger("InfoCmds");

	private static String ratePing(long ping) {
		if (ping <= 0) return "which doesn't even make any sense at all. :upside_down:";
		if (ping <= 10) return "which is faster than Sonic. :smiley:";
		if (ping <= 100) return "which is great! :smiley:";
		if (ping <= 200) return "which is nice! :slight_smile:";
		if (ping <= 300) return "which is decent. :neutral_face:";
		if (ping <= 400) return "which is average... :confused:";
		if (ping <= 500) return "which is slightly slow. :slight_frown:";
		if (ping <= 600) return "which is kinda slow.. :frowning2:";
		if (ping <= 700) return "which is slow.. :worried:";
		if (ping <= 800) return "which is too slow. :disappointed:";
		if (ping <= 800) return "which is awful. :weary:";
		if (ping <= 900) return "which is bad. :sob: (helpme)";
		if (ping <= 1600) return "which is because Discord is lagging. :angry:";
		if (ping <= 10000) return "which makes less sense than 0 ping. :thinking:";
		return "which is slow af. :dizzy_face:";
	}

	public InfoCmds() {
		super(Category.INFO);
		start();

		avatar();
		about();
		guildinfo();
		help();
		ping();
		userinfo();
		cmdstats();
		stats();
	}

	private void about() {
		super.register("about", new SimpleCommand() {
			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				List<Guild> guilds = event.getJDA().getGuilds();
				List<TextChannel> textChannels = event.getJDA().getTextChannels();
				List<VoiceChannel> voiceChannels = event.getJDA().getVoiceChannels();
				long millis = ManagementFactory.getRuntimeMXBean().getUptime();

				event.getChannel().sendMessage(new EmbedBuilder()
					.setColor(Color.PINK)
					.setAuthor("About Mantaro", "http://polr.me/mantaro", "https://puu.sh/suxQf/e7625cd3cd.png")
					.setThumbnail("https://puu.sh/suxQf/e7625cd3cd.png")
					.setDescription("Hello, I'm **MantaroBot**! I'm here to make your life a little easier. To get started, type `~>help`!\n" +
						"Some of my features include:\n" +
						"\u2713 Moderation made easy (``Mass kick/ban, prune commands, logs and more!``)\n" +
						"\u2713 Funny and useful commands see `~>help anime` or `~>help action` for examples.\n" +
						"\u2713 [Extensive support](https://discordapp.com/invite/cMTmuPa)!"
					)
					.addField("MantaroBot Version", MantaroInfo.VERSION, false)
					.addField("Uptime", String.format(
						"%02d hrs, %02d min, %02d sec",
						MILLISECONDS.toHours(millis),
						MILLISECONDS.toMinutes(millis) - MINUTES.toSeconds(MILLISECONDS.toHours(millis)),
						MILLISECONDS.toSeconds(millis) - MINUTES.toSeconds(MILLISECONDS.toMinutes(millis))
					), true)
					.addField("Threads", String.valueOf(Thread.activeCount()), true)
					.addField("Guilds", String.valueOf(guilds.size()), true)
					.addField("Users (Online/Unique)", guilds.stream().flatMap(g -> g.getMembers().stream()).filter(u -> !u.getOnlineStatus().equals(OnlineStatus.OFFLINE)).count() + "/" + event.getJDA().getUsers().size(), true)
					.addField("Text Channels", String.valueOf(textChannels.size()), true)
					.addField("Voice Channels", String.valueOf(voiceChannels.size()), true)
					.setFooter(String.format("Invite link: http://polr.me/mantaro (Commands this session: %s | Logs this session: %s)", MantaroListener.getCommandTotal(), MantaroListener.getLogTotal()), null)
					.build()
				).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "About Command")
					.addField("Description:", "Sends a message of what the bot is.", false)
					.setColor(Color.PINK)
					.build();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

		});
	}

	private void avatar() {
		super.register("avatar", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (!event.getMessage().getMentionedUsers().isEmpty()) {
					event.getChannel().sendMessage(String.format(EmoteReference.OK + "Avatar for: **%s**\n%s", event.getMessage().getMentionedUsers().get(0).getName(), event.getMessage().getMentionedUsers().get(0).getAvatarUrl())).queue();
					return;
				}
				event.getChannel().sendMessage(String.format("Avatar for: **%s**\n%s", event.getAuthor().getName(), event.getAuthor().getAvatarUrl())).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Avatar")
					.setDescription("Gets your user avatar")
					.addField("Usage",
						"~>avatar - Gets your avatar url" +
							"\n ~>avatar <mention> - Gets a user's avatar url.", false)
					.build();
			}
		});
	}

	private void cmdstats() {
		super.register("cmdstats", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (args.length != 0) {
					String what = args[0];
					if (what.equals("total")) {
						event.getChannel().sendMessage(fillEmbed(TOTAL_CMDS, baseEmbed(event, "Command Stats | Total")).build()).queue();
						return;
					}

					if (what.equals("daily")) {
						event.getChannel().sendMessage(fillEmbed(DAY_CMDS, baseEmbed(event, "Command Stats | Daily")).build()).queue();
						return;
					}

					if (what.equals("hourly")) {
						event.getChannel().sendMessage(fillEmbed(HOUR_CMDS, baseEmbed(event, "Command Stats | Hourly")).build()).queue();
						return;
					}

					if (what.equals("now")) {
						event.getChannel().sendMessage(fillEmbed(MINUTE_CMDS, baseEmbed(event, "Command Stats | Now")).build()).queue();
						return;
					}
				}

				//Default
				event.getChannel().sendMessage(baseEmbed(event, "Command Stats")
					.addField("Now", resume(MINUTE_CMDS), false)
					.addField("Hourly", resume(HOUR_CMDS), false)
					.addField("Daily", resume(DAY_CMDS), false)
					.addField("Total", resume(TOTAL_CMDS), false)
					.build()
				).queue();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Command stats")
					.addField("Description", "Shows the statistics of the commands that has been run on this bot for its uptime.", false)
					.addField("Usage",
						"~>cmdstats - Shows all command statistics.\n"
							+ "~>cmdstats now - Shows commands run in the last minute except for this one.\n"
							+ "~>cmdstats total - Shows commands run in the bot's uptime\n"
							+ "~>cmdstats daily - Shows commands statistics of today.\n"
							+ "~>cmdstats hourly- Shows commands statistics of the last hour.\n"
						, false)
					.build();
			}
		});
	}

	private void guildinfo() {
		super.register("guildinfo", new SimpleCommand() {
			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				Guild guild = event.getGuild();
				TextChannel channel = event.getChannel();

				String roles = guild.getRoles().stream()
					.filter(role -> !guild.getPublicRole().equals(role))
					.map(Role::getName)
					.collect(Collectors.joining(", "));

				if (roles.length() > EmbedBuilder.TEXT_MAX_LENGTH)
					roles = roles.substring(0, EmbedBuilder.TEXT_MAX_LENGTH - 4) + "...";

				channel.sendMessage(new EmbedBuilder()
					.setAuthor("Guild Information", null, guild.getIconUrl())
					.setColor(guild.getOwner().getColor() == null ? Color.ORANGE : guild.getOwner().getColor())
					.setDescription("Guild information for server " + guild.getName())
					.setThumbnail(guild.getIconUrl())
					.addField("Users (Online/Unique)", (int) guild.getMembers().stream().filter(u -> !u.getOnlineStatus().equals(OnlineStatus.OFFLINE)).count() + "/" + guild.getMembers().size(), true)
					.addField("Main Channel", "#" + guild.getPublicChannel().getName(), true)
					.addField("Creation Date", guild.getCreationTime().format(DateTimeFormatter.ISO_DATE_TIME).replaceAll("[^0-9.:-]", " "), true)
					.addField("Voice/Text Channels", guild.getVoiceChannels().size() + "/" + guild.getTextChannels().size(), true)
					.addField("Owner", guild.getOwner().getUser().getName() + "#" + guild.getOwner().getUser().getDiscriminator(), true)
					.addField("Region", guild.getRegion().getName(), true)
					.addField("Roles (" + guild.getRoles().size() + ")", roles, false)
					.setFooter("Server ID: " + String.valueOf(guild.getId()), null)
					.build()
				).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "GuildInfo Command")
					.addField("Description:", "Sends the current Guild information.", false)
					.setColor(event.getGuild().getOwner().getColor() == null ? Color.ORANGE : event.getGuild().getOwner().getColor())
					.build();
			}
		});
	}

	private void help() {
		Random r = new Random();
		List<String> jokes = Collections.unmodifiableList(Arrays.asList(
			"Yo damn I heard you like help, because you just issued the help command to get the help about the help command.",
			"Congratulations, you managed to use the help command.",
			"Helps you to help yourself.",
			"Help Inception.",
			"A help helping helping helping help."
		));
		super.register("help", new SimpleCommand() {

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (content.isEmpty()) {
					String defaultPrefix = MantaroData.getData().get().defaultPrefix, guildPrefix = MantaroData.getData().get().getGuild(event.getGuild(), false).prefix;
					String prefix = guildPrefix == null ? defaultPrefix : guildPrefix;

					EmbedBuilder embed = baseEmbed(event, "MantaroBot Help")
						.setColor(Color.PINK)
						.setDescription("Command help. For extended usage please use " + String.format("%shelp <command>.", prefix))
						.setFooter(String.format("To check the command usage do %shelp <command> // -> Commands: " +
								Manager.commands.entrySet().stream().filter(
									(command) -> !command.getValue().getKey().isHiddenFromHelp()).count()
							, prefix), null)
						.addField("Music Commands:", forType(Category.MUSIC), false)
						.addField("Custom Commands:", forType(Category.CUSTOM), false)
						.addField("Action Commands:", forType(Category.ACTION), false)
						.addField("Fun Commands:", forType(Category.FUN), false)
						.addField("Currency Commands:", forType(Category.CURRENCY), false)
						.addField("Gif Commands:", forType(Category.GIF), false)
						.addField("Gaming Commands:", forType(Category.GAMES), false);

					if (CommandPermission.ADMIN.test(event.getMember()))
						embed.addField("Moderation Commands:", forType(Category.MODERATION), false);

					if (CommandPermission.BOT_OWNER.test(event.getMember()))
						embed.addField("Owner Commands:", forType(Category.OWNER), false);

					event.getChannel().sendMessage(embed
						.addField("Info Commands:", forType(Category.INFO), false)
						.addField("Misc Commands:", forType(Category.MISC), false)
						.build()
					).queue();

				} else {
					Pair<Command, Category> command = Manager.commands.get(content);

					if (command != null && command.getValue() != null) {
						final MessageEmbed help = command.getKey().help(event);
						Optional.ofNullable(help).ifPresent((help1) -> event.getChannel().sendMessage(help1).queue());
						if (help == null)
							event.getChannel().sendMessage(EmoteReference.ERROR + "No extended help set for this command.").queue();
					} else {
						event.getChannel().sendMessage(EmoteReference.ERROR + "This command doesn't exist.").queue();
					}
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Help Command")
					.setColor(Color.PINK)
					.addField("Description:", jokes.get(r.nextInt(jokes.size())), false)
					.addField(
						"Usage:",
						"`~>help`: Returns information about who issued the command.\n~>help [command]`: Returns information about the specific command.",
						false
					).build();
			}
		});
	}

	private void ping() {
		super.register("ping", new SimpleCommand() {
			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				long start = System.currentTimeMillis();
				event.getChannel().sendTyping().queue(v -> {
					long ping = System.currentTimeMillis() - start;
					event.getChannel().sendMessage(EmoteReference.MEGA + "The ping is " + ping + " ms, " + ratePing(ping)).queue();
					TextChannelGround.of(event).dropItemWithChance(5, 5);
				});
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Ping Command")
					.addField("Description:", "Plays Ping-Pong with Discord and prints out the result.", false)
					.build();
			}
		});
	}

	private void stats() {
		super.register("stats", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (content.isEmpty()) {
					List<Guild> guilds = event.getJDA().getGuilds();

					List<VoiceChannel> voiceChannels = event.getJDA().getVoiceChannels();
					List<VoiceChannel> musicChannels = voiceChannels.parallelStream().filter(vc -> vc.getMembers().contains(vc.getGuild().getSelfMember())).collect(Collectors.toList());

					CalculatedIntValues usersPerGuild = calculateInt(guilds, value -> value.getMembers().size());
					CalculatedIntValues onlineUsersPerGuild = calculateInt(guilds, value -> (int) value.getMembers().stream().filter(member -> !member.getOnlineStatus().equals(OnlineStatus.OFFLINE)).count());
					CalculatedDoubleValues onlineUsersPerUserPerGuild = calculateDouble(guilds, value -> (double) value.getMembers().stream().filter(member -> !member.getOnlineStatus().equals(OnlineStatus.OFFLINE)).count() / (double) value.getMembers().size() * 100);
					CalculatedDoubleValues listeningUsersPerUsersPerGuilds = calculateDouble(musicChannels, value -> (double) value.getMembers().size() / (double) value.getGuild().getMembers().size() * 100);
					CalculatedDoubleValues listeningUsersPerOnlineUsersPerGuilds = calculateDouble(musicChannels, value -> (double) value.getMembers().size() / (double) value.getGuild().getMembers().stream().filter(member -> !member.getOnlineStatus().equals(OnlineStatus.OFFLINE)).count() * 100);
					CalculatedIntValues textChannelsPerGuild = calculateInt(guilds, value -> value.getTextChannels().size());
					CalculatedIntValues voiceChannelsPerGuild = calculateInt(guilds, value -> value.getVoiceChannels().size());
					int c = (int) voiceChannels.stream().filter(voiceChannel -> voiceChannel.getMembers().contains(
						voiceChannel.getGuild().getSelfMember())).count();
					double cG = (double) c / (double) guilds.size() * 100;

					event.getChannel().sendMessage(
						new EmbedBuilder()
							.setColor(Color.PINK)
							.setAuthor("Mantaro Statistics", "https://github.com/Kodehawa/MantaroBot/", "https://puu.sh/suxQf/e7625cd3cd.png")
							.setThumbnail("https://puu.sh/suxQf/e7625cd3cd.png")
							.setDescription("Well... I did my maths!")
							.addField("Users per Guild", String.format(Locale.ENGLISH, "Min: %d\nAvg: %.1f\nMax: %d", usersPerGuild.min, usersPerGuild.avg, usersPerGuild.max), true)
							.addField("Online Users per Guild", String.format(Locale.ENGLISH, "Min: %d\nAvg: %.1f\nMax: %d", onlineUsersPerGuild.min, onlineUsersPerGuild.avg, onlineUsersPerGuild.max), true)
							.addField("Online Users per Users per Guild", String.format(Locale.ENGLISH, "Min: %.1f%%\nAvg: %.1f%%\nMax: %.1f%%", onlineUsersPerUserPerGuild.min, onlineUsersPerUserPerGuild.avg, onlineUsersPerUserPerGuild.max), true)
							.addField("Text Channels per Guild", String.format(Locale.ENGLISH, "Min: %d\nAvg: %.1f\nMax: %d", textChannelsPerGuild.min, textChannelsPerGuild.avg, textChannelsPerGuild.max), true)
							.addField("Voice Channels per Guild", String.format(Locale.ENGLISH, "Min: %d\nAvg: %.1f\nMax: %d", voiceChannelsPerGuild.min, voiceChannelsPerGuild.avg, voiceChannelsPerGuild.max), true)
							.addField("Music Listeners per Users per Guild", String.format(Locale.ENGLISH, "Min: %.1f%%\nAvg: %.1f%%\nMax: %.1f%%", listeningUsersPerUsersPerGuilds.min, listeningUsersPerUsersPerGuilds.avg, listeningUsersPerUsersPerGuilds.max), true)
							.addField("Music Listeners per Online Users per Guild", String.format(Locale.ENGLISH, "Min: %.1f%%\nAvg: %.1f%%\nMax: %.1f%%", listeningUsersPerOnlineUsersPerGuilds.min, listeningUsersPerOnlineUsersPerGuilds.avg, listeningUsersPerOnlineUsersPerGuilds.max), true)
							.addField("Music Connections per Guilds", String.format(Locale.ENGLISH, "%.1f%% (%d Connections)", cG, c), true)
							.addField("Total queue size", Long.toString(MantaroBot.getAudioManager().getTotalQueueSize()), true)
							.addField("Total commands (including custom)", String.valueOf(Manager.commands.size()), true)
							.addField("MantaroCredits to USD conversion:", String.format("1 MantaroCredit worth %.2f USD", CurrencyManager.creditsWorth()), true)
							.build()
					).queue();
					TextChannelGround.of(event).dropItemWithChance(4, 5);
					return;
				}

				if (args[0].equals("usage")) {
					event.getChannel().sendMessage(new EmbedBuilder()
						.setAuthor("Mantaro's usage information", null, "https://puu.sh/sMsVC/576856f52b.png")
						.setDescription("Hardware and usage information.")
						.setThumbnail("https://puu.sh/suxQf/e7625cd3cd.png")
						.addField("Threads:", getThreadCount() + " Threads", true)
						.addField("Memory Usage:", getTotalMemory() - getFreeMemory() + "MB/" + getMaxMemory() + "MB", true)
						.addField("CPU Cores:", getAvailableProcessors() + " Cores", true)
						.addField("CPU Usage:", Math.round(getCpuUsage()) + "%", true)
						.addField("Assigned Memory:", getTotalMemory() + "MB", true)
						.addField("Remaining from assigned:", getFreeMemory() + "MB", true)
						.build()
					).queue();
					TextChannelGround.of(event).dropItemWithChance(4, 5);
					return;
				}

				if (args[0].equals("vps")) {
					TextChannelGround.of(event).dropItemWithChance(4, 5);
					EmbedBuilder embedBuilder = new EmbedBuilder()
						.setAuthor("Mantaro's VPS information", null, "https://puu.sh/sMsVC/576856f52b.png")
						.setThumbnail("https://puu.sh/suxQf/e7625cd3cd.png")
						.addField("CPU Usage", String.format("%.2f", getVpsCPUUsage()) + "%", true)
						.addField("RAM (TOTAL/FREE/USED)", String.format("%.2f", getVpsMaxMemory()) + "GB/" + String.format("%.2f", getVpsFreeMemory())
							+ "GB/" + String.format("%.2f", getVpsUsedMemory()) + "GB", false);

					event.getChannel().sendMessage(embedBuilder.build()).queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Statistics command")
					.setDescription("Returns bot, usage or vps statistics")
					.addField("Usage", "~>stats <bot/usage/vps>", true)
					.addField("Parameters", "bot: returns the bot statistics\n" +
						"usage: returns the resources used by the bot\n" +
						"vps: returns how much of the vps resources are used.", true)
					.build();
			}
		});
	}

	private void userinfo() {
		super.register("userinfo", new SimpleCommand() {
			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				User user = event.getMessage().getMentionedUsers().size() > 0 ? event.getMessage().getMentionedUsers().get(0) : event.getAuthor();
				Member member = event.getGuild().getMember(user);
				if (member == null) {
					String name = user == null ? "Unknown User" : user.getName();
					event.getChannel().sendMessage(EmoteReference.ERROR + "Sorry but I couldn't get the Info for the user " + name + ". Please make sure you and the user are in the same guild.").queue();
					return;
				}

				String roles = member.getRoles().stream()
					.map(Role::getName)
					.collect(Collectors.joining(", "));

				if (roles.length() > EmbedBuilder.TEXT_MAX_LENGTH)
					roles = roles.substring(0, EmbedBuilder.TEXT_MAX_LENGTH - 4) + "...";
				event.getChannel().sendMessage(new EmbedBuilder()
					.setColor(member.getColor())
					.setAuthor(String.format("User info for %s#%s", user.getName(), user.getDiscriminator()), null, event.getAuthor().getEffectiveAvatarUrl())
					.setThumbnail(user.getAvatarUrl())
					.addField("Join Date:", member.getJoinDate().format(DateTimeFormatter.ISO_DATE).replace("Z", ""), true)
					.addField("Account Created:", user.getCreationTime().format(DateTimeFormatter.ISO_DATE).replace("Z", ""), true)
					.addField("Voice Channel:", member.getVoiceState().getChannel() != null ? member.getVoiceState().getChannel().getName() : "None", false)
					.addField("Playing:", member.getGame() == null ? "None" : member.getGame().getName(), false)
					.addField("Color:", member.getColor() == null ? "Default" : "#" + Integer.toHexString(member.getColor().getRGB()).substring(2).toUpperCase(), true)
					.addField("Status:", Utils.capitalize(member.getOnlineStatus().getKey().toLowerCase()), true)
					.addField("Roles: [" + String.valueOf(member.getRoles().size()) + "]", roles, true)
					.setFooter("User ID: " + user.getId(), null)
					.build()
				).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "UserInfo Command")
					.addField("Description:", "Sends the information about a specific user.", false)
					.addField("Usage:", "`~>userinfo [@userMention]`: Returns information about the specific user.\n`~>userinfo`: Returns information about who issued the command.", false)
					.build();
			}

		});
	}
}
