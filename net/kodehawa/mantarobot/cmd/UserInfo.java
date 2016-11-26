package net.kodehawa.mantarobot.cmd;

import java.time.format.DateTimeFormatter;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;


public class UserInfo extends Command {
	
	public UserInfo()
	{
		setName("userinfo");
	}
	
	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
		guild = evt.getGuild();
        author = evt.getAuthor();
        channel = evt.getChannel();
        receivedMessage = evt.getMessage();
        
		if(!beheadedMessage.isEmpty())
		{
			User user1 = null;
					
			//Which user to get the info for?
			if(receivedMessage.getMentionedUsers() != null)
			{
				user1 = receivedMessage.getMentionedUsers().get(0);
			}
			
			//Member gives way, way more info than User.
			Member member1 = guild.getMember(user1);

			if(user1 != null && member1 != null)
			{
				
				//This is all done using embeds. It looks nicer and cleaner.
				EmbedBuilder embed = new EmbedBuilder();
				channel.sendTyping().queue();
				embed.setColor(member1.getColor());
				//If we are dealing with the owner, mark him as owner on the title.
				if(member1.isOwner()){
					embed.setTitle("User info for " + user1.getName() + " (Server owner)");
				} else
				{
					//If not, just use the normal title.
					embed.setTitle("User info for " + user1.getName());
				}
				embed.setThumbnail(user1.getAvatarUrl());
				//Only get the date from the Join Date. Also replace that random Z because I'm not using time.
				embed.addField("Join Date: ", member1.getJoinDate().format(DateTimeFormatter.ISO_DATE).replace("Z", ""), false);
				if(member1.getVoiceState().getChannel() != null){
					embed.addField("Voice channel: ", member1.getVoiceState().getChannel().getName(), false);
				}
				embed.addField("Playing: ", guild.getMember(user1).getGame().getName(), false);
				embed.addField("Roles", String.valueOf(member1.getRoles().size()), false);
				//Getting the hex value of the RGB color assuming no alpha that is >16 in value is required.
				embed.addField("Color", "#"+Integer.toHexString(member1.getColor().getRGB()).substring(2).toUpperCase(), false);
				embed.setFooter("User ID: " + user1.getId(), null);
				channel.sendMessage(embed.build()).queue();
			}
			
		}
		else
		{
			//If the author wants to get self info.
			User user1 = author;
			//From author id, get the Member, so I can fetch the info.
			Member member1 = guild.getMemberById(author.getId());

			//This is all done using embeds. It looks nicer and cleaner.
			EmbedBuilder embed = new EmbedBuilder();
			channel.sendTyping().queue();
			embed.setColor(member1.getColor());
			//If we are dealing with the owner, mark him as owner on the title.
			if(member1.isOwner()){
				embed.setTitle("Self user info for " + user1.getName() + " (Server owner)");
			} else
			{
				//If not, just use the normal title.
				embed.setTitle("Self user info for " + user1.getName());
			}
			embed.setThumbnail(user1.getAvatarUrl());
			//Only get the date from the Join Date. Also replace that random Z because I'm not using time.
			embed.addField("Join Date: ", member1.getJoinDate().format(DateTimeFormatter.ISO_DATE).replace("Z", ""), false);
			if(member1.getVoiceState().getChannel() != null){
				embed.addField("Voice channel: ", member1.getVoiceState().getChannel().getName(), false);
			}
			embed.addField("Playing: ", guild.getMember(user1).getGame().getName(), false);
			embed.addField("Roles", String.valueOf(member1.getRoles().size()), false);
			//Getting the hex value of the RGB color assuming no alpha that is >16 in value is required.
			embed.addField("Color", "#"+Integer.toHexString(member1.getColor().getRGB()).substring(2).toUpperCase(), false);
			embed.setFooter("User ID: " + user1.getId(), null);
			channel.sendMessage(embed.build()).queue();
		}
	}
}