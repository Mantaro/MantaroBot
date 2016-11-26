package net.kodehawa.mantarobot.cmd;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.util.StringArrayFile;

public class Action extends Command {

	private CopyOnWriteArrayList<String> pats = new CopyOnWriteArrayList<String>();
	private CopyOnWriteArrayList<String> hugs = new CopyOnWriteArrayList<String>();
	int i = 0;

	public Action()
	{
		setName("action");
		setDescription("");
		new StringArrayFile("patting", pats, false);
		new StringArrayFile("hugs", hugs, false);
	}
	
	
	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
        guild = evt.getGuild();
        author = evt.getAuthor();
        channel = evt.getChannel();
        receivedMessage = evt.getMessage();

		Random rand = new Random();
		
		if(beheadedMessage.startsWith("pat"))
		{
			List<User> menctions = evt.getMessage().getMentionedUsers();
			StringBuilder listString = new StringBuilder();

			int patInteger = rand.nextInt(pats.size());
			
			for(User s : menctions)
			{
				listString.append(s.getAsMention());
			}

			channel.sendMessage(":speech_balloon: " + listString + " you have been patted by " + "" + evt.getAuthor().getAsMention() + "\r" + pats.get(patInteger)).queue();
		}
		
		else if(beheadedMessage.startsWith("hug"))
		{
			List<User> menctions = evt.getMessage().getMentionedUsers();
			StringBuilder listString = new StringBuilder();

			int hugInteger = rand.nextInt(hugs.size());
			
			for(User s : menctions)
			{
				listString.append(s.getAsMention());
			}

			channel.sendMessage(":speech_balloon: " + listString + " you have been hugged by " + "" + evt.getAuthor().getAsMention() + "\r" + hugs.get(hugInteger)).queue();;
		}
		
		else if(beheadedMessage.startsWith("bloodsuck"))
		{
			if(evt.getMessage().getMentionedUsers().isEmpty())
			{
				channel.sendMessage("http://puu.sh/qEYYH/e5094405a5.jpg").queue();
			}
			else
			{
				StringBuilder listString = new StringBuilder();

				for(User s : evt.getMessage().getMentionedUsers())
				{
					listString.append(s.getAsMention());
				}
				channel.sendMessage("http://puu.sh/qEYYH/e5094405a5.jpg \r Sucks the blood of " + listString.toString()).queue();;
			}
		}
		
		else if(beheadedMessage.startsWith("meow2"))
		{
			channel.sendMessage(":speech_balloon: " + "Meeeeow.\r " + "http://puu.sh/rK5K7/034039286e.gif").queue();;
		}
		
		else if(beheadedMessage.startsWith("meow"))
		{
			if(receivedMessage.getMentionedUsers().isEmpty() != true)
			{
				List<User> mentions = receivedMessage.getMentionedUsers();
	            StringBuilder builder = new StringBuilder();
	            for (User user: mentions)
	            {
	                builder.append(user.getName());
	            }
	            String mention = builder.toString().replace("MantaroBot", "");
	            
				channel.sendMessage(":speech_balloon: " + "*meows at " + mention + ".*\r" + "http://puu.sh/rK5Nf/63d90628c2.gif").queue();;
	            
			}
			else
			{
				channel.sendMessage(":speech_balloon: " + "Who am I gonna meow at, silly?\r\nAnyway, I guess I'll have to meow you.\r\n*meows at " + evt.getAuthor().getAsMention() + " .*").queue();;
			}
			
		}
		else if(beheadedMessage.startsWith("facedesk"))
		{
			channel.sendMessage("http://puu.sh/rK6E7/0b745e5544.gif");
		}
		
		else if(beheadedMessage.startsWith("nom"))
		{
			channel.sendMessage("http://puu.sh/rK7t2/330182c282.gif");
		}
	}
	
	/**
	 * I might need this soon lol
	 * @return a random hex color.
	 */
	public static String randomColor()
	{

		String[] letters = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
	    String color = "#";
	    for (int i = 0; i < 6; i++ ) {
	        color += letters[(int) Math.floor(Math.random() * 16)];
	    }
	    return color;
	}
}
