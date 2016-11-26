package net.kodehawa.mantarobot.cmd.owner;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import bsh.Interpreter;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.core.Mantaro;

public class Eval extends Command {
	
	public Eval()
	{
		setName("eval");
		setDescription("");
	}
	
	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
		if(evt.getAuthor().getId().equals(Mantaro.OWNER_ID))
		{
			try {
		    	Interpreter interpreter = new Interpreter();
		        ByteArrayOutputStream baos = new ByteArrayOutputStream();
		        PrintStream ps = new PrintStream(baos);
		        PrintStream old = System.out;
		        System.setOut(ps);
		    	interpreter.eval(beheadedMessage);
		        System.out.flush();
		        System.setOut(old);
		        evt.getChannel().sendMessage(baos.toString()).queue();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
