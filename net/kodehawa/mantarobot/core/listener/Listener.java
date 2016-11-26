package net.kodehawa.mantarobot.core.listener;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.kodehawa.mantarobot.core.Mantaro;

public class Listener extends ListenerAdapter {

	@Override
	public void onMessageReceived(MessageReceivedEvent event)
	{
		if(event.getMessage().getContent().startsWith(Mantaro.instance().getPrefix()) && event.getMessage().getAuthor().getId() != event.getJDA().getSelfUser().getId())
		{
			try {
				Mantaro.instance().onCommand(Mantaro.instance().getParser().parse(event.getMessage().getContent(), event));
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		
		
		if(event.getMessage().getContent().contains("you broke")  || event.getMessage().getContent().contains("You broke") || event.getMessage().getContent().contains("it's broken") || event.getMessage().getContent().contains("I broke the")){
			event.getChannel().sendMessage("It's not broken, it's a feature.");
		}
		if(event.getMessage().getContent().contains("awoo") || event.getMessage().getContent().contains("Awoo")){
			event.getChannel().sendMessage("https://pbs.twimg.com/profile_images/578805576701870080/jr1_XDbp.jpeg");
		}
	}
}
