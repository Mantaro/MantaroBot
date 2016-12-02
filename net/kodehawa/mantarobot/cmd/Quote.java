package net.kodehawa.mantarobot.cmd;

import java.util.concurrent.CopyOnWriteArrayList;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.util.StringArrayFile;

public class Quote extends Command {

	public static CopyOnWriteArrayList<String> quotes = new CopyOnWriteArrayList<String>();
	int i = 0;

	public Quote()
	{
		setName("quote");
		setDescription("Adds or retrieves quotes. See ~>quote help.");
		setCommandType("user");
		setExtendedHelp(
				"This command **quotes** a phrase.\r"
				+ "> Usage:\r"
				+ "~>quote content: Adds a quote with content defined by *content*.\r"
				+ "~>quote list: Gets a list of all avaliable quotes \r"
				+ "~>quote read number: Gets a quote matching the number. \r"
				+ "~>quote get phrase phrase: Searches for the first quote which matches your search criteria and prints it.\r"
				+ "> Parameters:\r"
				+ "*number*: A number from 0 to the total number of quotes.\r"
				+ "*content*: The content of the quote.\r"
				+ "*phrase*: A phrase used to match a quote with it.");
		new StringArrayFile("quotes", quotes , false);
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
				new StringArrayFile("quotes", quotes, true, false);
				channel.sendMessage(":pencil2: Quote succesfully added: " + quote + " (Do ~>quote list in #spam to get the call number for now." ).queue();
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
			    listString.append(quotes + " (Call number: " + n + ")" +"\r\n");
			}
			channel.sendMessage("``` Avaliable Quotes: \r" + listString.toString() + "```").queue();
			break;
		}
	}
}
