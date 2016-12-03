package net.kodehawa.mantarobot.listeners;

import java.util.TreeMap;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.kodehawa.mantarobot.core.Mantaro;

public class Listener extends ListenerAdapter {

	//For later usage in LogListener.
	public static TreeMap<String, Message> shortMessageHistory = new TreeMap<String, Message>();
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event){
		Thread thread = new Thread(){
			public void run(){
				if(shortMessageHistory.size() < 15){
					shortMessageHistory.put(event.getMessage().getId(), event.getMessage());
				} else {
					shortMessageHistory.remove(shortMessageHistory.firstKey());
					shortMessageHistory.put(event.getMessage().getId(), event.getMessage());
				}
				
				if(event.getMessage().getContent().startsWith(Mantaro.instance().getPrefix()) && 
						event.getMessage().getAuthor().getId() != event.getJDA().getSelfUser().getId() && !event.getAuthor().isBot())
				{
					try {
						Mantaro.instance().onCommand(Mantaro.instance().getParser().parse(event.getMessage().getContent(), event));
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}
				
				if(event.getMessage().getContent().contains("you broke")  || event.getMessage().getContent().contains("You broke") 
						|| event.getMessage().getContent().contains("it's broken") || event.getMessage().getContent().contains("I broke the")){
					event.getChannel().sendMessage("It's not broken, it's a feature.");
				}
				if(event.getMessage().getContent().contains("awoo") || event.getMessage().getContent().contains("Awoo")){
					event.getChannel().sendMessage("https://pbs.twimg.com/profile_images/578805576701870080/jr1_XDbp.jpeg");
				}
			}
		};
		thread.setName("Message thread");
		thread.start();
		thread.interrupt();
	}
}
