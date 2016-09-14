package net.kodehawa.discord.Mantaro.commands;

import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;
import net.kodehawa.discord.Mantaro.utils.StringArrayFile;

public class CHi implements Command {

	public static CopyOnWriteArrayList<String> greeting = new CopyOnWriteArrayList<String>();
	
	public CHi()
	{
		new StringArrayFile("Greetings", "mantaro", greeting, false);
	}
	
	@Override
	@ModuleProperties(level = "user", name = "hi", type = "common", description = "Greets you with a random message.")
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		Random rd = new Random();
	    int greetRandomizer = rd.nextInt(greeting.size());

	    evt.getChannel().sendMessage(greeting.get(greetRandomizer));

	}
}
