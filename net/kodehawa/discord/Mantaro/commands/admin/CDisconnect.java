package net.kodehawa.discord.Mantaro.commands.admin;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.commands.CQuotation;
import net.kodehawa.discord.Mantaro.main.Command;
import net.kodehawa.discord.Mantaro.utils.StringArrayFile;

public class CDisconnect implements Command {

	@Override
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	@ModuleProperties(level = "master", name = "disconnect", type = "control", description = "Disconnects the bot.")
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		if(evt.getAuthor().getId().equals("155867458203287552") || evt.getAuthor().getId().equals("155035543984537600"))
		{
			evt.getChannel().sendMessage("Gathering information...");
			new StringArrayFile("Quotes", "mantaro", CQuotation.quotes, true);
			
			evt.getChannel().sendMessage("Gathered.");
			evt.getChannel().sendMessage("Starting bot shutdown.");

			try{
				evt.getChannel().sendMessage("*goes to sleep*");
				System.exit(1);
			}
			catch (Exception e)
			{
				System.out.println("Couldn't shut down." + e.toString());
			}
		}
		else
		{
			evt.getChannel().sendMessage("You cannot do that, silly.");
		}
		
	}
}
