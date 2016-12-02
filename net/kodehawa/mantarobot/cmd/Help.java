package net.kodehawa.mantarobot.cmd;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.core.Mantaro;

public class Help extends Command {

	private final Mantaro mantaro = Mantaro.instance();
	
	public Help(){
		setName("help");
	}
	
	@Override
	public void onCommand(String[] split, String content, MessageReceivedEvent event) {
		channel = event.getChannel();
		String command = content;
		if(content.isEmpty()){
			
		} else{
			if(mantaro.commands.containsKey(command)){
				channel.sendMessage(mantaro.commands.get(command).getExtendedHelp()).queue();
			}
		}
	}

}
