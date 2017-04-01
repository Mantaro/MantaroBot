package net.kodehawa.mantarobot.core.listeners.command;

import br.com.brjdevs.java.utils.extensions.Async;
import com.rethinkdb.gen.exc.ReqlError;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.hooks.EventListener;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.CommandProcessor;
import net.kodehawa.mantarobot.core.ShardMonitorEvent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CommandListener implements EventListener {

	//Message cache of 2500 messages. If it reaches 2500 it will delete the first one stored, and continue being 2500
	@Getter
	private static final Map<String, Message> messageCache = Collections.synchronizedMap(new LinkedHashMap<>(2500));
	private static int commandTotal = 0;
	private Random random = new Random();
	private static final Map<String, CommandProcessor> CUSTOM_PROCESSORS = new ConcurrentHashMap<>();
	private static final CommandProcessor DEFAULT_PROCESSOR = new CommandProcessor();
	private final int shardId;

	public CommandListener(int shardId) {
	    this.shardId = shardId;
    }

	public static void setCustomProcessor(String channelId, CommandProcessor processor) {
		CUSTOM_PROCESSORS.put(channelId, processor);
	}

	@Override
	public void onEvent(Event event) {
	    if(event instanceof ShardMonitorEvent) {
	        ((ShardMonitorEvent) event).alive(shardId, ShardMonitorEvent.COMMAND_LISTENER);
	        return;
        }
		if (event instanceof GuildMessageReceivedEvent) {
			GuildMessageReceivedEvent e = (GuildMessageReceivedEvent) event;
			Async.thread("CmdThread", () -> onCommand(e));

			if(random.nextInt(500) > 450){
				Player player = MantaroData.db().getPlayer(((GuildMessageReceivedEvent) event).getMember());
				player.getData().incrementExperience();
				if(player.getData().getExperience() > Math.pow(player.getLevel(), 17)){
					player.setLevel(player.getLevel() + 1);
				}
				player.saveAsync();
			}
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
		if (event.getMessage().getRawContent().startsWith(event.getJDA().getSelfUser().getAsMention())) {
			event.getChannel().sendMessage(MantaroBot.CLEVERBOT.getResponse(
					event.getMessage().getRawContent().replaceFirst("<!?@.+?> ", ""))
			).queue();
			return;
		}

		try {
			if (!event.getGuild().getSelfMember().getPermissions(event.getChannel()).contains(Permission.MESSAGE_WRITE))
				return;
			if (event.getAuthor().isBot()) return;
			if (CUSTOM_PROCESSORS.getOrDefault(event.getChannel().getId(), DEFAULT_PROCESSOR).run(event))
				commandTotal++;
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

	public static String getCommandTotal() {
		return String.valueOf(commandTotal);
	}
	public static void clearCustomProcessor(String channelId) {
		CUSTOM_PROCESSORS.remove(channelId);
	}
}
