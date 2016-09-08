package net.kodehawa.discord.Mantaro.commands;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;

public class CMarco implements Command {

	@Override
	@ModuleProperties(level = "user", name = "marco", type = "common", description = "Polo.")
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		evt.getTextChannel().sendMessage("Polo!");
	}

	@Override
	public void actionResult(boolean result, MessageReceivedEvent evt) 
	{
		System.out.println("Command executed " + this.getClass().getName() + ".");
	}

}
