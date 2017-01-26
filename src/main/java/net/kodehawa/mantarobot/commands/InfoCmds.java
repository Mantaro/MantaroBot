package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.core.listeners.MantaroListener;
import net.kodehawa.mantarobot.data.Data.GuildData;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.*;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.Color;
import java.lang.management.ManagementFactory;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static net.kodehawa.mantarobot.commands.info.AsyncInfoMonitor.*;
import static net.kodehawa.mantarobot.commands.info.HelpUtils.forType;

public class InfoCmds extends Module {
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

		//Register Commands
		about();
		guildinfo();
		help();
		ping();
		usageinfo();
		userinfo();
	}

	private void about() {
		super.register("about", new SimpleCommand() {
			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				long millis = ManagementFactory.getRuntimeMXBean().getUptime();

				event.getChannel().sendMessage(new EmbedBuilder()
					.setColor(Color.PINK)
					.setAuthor("About Mantaro", "https://github.com/Kodehawa/MantaroBot/", "https://puu.sh/suxQf/e7625cd3cd.png")
					.setThumbnail("https://puu.sh/suxQf/e7625cd3cd.png")
					.setDescription("Hello, I'm **MantaroBot**! I'm here to make your life a little easier. To get started, type `~>help`!\n" +
						"Some of my features include:\n" +
						"\u2713 Moderation made easy (``Mass kick/ban, prune commands, logs and more!``)\n" +
						"\u2713 Funny and useful commands ``see `~>help anime` or `~>help action` for examples``.	\n" +
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
					.setFooter("Invite link: https://is.gd/mantaro (Commands this session: " + MantaroListener.getCommandTotal() + " | Logs this session: " + MantaroListener.getLogTotal() + ")", null)
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
		});
	}

	private void guildinfo() {
		super.register("guildinfo", new SimpleCommand() {
			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
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
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				if (content.isEmpty()) {
					String defaultPrefix = MantaroData.getData().get().defaultPrefix, guildPrefix = MantaroData.getData().get().guilds.getOrDefault(event.getGuild().getId(), new GuildData()).prefix;
					String prefix = guildPrefix == null ? defaultPrefix : guildPrefix;

					event.getChannel().sendMessage(baseEmbed(event, "MantaroBot Help")
						.setColor(Color.PINK)
						.setDescription(String.format("(For extended help about a command run `%shelp <command>)`", prefix))
						.addField("Audio Commands:", forType(Category.AUDIO), false)
						.addField("Custom Commands:", forType(Category.CUSTOM), false)
						.addField("Action Commands:", forType(Category.ACTION), false)
						.addField("Fun Commands:", forType(Category.FUN), false)
						.addField("Gaming Commands:", forType(Category.GAMES), false)
						.addField("Moderation Commands:", forType(Category.MODERATION), false)
						.addField("Info Commands:", forType(Category.INFO), false)
						.addField("Misc Commands:", forType(Category.MISC), false)
						.setFooter(String.format("To check the command usage do %shelp <command>", prefix), null)
						.build()
					).queue();
				} else {
					Pair<Command, Category> command = Manager.commands.get(content);
					if (command != null && command.getValue() != null) {
						MessageEmbed help = command.getKey().help(event);
						if (help != null) {
							event.getChannel().sendMessage(help).queue();
						} else {
							event.getChannel().sendMessage("\u274C No extended help set for this command.").queue();
						}
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
						"`~>help`: Returns information about who issued the command." + "`~>help [command]`: Returns information about the specific command.\n",
						false
					).build();
			}
		});
	}

	private void ping() {
		super.register("ping", new SimpleCommand() {
			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
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
		start(500);
		super.register("usageinfo", new SimpleCommand() {
			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				event.getChannel().sendMessage(new EmbedBuilder()
					.setAuthor("MantaroBot information", null, "https://puu.sh/sMsVC/576856f52b.png")
					.setDescription("Hardware and usage information.")
					.setThumbnail("https://puu.sh/suxQf/e7625cd3cd.png")
					.addField("Threads:", getThreadCount() + " Threads", true)
					.addField("Memory Usage:", getTotalMemory() - getFreeMemory() + "MB/" + getMaxMemory() + "MB", true)
					.addField("CPU Cores:", getAvailableProcessors() + " Cores", true)
					.addField("CPU Usage:", getCpuUsage() + "%", true)
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
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				User user = event.getMessage().getMentionedUsers().size() > 0 ? event.getMessage().getMentionedUsers().get(0) : event.getAuthor();
				Member member = event.getGuild().getMember(user);
				if (member == null) {
					String name = user == null ? "Unknown User" : user.getName();
					event.getChannel().sendMessage("Sorry but I couldn't get the Info fo the user " + name + ". Please make sure you and the user are in the same guild.");
					return;
				}

				String roles = member.getRoles().stream()
					.map(Role::getName)
					.collect(Collectors.joining(", "));

				if (roles.length() > EmbedBuilder.TEXT_MAX_LENGTH)
					roles = roles.substring(0, EmbedBuilder.TEXT_MAX_LENGTH - 4) + "...";

				event.getChannel().sendMessage(new EmbedBuilder()
					.setColor(member.getColor())
					.setAuthor("User info for " + user.getName() + "#" + user.getDiscriminator() + ":", null, event.getAuthor().getEffectiveAvatarUrl())
					.setThumbnail(user.getAvatarUrl())
					.addField("Join Date:", member.getJoinDate().format(DateTimeFormatter.ISO_DATE_TIME).replaceAll("[^0-9.:-]", " "), false)
					.addField("Account Created:", user.getCreationTime().format(DateTimeFormatter.ISO_DATE_TIME).replaceAll("[^0-9.:-]", " "), false)
					.addField("Voice Channel:", member.getVoiceState().getChannel() != null ? member.getVoiceState().getChannel().getName() : "None", false)
					.addField("Playing:", member.getGame().getName(), false)
					.addField("Color:", member.getColor() != null ? "#" + Integer.toHexString(member.getColor().getRGB()).substring(2).toUpperCase() : "Default", true)
					.addField("Status:", member.getOnlineStatus().getKey().toLowerCase(), true)
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
