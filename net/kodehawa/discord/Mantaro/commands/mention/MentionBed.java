package net.kodehawa.discord.Mantaro.commands.mention;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;

public class MentionBed implements Command {

	@Override
	@ModuleProperties(level = "user", name = "bed", type = "mention", description = "Wanna do lewd? <3.")
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		evt.getChannel().sendMessage("W-What are you gonna do to me?! L-Lewd!");
	}

	@Override
	public void actionResult(boolean result, MessageReceivedEvent evt) {
		// TODO Auto-generated method stub
		System.out.println("Command executed " + this.getClass().getName() + ".");
	}

}
