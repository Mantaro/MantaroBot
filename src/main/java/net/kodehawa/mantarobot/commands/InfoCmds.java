package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.commands.audio.MantaroAudioManager;
import net.kodehawa.mantarobot.core.listeners.MantaroListener;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.*;
import net.kodehawa.mantarobot.utils.Utils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.lang.management.ManagementFactory;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static net.kodehawa.mantarobot.commands.info.AsyncInfoMonitor.*;
import static net.kodehawa.mantarobot.commands.info.CommandStatsManager.*;
import static net.kodehawa.mantarobot.commands.info.HelpUtils.forType;

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

		avatar();
		about();
		guildinfo();
		help();
		ping();
		usageinfo();
		userinfo();
		cmdstats();
	}

	private void about() {
		super.register("about", new SimpleCommand() {
			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (content.equals("stats")) {
					Function<ToIntFunction<Guild>, IntStream> guildToInt = f -> event.getJDA().getGuilds().stream().mapToInt(f);

					int minUG = guildToInt.apply(value -> value.getMembers().size()).min().orElse(0);
					double avgUG = guildToInt.apply(value -> value.getMembers().size()).average().orElse(0);
					int maxUG = guildToInt.apply(value -> value.getMembers().size()).max().orElse(0);

					int minOG = guildToInt.apply(value -> (int) value.getMembers().stream().filter(member -> !member.getOnlineStatus().equals(OnlineStatus.OFFLINE)).count()).min().orElse(0);
					double avgOG = guildToInt.apply(value -> (int) value.getMembers().stream().filter(member -> !member.getOnlineStatus().equals(OnlineStatus.OFFLINE)).count()).average().orElse(0);
					int maxOG = guildToInt.apply(value -> (int) value.getMembers().stream().filter(member -> !member.getOnlineStatus().equals(OnlineStatus.OFFLINE)).count()).max().orElse(0);

					List<Double> UOG = event.getJDA().getGuilds().stream().map(value -> (double) value.getMembers().stream().filter(member -> !member.getOnlineStatus().equals(OnlineStatus.OFFLINE)).count() / (double) value.getMembers().size() * 100).collect(Collectors.toList());
					double minUOG = UOG.stream().mapToDouble(Double::doubleValue).min().orElse(0);
					double avgUOG = UOG.stream().mapToDouble(Double::doubleValue).average().orElse(0);
					double maxUOG = UOG.stream().mapToDouble(Double::doubleValue).max().orElse(0);

					List<Double> LUG = event.getJDA().getVoiceChannels().stream().filter(voiceChannel -> voiceChannel.getMembers().contains(
							voiceChannel.getGuild().getSelfMember())).map(value -> (double) value.getMembers().size() /
							(double) value.getGuild().getMembers().size() * 100).collect(Collectors.toList());
					double minLUG = LUG.stream().mapToDouble(Double::doubleValue).min().orElse(0);
					double avgLUG = LUG.stream().mapToDouble(Double::doubleValue).average().orElse(0);
					double maxLUG = LUG.stream().mapToDouble(Double::doubleValue).max().orElse(0);

					List<Double> LOG = event.getJDA().getVoiceChannels().stream().filter(voiceChannel -> voiceChannel.getMembers().contains(
							voiceChannel.getGuild().getSelfMember())).map(value -> (double) value.getMembers().size() /
							(double) value.getGuild().getMembers().stream().filter(member -> !member.getOnlineStatus().equals(OnlineStatus.OFFLINE)).count()
							* 100).collect(Collectors.toList());
					double minLOG = LOG.stream().mapToDouble(Double::doubleValue).min().orElse(0);
					double avgLOG = LOG.stream().mapToDouble(Double::doubleValue).average().orElse(0);
					double maxLOG = LOG.stream().mapToDouble(Double::doubleValue).max().orElse(0);

					int minTG = guildToInt.apply(value -> value.getTextChannels().size()).min().orElse(0);
					double avgTG = guildToInt.apply(value -> value.getTextChannels().size()).average().orElse(0);
					int maxTG = guildToInt.apply(value -> value.getTextChannels().size()).max().orElse(0);

					int minVG = guildToInt.apply(value -> value.getVoiceChannels().size()).min().orElse(0);
					double avgVG = guildToInt.apply(value -> value.getVoiceChannels().size()).average().orElse(0);
					int maxVG = guildToInt.apply(value -> value.getVoiceChannels().size()).max().orElse(0);

					int c = (int) event.getJDA().getVoiceChannels().stream().filter(voiceChannel -> voiceChannel.getMembers().contains(
							voiceChannel.getGuild().getSelfMember())).count();
					double cG = (double) c / (double) event.getJDA().getGuilds().size() * 100;

					event.getChannel().sendMessage(
						new EmbedBuilder()
							.setColor(Color.PINK)
							.setAuthor("Mantaro Statistics", "https://github.com/Kodehawa/MantaroBot/", "https://puu.sh/suxQf/e7625cd3cd.png")
							.setThumbnail("https://puu.sh/suxQf/e7625cd3cd.png")
							.setDescription("Well... I did my maths!")
							.addField("Users per Guild", String.format(Locale.ENGLISH, "Min: %d\nAvg: %.1f\nMax: %d", minUG, avgUG, maxUG), true)
							.addField("Online Users per Guild", String.format(Locale.ENGLISH, "Min: %d\nAvg: %.1f\nMax: %d", minOG, avgOG, maxOG), true)
							.addField("Online Users per Users per Guild", String.format(Locale.ENGLISH, "Min: %.1f%%\nAvg: %.1f%%\nMax: %.1f%%", minUOG, avgUOG, maxUOG), true)
							.addField("Text Channels per Guild", String.format(Locale.ENGLISH, "Min: %d\nAvg: %.1f\nMax: %d", minTG, avgTG, maxTG), true)
							.addField("Voice Channels per Guild", String.format(Locale.ENGLISH, "Min: %d\nAvg: %.1f\nMax: %d", minVG, avgVG, maxVG), true)
							.addField("Music Listeners per Users per Guild", String.format(Locale.ENGLISH, "Min: %.1f%%\nAvg: %.1f%%\nMax: %.1f%%", minLUG, avgLUG, maxLUG), true)
							.addField("Music Listeners per Online Users per Guild", String.format(Locale.ENGLISH, "Min: %.1f%%\nAvg: %.1f%%\nMax: %.1f%%", minLOG, avgLOG, maxLOG), true)
							.addField("Music Connections per Guilds", String.format(Locale.ENGLISH, "%.1f%% (%d Connections)", cG, c), true)
							.addField("Total queue size", Integer.toString(MantaroAudioManager.getTotalQueueSize()), true)
							.build()
					).queue();
					return;
				}

				long millis = ManagementFactory.getRuntimeMXBean().getUptime();

				event.getChannel().sendMessage(new EmbedBuilder()
					.setColor(Color.PINK)
					.setAuthor("About Mantaro", "https://github.com/Kodehawa/MantaroBot/", "https://puu.sh/suxQf/e7625cd3cd.png")
					.setThumbnail("https://puu.sh/suxQf/e7625cd3cd.png")
					.setDescription("Hello, I'm **MantaroBot**! I'm here to make your life a little easier. To get started, type `~>help`!\n" +
						"Some of my features include:\n" +
						"\u2713 Moderation made easy (``Mass kick/ban, prune commands, logs and more!``)\n" +
						"\u2713 Funny and useful commands see `~>help anime` or `~>help action` for examples.\n" +
						"\u2713 Extensive support!"
					)
					.addField("MantaroBot Version", MantaroInfo.VERSION, false)
					.addField("Uptime", String.format(
						"%02d hrs, %02d min, %02d sec",
						MILLISECONDS.toHours(millis),
						MILLISECONDS.toMinutes(millis) - MINUTES.toSeconds(MILLISECONDS.toHours(millis)),
						MILLISECONDS.toSeconds(millis) - MINUTES.toSeconds(MILLISECONDS.toMinutes(millis))
					), true)
					.addField("Threads", String.valueOf(Thread.activeCount()), true)
					.addField("Guilds", String.valueOf(event.getJDA().getGuilds().size()), true)
					.addField("Users (Online/Unique)", event.getJDA().getGuilds().stream().flatMap(g -> g.getMembers().stream()).filter(u -> !u.getOnlineStatus().equals(OnlineStatus.OFFLINE)).count() + "/" + event.getJDA().getUsers().size(), true)
					.addField("Text Channels", String.valueOf(event.getJDA().getTextChannels().size()), true)
					.addField("Voice Channels", String.valueOf(event.getJDA().getVoiceChannels().size()), true)
					.setFooter(String.format("Invite link: https://is.gd/mantaro (Commands this session: %s | Logs this session: %s)", MantaroListener.getCommandTotal(), MantaroListener.getLogTotal()), null)
					.build()
				).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "About Command")
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
					event.getChannel().sendMessage(String.format("Avatar for: **%s**\n%s", event.getMessage().getMentionedUsers().get(0).getName(), event.getMessage().getMentionedUsers().get(0).getAvatarUrl())).queue();
					return;
				}
				event.getChannel().sendMessage(String.format("Avatar for: **%s**\n%s", event.getAuthor().getName(), event.getAuthor().getAvatarUrl())).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Avatar")
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
				return baseEmbed(event, "Command stats")
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
				return baseEmbed(event, "GuildInfo Command")
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
						.setFooter(String.format("To check the command usage do %shelp <command>", prefix) + " >> Commands: " + Manager.commands.size(), null)
						.addField("Audio Commands:", forType(Category.AUDIO), false)
						.addField("Custom Commands:", forType(Category.CUSTOM), false)
						.addField("Action Commands:", forType(Category.ACTION), false)
						.addField("Fun Commands:", forType(Category.FUN), false)
						.addField("Gif Commands:", forType(Category.GIF), false)
						.addField("Gaming Commands:", forType(Category.GAMES), false);

					if (event.getMember().hasPermission(Permission.ADMINISTRATOR))
						embed.addField("Moderation Commands:", forType(Category.MODERATION), false);

					if (MantaroData.getConfig().get().isOwner(event.getMember()))
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
							event.getChannel().sendMessage("\u274C No extended help set for this command.").queue();
					} else {
						event.getChannel().sendMessage("\u274C This command doesn't exist.").queue();
					}
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Help Command")
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
					event.getChannel().sendMessage("\uD83D\uDCE3 The ping is " + ping + " ms, " + ratePing(ping)).queue();
				});
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Ping Command")
					.addField("Description:", "Plays Ping-Pong with Discord and prints out the result.", false)
					.build();
			}
		});
	}

	private void usageinfo() {
		start();
		super.register("usageinfo", new SimpleCommand() {
			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public void call(String[] args, String content, GuildMessageReceivedEvent event) {
				event.getChannel().sendMessage(new EmbedBuilder()
					.setAuthor("MantaroBot information", null, "https://puu.sh/sMsVC/576856f52b.png")
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
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "UsageInfo Command")
					.addField("Description:", "Sends the current Bot Hardware Usage.", false)
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
					event.getChannel().sendMessage("Sorry but I couldn't get the Info for the user " + name + ". Please make sure you and the user are in the same guild.").queue();
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
				return baseEmbed(event, "UserInfo Command")
					.addField("Description:", "Sends the information about a specific user.", false)
					.addField("Usage:", "`~>userinfo [@userMention]`: Returns information about the specific user.\n`~>userinfo`: Returns information about who issued the command.", false)
					.build();
			}

		});
	}
}
