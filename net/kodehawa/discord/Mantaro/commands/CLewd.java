package net.kodehawa.discord.Mantaro.commands;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;

public class CLewd implements Command {

	@Override
	@ModuleProperties(level = "user", name = "lewd", type = "common", description = "Is it lood? I'm sure it's lewd.")
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		evt.getChannel().sendMessage("Y-You lewdie!" + "\r\n" 
		 + "http://puu.sh/rzVEe/c8272e7c84.png");
	}
}
