package net.kodehawa.discord.Mantaro.commands;

import java.util.List;

import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;

public class CMeow implements Command {

	@Override
	@ModuleProperties(level = "user", name = "meow", type = "common", description = " Meows to someone, make sure to mention or the command will get mad at you. (Tsun reply if there is no mention present)", takesArgs = true)
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
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

	@Override
	public void actionResult(boolean result, MessageReceivedEvent evt) {
		System.out.println("Command executed " + this.getClass().getName() + ".");

	}

}
