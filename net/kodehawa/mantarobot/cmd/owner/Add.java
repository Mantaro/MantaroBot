package net.kodehawa.mantarobot.cmd.owner;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.Hi;
import net.kodehawa.mantarobot.cmd.Tsundere;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.util.StringArrayFile;

public class Add extends Command {

	public Add()
	{
		setName("add");
		setDescription("Adds a item to a list.");
		setCommandType("owner");
	}
	
	@Override
	public void onCommand(String[] message, String content, MessageReceivedEvent event){
		if(event.getAuthor().getId().equals(Mantaro.OWNER_ID)){
			String[] cases = {"greeting", "tsun"};

			if(content.startsWith(cases[0])){
				String greet = content.replace(cases[0] + " ", "");
				Hi.greeting.add(greet);
				new StringArrayFile("Greetings", Hi.greeting, true, true);
				channel.sendMessage(":speech_balloon:" + "Added to greeting list: " + greet);
			}
			else if(content.startsWith(cases[1])){
				String tsun = content.replace(cases[1] + " ", "");
				Tsundere.tsunLines.add(tsun);
				new StringArrayFile("tsunderelines", Tsundere.tsunLines, true, true);
				channel.sendMessage(":speech_balloon:" + "Added to tsundere list: " + tsun);
			}
			else{
				channel.sendMessage(":speech_balloon:" + "Silly master, use ~>add greeting or ~>add tsun");
			}
		}
		else{
			channel.sendMessage(":heavy_multiplication_x:" + "How did you even know?");
		}
	}
}
