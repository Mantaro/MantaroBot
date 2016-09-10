package net.kodehawa.discord.Mantaro.listeners;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.kodehawa.discord.Mantaro.bot.MantaroBot;
import net.kodehawa.discord.Mantaro.utils.Values;

public class MessageListener extends ListenerAdapter 
{
	
	public static boolean isMenction = false;
	
	@Override
	public void onMessageReceived(MessageReceivedEvent evt)
	{
		boolean isPrivate = evt.isPrivate();
		
		if(!isPrivate && !Values.disabledServers.contains(evt.getGuild().getId()))
		{
			if(evt.getMessage().getContent().startsWith("@MantaroBot") || evt.getMessage().getContent().startsWith(MantaroBot.getInstance().getBotPrefix()) && evt.getMessage().getAuthor().getId() != evt.getJDA().getSelfInfo().getId())
			{
				if(evt.getMessage().getContent().startsWith("@MantaroBot")){ isMenction = true; } else { isMenction = false; }
				MantaroBot.onCommand(MantaroBot.getInstance().getParser().parse(evt.getMessage().getContent(), evt));
			}
		}
		else if(evt.getMessage().getContent().startsWith("~>bot.status "))
		{
			MantaroBot.onCommand(MantaroBot.getInstance().getParser().parse(evt.getMessage().getContent(), evt));
		}
	}
}

