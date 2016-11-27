package net.kodehawa.mantarobot.cmd;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;

public class Lewd extends Command {

	public Lewd(){
		setName("lewd");
		setCommandType("user");
		setDescription("T-Too lewd~");
	}

	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
        channel = evt.getChannel();

		channel.sendMessage("Y-You lewdie!" + "\r\n" 
		 + "http://puu.sh/rzVEe/c8272e7c84.png").queue();
	}
}