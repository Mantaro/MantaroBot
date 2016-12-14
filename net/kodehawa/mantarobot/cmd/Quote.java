package net.kodehawa.mantarobot.cmd;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.util.StringArrayUtils;

public class Quote extends Command {

	public CopyOnWriteArrayList<String> quotes = new CopyOnWriteArrayList<>();

	public Quote()
	{
		setName("quote");
		setDescription("Adds or retrieves quotes. See ~>help quote.");
		setCommandType("user");
		setExtendedHelp(
				"(Disabled for now) This command **quotes** a phrase.\n"
				+ "> Usage:\n"
				+ "~>quote content: Adds a quote with content defined by *content*.\n"
				+ "~>quote list: Gets a list of all avaliable quotes \n"
				+ "~>quote read number: Gets a quote matching the number. \n"
				+ "~>quote get phrase phrase: Searches for the first quote which matches your search criteria and prints it.\n"
				+ "> Parameters:\n"
				+ "*number*: A number from 0 to the total number of quotes.\n"
				+ "*content*: The content of the quote.\n"
				+ "*phrase*: A phrase used to match a quote with it.");
		new StringArrayUtils("quote", quotes, false, true);
	}

	@SuppressWarnings("unused")
	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) throws ArrayIndexOutOfBoundsException, NumberFormatException {
		guild = evt.getGuild();
        author = evt.getAuthor();
        channel = evt.getChannel();
        receivedMessage = evt.getMessage();
        receivedMessage.getRawContent();
        List<Message> messageHistory; 
        try {
			messageHistory = channel.getHistory().retrievePast(100).block();
		} catch (RateLimitedException e) {
			e.printStackTrace();
		}
        
        String noArgs = beheadedMessage.split(" ")[0];
		switch(noArgs){
		case "add":
			
		case "":
			channel.sendMessage(getExtendedHelp()).queue();
			break;
		}
	}
}
