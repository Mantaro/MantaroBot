package net.kodehawa.mantarobot.cmd.servertools;

import java.util.List;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.kodehawa.mantarobot.management.Command;

public class Ban extends Command {

	public Ban(){
		setName("ban");
		setDescription("Bans mentioned users.");
		setCommandType("servertool");
	}
	
	@Override
	public void onCommand(String[] string, String content, MessageReceivedEvent event)
	{
		//Initialize the variables I'll need.
        guild = event.getGuild();
        author = event.getAuthor();
        channel = event.getChannel();
        receivedMessage = event.getMessage();
        
		//We need to check if this is in a guild AND if the member trying to kick the person has KICK_MEMBERS permission.
		if(receivedMessage.isFromType(ChannelType.TEXT) && guild.getMember(author).hasPermission(Permission.BAN_MEMBERS))
		{
			//If you mentioned someone to ban, continue.
			if(receivedMessage.getMentionedUsers().isEmpty())
			{
				channel.sendMessage(":heavy_multiplication_x:" + "You need to mention at least one user to ban.").queue();
				return;
			}
			
			//For all mentioned members..
            List<User> mentionedUsers = receivedMessage.getMentionedUsers();
			for (User user : mentionedUsers)
            {
                Member member = guild.getMember(user); 
                //If one of them is in a higher hierarchy than the bot, I cannot ban them.
                if(!guild.getSelfMember().canInteract(member))
                {
                	channel.sendMessage(":heavy_multiplication_x:" + "Cannot ban member " + member.getEffectiveName() + ", they are higher or the same " + "hierachy than I am!").queue();
                	return;
                }
                
                //If I cannot ban, well..
                if (!guild.getSelfMember().hasPermission(Permission.BAN_MEMBERS))
                {
                    channel.sendMessage(":heavy_multiplication_x:" + "Sorry! I don't have permission to ban members in this server!").queue();
                    return; 
                }
                                
                //Proceed to ban them. Again, using queue so I don't get rate limited.
                //Also delete all messages from past 7 days.
                guild.getController().ban(member, 7).queue(
                		success -> channel.sendMessage(":zap: You will be missed... or not " + member.getEffectiveName()).queue(),
                        error ->
                        {
                            if (error instanceof PermissionException)
                            {
                                PermissionException pe = (PermissionException) error; //Which permission am I missing?

                                channel.sendMessage(":heavy_multiplication_x:" + "Error banning " + member.getEffectiveName()
                                        + ": " + "(No permission provided: " + pe.getPermission() + ")").queue();
                            }
                            else
                            {
                            	channel.sendMessage(":heavy_multiplication_x:" + "Unknown error while banning " + member.getEffectiveName()
                                        + ": " + "<" + error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();
                            	
                            	//I need more information in the case of an unexpected error.
                            	error.printStackTrace();
                            }
                        });
            }
		}
		else
        {
			channel.sendMessage(":heavy_multiplication_x: " + "Cannot ban. Possible errors: You have no Ban Members permission or this was triggered outside of a guild.").queue();
        }
	}
}
