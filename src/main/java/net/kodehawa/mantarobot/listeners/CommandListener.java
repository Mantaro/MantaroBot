package net.kodehawa.mantarobot.listeners;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.thread.ThreadPoolHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TreeMap;

public class CommandListener extends OptimizedListener<GuildMessageReceivedEvent> {
	private static Logger LOGGER = LoggerFactory.getLogger("CommandListener");
	public static int commandTotal = 0;

	//For later usage in LogListener.
	//A short message cache of 250 messages. If it reaches 250 it will delete the first one stored, and continue being 250
	static TreeMap<String, Message> shortMessageHistory = new TreeMap<>();

	public static String getCommandTotal() {
		return String.valueOf(commandTotal);
	}

	public CommandListener() {
		super(GuildMessageReceivedEvent.class);
	}

	@Override
	public void event(GuildMessageReceivedEvent event) {
		if (shortMessageHistory.size() < 250) {
			shortMessageHistory.put(event.getMessage().getId(), event.getMessage());
		} else {
			shortMessageHistory.remove(shortMessageHistory.firstKey());
			shortMessageHistory.put(event.getMessage().getId(), event.getMessage());
		}

		ThreadPoolHelper.DEFAULT().startThread("CmdThread", () -> onCommand(event));
	}

	private void onCommand(GuildMessageReceivedEvent event) {
		try {
			if (Mantaro.getParser().parse(event).onCommand()) commandTotal++;
		} catch (Exception e) {
			//TODO HANDLE THIS PROPERLY NOW.
			//Now this catch block handles the exceptions that can happen while on Command Execution.
			//Should look a better way of handling/logging this.

			LOGGER.warn("\"Cannot process command? Prefix is probably null, look into this. \" + event.getMessage().getRawContent()", e);
			e.printStackTrace();
		}
	}
}
