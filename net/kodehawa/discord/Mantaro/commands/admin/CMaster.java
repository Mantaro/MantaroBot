package net.kodehawa.discord.Mantaro.commands.admin;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;

public class CMaster implements Command {

	@Override
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	@ModuleProperties(level = "master", name = "master", type = "common", description = "M-Master is here!")
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		// TODO Auto-generated method stub
		if(evt.getAuthor().getId().equals("155867458203287552"))
		{
			evt.getChannel().sendMessage("https://d.wattpad.com/story_parts/198038817/images/1421cc6b94affc88.gif" + "\r\n" + "Master!");
			//evt.getChannel().sendMessage("Master!");
		}
		else
		{
			evt.getChannel().sendMessage("B-But you're not my master");
		}
	}

	@Override
	public void actionResult(boolean result, MessageReceivedEvent evt) {
		System.out.println("Command executed " + this.getClass().getName() + ", in channel " + evt.getChannel().toString() + " (" + evt.getMessage().toString() + " )");
	}

}
