package net.kodehawa.discord.Mantaro.commands.admin;

import java.util.ArrayList;

import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.commands.CHi;
import net.kodehawa.discord.Mantaro.commands.CTsundere;
import net.kodehawa.discord.Mantaro.file.StringArrayFile;
import net.kodehawa.discord.Mantaro.main.Command;

public class AddList implements Command {

	@Override
	@ModuleProperties(level = "master", name = "add", type = "admin", description = "add a phrase to the lists",
	additionalInfo = "Possible args: greeting/tsun")
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		if(evt.getAuthor().getId().equals("155867458203287552")){
			if(beheaded.startsWith("greeting") && !beheaded.startsWith("tsun"))
			{
				String greet = whole.replace("~>add greeting ", "");
				CHi.greeting.add(greet);
				new StringArrayFile("Greetings", "mantaro", CHi.greeting, true, true);
				evt.getChannel().sendMessage("Added to greeting list: " + greet);
			}
			else if(beheaded.startsWith("tsun"))
			{
				String tsun = whole.replace("~>add tsun ", "");
				CTsundere.tsunLines.add(tsun);
				new StringArrayFile("tsunderelines", "mantaro", CTsundere.tsunLines, true, true);
				evt.getChannel().sendMessage("Added to greeting list: " + tsun);
			}
			
			//Debugging server roles for a friend.
			else if(beheaded.startsWith("listRoles"))
			{
				ArrayList<String> roles = new ArrayList<String>();

				for(Role r : evt.getGuild().getRoles())
				{
					roles.add("Name: " + r.getName() + " Color: #" + Integer.toHexString(r.getColor()) + " ID: " + r.getId() + " Users: " + r.getGuild().getUsersWithRole(r).toString().replaceAll("U:", "").replaceAll("\\([^\\(]*\\)", " "));
				}
				
				int n = -1;
				for(@SuppressWarnings("unused") String s : roles)
				{
					n = n + 1;
					System.out.println(roles.get(n));
				}
			}
			else
			{
				evt.getChannel().sendMessage("Silly master, use ~>add greeting or ~>add tsun");
			}
		}
		else
		{
			evt.getChannel().sendMessage("How did you even know?");
		}
	}

	@Override
	public void actionResult(boolean result, MessageReceivedEvent evt) {
		// TODO Auto-generated method stub

	}

}
