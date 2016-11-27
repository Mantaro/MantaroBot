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
		
		embed.setColor(guild.getOwner().getColor());
		embed.setTitle("Server info");
		embed.setDescription("Guild information for server " + guild.getName());
		embed.setThumbnail(guild.getIconUrl());
		embed.addField("Roles / Text Channels", String.valueOf(guild.getRoles().size()) + "/" + guild.getTextChannels().size() , false);
		embed.addField("Owner", guild.getOwner().getUser().getName(), false);
		embed.addField("Region", guild.getRegion().getName(), false);
		embed.setFooter("Server ID" + String.valueOf(guild.getId()), null);
		
		channel.sendMessage(embed.build()).queue();
	}
}
