package net.kodehawa.mantarobot.core.listeners;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.GuildBanEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
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
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.TreeMap;

public class MantaroListener implements EventListener {
	private static Logger LOGGER = LoggerFactory.getLogger("CommandListener");
	private static int commandTotal = 0;
	private static int logTotal = 0;
	//Message cache of 350 messages. If it reaches 350 it will delete the first one stored, and continue being 350
	private static TreeMap<String, Message> messageCache = new TreeMap<>();

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

		//Log intensifies
		if (event instanceof GuildMessageUpdateEvent) {
			ThreadPoolHelper.defaultPool().startThread("LogThread", () -> logEdit((GuildMessageUpdateEvent) event));
			return;
		}

		if (event instanceof GuildMessageDeleteEvent) {
			ThreadPoolHelper.defaultPool().startThread("LogThread", () -> logDelete((GuildMessageDeleteEvent) event));
			return;
		}

		if (event instanceof GuildMemberJoinEvent) {
			ThreadPoolHelper.defaultPool().startThread("LogThread", () -> onUserJoin((GuildMemberJoinEvent) event));
			return;
		}

		if (event instanceof GuildUnbanEvent) {
			ThreadPoolHelper.defaultPool().startThread("LogThread", () -> logUnban((GuildUnbanEvent) event));
			return;
		}

		if (event instanceof GuildBanEvent) {
			ThreadPoolHelper.defaultPool().startThread("LogThread", () -> logBan((GuildBanEvent) event));
			return;
		}

		if (event instanceof GuildJoinEvent) {
			ThreadPoolHelper.defaultPool().startThread("LogThread", () -> onJoin((GuildJoinEvent) event));
			return;
		}

		if (event instanceof GuildLeaveEvent) {
			ThreadPoolHelper.defaultPool().startThread("LogThread", () -> onLeave((GuildLeaveEvent) event));
		}
	}

	private void logBan(GuildBanEvent event) {
		String hour = df.format(new Date(System.currentTimeMillis()));
		String logChannel = MantaroData.getData().get().getGuild(event.getGuild(), false).logChannel;
		if (logChannel != null) {
			TextChannel tc = event.getGuild().getTextChannelById(logChannel);
			tc.sendMessage
				(EmoteReference.WARNING + "`[" + hour + "]` " + event.getUser().getName() + "#" + event.getUser().getDiscriminator() + " just got banned.").queue();
			logTotal++;
		}
	}

	private void logDelete(GuildMessageDeleteEvent event) {
		try {
			String hour = df.format(new Date(System.currentTimeMillis()));
			String logChannel = MantaroData.getData().get().getGuild(event.getGuild(), false).logChannel;
			if (logChannel != null) {
				TextChannel tc = event.getGuild().getTextChannelById(logChannel);
				Message deletedMessage = messageCache.get(event.getMessageId());
				if (!deletedMessage.getContent().isEmpty() && !event.getChannel().getId().equals(logChannel)) {
					logTotal++;
					tc.sendMessage(String.format(EmoteReference.WARNING + "`[%s]` %s#%s *deleted* a message in #%s\n```diff\n-%s```", hour, deletedMessage.getAuthor().getName(), deletedMessage.getAuthor().getDiscriminator(), event.getChannel().getName(), deletedMessage.getContent().replace("```", ""))).queue();
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
				Message editedMessage = messageCache.get(event.getMessage().getId());
				if (!editedMessage.getContent().isEmpty() && !event.getChannel().getId().equals(logChannel)) {
					tc.sendMessage(String.format(EmoteReference.WARNING + "`[%s]` %s#%s *modified* a message in #%s.\n```diff\n-%s\n+%s```", hour, author.getName(), author.getDiscriminator(), event.getChannel().getName(), editedMessage.getContent().replace("```", ""), event.getMessage().getContent().replace("```", ""))).queue();
					messageCache.put(event.getMessage().getId(), event.getMessage());
					logTotal++;
				}
			}
		} catch (Exception e) {
			if (!(e instanceof NullPointerException)) {
				LOGGER.warn("Unexpected error while logging a edit.", e);
			}
		}
	}

	private void logUnban(GuildUnbanEvent event) {
		String hour = df.format(new Date(System.currentTimeMillis()));
		String logChannel = MantaroData.getData().get().getGuild(event.getGuild(), false).logChannel;
		if (logChannel != null) {
			TextChannel tc = event.getGuild().getTextChannelById(logChannel);
			tc.sendMessage(String.format(EmoteReference.WARNING + "`[%s]` %s#%s just got unbanned.", hour, event.getUser().getName(), event.getUser().getDiscriminator())).queue();
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
									tc.sendMessage(String.format(EmoteReference.POPPER + "**%s is a year older now! Wish them a happy birthday.** :tada:", member.getEffectiveName())).queue();
								},
								error -> {
									if (error instanceof PermissionException) {
										PermissionException pe = (PermissionException) error;
										TextChannel tc = guild.getTextChannelById(
											MantaroData.getData().get().getGuild(guild, false).birthdayChannel);
										tc.sendMessage(String.format(EmoteReference.ERROR + "PermissionError while appling roles, (No permission provided: %s) Birthday module will be disabled. Check permissions and enable it again", pe.getPermission())).queue();
										MantaroData.getData().get().getGuild(guild, false).birthdayChannel = null;
										MantaroData.getData().get().getGuild(guild, false).birthdayRole = null;
										MantaroData.getData().save();
									} else {
										channel.sendMessage(String.format(EmoteReference.ERROR + "Unknown error while applying roles [%s]: <%s>: %s", birthdayRole.getName(), error.getClass().getSimpleName(), error.getMessage())).queue();
										LOGGER.warn("Unknown error while applying roles", error);
										MantaroData.getData().get().getGuild(guild, false).birthdayChannel = null;
										MantaroData.getData().get().getGuild(guild, false).birthdayRole = null;
										MantaroData.getData().save();
									}
								});
						}
					} else {
						Member memberToRemove = event.getGuild().getMember(event.getAuthor());
						Role birthdayRole1 = guild.getRoleById(MantaroData.getData().get().getGuild(guild, false).birthdayRole);
						if (memberToRemove.getRoles().contains(birthdayRole1))
							guild.getController().removeRolesFromMember(memberToRemove, birthdayRole1).queue();
					}
				} catch (Exception e) {
					if (e instanceof PermissionException) {
						PermissionException pe = (PermissionException) e;
						TextChannel tc = guild.getTextChannelById(
							MantaroData.getData().get().getGuild(guild, false).birthdayChannel);
						tc.sendMessage(String.format(EmoteReference.ERROR + "PermissionError while removing roles, (No permission provided: %s) Birthday module will be disabled. Check permissions and enable it again", pe.getPermission())).queue();
						MantaroData.getData().get().getGuild(guild, false).birthdayChannel = null;
						MantaroData.getData().get().getGuild(guild, false).birthdayRole = null;
					} else
						LOGGER.warn("Cannot process birthday for: " + event.getAuthor().getName() + " program will be still running.", this.getClass(), e);
				}
			}
		}
	}

	private void onCommand(GuildMessageReceivedEvent event) {
		if (messageCache.size() <= 350) {
			messageCache.put(event.getMessage().getId(), event.getMessage());
		} else {
			messageCache.remove(messageCache.firstKey());
			messageCache.put(event.getMessage().getId(), event.getMessage());
		}
		try {
			if (!event.getGuild().getSelfMember().getPermissions(event.getChannel()).contains(Permission.MESSAGE_WRITE))
				return;
			if (event.getAuthor().isBot()) return;
			if (CommandProcessor.run(event)) commandTotal++;
		} catch (Exception e) {
			if (e instanceof NumberFormatException) {
				event.getChannel().sendMessage(EmoteReference.ERROR + "Incorrect type arguments. Check command help.").queue();
				return;
			}

			if (e instanceof IndexOutOfBoundsException) {
				event.getChannel().sendMessage(EmoteReference.ERROR + "Query returned no results or incorrect type arguments. Check command help.").queue();
				return;
			}

			if (e instanceof PermissionException) {
				PermissionException ex = (PermissionException) e;
				event.getChannel().sendMessage(EmoteReference.ERROR + "The bot has no permission to execute this action. I need the permission: " + ex.getPermission()).queue();
				return;
			}

			event.getChannel().sendMessage(String.format("We caught a unfetched error while processing the command: ``%s`` with description: ``%s``\n"
					+ "**You might  want to contact Kodehawa#3457 with a description of how it happened or join the support guild** " +
					"(you can find it on bots.discord.pw [search for Mantaro] or on ~>about)"
				, e.getClass().getSimpleName(), e.getMessage())).queue();

			LOGGER.warn(String.format("Cannot process command: %s. All we know is what's here and that the error is a ``%s``", event.getMessage().getRawContent(), e.getClass().getSimpleName()), e);
		}
	}

	private void onJoin(GuildJoinEvent event) {
		TextChannel tc = event.getJDA().getTextChannelById("266231083341840385");

		if (MantaroData.getData().get().blacklistedGuilds.contains(event.getGuild().getId())) {
			event.getGuild().leave().queue();
			tc.sendMessage(String.format(EmoteReference.MEGA + "I left a guild with name: ``%s`` (%s members) since it was blacklisted.", event.getGuild().getName(), event.getGuild().getMembers().size())).queue();
			return;
		}

		tc.sendMessage(String.format(EmoteReference.MEGA + "I joined a new guild with name: ``%s`` (%s members)", event.getGuild().getName(), event.getGuild().getMembers().size())).queue();
		logTotal++;
	}

	private void onLeave(GuildLeaveEvent event) {
		TextChannel tc = event.getJDA().getTextChannelById("266231083341840385");
		if (event.getGuild().getMembers().isEmpty()) {
			tc.sendMessage(String.format(EmoteReference.THINKING + "A guild with name: ``%s`` just got deleted.", event.getGuild().getName())).queue();
			logTotal++;
			return;
		}

		tc.sendMessage(String.format(EmoteReference.SAD + "I left a guild with name: ``%s`` (%s members)", event.getGuild().getName(), event.getGuild().getMembers().size())).queue();
		logTotal++;
	}

	private void onUserJoin(GuildMemberJoinEvent event) {
		String role = MantaroData.getData().get().getGuild(event.getGuild(), false).autoRole;
		if (role != null) {
			event.getGuild().getController().addRolesToMember(event.getMember(), event.getGuild().getRoleById(role)).queue(s -> LOGGER.debug("Successfully added a new role to " + event.getMember()), error -> {
				if (error instanceof PermissionException) {
					MantaroData.getData().get().getGuild(event.getGuild(), false).autoRole = null;
					event.getGuild().getOwner().getUser().openPrivateChannel().queue(messageChannel ->
						messageChannel.sendMessage("Removed autorole since I don't have the permissions to assign that role").queue());
				} else {
					LOGGER.warn("Error while applying roles", error);
				}
			});
		}

		String logChannel = MantaroData.getData().get().getGuild(event.getGuild(), false).logChannel;
		if (logChannel != null) {
			TextChannel tc = event.getGuild().getTextChannelById(logChannel);
			tc.sendMessage("\uD83D\uDCE3 " + event.getMember().getEffectiveName() + " just joined").queue();
			logTotal++;
		}
	}
}
