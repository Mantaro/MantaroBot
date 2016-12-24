package net.kodehawa.mantarobot.listeners;

import java.util.TreeMap;

import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.kodehawa.mantarobot.cmd.guild.Parameters;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.thread.ThreadPoolHelper;

public class Listener extends ListenerAdapter {

	//For later usage in LogListener. A short message cache of 250 messages. If it reaches 150 it will delete the first one stored, and continue being 250
	static TreeMap<String, Message> shortMessageHistory = new TreeMap<>();
	private static int commandTotal = 0;
	private String px;

	@Override
	public void onMessageReceived(MessageReceivedEvent event){
		if(event.isFromType(ChannelType.TEXT)){
			px = Parameters.getPrefixForServer(event.getGuild().getId());
			String content = event.getMessage().getContent();

			if(shortMessageHistory.size() < 250){
				shortMessageHistory.put(event.getMessage().getId(), event.getMessage());
			} else {
				shortMessageHistory.remove(shortMessageHistory.firstKey());
				shortMessageHistory.put(event.getMessage().getId(), event.getMessage());
			}

			if(content.startsWith(px) || content.startsWith(Parameters.getPrefixForServer("default")) && !event.getAuthor().isBot())
			{
				commandTotal++;
				Runnable messageThread = () ->{
					try {
						Mantaro.instance().onCommand(Mantaro.instance().getParser().parse(px, content, event));
					} catch (Exception e) {
						try {
							Mantaro.instance().onCommand(Mantaro.instance().getParser().parse(Parameters.getPrefixForServer("default"), content, event));
						} catch (NullPointerException e1) {
							e1.printStackTrace();
						}
					}
				};
				ThreadPoolHelper.instance().startThread("Message Thread", messageThread);
			}
		}
	}

	public static String getCommandTotal(){
		return String.valueOf(commandTotal);
	}
}
