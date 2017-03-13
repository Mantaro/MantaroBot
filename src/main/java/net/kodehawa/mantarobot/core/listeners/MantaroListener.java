package net.kodehawa.mantarobot.core.listeners;

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
import net.kodehawa.mantarobot.commands.rpg.entity.player.EntityPlayerMP;
import net.kodehawa.mantarobot.commands.rpg.world.TextChannelWorld;
import net.kodehawa.mantarobot.core.CommandProcessor;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.ThreadPoolHelper;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class MantaroListener implements EventListener {
	private static Logger LOGGER = LoggerFactory.getLogger("CommandListener");
	private static int commandTotal = 0;
	private static int logTotal = 0;
	//Message cache of 2500 messages. If it reaches 2500 it will delete the first one stored, and continue being 2500
	private static LinkedHashMap<String, Message> messageCache = new LinkedHashMap<>(2500);
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
	private int logChannelShardId;

	public MantaroListener() {
		logChannelShardId = Arrays.stream(MantaroBot.getInstance().getShards()).filter(shard -> shard.getJDA().getTextChannelById("266231083341840385") != null).findFirst().orElse(null).getId();
	}

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
		return MantaroBot.getInstance().getShard(logChannelShardId).getJDA().getTextChannelById("266231083341840385");
	}

	private void logStatusChange(StatusChangeEvent event) {
		JDA jda = event.getJDA();
		getLogChannel().sendMessage("Status Change Event on Shard " + jda.getShardInfo().getShardId() + ": Changed from " + event.getOldStatus() + " to " + event.getStatus()).queue();
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
		if(event.getMessage().getContent().length() > 1990){
			return;
		}

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
		if(event.getMessage().getContent().length() > 1990){
			return;
		}

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
		try {
			Role birthdayRole = event.getGuild().getRoleById(MantaroData.getData().get().getGuild(event.getGuild(), false).birthdayRole);
			EntityPlayerMP user = EntityPlayerMP.getPlayer(event.getAuthor());
			if (birthdayRole != null && user.getBirthdayDate() != null) {
				TextChannel channel = event.getGuild().getTextChannelById(MantaroData.getData().get().getGuild(event.getGuild(), false).birthdayChannel);
				Calendar cal = Calendar.getInstance();
				SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
				if (user.getBirthdayDate().substring(0, 5).equals(dateFormat.format(cal.getTime()).substring(0, 5))) {
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

		//Tick worlds and entities.
		if (r.nextInt(200) > 100) {
			ticks++;
			TextChannelWorld world = TextChannelWorld.of(event);
			world.tick(event);
		}

        messageCache.put(event.getMessage().getId(), event.getMessage());
		assert messageCache.size() <= 2500 : "Cache not deleting";

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
		TextChannel tc = getLogChannel();

		if (MantaroData.getData().get().blacklistedGuilds.contains(event.getGuild().getId())) {
			event.getGuild().leave().queue();
			tc.sendMessage(String.format(EmoteReference.MEGA + "I left a guild with name: ``%s`` (%s members) since it was blacklisted.", event.getGuild().getName(), event.getGuild().getMembers().size())).queue();
			return;
		}

		tc.sendMessage(String.format(EmoteReference.MEGA + "I joined a new guild with name: ``%s`` (%s members)", event.getGuild().getName(), event.getGuild().getMembers().size())).queue();
		logTotal++;

		GuildStatsManager.log(LoggedEvent.JOIN);
	}

	private void onLeave(GuildLeaveEvent event) {
		TextChannel tc = getLogChannel();
		if (event.getGuild().getMembers().isEmpty()) {
			tc.sendMessage(String.format(EmoteReference.THINKING + "A guild with name: ``%s`` just got deleted.", event.getGuild().getName())).queue();
			logTotal++;
			return;
		}

		tc.sendMessage(String.format(EmoteReference.SAD + "I left a guild with name: ``%s`` (%s members)", event.getGuild().getName(), event.getGuild().getMembers().size())).queue();
		logTotal++;

		GuildStatsManager.log(LoggedEvent.LEAVE);
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

	private void resetBirthdays(Guild guild) {
		MantaroData.getData().get().getGuild(guild, false).birthdayChannel = null;
		MantaroData.getData().get().getGuild(guild, false).birthdayRole = null;
		MantaroData.getData().save();
	}
}
