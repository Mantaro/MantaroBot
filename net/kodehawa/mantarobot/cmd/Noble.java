package net.kodehawa.mantarobot.cmd;

import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.util.StringArrayFile;

public class Noble extends Command {

	private CopyOnWriteArrayList<String> nobleQuotes = new CopyOnWriteArrayList<String>();
	int i = 0;

	public Noble()
	{
		setName("noble");
		setCommandType("user");
		setDescription("Retrieves a random Lost Pause quote.");
		new StringArrayFile("noble", nobleQuotes, false);
	}

	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
		Random rand = new Random();
		int nobleQuote = rand.nextInt(nobleQuotes.size());
		
        channel = evt.getChannel();
        
		channel.sendMessage(":speech_balloon:" + nobleQuotes.get(nobleQuote) + " -Noble").queue();
	}

}
