package net.kodehawa.mantarobot.core.listeners;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.GuildBanEvent;
import net.dv8tion.jda.core.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.kodehawa.mantarobot.core.CommandProcessor;
import net.kodehawa.mantarobot.utils.ThreadPoolHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TreeMap;

public class MantaroListener implements EventListener {
	public static int commandTotal = 0;
	private static Logger LOGGER = LoggerFactory.getLogger("CommandListener");
	private static int logTotal = 0;
	//For later usage in LogListener.
	//A short message cache of 250 messages. If it reaches 250 it will delete the first one stored, and continue being 250
	private static TreeMap<String, Message> shortMessageHistory = new TreeMap<>();

	public static String getCommandTotal() {
		return String.valueOf(commandTotal);
	}

	public static String getLogTotal() {
		return String.valueOf(logTotal);
	}

	private final DateFormat df = new SimpleDateFormat("HH:mm:ss");

	@Override
	public void onEvent(Event event) {
		if (event instanceof GuildMessageReceivedEvent) {
			GuildMessageReceivedEvent e = (GuildMessageReceivedEvent) event;
			ThreadPoolHelper.defaultPool().startThread("CmdThread", () -> onCommand(e));
			//ThreadPoolHelper.defaultPool().startThread("BirthdayThread", () -> onBirthday(e));
			return;
		}

		if (event instanceof GuildMessageUpdateEvent) {
			//ThreadPoolHelper.defaultPool().startThread("LogThread", () -> logEdit((GuildMessageUpdateEvent) event));
			return;
		}

		if (event instanceof GuildMessageDeleteEvent) {
			//ThreadPoolHelper.defaultPool().startThread("LogThread", () -> logDelete((GuildMessageDeleteEvent) event));
			return;
		}

		if (event instanceof GuildMemberJoinEvent) {
			//ThreadPoolHelper.defaultPool().startThread("LogThread", () -> logJoin((GuildMemberJoinEvent) event));
			return;
		}

		if (event instanceof GuildUnbanEvent) {
			//ThreadPoolHelper.defaultPool().startThread("LogThread", () -> logUnban((GuildUnbanEvent) event));
			return;
		}

		if (event instanceof GuildBanEvent) {
			//ThreadPoolHelper.defaultPool().startThread("LogThread", () -> logBan((GuildBanEvent) event));
			return;
		}
	}

	/*public void logBan(GuildBanEvent event) {
		String hour = df.format(new Date(System.currentTimeMillis()));
		if (Parameters.getLogHash().containsKey(event.getGuild().getId())) {
			TextChannel tc = event.getGuild().getTextChannelsByName(Parameters.getLogChannelForServer(event.getGuild().getId()), true).get(0);
			tc.sendMessage(":warning: `[" + hour + "]` " + event.getUser().getName() + "#" + event.getUser().getDiscriminator() + " just got banned.").queue();
			logTotal++;
		}
	}

	private void logDelete(GuildMessageDeleteEvent event) {
		try {
			String hour = df.format(new Date(System.currentTimeMillis()));
			if (Parameters.getLogHash().containsKey(event.getGuild().getId())) {
				TextChannel tc = event.getGuild().getTextChannelsByName(Parameters.getLogChannelForServer(event.getGuild().getId()), true).get(0);
				Message deletedMessage = shortMessageHistory.get(event.getMessageId());
				if (!deletedMessage.getContent().isEmpty() && !event.getChannel().getName().equals(Parameters.getLogChannelForServer(event.getGuild().getId()))) {
					logTotal++;
					tc.sendMessage(":warning: `[" + hour + "]` " + deletedMessage.getAuthor().getName() + "#" + deletedMessage.getAuthor().getDiscriminator() + " *deleted*"
						+ " a message in #" + event.getChannel().getName() + "\n" + "```diff\n-" + deletedMessage.getContent().replace("```", "") + "```").queue();
				}
			}
		} catch (Exception e) {
			if (!(e instanceof NullPointerException)) {
				//Unexpected exception, log.
				e.printStackTrace();
			}
		}
	}

	private void logEdit(GuildMessageUpdateEvent event) {
		try {
			String hour = df.format(new Date(System.currentTimeMillis()));
			if (Parameters.getLogHash().containsKey(event.getGuild().getId())) {
				TextChannel tc = event.getGuild().getTextChannelsByName(Parameters.getLogChannelForServer(event.getGuild().getId()), true).get(0);
				User author = event.getAuthor();
				Message editedMessage = shortMessageHistory.get(event.getMessage().getId());
				if (!editedMessage.getContent().isEmpty() && !event.getChannel().getName().equals(Parameters.getLogChannelForServer(event.getGuild().getId()))) {
					tc.sendMessage(":warning: `[" + hour + "]` " + author.getName() + "#" + author.getDiscriminator() + " *modified* a message in #" + event.getChannel().getName() + ".\n"
						+ "```diff\n-" + editedMessage.getContent().replace("```", "") +
						"\n+" + event.getMessage().getContent().replace("```", "") + "```").queue();
					//Update old message
					shortMessageHistory.put(event.getMessage().getId(), event.getMessage());
					logTotal++;
				}
			}
		} catch (Exception e) {
			if (!(e instanceof NullPointerException)) {
				//Unexpected exception, log.
				e.printStackTrace();
			}
		}
	}

	private void logJoin(GuildMemberJoinEvent event) {
		String hour = df.format(new Date(System.currentTimeMillis()));
		if (Parameters.getLogHash().containsKey(event.getGuild().getId())) {
			TextChannel tc = event.getGuild().getTextChannelsByName(Parameters.getLogChannelForServer(event.getGuild().getId()), true).get(0);
			tc.sendMessage("\uD83D\uDCE3 " + event.getMember().getEffectiveName() + " just joined").queue();
			logTotal++;
		}
	}

	private void logUnban(GuildUnbanEvent event) {
		String hour = df.format(new Date(System.currentTimeMillis()));
		if (Parameters.getLogHash().containsKey(event.getGuild().getId())) {
			TextChannel tc = event.getGuild().getTextChannelsByName(Parameters.getLogChannelForServer(event.getGuild().getId()), true).get(0);
			tc.sendMessage(":warning: `[" + hour + "]` " + event.getUser().getName() + "#" + event.getUser().getDiscriminator() + " just got unbanned.").queue();
			logTotal++;
		}
	}

	private void onBirthday(GuildMessageReceivedEvent event) {
		Guild guild = event.getGuild();
		TextChannel channel = event.getChannel();
		if (Parameters.getBirthdayHash().containsKey(guild.getId())) {
			String userKey = event.getGuild().getId() + ":" + event.getAuthor().getId();
			String[] data = userKey.split(":");
			if (Birthday.bd.containsKey(userKey)) {
				if (!Birthday.bd.get(userKey).isEmpty()) {
					try {
						Calendar cal = Calendar.getInstance();
						SimpleDateFormat format1 = new SimpleDateFormat("dd-MM-yyyy");
						if (Birthday.bd.get(userKey).substring(0, 5).equals(format1.format(cal.getTime()).substring(0, 5))) {
							String guildId = data[0];
							Role birthdayRole = guild.getRoleById(Parameters.getBirthdayRoleForServer(guildId));
							if (data[1].equals(event.getAuthor().getId())) {
								Member member = event.getGuild().getMember(event.getAuthor());
								if (!member.getRoles().contains(birthdayRole)) {
									guild.getController().addRolesToMember(member, birthdayRole).queue(
										success -> {
											TextChannel tc = guild.getTextChannelById(Parameters.getBirthdayChannelForServer(guildId));
											tc.sendMessage(":tada: **" + member.getEffectiveName() +
												" is a year older now! Wish them a happy birthday.** :tada:").queue();
										},
										error -> {
											if (error instanceof PermissionException) {
												PermissionException pe = (PermissionException) error;
												TextChannel tc = guild.getTextChannelById(Parameters.getBirthdayChannelForServer(guildId));
												tc.sendMessage("\u274C PermissionError while appling roles, (No permission provided: " + pe.getPermission() + ")").queue();
											} else {
												channel.sendMessage("\u274C" + "Unknown error while applying roles [" + birthdayRole.getName()
													+ "]: " + "<" + error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();
												error.printStackTrace();
											}
										});
								}
							}
						} else {
							String guildId1 = data[0];
							Member membertoRemove = event.getGuild().getMember(event.getAuthor());
							Role birthdayRole1 = guild.getRoleById(Parameters.getBirthdayRoleForServer(guildId1));
							if (membertoRemove.getRoles().contains(birthdayRole1)) {
								guild.getController().removeRolesFromMember(membertoRemove, birthdayRole1).queue();
							}
						}
					} catch (Exception e) {
						LOGGER.warn("Cannot process birthday for: " + userKey + " program will be still running.", this.getClass(), e);
						e.printStackTrace();
					}
				}
			}
		}
	}*/

	private void onCommand(GuildMessageReceivedEvent event) {
		if (shortMessageHistory.size() < 250) {
			shortMessageHistory.put(event.getMessage().getId(), event.getMessage());
		} else {
			shortMessageHistory.remove(shortMessageHistory.firstKey());
			shortMessageHistory.put(event.getMessage().getId(), event.getMessage());
		}

		try {
			if (CommandProcessor.run(event)) commandTotal++;
		} catch (Exception e) {
			//TODO HANDLE THIS PROPERLY NOW.
			//Now this catch block handles the exceptions that can happen while on Command Execution.
			//Should look a better way of handling/logging this.

			LOGGER.warn("Cannot process command? Prefix is probably null, look into this. " + event.getMessage().getRawContent(), e);
			e.printStackTrace();
		}
	}
}
