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
		new StringArrayFile("quotes", quotes , false);
	}

	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) throws ArrayIndexOutOfBoundsException, NumberFormatException {
		guild = evt.getGuild();
        author = evt.getAuthor();
        channel = evt.getChannel();
        receivedMessage = evt.getMessage();
        
		if(!beheadedMessage.startsWith("read") && !beheadedMessage.startsWith("list") && !beheadedMessage.startsWith("get phrase") && !beheadedMessage.startsWith("help"))
		{
			String quote = beheadedMessage;
			if(!beheadedMessage.isEmpty())
			{
				quotes.add(quote);
				new StringArrayFile("quotes", quotes, true, false);
				channel.sendMessage(":pencil2: Quote succesfully added: " + quote + " (Do ~>quote list in #spam to get the call number for now." ).queue();
			}
			else
			{
				channel.sendMessage(":heavy_multiplication_x: " + "Why are you trying to add an empty quote ;-;").queue();
			}
		}
		else if(beheadedMessage.startsWith("read"))
		{
			String number = beheadedMessage.replace("read ", "");
			try
			{
				int number2 = Integer.parseInt(number);
				channel.sendMessage(quotes.get(number2)).queue();
			}
			catch(Exception e)
			{
				if(e instanceof ArrayIndexOutOfBoundsException)
				{
					channel.sendMessage(":heavy_multiplication_x: " + "Number specified is larger than the last quote call number.");
				}
				else if(e instanceof NumberFormatException)
				{
					channel.sendMessage(":heavy_multiplication_x: " + "Not a number, silly you");
				}
			}

		}
		else if(beheadedMessage.startsWith("get phrase"))
		{
			System.out.println("Hello");
			String phrase = beheadedMessage.replace("get phrase ", "");
			try
			{
				int n = -1;
				for(String message1 : quotes)
				{
					n = n + 1;

					if(message1.contains(phrase))
					{
						channel.sendMessage(quotes.get(n)).queue();
						break;
					}
				}
			}
			catch(Exception e)
			{
				channel.sendMessage(":heavy_multiplication_x: " + "Not valid, silly you").queue();
			}

		}
		else if(beheadedMessage.startsWith("list"))
		{
			StringBuilder listString = new StringBuilder();

			int n = -1;
			for (String quotes : quotes)
			{
				n = n + 1;
			    listString.append(quotes + " (Call number: " + n + ")" +"\r\n");
			}

			channel.sendMessage("``` Avaliable Quotes: \r" + listString.toString() + "```").queue();
		}
		else if(beheadedMessage.startsWith("help"))
		{
			channel.sendMessage("```| MantaroBot quote module help: \r \r"
					+ "~>quote 'example', adds a quote to the quote list. (Usage example: ~>quote I like dogs -chuchy.)  \r \r"
					+ "~>quote list, gets a list of all the quotes avaliable \r \r"
					+ "~>quote read number, gets the quote matching the number. (Usage example: ~>quote read 0) \r \r"
					+ "~>quote get phrase example, searches for the first quote which matches your search criteria and prints it. (Usage example: ~>quote get phrase dogs).``` \n"
					).queue();
		}
		else{
			channel.sendMessage("Silly you, this won't happen.").queue();
		}
	}
}
