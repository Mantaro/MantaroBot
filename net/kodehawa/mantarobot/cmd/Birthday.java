package net.kodehawa.mantarobot.cmd;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.management.Command;
import net.kodehawa.mantarobot.util.HashMapUtils;

public class Birthday extends Command {

	private static HashMap<String, String> bd = new HashMap<>();
	private String FILE_SIGN = "d41d8cd98f00b204e9800998ecf8427e";
	
	public Birthday()
	{
		setName("birthday");
		setDescription("Sets your birthday date.");
		setCommandType("user");
		new HashMapUtils("mantaro", "bd", bd, FILE_SIGN, false);
	}
	
	@Override
	public void onCommand(String[] split, String content, MessageReceivedEvent event) {
		String userId = event.getMessage().getAuthor().getId();
		SimpleDateFormat format1 = new SimpleDateFormat("dd-MM-yyyy");
		Date bd1 = null;
		//So they don't input something that isn't a date...
		try{
			bd1 = format1.parse(split[0]);
		} catch(Exception e){
			event.getChannel().sendMessage(":heavy_multiplication_x: a valid date.").queue();
			e.printStackTrace();
		}
		
		if(bd1 != null){
			if(!bd.containsKey(userId)){
				String finalBirthday = format1.format(bd1);
				
				bd.put(event.getGuild().getId()+ ":" +userId, finalBirthday);
				new HashMapUtils("mantaro", "bd", bd, FILE_SIGN, true);
				event.getChannel().sendMessage("Added birthday date.").queue();
			}
			else{
				String finalBirthday = format1.format(bd1);
				
				bd.remove(userId);
				bd.put(event.getGuild().getId()+ ":" +userId, finalBirthday);
				new HashMapUtils("mantaro", "bd", bd, FILE_SIGN, true);
				event.getChannel().sendMessage("Changed birthday date.").queue();
			}
		}
	}

}
