package net.kodehawa.discord.Mantaro.commands.admin;

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
			if(whole.replace("~>add ", "").startsWith("greeting") && !whole.replace("~>add ", "").startsWith("tsun"))
			{
				String greet = whole.replace("~>add greeting ", "");
				CHi.greeting.add(greet);
				new StringArrayFile("Greetings", "mantaro", CHi.greeting, true, true);
				evt.getChannel().sendMessage("Added to greeting list: " + greet);
			}
			else if(whole.replace("~>add ", "").startsWith("tsun"))
			{
				String tsun = whole.replace("~>add tsun ", "");
				CTsundere.tsunLines.add(tsun);
				new StringArrayFile("tsunderelines", "mantaro", CTsundere.tsunLines, true, true);
				evt.getChannel().sendMessage("Added to greeting list: " + tsun);
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
