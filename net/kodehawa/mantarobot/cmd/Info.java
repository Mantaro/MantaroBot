package net.kodehawa.mantarobot.cmd;

import java.awt.Color;
import java.time.format.DateTimeFormatter;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;

/**
 * Information module.
 * @author Yomura
 *
 */
public class Info extends Command {
	
	public Info()
	{
		setName("info");
		setDescription("Retrieves user and server info. In the case of user takes one argument in the form of a mention. "
				+ "With no argument you get your own info.");
		setCommandType("user");
		setExtendedHelp(
				"Retrieves information from user and server alike..\n"
				+ "Usage: \n"
				+ "~>info user @user: Retrieves the specified user information.\n"
				+ "~>info user: Retrieves self user information.\n"
				+ "~>info server: Retrieves guild/server information.\n"
				+ "Parameter description:\n"
				+ "*@user*: User to mention.\n");
	}
	
	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
		guild = evt.getGuild();
        author = evt.getAuthor();
        channel = evt.getChannel();
        receivedMessage = evt.getMessage();
        EmbedBuilder embed = new EmbedBuilder();

        String noArgs = beheadedMessage.split(" ")[0];
		switch(noArgs){
		case "user":
			if(!beheadedMessage.replace("user", "").isEmpty())
			{
				User user1 = null;
						
				//Which user to get the info for?
				if(receivedMessage.getMentionedUsers() != null){
					user1 = receivedMessage.getMentionedUsers().get(0);
				}
				//Member gives way, way more info than User.
				Member member1 = guild.getMember(user1);

				if(user1 != null && member1 != null){
					//This is all done using embeds. It looks nicer and cleaner.
					embed.setColor(member1.getColor());
					//If we are dealing with the owner, mark him as owner on the title.
					if(member1.isOwner()){
						embed.setTitle("User info for " + user1.getName() + " (Server owner)");
					} else{
						//If not, just use the normal title.
						embed.setTitle("User info for " + user1.getName());
					}
					embed.setThumbnail(user1.getAvatarUrl())
					//Only get the date from the Join Date. Also replace that random Z because I'm not using time.
						.addField("Join Date: ", member1.getJoinDate().format(DateTimeFormatter.ISO_DATE_TIME).replaceAll("[^0-9.:-]", " "), false);
					if(member1.getVoiceState().getChannel() != null){ 
						embed.addField("Voice channel: ", member1.getVoiceState().getChannel().getName(), false); 
						}
					if(guild.getMember(user1).getGame() != null){ 
						embed.addField("Playing: ", guild.getMember(user1).getGame().getName(), false); 
						}
					embed.addField("Roles", String.valueOf(member1.getRoles().size()), true);
					//Getting the hex value of the RGB color assuming no alpha that is >16 in value is required.
					if(!String.valueOf(member1.getColor().getRGB()).isEmpty()){
						embed.addField("Color", "#"+Integer.toHexString(member1.getColor().getRGB()).substring(2).toUpperCase(), true);
						}
					embed.setFooter("User ID: " + user1.getId(), null);
					channel.sendMessage(embed.build()).queue();
				}
				
			}
			else {
				//If the author wants to get self info.
				User user1 = author;
				//From author id, get the Member, so I can fetch the info.
				Member member1 = guild.getMemberById(author.getId());

				//This is all done using embeds. It looks nicer and cleaner.
				embed.setColor(member1.getColor());
				//If we are dealing with the owner, mark him as owner on the title.
				if(member1.isOwner()){
					embed.setTitle("Self user info for " + user1.getName() + " (Server owner)");
				} else{
					//If not, just use the normal title.
					embed.setTitle("Self user info for " + user1.getName());
				}
				embed.setThumbnail(user1.getAvatarUrl());
				//Only get the date from the Join Date. Also replace that random Z because I'm not using time.
				embed.addField("Join Date: ", member1.getJoinDate().format(DateTimeFormatter.ISO_DATE_TIME).replaceAll("[^0-9.:-]", " "), false);
				if(member1.getVoiceState().getChannel() != null){ 
					embed.addField("Voice channel: ", member1.getVoiceState().getChannel().getName(), false); 
					}
				if(guild.getMember(user1).getGame() != null){ 
					embed.addField("Playing: ", guild.getMember(user1).getGame().getName(), false);
				}
				embed.addField("Roles", String.valueOf(member1.getRoles().size()), true);
				//Getting the hex value of the RGB color assuming no alpha that is >16 in value is required.
				if(!String.valueOf(member1.getColor().getRGB()).isEmpty()){
					embed.addField("Color", "#"+Integer.toHexString(member1.getColor().getRGB()).substring(2).toUpperCase(), true);
				}
				embed.setFooter("User ID: " + user1.getId(), null);
				channel.sendMessage(embed.build()).queue();
			}
			break;
		case "server":
			StringBuilder sb = new StringBuilder();
			int i = 0;
			for(Role tc : guild.getRoles()){
				i++;
				if(i <= 79){
					if(!tc.getName().contains("everyone") && i != guild.getRoles().size() - 1){
						sb.append(tc.getName() + ", ");
					} else if(i == guild.getRoles().size() - 1 || i == 79){
						sb.append(tc.getName() + ".");
					}
				} else { break; } 
			}	
			embed.setColor(guild.getOwner().getColor())
			.setAuthor("Guild Information", null, guild.getIconUrl())
			.setColor(Color.orange)
			.setDescription("Guild information for server " + guild.getName())
			.setThumbnail(guild.getIconUrl())
			.addField("Users", String.valueOf(guild.getMembers().size()), true)
			.addField("Main Channel", "#" + guild.getPublicChannel().getName(), true)
			.addField("Creation Date", guild.getCreationTime().format(DateTimeFormatter.ISO_DATE_TIME).replaceAll("[^0-9.:-]", " "), true)
			.addField("Voice/Text Channels", guild.getVoiceChannels().size() + "/" + guild.getTextChannels().size() , true)
			.addField("Owner", guild.getOwner().getUser().getName() + "#" + guild.getOwner().getUser().getDiscriminator(), true)
			.addField("Region", guild.getRegion().getName(), true)
			.addField("Roles ("+guild.getRoles().size() + ")", sb.toString(), false)
			.setFooter("Server ID: " + String.valueOf(guild.getId()), null);
			channel.sendMessage(embed.build()).queue();
			break;
		default:
			channel.sendMessage(getExtendedHelp()).queue();
			break;
		}
	}
}