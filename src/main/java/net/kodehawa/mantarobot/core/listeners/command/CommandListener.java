package net.kodehawa.mantarobot.core.listeners.command;

import br.com.brjdevs.java.utils.extensions.Async;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.rethinkdb.gen.exc.ReqlError;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.hooks.EventListener;
import net.kodehawa.mantarobot.core.CommandProcessor;
import net.kodehawa.mantarobot.core.ShardMonitorEvent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.utils.Cleverbot;
import net.kodehawa.mantarobot.utils.Snow64;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CommandListener implements EventListener {

	private static final Map<String, CommandProcessor> CUSTOM_PROCESSORS = new ConcurrentHashMap<>();
	private static final CommandProcessor DEFAULT_PROCESSOR = new CommandProcessor();
	//Message cache of 2500 messages. If it reaches 2500 it will delete the first one stored, and continue being 2500
	@Getter
	private static final Cache<String, Optional<Message>> messageCache = CacheBuilder.newBuilder().concurrencyLevel(10).maximumSize(2500).build();
	private static int commandTotal = 0;

	public static void clearCustomProcessor(String channelId) {
		CUSTOM_PROCESSORS.remove(channelId);
	}

	public static String getCommandTotal() {
		return String.valueOf(commandTotal);
	}

	public static void setCustomProcessor(String channelId, CommandProcessor processor) {
		CUSTOM_PROCESSORS.put(channelId, processor);
	}
	private final Random random = new Random();
	private final int shardId;

	public CommandListener(int shardId) {
		this.shardId = shardId;
	}

	@Override
	public void onEvent(Event event) {
		if (event instanceof ShardMonitorEvent) {
			((ShardMonitorEvent) event).alive(shardId, ShardMonitorEvent.COMMAND_LISTENER);
			return;
		}

		if (event instanceof GuildMessageReceivedEvent) {
			GuildMessageReceivedEvent e = (GuildMessageReceivedEvent) event;
			Async.thread("CmdThread", () -> onCommand(e));

			if (random.nextInt(150) > 100) {
				if (((GuildMessageReceivedEvent) event).getMember() == null) return;
				Player player = MantaroData.db().getPlayer(((GuildMessageReceivedEvent) event).getMember());
				if (player != null) {
					player.getData().setExperience(player.getData().getExperience() + Math.round(random.nextInt(6)));
					if (player.getData().getExperience() > Math.pow(player.getLevel(), 6)) {
						player.setLevel(player.getLevel() + 1);
					}
					player.saveAsync();
				}
			}
		}
	}

	private void onCommand(GuildMessageReceivedEvent event) {
        messageCache.put(event.getMessage().getId(), Optional.of(event.getMessage()));

		if (MantaroData.db().getGuild(event.getGuild()).getData().getDisabledChannels().contains(event.getChannel().getId())) {
			return;
		}

		//Cleverbot.
		if (event.getMessage().getRawContent().startsWith(event.getJDA().getSelfUser().getAsMention())) {
			Cleverbot.handle(event);
			return;
		}

		try {
			if (!event.getGuild().getSelfMember().getPermissions(event.getChannel()).contains(Permission.MESSAGE_WRITE))
				return;
			if (event.getAuthor().isBot()) return;
			if (CUSTOM_PROCESSORS.getOrDefault(event.getChannel().getId(), DEFAULT_PROCESSOR).run(event))
				commandTotal++;
		} catch (IndexOutOfBoundsException e) {
			event.getChannel().sendMessage(EmoteReference.ERROR + "Your query returned no results or incorrect type arguments. Check the command help.").queue();
			log.warn("Exception catched and alternate message sent. We should look into this, anyway.", e);
		} catch (PermissionException e) {
			event.getChannel().sendMessage(EmoteReference.ERROR + "I don't have permission to do this! I need the permission: " + e.getPermission()).queue();
			log.warn("Exception catched and alternate message sent. We should look into this, anyway.", e);
		} catch (IllegalArgumentException e) { //NumberFormatException == IllegalArgumentException
			event.getChannel().sendMessage(EmoteReference.ERROR + "Incorrect type arguments. Check command help.").queue();
			log.warn("Exception catched and alternate message sent. We should look into this, anyway.", e);
		} catch (ReqlError e) {
			event.getChannel().sendMessage(EmoteReference.ERROR + "Sorry! I'm having some problems with my database... ").queue();
			log.warn("<@217747278071463937> RethinkDB is on fire. Go fix it.", e);
		} catch (Exception e) {
			String id = Snow64.toSnow64(Long.parseLong(event.getMessage().getId()));

			event.getChannel().sendMessage(
				EmoteReference.ERROR + "I ran into an unexpected error. (Error ID: ``" + id + "``)\n" +
					"If you want, **contact ``Kodehawa#3457`` on DiscordBots** (popular bot guild), or join our **support guild** (Link on ``~>about``). Don't forget the Error ID!"
			).queue();

			log.warn("Unexpected Exception on Command ``" + event.getMessage().getRawContent() + "`` (Error ID: ``" + id + "``)", e);
		}
	}
}
