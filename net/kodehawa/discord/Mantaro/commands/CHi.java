package net.kodehawa.discord.Mantaro.commands;

import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.file.StringArrayFile;
import net.kodehawa.discord.Mantaro.main.Command;

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
		if(!evt.getAuthor().getId().equals("205505246645059586"))
		{
			Random rd = new Random();
	        int greetRandomizer = rd.nextInt(greeting.size());

			evt.getChannel().sendMessage(greeting.get(greetRandomizer));
		}
		
		else
		{
			evt.getChannel().sendMessage("Oh.. Bonjour.. J-je te trouve resplendissante aujourd'hui!");
		}
		
	}

	@Override
	public void actionResult(boolean result, MessageReceivedEvent evt) {
		System.out.println("Command executed " + this.getClass().getName() + ".");
	}

}
