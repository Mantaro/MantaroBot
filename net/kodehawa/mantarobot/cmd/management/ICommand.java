package net.kodehawa.mantarobot.cmd.management;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public interface ICommand {
	public void onCommand(String[] split, String content, MessageReceivedEvent event);	
}
