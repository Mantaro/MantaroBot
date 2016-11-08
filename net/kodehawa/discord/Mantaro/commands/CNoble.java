package net.kodehawa.discord.Mantaro.commands;

import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;
import net.kodehawa.discord.Mantaro.utils.StringArrayFile;

public class CNoble implements Command {

	private CopyOnWriteArrayList<String> nobleQuotes = new CopyOnWriteArrayList<String>();
	
	public CNoble()
	{
		new StringArrayFile("noble", "mantaro", nobleQuotes, false);
	}
	
	@Override
	@ModuleProperties(level = "user", name = "noble", type = "common", description = "Says noble (Lost Pause) quotes.")
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		Random rand = new Random();
		int nobleQuote = rand.nextInt(nobleQuotes.size());
		
		evt.getChannel().sendMessageAsync(nobleQuotes.get(nobleQuote) + " -Noble", null);
	}

}
