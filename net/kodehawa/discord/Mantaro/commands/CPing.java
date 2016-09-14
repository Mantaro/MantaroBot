package net.kodehawa.discord.Mantaro.commands;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;

public class CPing implements Command {

	@Override
	@ModuleProperties(level = "user", name = "ping", type = "common", description = "Pong?.")
	public boolean isAvaliable(String[] message, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
	    long start = System.currentTimeMillis();
		evt.getTextChannel().sendTyping();
	    long end = System.currentTimeMillis() - start;
		evt.getTextChannel().sendMessage("Pong to " + evt.getAuthor().getAsMention() + "in " + end + " ms.");

	}
}
