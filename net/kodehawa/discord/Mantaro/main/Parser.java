package net.kodehawa.discord.Mantaro.main;

import java.util.ArrayList;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.bot.MantaroBot;

public class Parser {	
	
	public CommandContainer parse(String rw, MessageReceivedEvent evt)
	{
		if(rw.startsWith(MantaroBot.getInstance().getBotPrefix()))
		{
			ArrayList<String> split = new ArrayList<String>();
			String raw = rw;
			String beheaded = raw.replaceFirst(MantaroBot.getInstance().getBotPrefix(), "");
			String[] splitBeheaded = beheaded.split(" ");
			for(String s : splitBeheaded)
			{
				split.add(s);
			}
			
			String invoke = split.get(0);
			String[] args = new String[split.size() - 1];
			split.subList(1, split.size()).toArray(args);
			return new CommandContainer(raw, beheaded, splitBeheaded, invoke, args, evt);
		}
		
		else if(rw.startsWith("@MantaroBot"))
		{
			//System.out.println(rw);
			ArrayList<String> split = new ArrayList<String>();
			String raw = rw;
			String beheaded = raw.replaceFirst("@MantaroBot ", "");
			String[] splitBeheaded = beheaded.split(" ");
			for(String s : splitBeheaded)
			{
				split.add(s);
			}
			
			String invoke = split.get(0);
			String[] args = new String[split.size() - 1];
			split.subList(1, split.size()).toArray(args);
			return new CommandContainer(raw, beheaded, splitBeheaded, invoke, args, evt);
		}
		
		return null;
		
		
	}
	
	public class CommandContainer
	{
		public final String rawCommand;
		public String beheaded1 = "";
		public final String beheadedMain;
		public final String[] splitBeheaded;
		public final String invoke;
		public final String[] args;
		public final MessageReceivedEvent event;

	
		
		public CommandContainer(String raw, String beheaded, String[] splitBeheaded, String invoke, String[]args, MessageReceivedEvent evt)
		{
			rawCommand = raw;
			beheadedMain = beheaded;
			this.splitBeheaded = splitBeheaded;
			if(beheadedMain.contains(" "))
			{
				beheaded1 = beheaded.replace(splitBeheaded[0] + " ", "");
			}
			else
			{
				beheaded1 = beheaded.replace(splitBeheaded[0], "");
			}
			this.invoke = invoke;
			this.args = args;
			event = evt;
		}
	 }
		
}
