package net.kodehawa.discord.Mantaro.commands;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;

public class CRoasted implements Command {

	@Override
	@ModuleProperties(level = "user", name = "roasted", type = "common", description = "OHH.")
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		evt.getChannel().sendMessage("https://i.imgflip.com/v4tdb.jpg");
	}
}
