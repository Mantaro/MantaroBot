package net.kodehawa.mantarobot.cmd.servertools;

import java.util.List;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.kodehawa.mantarobot.cmd.management.Command;

/**
 * 	I had absolutely NO examples on how to do this. If you wanna copy and paste this code just do it, it was a pain to have no examples or docs on how to do it tbh.
 * @author Yomura
 */
public class BulkDelete extends Command {

	public BulkDelete()
	{
		setName("prune");
		setDescription("Prunes x messages. Usage: ~>prune x");
		setCommandType("servertool");
	}

	int messagesToPrune;
	List<Message> messageHistory;

	@Override
	public void onCommand(String[] messageSplit, String content, MessageReceivedEvent event)
	{
		//Initialize normal variables declared in Command so I can use them here. 
        guild = event.getGuild();
        author = event.getAuthor();
        channel = event.getChannel();
        receivedMessage = event.getMessage();

        //If the received message is from a guild and the person who triggers the command has Manage Messages permissions, continue.
		if(receivedMessage.isFromType(ChannelType.TEXT) && guild.getMember(author).hasPermission(Permission.MESSAGE_MANAGE)){
			//If you specified how many messages.
			if(!content.isEmpty())
			{
				messagesToPrune = Integer.parseInt(content); //Content needs to be a number, you know.
				//I cannot get more than 100 messages from the past, so if the number is more than 100, proceed to default to 100.
				if(messagesToPrune > 100){
					messagesToPrune = 100;
				}
				TextChannel channel2 = event.getGuild().getTextChannelById(channel.getId());
				//Retrieve the past x messages to delete as a List<Message>
				//You *need* to use .block(), with .queue() and using lambda expressions it just bugs out and it doesn't retrieve anything.
				try {
					messageHistory = channel2.getHistory().retrievePast(messagesToPrune).block();
				} catch (RateLimitedException e) {
					e.printStackTrace();
				}
				
				//Delete the last x messages. Doing this as a queue so I can avoid rate limiting too, after queuing check if it was successful or no, and if it wasn't warn the user.
				channel2.deleteMessages(messageHistory).queue(
						success -> channel.sendMessage(":pencil: Successfully pruned " + messagesToPrune + " messages").queue(),
						error -> 
						{
							if (error instanceof PermissionException){
                                PermissionException pe = (PermissionException) error; //Which permission am I missing?

                                channel.sendMessage(":heavy_multiplication_x: " + "Lack of permission while pruning messages" + "(No permission provided: " + pe.getPermission() + ")").queue();
                            } else
                            {
                            	channel.sendMessage(":heavy_multiplication_x: " + "Unknown error while pruning messages" + "<" + error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();
                            	//Just so I get more data in a unexpected scenario.
                            	error.printStackTrace();
                            }
						});
			}
			else{
				channel.sendMessage(":heavy_multiplication_x: No messages to prune.").queue();
			}
		}
		else{
			channel.sendMessage(":heavy_multiplication_x: " + "Cannot prune. Possible errors: You have no Manage Messages permission or this was triggered outside of a guild.").queue();
		}
	}
}
