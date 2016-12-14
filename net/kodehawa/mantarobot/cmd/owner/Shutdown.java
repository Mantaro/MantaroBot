package net.kodehawa.mantarobot.cmd.owner;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.Action;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.listeners.Listener;

public class Shutdown extends Command {

	public Shutdown()
	{
		setName("shutdown");
		setDescription("Shuts down the bot.");
		setCommandType("owner");
	}
	
	@Override
	public void onCommand(String[] message, String content, MessageReceivedEvent event)
	{
		if(event.getAuthor().getId().equals(Mantaro.OWNER_ID) || event.getAuthor().getId().equals(Mantaro.SERVER_MGR_ID)){
			channel.sendMessage("Gathering information...");
			try {
				//new StringArrayUtils("quotes", Quote.quotes, true);
			    Thread.sleep(50);
			} catch (InterruptedException ignored) {	}
			
			channel.sendMessage("Gathered.").queue();
			
			channel.sendMessage("Starting bot shutdown...").queue();
			try {
				Mantaro.instance().getSelf().removeEventListener(new Listener());
				Action.tsunLines.clear();
				System.gc();
				Mantaro.instance().modules.clear();
			    Thread.sleep(50);
			} catch (InterruptedException ignored) {	}

			channel.sendMessage("*goes to sleep*").queue();
			try {
				Thread.sleep(50);
			} catch (InterruptedException ignored) {	}

			try{
				System.exit(1);
			}
			catch (Exception e)
			{
				System.out.println(":heavy_multiplication_x: " + "Couldn't shut down." + e.toString());
			}
		}
		else{
			channel.sendMessage(":heavy_multiplication_x:" + "You cannot do that, silly.").queue();
		}
	}
}
