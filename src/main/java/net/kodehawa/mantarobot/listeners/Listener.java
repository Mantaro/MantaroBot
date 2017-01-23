package net.kodehawa.mantarobot.listeners;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.guild.Parameters;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.log.Log;
import net.kodehawa.mantarobot.log.State;
import net.kodehawa.mantarobot.log.Type;
import net.kodehawa.mantarobot.thread.ThreadPoolHelper;

import java.util.TreeMap;

public class Listener extends OptimizedListener<GuildMessageReceivedEvent> {

	public static int commandTotal = 0;
	//For later usage in LogListener. A short message cache of 250 messages. If it reaches 150 it will delete the first one stored, and continue being 250
	static TreeMap<String, Message> shortMessageHistory = new TreeMap<>();

	public static String getCommandTotal() {
		return String.valueOf(commandTotal);
	}

	public Listener() {
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

		try {
			String px = Parameters.getPrefixForServer(event.getGuild().getId());
			String content = event.getMessage().getContent();

			if (content.startsWith(px) || content.startsWith(Parameters.getPrefixForServer("default")) && !event.getAuthor().isBot()) {
				commandTotal++;
				Runnable messageThread = () -> {
					try {
						Mantaro.instance().onCommand(Mantaro.instance().getParser().parse(px, content, event));
					} catch (NullPointerException e) {
						try {
							Mantaro.instance().onCommand(Mantaro.instance().getParser().parse(Parameters.getPrefixForServer("default"), content, event));
						} catch (NullPointerException e1) {
							Log.instance().print("Cannot process command? Prefix is probably null, look into this. " + content, this.getClass(), Type.WARNING, e);
							e1.printStackTrace();
						}
					}
				};
				ThreadPoolHelper.instance().startThread("Message Thread", messageThread);
			}
		} catch (NullPointerException e) {
			if (Mantaro.instance().getState().equals(State.POSTLOAD)) {
				e.printStackTrace();
				Log.instance().print("Caught an error while processsing a command during POSTLOAD state. This is very wrong!", this.getClass(), Type.WARNING, e);
			}
		}

	}
}
