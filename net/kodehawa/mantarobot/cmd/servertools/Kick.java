package net.kodehawa.mantarobot.cmd.servertools;

import java.util.List;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.kodehawa.mantarobot.cmd.management.Command;

public class Kick extends Command {

	public Kick()
	{
		setName("kick");
	}
	
	@Override
	public void onCommand(String[] msg, String content, MessageReceivedEvent event)
	{
        guild = event.getGuild();
        author = event.getAuthor();
        channel = event.getChannel();
        receivedMessage = event.getMessage();

		//We need to check if this is in a guild AND if the member trying to kick the person has KICK_MEMBERS permission.
		if (receivedMessage.isFromType(ChannelType.TEXT) && guild.getMember(author).hasPermission(Permission.KICK_MEMBERS))
        {
			//If they mentioned a user this gets passed, if they didn't it just doesn't.
            if (receivedMessage.getMentionedUsers().isEmpty())
            {
                channel.sendMessage("You must mention 1 or more users to be kicked!").queue();
                return;
            }
            else
            {
                Member selfMember = guild.getSelfMember(); 
                
                //Do I have permissions to kick members, if yes continue, if no end command.
                if (!selfMember.hasPermission(Permission.KICK_MEMBERS))
                {
                    channel.sendMessage("Sorry! I don't have permission to kick members in this server!").queue();
                    return; 
                }
                
                List<User> mentionedUsers = receivedMessage.getMentionedUsers();
                //For all mentioned users in the command.
                for (User user : mentionedUsers)
                {
                    Member member = guild.getMember(user); 
                    //If one of them is in a higher hierarchy than the bot, cannot kick.
                    if (!selfMember.canInteract(member))
                    {
                    	channel.sendMessage("Cannot kick member: " + member.getEffectiveName() +", they are higher or the same " + "hierachy than I am!").queue();
                        return; 
                    }

                    //Proceed to kick them. Again, using queue so I don't get rate limited.
                    guild.getController().kick(member).queue(
                        success -> channel.sendMessage("You will be missed... or not " + member.getEffectiveName()).queue(), //Quite funny, I think.
                        error ->
                        {
                            if (error instanceof PermissionException)
                            {
                                PermissionException pe = (PermissionException) error; //Which permission?

                                channel.sendMessage("PermissionError kicking [" + member.getEffectiveName()
                                        + "]: " + "(No permission provided: " + pe.getPermission() + ")").queue();
                            }
                            else
                            {
                            	channel.sendMessage("Unknown error while kicking [" + member.getEffectiveName()
                                        + "]: " + "<" + error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();
                            	
                            	//Just so I get more info in the case of an unexpected error.
                            	error.printStackTrace();
                            }
                        });
                }
            }
        }
        else
        {
            channel.sendMessage("This is a server side command!").queue();
        }
	}
}
