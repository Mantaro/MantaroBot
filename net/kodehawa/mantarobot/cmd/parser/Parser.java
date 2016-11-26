package net.kodehawa.mantarobot.cmd.parser;

import java.util.ArrayList;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.core.Mantaro;

public class Parser {
	
	public Container parse(String rw, MessageReceivedEvent evt)
	{
		if(rw.startsWith(Mantaro.instance().getPrefix()))
		{
			ArrayList<String> split = new ArrayList<String>();
			String raw = rw;
			String beheaded = raw.replaceFirst(Mantaro.instance().getPrefix(), "");
			String[] splitBeheaded = beheaded.split(" ");
			for(String s : splitBeheaded)
			{
				split.add(s);
			}
			
			String invoke = split.get(0);
			String[] args = new String[split.size() - 1];
			split.subList(1, split.size()).toArray(args);
			return new Container(raw, beheaded, splitBeheaded, invoke, args, evt);
		}
		
		return null;
}
	
	public class Container
	{
		public final String rawCommand;
		public String content = "";
		public final String beheadedMain;
		public final String[] splitBeheaded;
		public final String invoke;
		public final String[] args;
		public final MessageReceivedEvent event;

		public Container(String raw, String beheaded, String[] splitBeheaded, String invoke, String[]args, MessageReceivedEvent evt)
		{
			rawCommand = raw;
			beheadedMain = beheaded;
			this.splitBeheaded = splitBeheaded;
			if(beheadedMain.contains(" "))
			{
				content = beheaded.replace(splitBeheaded[0] + " ", "");
			}
			else
			{
				content = beheaded.replace(splitBeheaded[0], "");
			}
			this.invoke = invoke;
			this.args = args;
			event = evt;
		}
	 }
}
