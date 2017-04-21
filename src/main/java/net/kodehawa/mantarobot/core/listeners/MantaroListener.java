package net.kodehawa.mantarobot.core.listeners;

import br.com.brjdevs.highhacks.eventbus.Listener;
import br.com.brjdevs.java.utils.extensions.Async;
import com.google.common.cache.CacheLoader;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.DisconnectEvent;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ExceptionEvent;
import net.dv8tion.jda.core.events.StatusChangeEvent;
import net.dv8tion.jda.core.events.guild.GuildBanEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.hooks.EventListener;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.info.GuildStatsManager;
import net.kodehawa.mantarobot.commands.info.GuildStatsManager.LoggedEvent;
import net.kodehawa.mantarobot.core.ShardMonitorEvent;
import net.kodehawa.mantarobot.core.listeners.command.CommandListener;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.DBGuild;
import net.kodehawa.mantarobot.data.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.ThreadPoolHelper;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static net.kodehawa.mantarobot.commands.custom.Mapifier.dynamicResolve;
import static net.kodehawa.mantarobot.commands.custom.Mapifier.map;

@Slf4j
public class MantaroListener implements EventListener {
	private static int logTotal = 0;

	public static String getLogTotal() {
		return String.valueOf(logTotal);
	}

	private final DateFormat df = new SimpleDateFormat("HH:mm:ss");

	private final int shardId;

	public MantaroListener(int shardId) {
		this.shardId = shardId;
	}

	@Listener
	@Override
	public void onEvent(Event event) {
		if (event instanceof ShardMonitorEvent) {
			((ShardMonitorEvent) event).alive(shardId, ShardMonitorEvent.MANTARO_LISTENER);
			return;
		}

		if (event instanceof GuildMessageReceivedEvent) {
			GuildMessageReceivedEvent e = (GuildMessageReceivedEvent) event;
			Async.thread("BirthdayThread", () -> onBirthday(e));
			return;
		}

		//Log intensifies
		if (event instanceof GuildMessageUpdateEvent) {
			Async.thread("LogThread", () -> logEdit((GuildMessageUpdateEvent) event));
			return;
		}

		if (event instanceof GuildMessageDeleteEvent) {
			Async.thread("LogThread", () -> logDelete((GuildMessageDeleteEvent) event));
			return;
		}

		if (event instanceof GuildMemberJoinEvent) {
			Async.thread("LogThread", () -> onUserJoin((GuildMemberJoinEvent) event));
			return;
		}

		if (event instanceof GuildMemberLeaveEvent) {
			Async.thread("LogThread", () -> onUserLeave((GuildMemberLeaveEvent) event));
			return;
		}

		if (event instanceof GuildUnbanEvent) {
			Async.thread("LogThread", () -> logUnban((GuildUnbanEvent) event));
			return;
		}

		if (event instanceof GuildBanEvent) {
			Async.thread("LogThread", () -> logBan((GuildBanEvent) event));
			return;
		}

		if (event instanceof GuildJoinEvent) {
			Async.thread("LogThread", () -> onJoin((GuildJoinEvent) event));
			return;
		}

		if (event instanceof GuildLeaveEvent) {
			Async.thread("LogThread", () -> onLeave((GuildLeaveEvent) event));
		}

		if (event instanceof StatusChangeEvent) {
			ThreadPoolHelper.defaultPool().startThread("LogThread", () -> logStatusChange((StatusChangeEvent) event));
		}

		//debug
		if (event instanceof DisconnectEvent) {
			onDisconnect((DisconnectEvent) event);
		}

		if (event instanceof ExceptionEvent) {
			onException((ExceptionEvent) event);
		}
	}

	public TextChannel getLogChannel() {
		return MantaroBot.getInstance().getTextChannelById(MantaroData.config().get().consoleChannel);
	}

	private void logBan(GuildBanEvent event) {
		String hour = df.format(new Date(System.currentTimeMillis()));
		String logChannel = MantaroData.db().getGuild(event.getGuild()).getData().getGuildLogChannel();
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
			String logChannel = MantaroData.db().getGuild(event.getGuild()).getData().getGuildLogChannel();
			if (logChannel != null) {
				TextChannel tc = event.getGuild().getTextChannelById(logChannel);
				Message deletedMessage = CommandListener.getMessageCache().get(event.getMessageId(), Optional::empty).orElse(null);
				if (deletedMessage != null && !deletedMessage.getContent().isEmpty() && !event.getChannel().getId().equals(logChannel) && !deletedMessage.getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) {
					logTotal++;
					tc.sendMessage(String.format(EmoteReference.WARNING + "`[%s]` %s#%s *deleted* a message in #%s\n```diff\n-%s```", hour, deletedMessage.getAuthor().getName(), deletedMessage.getAuthor().getDiscriminator(), event.getChannel().getName(), deletedMessage.getContent().replace("```", ""))).queue();
				}
			}
		} catch (Exception e) {
			if (!(e instanceof IllegalArgumentException) && !(e instanceof NullPointerException) && !(e instanceof CacheLoader.InvalidCacheLoadException)) {
				log.warn("Unexpected exception while logging a deleted message.", e);
			}
		}
	}

	private void logEdit(GuildMessageUpdateEvent event) {
		try {
			String hour = df.format(new Date(System.currentTimeMillis()));
			String logChannel = MantaroData.db().getGuild(event.getGuild()).getData().getGuildLogChannel();
			if (logChannel != null) {
				TextChannel tc = event.getGuild().getTextChannelById(logChannel);
				User author = event.getAuthor();
				Message editedMessage = CommandListener.getMessageCache().get(event.getMessage().getId(), Optional::empty).orElse(null);
				if (editedMessage != null && !editedMessage.getContent().isEmpty() && !event.getChannel().getId().equals(logChannel)) {
					tc.sendMessage(String.format(EmoteReference.WARNING + "`[%s]` %s#%s *modified* a message in #%s.\n```diff\n-%s\n+%s```", hour, author.getName(), author.getDiscriminator(), event.getChannel().getName(), editedMessage.getContent().replace("```", ""), event.getMessage().getContent().replace("```", ""))).queue();
					CommandListener.getMessageCache().put(event.getMessage().getId(), Optional.of(event.getMessage()));
					logTotal++;
				}
			}
		} catch (Exception e) {
			if (!(e instanceof NullPointerException) && !(e instanceof IllegalArgumentException) && !(e instanceof CacheLoader.InvalidCacheLoadException)) {
				log.warn("Unexpected error while logging a edit.", e);
			}
		}
	}

	private void logStatusChange(StatusChangeEvent event) {
		JDA jda = event.getJDA();
		if (jda.getShardInfo() == null) return;
		log.info(String.format("`Shard #%d`: Changed from `%s` to `%s`", jda.getShardInfo().getShardId(), event.getOldStatus(), event.getStatus()));
	}
	//endregion

	private void logUnban(GuildUnbanEvent event) {
		try {
			String hour = df.format(new Date(System.currentTimeMillis()));
			String logChannel = MantaroData.db().getGuild(event.getGuild()).getData().getGuildLogChannel();
			if (logChannel != null) {
				TextChannel tc = event.getGuild().getTextChannelById(logChannel);
				tc.sendMessage(String.format(EmoteReference.WARNING + "`[%s]` %s#%s just got unbanned.", hour, event.getUser().getName(), event.getUser().getDiscriminator())).queue();
				logTotal++;
			}
		} catch (Exception e) {
			if (!(e instanceof NullPointerException) && !(e instanceof IllegalArgumentException)) {
				log.warn("Unexpected error while logging a edit.", e);
			}
		}
	}

	private void onBirthday(GuildMessageReceivedEvent event) {
		try {
			Role birthdayRole = event.getGuild().getRoleById(MantaroData.db().getGuild(event.getGuild()).getData().getBirthdayRole());
			UserData user = MantaroData.db().getUser(event.getMember()).getData();
			if (birthdayRole != null && user.getBirthday() != null) {
				TextChannel channel = event.getGuild().getTextChannelById(MantaroData.db().getGuild(event.getGuild()).getData().getBirthdayChannel());
				Calendar cal = Calendar.getInstance();
				SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
				if (user.getBirthday().substring(0, 5).equals(dateFormat.format(cal.getTime()).substring(0, 5))) {
					if (!event.getMember().getRoles().contains(birthdayRole)) {
						event.getGuild().getController().addRolesToMember(event.getMember(), birthdayRole).queue(s ->
							channel.sendMessage(String.format(EmoteReference.POPPER + "**%s is a year older now! Wish them a happy birthday.** :tada:",
								event.getMember().getEffectiveName())).queue()
						);
					}
				} else {
					if (event.getGuild().getRoles().contains(birthdayRole)) {
						event.getGuild().getController().removeRolesFromMember(event.getMember(), birthdayRole).queue();
					}
				}
			}
		} catch (Exception e) {
			if (e instanceof PermissionException) {
				resetBirthdays(event.getGuild());
				event.getChannel().sendMessage(EmoteReference.ERROR + "Error while applying birthday role, so the role assigner will be resetted.").queue();
			}
			//else ignore
		}
	}

	//region minn
	private void onDisconnect(DisconnectEvent event) {
		if (event.isClosedByServer()) {
			log.warn(String.format("---- DISCONNECT [SERVER] CODE: [%d] %s%n",
				event.getServiceCloseFrame().getCloseCode(), event.getCloseCode()));
		} else {
			log.warn(String.format("---- DISCONNECT [CLIENT] CODE: [%d] %s%n",
				event.getClientCloseFrame().getCloseCode(), event.getClientCloseFrame().getCloseReason()));
		}
	}

	private void onException(ExceptionEvent event) {
		if (!event.isLogged()) event.getCause().printStackTrace();
	} //endregion

	private void onJoin(GuildJoinEvent event) {
		try {
			TextChannel tc = getLogChannel();
			String hour = df.format(new Date(System.currentTimeMillis()));

			if (MantaroData.db().getMantaroData().getBlackListedGuilds().contains(event.getGuild().getId())
				|| MantaroData.db().getMantaroData().getBlackListedUsers().contains(event.getGuild().getOwner().getUser().getId())) {
				event.getGuild().leave().queue();
				tc.sendMessage(String.format(EmoteReference.MEGA + "`[%s]` I left a guild with name: ``%s`` (%s members) since it was blacklisted.", hour, event.getGuild().getName(), event.getGuild().getMembers().size())).queue();
				return;
			}

			tc.sendMessage(String.format(EmoteReference.MEGA + "`[%s]` I joined a new guild with name: ``%s`` (%s members) [ID: `%s`, Owner:%s (%s)]",
					hour, event.getGuild().getName(), event.getGuild().getMembers().size(), event.getGuild().getId(),
					event.getGuild().getOwner().getEffectiveName() + "#" + event.getGuild().getOwner().getUser().getDiscriminator(),
					event.getGuild().getOwner().getUser().getId())).queue();
			logTotal++;

			GuildStatsManager.log(LoggedEvent.JOIN);
		} catch (Exception e) {
			if (!(e instanceof NullPointerException) && !(e instanceof IllegalArgumentException)) {
				log.warn("Unexpected error while logging a edit.", e);
			}
		}
	}

	private void onLeave(GuildLeaveEvent event) {
		try {
			TextChannel tc = getLogChannel();
			String hour = df.format(new Date(System.currentTimeMillis()));

			if (event.getGuild().getMembers().isEmpty()) {
				tc.sendMessage(String.format(EmoteReference.THINKING + "`[%s]` A guild with name: ``%s`` just got deleted.", hour, event.getGuild().getName())).queue();
				logTotal++;
				return;
			}

			tc.sendMessage(String.format(EmoteReference.SAD + "`[%s]` I left a guild with name: ``%s`` (%s members)", hour, event.getGuild().getName(), event.getGuild().getMembers().size())).queue();
			logTotal++;

			GuildStatsManager.log(LoggedEvent.LEAVE);
		} catch (Exception e) {
			if (!(e instanceof NullPointerException) && !(e instanceof IllegalArgumentException)) {
				log.warn("Unexpected error while logging a leave event.", e);
			}
		}
	}

	private void onUserJoin(GuildMemberJoinEvent event) {
		try {
			String role = MantaroData.db().getGuild(event.getGuild()).getData().getGuildAutoRole();

			String hour = df.format(new Date(System.currentTimeMillis()));
			if (role != null) {
				event.getGuild().getController().addRolesToMember(event.getMember(), event.getGuild().getRoleById(role)).queue(s -> log.debug("Successfully added a new role to " + event.getMember()), error -> {
					if (error instanceof PermissionException) {
						MantaroData.db().getGuild(event.getGuild()).getData().setGuildAutoRole(null);
						MantaroData.db().getGuild(event.getGuild()).save();
						event.getGuild().getOwner().getUser().openPrivateChannel().queue(messageChannel ->
							messageChannel.sendMessage("Removed autorole since I don't have the permissions to assign that role").queue());
					} else {
						log.warn("Error while applying roles", error);
					}
				});
			}

			String joinChannel = MantaroData.db().getGuild(event.getGuild()).getData().getLogJoinLeaveChannel();
			String joinMessage = MantaroData.db().getGuild(event.getGuild()).getData().getJoinMessage();

			if (joinChannel != null && joinMessage != null) {
				if (joinMessage.contains("$(")) {
					Map<String, String> dynamicMap = new HashMap<>();
					map("event", dynamicMap, event);
					joinMessage = dynamicResolve(joinMessage, dynamicMap);
				}
				TextChannel tc = event.getGuild().getTextChannelById(joinChannel);
				tc.sendMessage(joinMessage).queue();
			}

			String logChannel = MantaroData.db().getGuild(event.getGuild()).getData().getGuildLogChannel();
			if (logChannel != null) {
				TextChannel tc = event.getGuild().getTextChannelById(logChannel);
				if (!event.getGuild().getSelfMember().hasPermission(tc, Permission.MESSAGE_READ)) return;
				tc.sendMessage(String.format("`[%s]` \uD83D\uDCE3 `%s#%s` just joined `%s` `(User #%d | ID:%s)`", hour, event.getMember().getEffectiveName(), event.getMember().getUser().getDiscriminator(), event.getGuild().getName(), event.getGuild().getMembers().size(), event.getGuild().getId())).queue();
				logTotal++;
			}
		} catch (Exception e) {
			if (!(e instanceof NullPointerException) && !(e instanceof IllegalArgumentException)) {
				log.warn("Unexpected error while logging a join event.", e);
			}
		}
	}

	private void onUserLeave(GuildMemberLeaveEvent event) {
		try {
			String hour = df.format(new Date(System.currentTimeMillis()));
			String leaveChannel = MantaroData.db().getGuild(event.getGuild()).getData().getLogJoinLeaveChannel();
			String leaveMessage = MantaroData.db().getGuild(event.getGuild()).getData().getLeaveMessage();

			if (leaveChannel != null && leaveMessage != null) {
				if (leaveMessage.contains("$(")) {
					Map<String, String> dynamicMap = new HashMap<>();
					map("event", dynamicMap, event);
					leaveMessage = dynamicResolve(leaveMessage, dynamicMap);
				}

				TextChannel tc = event.getGuild().getTextChannelById(leaveChannel);
				tc.sendMessage(leaveMessage).queue();
			}

			String logChannel = MantaroData.db().getGuild(event.getGuild()).getData().getGuildLogChannel();
			if (logChannel != null) {
				TextChannel tc = event.getGuild().getTextChannelById(logChannel);
				tc.sendMessage("`[" + hour + "]` " + "\uD83D\uDCE3 `" + event.getMember().getEffectiveName() + "#" + event.getMember().getUser().getDiscriminator() + "` just left `" + event.getGuild().getName() + "` `(User #" + event.getGuild().getMembers().size() + ")`").queue();
				logTotal++;
			}
		} catch (Exception e) {
			if (!(e instanceof NullPointerException) && !(e instanceof IllegalArgumentException)) {
				log.warn("Unexpected error while logging a leave event.", e);
			}
		}
	}

	private void resetBirthdays(Guild guild) {
		DBGuild data = MantaroData.db().getGuild(guild);
		data.getData().setBirthdayChannel(null);
		data.getData().setBirthdayRole(null);
		data.save();
	}
}
