package net.kodehawa.mantarobot.cmd;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;

public class ServerInfo extends Command {

	public ServerInfo(){
		setName("serverinfo");
		setDescription("Retrieves server information.");
		setCommandType("user");
	}

	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {	
        channel = evt.getChannel();
        guild = evt.getGuild();
        
		EmbedBuilder embed = new EmbedBuilder();
		
		channel.sendTyping().queue();
		
		embed.setColor(guild.getOwner().getColor())
			.setTitle("Server info")
			.setDescription("Guild information for server " + guild.getName())
			.setThumbnail(guild.getIconUrl())
			.addField("Roles / Text Channels", String.valueOf(guild.getRoles().size()) + "/" + guild.getTextChannels().size() , false)
			.addField("Owner", guild.getOwner().getUser().getName(), false)
			.addField("Region", guild.getRegion().getName(), false)
			.setFooter("Server ID" + String.valueOf(guild.getId()), null);
		
		channel.sendMessage(embed.build()).queue();
	}
}
