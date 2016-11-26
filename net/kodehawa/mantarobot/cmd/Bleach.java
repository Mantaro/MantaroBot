package net.kodehawa.mantarobot.cmd;

import java.util.ArrayList;
import java.util.Random;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;

public class Bleach extends Command {

	private ArrayList<String> bleach = new ArrayList<String>();
	
	public Bleach()
	{
		setName("bleach");
		setDescription("Random guy/girl drinking bleach.");
		bleach.add("http://puu.sh/qyoDQ/9df29f6b30.jpg");
		bleach.add("http://data.whicdn.com/images/13651431/superthumb.jpg");
		bleach.add("https://i.ytimg.com/vi/IjgPHJTbfK4/maxresdefault.jpg");
		bleach.add("https://media0.giphy.com/media/fN96l0NwjjOGQ/200_s.gif");
		bleach.add("https://www.youtube.com/watch?v=5PIx19ha9MY");
	}
	
	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
		Random rand = new Random();
        int bleachRandomizer = rand.nextInt(bleach.size());
        channel = evt.getChannel();

		channel.sendMessage(bleach.get(bleachRandomizer)).queue();
	}
}
