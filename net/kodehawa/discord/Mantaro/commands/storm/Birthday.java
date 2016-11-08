package net.kodehawa.discord.Mantaro.commands.storm;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;
import net.kodehawa.discord.Mantaro.utils.HashMapUtils;

public class Birthday implements Command {

	public static HashMap<String, String> bd = new HashMap<String, String>();
	private String FILE_SIGN = "d41d8cd98f00b204e9800998ecf8427e";
	
	public Birthday()
	{
		new HashMapUtils("mantaro", "bd", bd, FILE_SIGN, false);
	}
	
	@Override
	@ModuleProperties(level = "user", name = "bd", type = "utils", description = "Sets or changes a birthday date.", additionalInfo = "Date format: dd-MM-yyyy") 
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt)
	{
		String userId = evt.getMessage().getAuthor().getId();
		SimpleDateFormat format1 = new SimpleDateFormat("dd-MM-yyyy");
		Date bd1 = null;
		//So they don't input something that isn't a date...
		try
		{
			bd1 = format1.parse(msg[0]);
		}
		catch(Exception e)
		{
			evt.getChannel().sendMessageAsync("Not a valid date.", null);
			e.printStackTrace();
		}
		
		if(bd1 != null)
		{
			if(!bd.containsKey(userId))
			{
				String finalBirthday = format1.format(bd1);
				
				bd.put(userId, finalBirthday);
				new HashMapUtils("mantaro", "bd", bd, FILE_SIGN, true);
				evt.getChannel().sendMessageAsync("Added birthday date.", null);
			}
			else
			{
				String finalBirthday = format1.format(bd1);
				
				bd.remove(userId);
				bd.put(userId, finalBirthday);
				new HashMapUtils("mantaro", "bd", bd, FILE_SIGN, true);
				evt.getChannel().sendMessageAsync("Changed birthday date.", null);
			}
		}
	}

}
