package net.kodehawa.mantarobot.listeners;

import java.util.TreeMap;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.kodehawa.mantarobot.cmd.servertools.Parameters;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.thread.ThreadPoolHelper;

public class Listener extends ListenerAdapter {

	//For later usage in LogListener. A short message cache of 150 messages. If it reaches 150 it will delete the first one stored, and continue being 150
	public static TreeMap<String, Message> shortMessageHistory = new TreeMap<>();
	
	private String px;
	@Override
	public void onMessageReceived(MessageReceivedEvent event){
		try{px = Parameters.getPrefixForServer(event.getGuild().getId());} catch(NullPointerException ignored){}
		if(px == null){	px = Parameters.getPrefixForServer("default"); }

		if(shortMessageHistory.size() < 250){
			shortMessageHistory.put(event.getMessage().getId(), event.getMessage());
		} else {
			shortMessageHistory.remove(shortMessageHistory.firstKey());
			shortMessageHistory.put(event.getMessage().getId(), event.getMessage());
		}
		
		if(event.getMessage().getContent().startsWith(px) && !event.getAuthor().isBot())
		{
			Runnable messageThread = () ->{
				try {
					Mantaro.instance().onCommand(Mantaro.instance().getParser().parse(px, event.getMessage().getContent(), event));
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
			ThreadPoolHelper.instance().startThread("Message Thread", messageThread);
		}
	}
}
