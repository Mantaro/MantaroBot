package net.kodehawa.discord.Mantaro.commands.mention;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;

public class MentionMeow implements Command {

	@Override
	@ModuleProperties(level = "user", name = "nya", type = "mention", description = "Meows.")
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		evt.getChannel().sendMessage("Meow.");
	}
}
