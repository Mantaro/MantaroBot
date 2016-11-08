package net.kodehawa.discord.Mantaro.commands;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;
import net.kodehawa.discord.Mantaro.utils.StringArrayFile;

public class CAction implements Command {

	private CopyOnWriteArrayList<String> pats = new CopyOnWriteArrayList<String>();
	
	public CAction()
	{
		new StringArrayFile("patting", "mantaro", pats, false);
	}
	
	@Override
	@ModuleProperties(level = "user", name = "action", type = "common", description = "Realizes an action. You need to tag someone."
	, additionalInfo = "Possible args: hug/pat/bloodsuck/meow/meow2", takesArgs = true)
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	/**
	 * else else if else intensifies.
	 */
	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {

		Random rand = new Random();
		
		if(beheaded.startsWith("pat"))
		{
			List<User> menctions = evt.getMessage().getMentionedUsers();
			StringBuilder listString = new StringBuilder();

			int patInteger = rand.nextInt(pats.size());
			
			for(User s : menctions)
			{
				listString.append(s.getAsMention());
			}

			evt.getChannel().sendMessageAsync(listString + "you have been patted by" + "" + evt.getAuthor().getAsMention() + "\r" + pats.get(patInteger), null);
		}
		
		else if(beheaded.startsWith("hug"))
		{
			List<User> menctions = evt.getMessage().getMentionedUsers();
			StringBuilder listString = new StringBuilder();

			for(User s : menctions)
			{
				listString.append(s.getAsMention());
			}

			evt.getChannel().sendMessageAsync(listString + "you have been hugged by" + "" + evt.getAuthor().getAsMention() + "\r http://puu.sh/qUy1d/2e00556636.gif", null);
		}
		
		else if(beheaded.startsWith("bloodsuck"))
		{
			if(evt.getMessage().getMentionedUsers().isEmpty())
			{
				evt.getChannel().sendMessageAsync("http://puu.sh/qEYYH/e5094405a5.jpg", null);
			}
			else
			{
				StringBuilder listString = new StringBuilder();

				for(User s : evt.getMessage().getMentionedUsers())
				{
					listString.append(s.getAsMention());
				}
				evt.getChannel().sendMessageAsync("http://puu.sh/qEYYH/e5094405a5.jpg \r Sucks the blood of " + listString.toString(), null);
			}
		}
		
		else if(beheaded.startsWith("meow2"))
		{
			evt.getChannel().sendMessageAsync("Meeeeow.\r " + "http://puu.sh/rK5K7/034039286e.gif", null);
		}
		
		else if(beheaded.startsWith("meow"))
		{
			if(evt.getMessage().getMentionedUsers().isEmpty() != true)
			{
				List<User> mentions = evt.getMessage().getMentionedUsers();
	            StringBuilder builder = new StringBuilder();
	            for (User user: mentions)
	            {
	                builder.append(user.getUsername());
	            }
	            String menction = builder.toString().replace("MantaroBot", "");
	            
				evt.getChannel().sendMessageAsync("*meows at " + menction + ".*\r" + "http://puu.sh/rK5Nf/63d90628c2.gif", null);
	            
			}
			else
			{
				evt.getChannel().sendMessageAsync("Who am I gonna meow at, silly?\r\nAnyway, I guess I'll have to meow you.\r\n*meows at " + evt.getAuthor().getAsMention() + " .*", null);
			}
			
		}
		else if(beheaded.startsWith("facedesk"))
		{
			evt.getChannel().sendMessageAsync("http://puu.sh/rK6E7/0b745e5544.gif", null);
		}
		
		else if(beheaded.startsWith("nom"))
		{
			evt.getChannel().sendMessageAsync("http://puu.sh/rK7t2/330182c282.gif", null);
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
