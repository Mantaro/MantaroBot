package net.kodehawa.discord.Mantaro.commands;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;

public class CServerInfo implements Command {

	@Override
	@ModuleProperties(level = "user", name = "serverinfo", type = "info", description = "Shows info about the server.")
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {	
		evt.getChannel().sendMessage(
				"```ruby\n Server info for: " + evt.getGuild().getName() + "\r \r" 
				
				
				+ "> Owner: " + evt.getGuild().getOwner().getUsername() + " \r"
				+ "> ID: " + evt.getGuild().getId() + "\r"
				+ "> Roles: " + evt.getGuild().getRoles().size() + "\r"
				+ "> Channels: " + evt.getGuild().getTextChannels().size()+ "\r"
				+ "> Voice Channels: " + evt.getGuild().getVoiceChannels().size() + "\r"
				+ "> Region: " + evt.getGuild().getRegion() + "\r"
				+ "> Icon URL: " + evt.getGuild().getIconUrl() + "```\r \r\n"
				);
	}

	@Override
	public void actionResult(boolean result, MessageReceivedEvent evt) {
		System.out.println("Command executed " + this.getClass().getName() + ".");
	}

}
