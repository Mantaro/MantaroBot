package net.kodehawa.discord.Mantaro.commands;

import java.util.List;

import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;

public class CAction implements Command {

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

		if(beheaded.startsWith("pat"))
		{
			List<User> menctions = evt.getMessage().getMentionedUsers();
			StringBuilder listString = new StringBuilder();

			for(User s : menctions)
			{
				listString.append(s.getAsMention());
			}

			evt.getChannel().sendMessageAsync(listString + "you have been patted by" + "" + evt.getAuthor().getAsMention() + "\r http://pa1.narvii.com/5947/f14b1451afa2fa16a6b9e6446d6039ee86db5641_hq.gif", null);
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
			evt.getChannel().sendMessage("Meeeow.");
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
	            
				evt.getChannel().sendMessage("*meows at " + menction + ".*");
	            
			}
			
			else
			{
				evt.getChannel().sendMessage("Who am I gonna meow at, silly?\r\nAnyway, I guess I'll have to meow you.\r\n*meows at " + evt.getAuthor().getAsMention() + " .*");
			}
			
		}
	}
}
