package net.kodehawa.discord.Mantaro.commands.mention;

import com.google.code.chatterbotapi.ChatterBot;
import com.google.code.chatterbotapi.ChatterBotFactory;
import com.google.code.chatterbotapi.ChatterBotSession;
import com.google.code.chatterbotapi.ChatterBotType;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;

public class MentionCleverbot implements Command {

    ChatterBot cleverboat = null;
    ChatterBotSession cleverbotsession = null;
	
	public MentionCleverbot()
	{
		
        ChatterBotFactory factory = new ChatterBotFactory();

		try {
			cleverboat = factory.create(ChatterBotType.CLEVERBOT);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(cleverboat != null)
		{
	        cleverbotsession = cleverboat.createSession();
		}

	}
	
	@Override
	@ModuleProperties(level = "user", name = "cleverbot", type = "mention", description = "Talk to cleverbot.")
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		String botString = whole.replace("@MantaroBot talk ", "");
		try {
			String s = cleverbotsession.think(botString);
			evt.getChannel().sendMessage(s);
		} catch (Exception e) {
			e.printStackTrace();
			evt.getChannel().sendMessage("Something went wrong trying to fetch cleverbot thoughts.");
		}
	}

	@Override
	public void actionResult(boolean result, MessageReceivedEvent evt) {
		System.out.println("Command executed " + this.getClass().getName() + ".");
	}

}
