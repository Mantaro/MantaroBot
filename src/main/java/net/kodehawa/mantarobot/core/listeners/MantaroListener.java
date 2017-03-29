package net.kodehawa.mantarobot.core.listeners;

import com.rethinkdb.gen.exc.ReqlError;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.StatusChangeEvent;
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
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.info.GuildStatsManager;
import net.kodehawa.mantarobot.commands.info.GuildStatsManager.LoggedEvent;
import net.kodehawa.mantarobot.commands.rpg.TextChannelGround;
import net.kodehawa.mantarobot.core.CommandProcessor;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.DBGuild;
import net.kodehawa.mantarobot.data.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.ThreadPoolHelper;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class MantaroListener implements EventListener {
	//Message cache of 2500 messages. If it reaches 2500 it will delete the first one stored, and continue being 2500
	private static final Map<String, Message> messageCache = Collections.synchronizedMap(new LinkedHashMap<>(2500));
	private static int commandTotal = 0;
	private static int logTotal = 0;
	private static long ticks;

	public static String getCommandTotal() {
		return String.valueOf(commandTotal);
	}

	public static String getLogTotal() {
		return String.valueOf(logTotal);
	}

	public static long getTotalTicks() {
		return ticks;
	}

	private final DateFormat df = new SimpleDateFormat("HH:mm:ss");
	Random r = new Random();

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

		if (event instanceof StatusChangeEvent) {
			ThreadPoolHelper.defaultPool().startThread("LogThread", () -> logStatusChange((StatusChangeEvent) event));
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
				Message deletedMessage = messageCache.get(event.getMessageId());
				if (deletedMessage != null && !deletedMessage.getContent().isEmpty() && !event.getChannel().getId().equals(logChannel)) {
					logTotal++;
					tc.sendMessage(String.format(EmoteReference.WARNING + "`[%s]` %s#%s *deleted* a message in #%s\n```diff\n-%s```", hour, deletedMessage.getAuthor().getName(), deletedMessage.getAuthor().getDiscriminator(), event.getChannel().getName(), deletedMessage.getContent().replace("```", ""))).queue();
				}
			}
		} catch (Exception e) {
			if (!(e instanceof IllegalArgumentException) && !(e instanceof NullPointerException)) {
				log.warn("Unexpected exception while logging a deleted message.", e);
			}
			e.printStackTrace();
		}
	}

	private void logEdit(GuildMessageUpdateEvent event) {
		try {
			String hour = df.format(new Date(System.currentTimeMillis()));
			String logChannel = MantaroData.db().getGuild(event.getGuild()).getData().getGuildLogChannel();
			if (logChannel != null) {
				TextChannel tc = event.getGuild().getTextChannelById(logChannel);
				User author = event.getAuthor();
				Message editedMessage = messageCache.get(event.getMessage().getId());
				if (editedMessage != null && !editedMessage.getContent().isEmpty() && !event.getChannel().getId().equals(logChannel)) {
					tc.sendMessage(String.format(EmoteReference.WARNING + "`[%s]` %s#%s *modified* a message in #%s.\n```diff\n-%s\n+%s```", hour, author.getName(), author.getDiscriminator(), event.getChannel().getName(), editedMessage.getContent().replace("```", ""), event.getMessage().getContent().replace("```", ""))).queue();
					messageCache.put(event.getMessage().getId(), event.getMessage());
					logTotal++;
				}
			}
		} catch (Exception e) {
			if (!(e instanceof NullPointerException) && !(e instanceof IllegalArgumentException)) {
				log.warn("Unexpected error while logging a edit.", e);
			}
			e.printStackTrace();
		}
	}

	private void logStatusChange(StatusChangeEvent event) {
		String hour = df.format(new Date(System.currentTimeMillis()));
		JDA jda = event.getJDA();
		log.info(String.format("`[%s] Shard #%d`: Changed from `%s` to `%s`", hour, jda.getShardInfo().getShardId(), event.getOldStatus(), event.getStatus()));
	}

	private void logUnban(GuildUnbanEvent event) {
		try{
			String hour = df.format(new Date(System.currentTimeMillis()));
			String logChannel = MantaroData.db().getGuild(event.getGuild()).getData().getGuildLogChannel();
			if (logChannel != null) {
				TextChannel tc = event.getGuild().getTextChannelById(logChannel);
				tc.sendMessage(String.format(EmoteReference.WARNING + "`[%s]` %s#%s just got unbanned.", hour, event.getUser().getName(), event.getUser().getDiscriminator())).queue();
				logTotal++;
			}
		}

		catch (Exception e) {
			if (!(e instanceof NullPointerException) && !(e instanceof IllegalArgumentException)) {
				log.warn("Unexpected error while logging a edit.", e);
			}
			e.printStackTrace();
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
			resetBirthdays(event.getGuild());
		}
	}

	private void onCommand(GuildMessageReceivedEvent event) {

		synchronized (messageCache) {
			if ((messageCache.size() + 1) > 2500) {
				Iterator<String> iterator = messageCache.keySet().iterator();
				iterator.next();
				iterator.remove();
			}

			messageCache.put(event.getMessage().getId(), event.getMessage());
		}

		//Cleverbot.
		if(event.getMessage().getRawContent().startsWith(event.getJDA().getSelfUser().getAsMention())){
			event.getChannel().sendMessage(MantaroBot.CLEVERBOT.getResponse(
					event.getMessage().getContent().replace(event.getAuthor().getAsMention() + " ", ""))
			).queue();
			return;
		}

		try {
			if (!event.getGuild().getSelfMember().getPermissions(event.getChannel()).contains(Permission.MESSAGE_WRITE))
				return;
			if (event.getAuthor().isBot()) return;
			if (CommandProcessor.run(event)) commandTotal++;
		} catch (IndexOutOfBoundsException e) {
			event.getChannel().sendMessage(EmoteReference.ERROR + "Query returned no results or incorrect type arguments. Check command help.").queue();
		} catch (PermissionException e) {
			event.getChannel().sendMessage(EmoteReference.ERROR + "The bot has no permission to execute this action. I need the permission: " + e.getPermission()).queue();
			log.warn("Exception catched and alternate message sent. We should look into this, anyway.", e);
		} catch (IllegalArgumentException e) { //NumberFormatException == IllegalArgumentException
			event.getChannel().sendMessage(EmoteReference.ERROR + "Incorrect type arguments. Check command help.").queue();
			log.warn("Exception catched and alternate message sent. We should look into this, anyway.", e);
		} catch (ReqlError e) {
			event.getChannel().sendMessage(EmoteReference.ERROR + "Seems that we are having some problems on our database... ").queue();
			log.warn("<@217747278071463937> RethinkDB is on fire. Go fix it.", e);
		} catch (Exception e) {
			event.getChannel().sendMessage(String.format("We caught a unfetched error while processing the command: ``%s`` with description: ``%s``\n"
					+ "**You might  want to contact Kodehawa#3457 with a description of how it happened or join the support guild** " +
					"(you can find it on bots.discord.pw [search for Mantaro] or on ~>about. There is probably people working on the fix already, though. (Also maybe you just got the arguments wrong))"
				, e.getClass().getSimpleName(), e.getMessage())).queue();

			log.warn(String.format("Cannot process command: %s. All we know is what's here and that the error is a ``%s``", event.getMessage().getRawContent(), e.getClass().getSimpleName()), e);
		}
	}

	private void onJoin(GuildJoinEvent event) {
		try{
			TextChannel tc = getLogChannel();
			String hour = df.format(new Date(System.currentTimeMillis()));

			if (MantaroData.db().getMantaroData().getBlackListedGuilds().contains(event.getGuild().getId())
					|| MantaroData.db().getMantaroData().getBlackListedUsers().contains(event.getGuild().getOwner().getUser().getId())) {
				event.getGuild().leave().queue();
				tc.sendMessage(String.format(EmoteReference.MEGA + "[%s] I left a guild with name: ``%s`` (%s members) since it was blacklisted.", hour, event.getGuild().getName(), event.getGuild().getMembers().size())).queue();
				return;
			}

			tc.sendMessage(String.format(EmoteReference.MEGA + "[%s] I joined a new guild with name: ``%s`` (%s members)", hour, event.getGuild().getName(), event.getGuild().getMembers().size())).queue();
			logTotal++;

			GuildStatsManager.log(LoggedEvent.JOIN);
		}
		catch (Exception e) {
			if (!(e instanceof NullPointerException) && !(e instanceof IllegalArgumentException)) {
				log.warn("Unexpected error while logging a edit.", e);
			}
			e.printStackTrace();
		}
	}

	private void onLeave(GuildLeaveEvent event) {
		try{
			TextChannel tc = getLogChannel();
			String hour = df.format(new Date(System.currentTimeMillis()));

			if (event.getGuild().getMembers().isEmpty()) {
				tc.sendMessage(String.format(EmoteReference.THINKING + "[%s] A guild with name: ``%s`` just got deleted.", hour, event.getGuild().getName())).queue();
				logTotal++;
				return;
			}

			tc.sendMessage(String.format(EmoteReference.SAD + "I left a guild with name: ``%s`` (%s members)", event.getGuild().getName(), event.getGuild().getMembers().size())).queue();
			logTotal++;

			GuildStatsManager.log(LoggedEvent.LEAVE);
		}
		catch (Exception e) {
			if (!(e instanceof NullPointerException) && !(e instanceof IllegalArgumentException)) {
				log.warn("Unexpected error while logging a edit.", e);
			}
			e.printStackTrace();
		}
	}

	private void onUserJoin(GuildMemberJoinEvent event) {
		try{
			String role = MantaroData.db().getGuild(event.getGuild()).getData().getGuildAutoRole();
			String hour = df.format(new Date(System.currentTimeMillis()));
			if (role != null) {
				event.getGuild().getController().addRolesToMember(event.getMember(), event.getGuild().getRoleById(role)).queue(s -> log.debug("Successfully added a new role to " + event.getMember()), error -> {
					if (error instanceof PermissionException) {
						MantaroData.db().getGuild(event.getGuild()).getData().setGuildAutoRole(null);
						event.getGuild().getOwner().getUser().openPrivateChannel().queue(messageChannel ->
								messageChannel.sendMessage("Removed autorole since I don't have the permissions to assign that role").queue());
					} else {
						log.warn("Error while applying roles", error);
					}
				});
			}

			String logChannel = MantaroData.db().getGuild(event.getGuild()).getData().getGuildLogChannel();
			if (logChannel != null) {
				TextChannel tc = event.getGuild().getTextChannelById(logChannel);
				tc.sendMessage("[" + hour + "] " + "\uD83D\uDCE3 " + event.getMember().getEffectiveName() + " just joined (User #" + event.getGuild().getMembers().size() + ")").queue();
				logTotal++;
			}
		}
		catch (Exception e) {
			if (!(e instanceof NullPointerException) && !(e instanceof IllegalArgumentException)) {
				log.warn("Unexpected error while logging a edit.", e);
			}
			e.printStackTrace();
		}
	}

	private void resetBirthdays(Guild guild) {
		DBGuild data = MantaroData.db().getGuild(guild);
		data.getData().setBirthdayChannel(null);
		data.getData().setBirthdayRole(null);
		data.save();
	}
}
