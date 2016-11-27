package net.kodehawa.mantarobot.cmd;

import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.util.StringArrayFile;

public class Tsundere extends Command {

	public static CopyOnWriteArrayList<String> tsunLines = new CopyOnWriteArrayList<String>();
	int i = 0;

	public Tsundere(){
		setName("tsundere");
		setDescription("Y-You baka!");
		new StringArrayFile("tsunderelines", tsunLines, false);
		setCommandType("user");
	}

	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
        channel = evt.getChannel();
        
		Random rd = new Random();
		
        int tsundereRandomizer = rd.nextInt(tsunLines.size());
		channel.sendMessage(":mega: " +  tsunLines.get(tsundereRandomizer)).queue();
	}
}
