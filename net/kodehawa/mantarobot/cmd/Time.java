package net.kodehawa.mantarobot.cmd;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;

public class Time extends Command {

	public Time()
	{
		setName("time");
		setDescription("Retrieves time in a specific timezone. Usage example: ~>time GMT-3");
		setCommandType("user");
	}

	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
        channel = evt.getChannel();
        
		try{
			if(beheadedMessage.startsWith("GMT")){
				channel.sendMessage(":mega: The time is: " + dateGMT(beheadedMessage.replace("time ", "")) + " in " + beheadedMessage).queue();
			} else{
				channel.sendMessage(":heavy_multiplication_x: " + "You didn't specify a valid timezone").queue();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String dateGMT(String timezone) throws ParseException, NullPointerException
	{
		SimpleDateFormat dateGMT = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
		dateGMT.setTimeZone(TimeZone.getTimeZone(timezone));
		SimpleDateFormat dateLocal = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
		Date date1 = dateLocal.parse(dateGMT.format(new Date()));
		return DateFormat.getInstance().format(date1);
	}
}
