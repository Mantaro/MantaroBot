package net.kodehawa.mantarobot.cmd.parser;

import java.util.ArrayList;
import java.util.Collections;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Parser {
	
	public Container parse(String prefix, String rw, MessageReceivedEvent evt)
	{
		if(rw.startsWith(prefix))
		{
			ArrayList<String> split = new ArrayList<>();
			String beheaded = rw.replaceFirst(prefix, "");
			String[] splitBeheaded = beheaded.split(" ");
			Collections.addAll(split, splitBeheaded);
			
			String invoke = split.get(0);
			String[] args = new String[split.size() - 1];
			split.subList(1, split.size()).toArray(args);
			return new Container(rw, beheaded, splitBeheaded, invoke, args, evt);
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
