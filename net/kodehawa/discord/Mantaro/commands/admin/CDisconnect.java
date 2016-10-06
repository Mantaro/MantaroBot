package net.kodehawa.discord.Mantaro.commands.admin;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.bot.MantaroBot;
import net.kodehawa.discord.Mantaro.commands.CQuotation;
import net.kodehawa.discord.Mantaro.commands.CTsundere;
import net.kodehawa.discord.Mantaro.listeners.Listener;
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
			try {
				new StringArrayFile("Quotes", "mantaro", CQuotation.quotes, true);
			    Thread.sleep(50);
			} catch (InterruptedException e1) {	}
			
			evt.getChannel().sendMessage("Gathered.");
			
			evt.getChannel().sendMessage("Starting bot shutdown.");
			try {
				MantaroBot.getInstance().getSelf().removeEventListener(new Listener());
				CQuotation.quotes.clear();
				CTsundere.tsunLines.clear();
				MantaroBot.getInstance().commandList.clear();
				MantaroBot.getInstance().mentionCommandList.clear();
			    Thread.sleep(50);
			} catch (InterruptedException e1) {	}

			evt.getChannel().sendMessage("*goes to sleep*");
			try {
				Thread.sleep(50);
			} catch (InterruptedException e1) {	}

			try{
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
