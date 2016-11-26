package net.kodehawa.mantarobot.cmd;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.kodehawa.mantarobot.cmd.management.Command;

public class Ping extends Command {

	public Ping()
	{
		setName("ping");
	}
	
	@Override
	public void onCommand(String[] message, String content, MessageReceivedEvent event)
	{
        channel = event.getChannel();
		
		event.getTextChannel().sendMessage(":mega: Pong").queue(sentMessage ->
		{
			long start = System.currentTimeMillis();
			try {
				channel.sendTyping().block();
			} catch (RateLimitedException e) {
				e.printStackTrace();
			}
		    long end = System.currentTimeMillis() - start;
			sentMessage.editMessage(":mega: Pong to " + event.getAuthor().getAsMention() + " in " + end + " ms.").queue();
		});
		
	}
}
