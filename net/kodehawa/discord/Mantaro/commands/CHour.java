package net.kodehawa.discord.Mantaro.commands;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;

public class CHour implements Command {

	@Override
	@ModuleProperties(level = "user", name = "time", type = "common", description = "Tells you the time in a specific GMT timezone of the world."
	, additionalInfo = "For example, ~>time GMT-3 gets you the time on GMT-3 (Chile)", takesArgs = true)
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		try
		{
			evt.getChannel().sendMessageAsync("```\n" + dateGMT(whole.replace("~>time ", "")) + "```" , null);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override	
	public void actionResult(boolean result, MessageReceivedEvent evt) {
		System.out.println("Command executed " + this.getClass().getName() + ".");
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
