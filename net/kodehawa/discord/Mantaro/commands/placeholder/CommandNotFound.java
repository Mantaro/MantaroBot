package net.kodehawa.discord.Mantaro.commands.placeholder;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.main.Command;

public class CommandNotFound implements Command {

	@Override
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		evt.getChannel().sendMessage("Command doesn't exist in this mode. Please use ~>help or @MantaroBot help to get a list of commands.");
	}

	@Override
	public void actionResult(boolean result, MessageReceivedEvent evt) {
		// TODO Auto-generated method stub
		System.out.println("Command executed " + this.getClass().getName() + "." + result);
	}

}
