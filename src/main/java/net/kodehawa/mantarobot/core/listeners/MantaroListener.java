package net.kodehawa.mantarobot.core.listeners;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.GuildBanEvent;
import net.dv8tion.jda.core.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.hooks.EventListener;
import net.kodehawa.mantarobot.core.CommandProcessor;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.ThreadPoolHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
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
			ThreadPoolHelper.defaultPool().startThread("BirthdayThread", () -> onBirthday(e));
			return;
		}

		if (event instanceof GuildMessageUpdateEvent) {
			ThreadPoolHelper.defaultPool().startThread("LogThread", () -> logEdit((GuildMessageUpdateEvent) event));
			return;
		}

		if (event instanceof GuildMessageDeleteEvent) {
			ThreadPoolHelper.defaultPool().startThread("LogThread", () -> logDelete((GuildMessageDeleteEvent) event));
			return;
		}

		if (event instanceof GuildMemberJoinEvent) {
			ThreadPoolHelper.defaultPool().startThread("LogThread", () -> logJoin((GuildMemberJoinEvent) event));
			return;
		}

		if (event instanceof GuildUnbanEvent) {
			ThreadPoolHelper.defaultPool().startThread("LogThread", () -> logUnban((GuildUnbanEvent) event));
			return;
		}

		if (event instanceof GuildBanEvent) {
			ThreadPoolHelper.defaultPool().startThread("LogThread", () -> logBan((GuildBanEvent) event));
		}
	}

	private void logBan(GuildBanEvent event) {
		String hour = df.format(new Date(System.currentTimeMillis()));
		String logChannel = MantaroData.getData().get().getGuild(event.getGuild(), false).logChannel;
		if (logChannel != null) {
			TextChannel tc = event.getGuild().getTextChannelById(logChannel);
			tc.sendMessage
				(":warning: `[" + hour + "]` " + event.getUser().getName() + "#" + event.getUser().getDiscriminator() + " just got banned.").queue();
			logTotal++;
		}
	}

	private void logDelete(GuildMessageDeleteEvent event) {
		try {
			String hour = df.format(new Date(System.currentTimeMillis()));
			String logChannel = MantaroData.getData().get().getGuild(event.getGuild(), false).logChannel;
			if (logChannel != null) {
				TextChannel tc = event.getGuild().getTextChannelById(logChannel);
				Message deletedMessage = shortMessageHistory.get(event.getMessageId());
				if (!deletedMessage.getContent().isEmpty() && !event.getChannel().getId().equals(logChannel)) {
					logTotal++;
					tc.sendMessage(":warning: `[" + hour + "]` " + deletedMessage.getAuthor().getName() + "#" + deletedMessage.getAuthor().getDiscriminator() + " *deleted*"
						+ " a message in #" + event.getChannel().getName() + "\n" + "```diff\n-" + deletedMessage.getContent().replace("```", "") + "```").queue();
				}
			}
		} catch (Exception e) {
			if (!(e instanceof NullPointerException)) {
				LOGGER.warn("Unexpected exception while logging a deleted message.", e);
			}
		}
	}

	private void logEdit(GuildMessageUpdateEvent event) {
		try {
			String hour = df.format(new Date(System.currentTimeMillis()));
			String logChannel = MantaroData.getData().get().getGuild(event.getGuild(), false).logChannel;
			if (logChannel != null) {
				TextChannel tc = event.getGuild().getTextChannelById(logChannel);
				User author = event.getAuthor();
				Message editedMessage = shortMessageHistory.get(event.getMessage().getId());
				if (!editedMessage.getContent().isEmpty() && !event.getChannel().getId().equals(logChannel)) {
					tc.sendMessage(":warning: `[" + hour + "]` " + author.getName() + "#" + author.getDiscriminator() + " *modified* a message in #" + event.getChannel().getName() + ".\n"
						+ "```diff\n-" + editedMessage.getContent().replace("```", "") +
						"\n+" + event.getMessage().getContent().replace("```", "") + "```").queue();
					shortMessageHistory.put(event.getMessage().getId(), event.getMessage());
					logTotal++;
				}
			}
		} catch (Exception e) {
			if (!(e instanceof NullPointerException)) {
				LOGGER.warn("Unexpected error while logging a edit.", e);
			}
		}
	}

	private void logJoin(GuildMemberJoinEvent event) {
		String logChannel = MantaroData.getData().get().getGuild(event.getGuild(), false).logChannel;
		if (logChannel != null) {
			TextChannel tc = event.getGuild().getTextChannelById(logChannel);
			tc.sendMessage("\uD83D\uDCE3 " + event.getMember().getEffectiveName() + " just joined").queue();
			logTotal++;
		}
	}

	private void logUnban(GuildUnbanEvent event) {
		String hour = df.format(new Date(System.currentTimeMillis()));
		String logChannel = MantaroData.getData().get().getGuild(event.getGuild(), false).logChannel;
		if (logChannel != null) {
			TextChannel tc = event.getGuild().getTextChannelById(logChannel);
			tc.sendMessage(":warning: `[" + hour + "]` " + event.getUser().getName() + "#" + event.getUser().getDiscriminator() + " just got unbanned.").queue();
			logTotal++;
		}
	}

	private void onBirthday(GuildMessageReceivedEvent event) {
		Guild guild = event.getGuild();
		TextChannel channel = event.getChannel();
		boolean birthdayCheck = Optional.ofNullable(MantaroData.getData().get().getGuild(guild, false).birthdayRole).isPresent();
		if (birthdayCheck) {
			if (Optional.ofNullable(MantaroData.getData().get().users.get(event.getAuthor().getId())).isPresent() &&
				Optional.ofNullable(MantaroData.getData().get().users.get(event.getAuthor().getId()).birthdayDate).isPresent()) {
				try {
					Calendar cal = Calendar.getInstance();
					SimpleDateFormat format1 = new SimpleDateFormat("dd-MM-yyyy");
					if (MantaroData.getData().get().users.get(event.getAuthor().getId()).birthdayDate.substring(0, 5).equals(format1.format(cal.getTime()).substring(0, 5))) {
						Role birthdayRole = guild.getRoleById(MantaroData.getData().get().getGuild(guild, false).birthdayRole);
						Member member = event.getGuild().getMember(event.getAuthor());
						if (!member.getRoles().contains(birthdayRole)) {
							guild.getController().addRolesToMember(member, birthdayRole).queue(
								success -> {
									TextChannel tc = event.getGuild().getTextChannelById(
										MantaroData.getData().get().getGuild(guild, false).birthdayChannel);
									tc.sendMessage(":tada: **" + member.getEffectiveName() +
										" is a year older now! Wish them a happy birthday.** :tada:").queue();
								},
								error -> {
									if (error instanceof PermissionException) {
										PermissionException pe = (PermissionException) error;
										TextChannel tc = guild.getTextChannelById(
											MantaroData.getData().get().getGuild(guild, false).birthdayChannel);
										tc.sendMessage(String.format("\u274C PermissionError while appling roles, (No permission provided: %s) Birthday module will be disabled. Check permissions and enable again", pe.getPermission())).queue();
									} else {
										channel.sendMessage(String.format("\u274C Unknown error while applying roles [%s]: <%s>: %s", birthdayRole.getName(), error.getClass().getSimpleName(), error.getMessage())).queue();
										LOGGER.warn("Unknown error while applying roles", error);
									}
									MantaroData.getData().get().getGuild(guild, false).birthdayChannel = null;
									MantaroData.getData().get().getGuild(guild, false).birthdayRole = null;
									MantaroData.getData().update();
								});
						}
					} else {
						Member memberToRemove = event.getGuild().getMember(event.getAuthor());
						Role birthdayRole1 = guild.getRoleById(MantaroData.getData().get().getGuild(guild, false).birthdayRole);
						if (memberToRemove.getRoles().contains(birthdayRole1))
							guild.getController().removeRolesFromMember(memberToRemove, birthdayRole1).queue();
					}
				} catch (Exception e) {
					LOGGER.warn("Cannot process birthday for: " + event.getAuthor().getName() + " program will be still running.", this.getClass(), e);
				}
			}
		}
	}

	private void onCommand(GuildMessageReceivedEvent event) {
		if (shortMessageHistory.size() <= 250) {
			shortMessageHistory.put(event.getMessage().getId(), event.getMessage());
		} else {
			shortMessageHistory.remove(shortMessageHistory.firstKey());
			shortMessageHistory.put(event.getMessage().getId(), event.getMessage());
		}
		try {
			if (event.getAuthor().isBot()) return;
			if (CommandProcessor.run(event)) commandTotal++;
		} catch (Exception e) {
			LOGGER.warn("Cannot process command: " + event.getMessage().getRawContent() +
				". All we know is what's here and that the error is a ``"
				+ e.getClass().getSimpleName() + "``", e);
		}
	}
}
