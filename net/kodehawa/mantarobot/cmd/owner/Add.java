package net.kodehawa.mantarobot.cmd.owner;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.Action;
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
			String noArgs = content.split(" ")[0];
			switch(noArgs){
			case "greeting":
				String greet = content.replace("greeting" + " ", "");
				Action.greeting.add(greet);
				new StringArrayFile("greeting", Action.greeting, true, true);
				channel.sendMessage(":speech_balloon:" + "Added to greeting list: " + greet);
				break;
			case "tsun":
				String tsun = content.replace("tsun" + " ", "");
				Action.tsunLines.add(tsun);
				new StringArrayFile("tsunderelines", Action.tsunLines, true, true);
				channel.sendMessage(":speech_balloon:" + "Added to tsundere list: " + tsun);
				break;
			default:
				channel.sendMessage(":speech_balloon:" + "Silly master, use ~>add greeting or ~>add tsun");
				break;
			}
		}
		else{
			channel.sendMessage(":heavy_multiplication_x:" + "How did you even know?");
		}
	}
}
