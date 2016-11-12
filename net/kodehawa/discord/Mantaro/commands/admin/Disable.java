package net.kodehawa.discord.Mantaro.commands.admin;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;
import net.kodehawa.discord.Mantaro.utils.Values;

public class Disable implements Command {

	public Disable()
	{
		Values.instance.read();
	}
	
	@Override
	@ModuleProperties(level = "owner", name = "botstatus", type = "control", description = "Disables the bot in the current server.")
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		
		String serverId = evt.getGuild().getId();

		if(beheaded.startsWith("disable"))
		{
			if(evt.getAuthor().getId().equals("155867458203287552")){
				
				Values.disabledServers.add(serverId);
				evt.getChannel().sendMessageAsync("Server will be ignored from now on.", null);
				Values.instance.check();
			}
		}
		else if(beheaded.startsWith("enable") )
		{
			if(evt.getAuthor().getId().equals("155867458203287552") && Values.disabledServers.contains(evt.getGuild().getId())){
				
				Values.disabledServers.remove(serverId);
				evt.getChannel().sendMessageAsync("Server won't be ignored from now on.", null);
				Values.instance.check();
			}
		}
	}
}
