package net.kodehawa.mantarobot.core.listeners;

import br.com.brjdevs.java.utils.extensions.Async;
import com.google.common.cache.CacheLoader;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.audio.hooks.ConnectionListener;
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
import net.kodehawa.mantarobot.commands.currency.RateLimiter;
import net.kodehawa.mantarobot.commands.custom.EmbedJSON;
import net.kodehawa.mantarobot.commands.info.GuildStatsManager;
import net.kodehawa.mantarobot.commands.info.GuildStatsManager.LoggedEvent;
import net.kodehawa.mantarobot.core.ShardMonitorEvent;
import net.kodehawa.mantarobot.core.listeners.command.CommandListener;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.DBGuild;
import net.kodehawa.mantarobot.data.entities.helpers.GuildData;
import net.kodehawa.mantarobot.data.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static net.dv8tion.jda.core.audio.hooks.ConnectionStatus.CONNECTING_AWAITING_ENDPOINT;
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
	public static final Pattern DISCORD_INVITE = Pattern.compile("(?:discord(?:(?:\\.|.?dot.?)gg|app(?:\\.|.?dot.?)com/invite)/(?<id>" +
			"([\\w]{10,16}|[a-zA-Z0-9]{4,8})))");
	private final RateLimiter slowModeLimiter = new RateLimiter(TimeUnit.SECONDS, 5);

	public MantaroListener(int shardId) {
		this.shardId = shardId;
	}

	@Override
	public void onEvent(Event event) {

		if (event instanceof ShardMonitorEvent) {
			((ShardMonitorEvent) event).alive(shardId, ShardMonitorEvent.MANTARO_LISTENER);
			return;
		}

		if (event instanceof GuildMessageReceivedEvent) {
			GuildMessageReceivedEvent e = (GuildMessageReceivedEvent) event;
			Async.thread("BirthdayThread", () -> onMessage(e));
			return;
		}

		//Log intensifies
		if (event instanceof GuildMessageUpdateEvent) {
			Async.thread("LogThread(Edit)", () -> logEdit((GuildMessageUpdateEvent) event));
			return;
		}

		if (event instanceof GuildMessageDeleteEvent) {
			Async.thread("LogThread(Delete)", () -> logDelete((GuildMessageDeleteEvent) event));
			return;
		}

		if (event instanceof GuildMemberJoinEvent) {
			Async.thread("LogThread(Join)", () -> onUserJoin((GuildMemberJoinEvent) event));
			return;
		}

		if (event instanceof GuildMemberLeaveEvent) {
			Async.thread("LogThread(Leave)", () -> onUserLeave((GuildMemberLeaveEvent) event));
			return;
		}

		if (event instanceof GuildUnbanEvent) {
			Async.thread("LogThread(Unban)", () -> logUnban((GuildUnbanEvent) event));
			return;
		}

		if (event instanceof GuildBanEvent) {
			Async.thread("LogThread(Ban)", () -> logBan((GuildBanEvent) event));
			return;
		}

		if (event instanceof GuildJoinEvent) {
			Async.thread("LogThread(GuildJoin)", () -> onJoin((GuildJoinEvent) event));
			return;
		}

		if (event instanceof GuildLeaveEvent) {
			Async.thread("LogThread(GuildLeave)", () -> onLeave((GuildLeaveEvent) event));
		}

		//debug
		if (event instanceof StatusChangeEvent) {
			logStatusChange((StatusChangeEvent) event);
		}

		if (event instanceof DisconnectEvent) {
			onDisconnect((DisconnectEvent) event);
		}

		if (event instanceof ExceptionEvent) {
			onException((ExceptionEvent) event);
		}
	}

	private TextChannel getLogChannel() {
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

					if(MantaroData.db().getGuild(event.getGuild()).getData().getLogExcludedChannels().contains(deletedMessage.getChannel().getId())){
						return;
					}

					logTotal++;
					tc.sendMessage(String.format(EmoteReference.WARNING + "`[%s]` Message created by **%s#%s** in channel **%s** was deleted.\n" +
							"```diff\n-%s```", hour, deletedMessage.getAuthor().getName(), deletedMessage.getAuthor().getDiscriminator(), event.getChannel().getName(), deletedMessage.getContent().replace("```", ""))).queue();
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

					if(MantaroData.db().getGuild(event.getGuild()).getData().getLogExcludedChannels().contains(editedMessage.getChannel().getId())){
						return;
					}

					tc.sendMessage(String.format(EmoteReference.WARNING + "`[%s]` Message created by **%s#%s** in channel **%s** was modified.\n```diff\n-%s\n+%s```",
							hour, author.getName(), author.getDiscriminator(), event.getChannel().getName(), editedMessage.getContent().replace("```", ""), event.getMessage().getContent().replace("```", ""))).queue();
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

	private void onMessage(GuildMessageReceivedEvent event) {

		//Moderation features
		GuildData guildData = MantaroData.db().getGuild(event.getGuild()).getData();

		if(guildData.isLinkProtection()){
			if(DISCORD_INVITE.matcher(event.getMessage().getContent()).find()
					&& !event.getMember().hasPermission(Permission.ADMINISTRATOR) && !event.getMember().hasPermission(Permission.MANAGE_SERVER)){
				Member bot = event.getGuild().getSelfMember();

				if(bot.hasPermission(Permission.MESSAGE_MANAGE) || bot.hasPermission(Permission.ADMINISTRATOR)){
					User author = event.getAuthor();

					//Ignore myself.
					if(event.getAuthor().getId().equals(event.getJDA().getSelfUser().getId())){
						return;
					}

					//Ignore log channel.
					if(guildData.getGuildLogChannel() != null && event.getChannel().getId().equals(guildData.getGuildLogChannel())){
						return;
					}

					//Yes, I know the check previously done is redundant, but in case someone decides to change the law of nature, it should do	.

					event.getMessage().delete().queue();
					event.getChannel().sendMessage(EmoteReference.ERROR + "**You cannot advertise here.** Deleted invite link sent by **" + author.getName() + "#" + author.getDiscriminator() + "**.").queue();
				} else {
					event.getChannel().sendMessage(EmoteReference.ERROR + "I cannot remove the invite link because I don't have permission to delete messages!").queue();
				}
			}
		}

		if(guildData.isSlowMode()){
			if (!slowModeLimiter.process(event.getAuthor().getId())) {
				Member bot = event.getGuild().getSelfMember();
				if(bot.hasPermission(Permission.MESSAGE_MANAGE) || bot.hasPermission(Permission.ADMINISTRATOR)
					&& !event.getMember().hasPermission(Permission.ADMINISTRATOR) && !event.getMember().hasPermission(Permission.MANAGE_SERVER)){
					event.getMessage().delete().queue();
				}else {
					event.getChannel().sendMessage(EmoteReference.ERROR + "I cannot engage slow mode because I don't have permission to delete messages!").queue();
					DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
					guildData.setSlowMode(false);
					dbGuild.save();
					event.getChannel().sendMessage(EmoteReference.WARNING + "**Disabled slowmode due to a lack of permissions.**").queue();
				}
			}
		}


		//Birthday role checker.
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
				event.getChannel().sendMessage(EmoteReference.ERROR + "Error while applying birthday role, so the role assigner will be resetted. **Remember that the bot MUST have permissions to apply roles to that person, always**").queue();
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

			tc.sendMessage(String.format(EmoteReference.MEGA + "`[%s]` I joined a new guild with name: ``%s`` (%s members) [ID: `%s`, Owner:`%s#%s`]",
				hour, event.getGuild().getName(), event.getGuild().getMembers().size(), event.getGuild().getId(),
				event.getGuild().getOwner().getEffectiveName(), event.getGuild().getOwner().getUser().getDiscriminator())).queue();
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

			MantaroBot.getInstance().getAudioManager().getMusicManagers().remove(event.getGuild().getId());
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
			DBGuild dbg = MantaroData.db().getGuild(event.getGuild());
			GuildData data = dbg.getData();

			String hour = df.format(new Date(System.currentTimeMillis()));
			if (role != null) {
				try{
					event.getGuild().getController().addRolesToMember(event.getMember(), event.getGuild().getRoleById(role)).queue(s ->
							log.debug("Successfully added a new role to " + event.getMember()));
				} catch (PermissionException e){
					MantaroData.db().getGuild(event.getGuild()).getData().setGuildAutoRole(null);
					MantaroData.db().getGuild(event.getGuild()).save();
					event.getGuild().getOwner().getUser().openPrivateChannel().queue(messageChannel ->
							messageChannel.sendMessage("Removed autorole since I don't have the permissions to assign that role").queue());
				}
			}

			String logChannel = MantaroData.db().getGuild(event.getGuild()).getData().getGuildLogChannel();
			if (logChannel != null) {
				TextChannel tc = event.getGuild().getTextChannelById(logChannel);
				if (!event.getGuild().getSelfMember().hasPermission(tc, Permission.MESSAGE_READ) || !event.getGuild().getSelfMember().hasPermission(tc, Permission.MESSAGE_WRITE)) return;
				tc.sendMessage(String.format("`[%s]` \uD83D\uDCE3 `%s#%s` just joined `%s` `(User #%d | ID:%s)`", hour, event.getMember().getEffectiveName(), event.getMember().getUser().getDiscriminator(), event.getGuild().getName(), event.getGuild().getMembers().size(), event.getGuild().getId())).queue();
				logTotal++;
			}

			String joinChannel = MantaroData.db().getGuild(event.getGuild()).getData().getLogJoinLeaveChannel();
			String joinMessage = MantaroData.db().getGuild(event.getGuild()).getData().getJoinMessage();

			if (joinChannel != null && joinMessage != null) {
				TextChannel tc = event.getGuild().getTextChannelById(joinChannel);

				if (!event.getGuild().getSelfMember().hasPermission(tc, Permission.MESSAGE_READ) || !event.getGuild().getSelfMember().hasPermission(tc, Permission.MESSAGE_WRITE)) return;

				if (joinMessage.contains("$(")) {
					Map<String, String> dynamicMap = new HashMap<>();
					map("event", dynamicMap, event);
					joinMessage = dynamicResolve(joinMessage, dynamicMap);
				}

				int c = joinMessage.indexOf(':');
				if (c != -1) {
					String m = joinMessage.substring(0, c);
					String v = joinMessage.substring(c + 1);

					if (m.equals("embed")) {
						EmbedJSON embed;
						try {
							embed = GsonDataManager.gson(false).fromJson('{' + v + '}', EmbedJSON.class);
						} catch (Exception ignored) {
							tc.sendMessage(EmoteReference.ERROR2 + "The string ``{" + v + "}`` isn't a valid JSON.").queue();
							return;
						}

						tc.sendMessage(embed.gen(event)).queue();
						return;
					}
				}

				tc.sendMessage(joinMessage).queue();
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
			DBGuild dbg = MantaroData.db().getGuild(event.getGuild());
			GuildData data = dbg.getData();


			String logChannel = MantaroData.db().getGuild(event.getGuild()).getData().getGuildLogChannel();
			if (logChannel != null) {
				TextChannel tc = event.getGuild().getTextChannelById(logChannel);
				if(event.getGuild().getSelfMember().hasPermission(Permission.ADMINISTRATOR) || event.getGuild().getSelfMember().hasPermission(tc, Permission.MESSAGE_WRITE)){
					tc.sendMessage("`[" + hour + "]` " + "\uD83D\uDCE3 `" + event.getMember().getEffectiveName() + "#" + event.getMember().getUser().getDiscriminator() + "` just left `" + event.getGuild().getName() + "` `(User #" + event.getGuild().getMembers().size() + ")`").queue();
					logTotal++;
				} else {
					event.getGuild().getPublicChannel().sendMessage(EmoteReference.ERROR + "I tried logging a leave event but I don't have permissions to write in the specified channel... Disabling for now.").queue();
					data.setGuildLogChannel(null);
					dbg.save();
					return;
				}
			}

			String leaveChannel = MantaroData.db().getGuild(event.getGuild()).getData().getLogJoinLeaveChannel();
			String leaveMessage = MantaroData.db().getGuild(event.getGuild()).getData().getLeaveMessage();


			if (leaveChannel != null && leaveMessage != null) {
				TextChannel tc = event.getGuild().getTextChannelById(leaveChannel);

				if(!event.getGuild().getSelfMember().hasPermission(Permission.ADMINISTRATOR) || !event.getGuild().getSelfMember().hasPermission(tc, Permission.MESSAGE_WRITE)){
					event.getGuild().getPublicChannel().sendMessage(EmoteReference.ERROR + "I tried saying the leave message but I don't have permissions to write in the specified channel... Disabling for now.").queue();
					data.setLogJoinLeaveChannel(null);
					dbg.save();
					return;
				}

				if (leaveMessage.contains("$(")) {
					Map<String, String> dynamicMap = new HashMap<>();
					map("event", dynamicMap, event);
					leaveMessage = dynamicResolve(leaveMessage, dynamicMap);
				}

				int c = leaveMessage.indexOf(':');
				if (c != -1) {
					String m = leaveMessage.substring(0, c);
					String v = leaveMessage.substring(c + 1);

					if (m.equals("embed")) {
						EmbedJSON embed;
						try {
							embed = GsonDataManager.gson(false).fromJson('{' + v + '}', EmbedJSON.class);
						} catch (Exception ignored) {
							tc.sendMessage(EmoteReference.ERROR2 + "The string ``{" + v + "}`` isn't a valid JSON.").queue();
							return;
						}

						tc.sendMessage(embed.gen(event)).queue();
						return;
					}
				}
				tc.sendMessage(leaveMessage).queue();
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
