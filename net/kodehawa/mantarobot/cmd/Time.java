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
	}

	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
        channel = evt.getChannel();
        
		try
		{
			channel.sendMessage("```\n" + dateGMT(beheadedMessage.replace("time ", "")) + "```").queue();
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
