package net.kodehawa.mantarobot.cmd;

import java.util.concurrent.CopyOnWriteArrayList;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.util.StringArrayUtils;

public class Quote extends Command {

	public static CopyOnWriteArrayList<String> quotes = new CopyOnWriteArrayList<>();

	public Quote()
	{
		setName("quote");
		setDescription("Adds or retrieves quotes. See ~>help quote.");
		setCommandType("user");
		setExtendedHelp(
				"This command **quotes** a phrase.\n"
				+ "> Usage:\n"
				+ "~>quote content: Adds a quote with content defined by *content*.\n"
				+ "~>quote list: Gets a list of all avaliable quotes \n"
				+ "~>quote read number: Gets a quote matching the number. \n"
				+ "~>quote get phrase phrase: Searches for the first quote which matches your search criteria and prints it.\n"
				+ "> Parameters:\n"
				+ "*number*: A number from 0 to the total number of quotes.\n"
				+ "*content*: The content of the quote.\n"
				+ "*phrase*: A phrase used to match a quote with it.");
		new StringArrayUtils("quotes", quotes , false);
	}

	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) throws ArrayIndexOutOfBoundsException, NumberFormatException {
		guild = evt.getGuild();
        author = evt.getAuthor();
        channel = evt.getChannel();
        receivedMessage = evt.getMessage();
        
        String noArgs = beheadedMessage.split(" ")[0];
		switch(noArgs){
		default:
			String quote = beheadedMessage;
			if(!beheadedMessage.isEmpty()){
				quotes.add(quote);
				new StringArrayUtils("quotes", quotes, true, false);
				channel.sendMessage(":pencil2: Quote successfully added: " + quote + " (Do ~>quote list in #spam to get the call number for now." ).queue();
			} else{
				channel.sendMessage(":heavy_multiplication_x: " + "Why are you trying to add an empty quote ;-;").queue();
			}
		case "read":
			String number = beheadedMessage.replace("read ", "");
			try{
				int number2 = Integer.parseInt(number);
				channel.sendMessage(quotes.get(number2)).queue();
			}
			catch(Exception e){
				if(e instanceof ArrayIndexOutOfBoundsException){
					channel.sendMessage(":heavy_multiplication_x: " + "Number specified is larger than the last quote call number.");
				}
				else if(e instanceof NumberFormatException){
					channel.sendMessage(":heavy_multiplication_x: " + "Not a number, silly you");
				}
			}
			break;
		case "get phrase":
			String phrase = beheadedMessage.replace("get phrase ", "");
			try{
				int n = -1;
				for(String message1 : quotes){
					n++;
					if(message1.contains(phrase)){
						channel.sendMessage(quotes.get(n)).queue();
						break;
					}
				}
			}
			catch(Exception e){
				channel.sendMessage(":heavy_multiplication_x: " + "Not valid, silly you").queue();
			}
			break;
		case "list":
			StringBuilder listString = new StringBuilder();
			int n = -1;
			for (String quotes : quotes){
				n++;
			    listString.append(quotes).append(" (Call number: ").append(n).append(")").append("\n\n");
			}
			channel.sendMessage("``` Avaliable Quotes: \n" + listString.toString() + "```").queue();
			break;
		}
	}
}
