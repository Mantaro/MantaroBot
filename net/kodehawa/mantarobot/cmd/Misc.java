package net.kodehawa.mantarobot.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;

public class Misc extends Command {

	private List<String> lyrics = new ArrayList<String>();
	ArrayList<User> users = new ArrayList<>();
	
	public Misc()
	{
		setName("misc");
		lyrics.add("Are you ready?");
		lyrics.add("O-oooooooooo AAAAE-A-A-I-A-U-");
		lyrics.add("E-eee-ee-eee AAAAE-A-E-I-E-A-");
		lyrics.add("JO-ooo-oo-oo-oo EEEEO-A-AAA-AAAA");
		lyrics.add("O-oooooooooo AAAAE-A-A-I-A-U-");
		lyrics.add("JO-oooooooooooo AAE-O-A-A-U-U-A-");
		lyrics.add("E-eee-ee-eee AAAAE-A-E-I-E-A-");
		lyrics.add("JO-ooo-oo-oo-oo EEEEO-A-AAA-AAAA");
		lyrics.add("O-oooooooooo AAAAE-A-A-I-A-U-");
		lyrics.add("E-eee-ee-eee AAAAE-A-E-I-E-A-");
		lyrics.add("JO-ooo-oo-oo-oo EEEEO-A-AAA-AAAA-");
	}

	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
		List<User> mentions = evt.getMessage().getMentionedUsers();
		
        guild = evt.getGuild();
        author = evt.getAuthor();
        channel = evt.getChannel();
        receivedMessage = evt.getMessage();

		
        StringBuilder mentioned = new StringBuilder();
        
        for (User user: mentions)
        {
            mentioned.append(user.getName());
            break;
        }
		
		if(beheadedMessage.startsWith("rob"))
		{
			Random r = new Random();
			int woah = r.nextInt(1200);
			channel.sendMessage("You robbed **" + woah + "USD** from " + mentioned).queue();
		}
		
		else if(beheadedMessage.startsWith("lottery"))
		{
			User user = author;
			
			if(!users.contains(user))
			{
				Random r = new Random();
				int lottery = r.nextInt(5000);
				channel.sendMessage("You won **" + lottery + "USD**, congrats!").queue();
				users.add(user);
			}
			else
			{
				channel.sendMessage("Try again in later! (10 minutes since you ran the command)").queue();
			}

			if(users.contains(user))
			{
				TimerTask timerTask = new TimerTask() 
			     { 
			         public void run()  
			         { 
			        	 users.remove(user);
						 this.cancel();
					} 
			     }; 
				 Timer timer = new Timer(); 
			     timer.scheduleAtFixedRate(timerTask, 600000, 1);
			}
		}
		
		else if(beheadedMessage.startsWith("reverse"))
		{
			String stringToReverse = beheadedMessage.replace("reverse ", "");
			
			String reversed = new StringBuilder(stringToReverse).reverse().toString();
			
			channel.sendMessage(reversed).queue();
		}
		
		else if(beheadedMessage.startsWith("brainpower") || beheadedMessage.startsWith("bp"))
		{
			StringBuilder finalMessage = new StringBuilder();

			for (String help : lyrics)
			{
				finalMessage.append(help+"\r\n");
			}
			
			channel.sendMessage(finalMessage.toString()).queue();
		}
	}
}
