package net.kodehawa.discord.Mantaro.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;

public class CRand implements Command {

	ArrayList<User> users = new ArrayList<>();
	
	@ModuleProperties(level = "user", name = "random", type = "common", description = "Random number related things.", additionalInfo = "Possible args: rob *menction*/lottery"
			, takesArgs = true)
	@Override
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		List<User> mentions = evt.getMessage().getMentionedUsers();
		
        StringBuilder mentioned = new StringBuilder();
        
        for (User user: mentions)
        {
            mentioned.append(user.getUsername());
            break;
        }
		
		if(beheaded.startsWith("rob"))
		{
			Random r = new Random();
			int woah = r.nextInt(1200);
			evt.getChannel().sendMessageAsync("You robbed **" + woah + "USD** from " + mentioned, null);
		}
		
		else if(beheaded.startsWith("lottery"))
		{
			User user = evt.getAuthor();
			
			if(!users.contains(user))
			{
				Random r = new Random();
				int lottery = r.nextInt(5000);
				evt.getChannel().sendMessageAsync("You won **" + lottery + "USD**, congrats!", null);
			    users.add(user);
			}
			else
			{
				evt.getChannel().sendMessageAsync("Try again in later! (20 minutes since you ran the command)", null);
			}

			if(!users.contains(user))
			{
				Timer timer = new Timer();
				timer.schedule(new TimerTask() {
					  @Override
					  public void run() {
						  users.remove(user);
					  }
					}, 20*60*1000, 20*60*1000);
			}
		}
		
	}

	@Override
	public void actionResult(boolean result, MessageReceivedEvent evt) {
		//This command will never end lol
	}
}
