package net.kodehawa.discord.Mantaro.commands;

import java.lang.reflect.Method;
import java.util.ArrayList;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.bot.MantaroBot;
import net.kodehawa.discord.Mantaro.main.Command;

public class CHelp implements Command {

	private ArrayList<String> info = new ArrayList<String>();
	private ArrayList<String> adminInfo = new ArrayList<String>();
	
	public CHelp()
	{
		for(@SuppressWarnings("rawtypes") Class c : MantaroBot.getInstance().classes)
		{
			Method[] methods = c.getMethods();
			for (Method m : methods)
			{
			     if (m.isAnnotationPresent(ModuleProperties.class))
			    {
			        ModuleProperties ta = m.getAnnotation(ModuleProperties.class);
			        if(ta.level().equals("user") && !ta.type().equals("mention"))
			        {
				       info.add("| ~>" + ta.name()+ ", " +ta.description() + " " +ta.additionalInfo());
			        }
			        if(ta.level().equals("master") && !ta.type().equals("mention"))
			        {
				       adminInfo.add("| ~>" + ta.name()+ ", " +ta.description() + " " +ta.additionalInfo());
			        }
			    }
			}
		}
	}
	
	@Override
	@ModuleProperties(level = "user", name = "help", type = "common", description = "Sends you this help message.")
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}
	

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		String msgId = evt.getMessage().getId();
		
		StringBuilder listString1 = new StringBuilder();
		
		for (String help1 : info)
		{
		    listString1.append(help1+"\r\n");
		}
		
		evt.getAuthor().getPrivateChannel().sendMessage("```ruby\nWelcome." + "\r" + "MantaroBot version: " + MantaroBot.getInstance().getBuild() + " running on JDA. Here is a list of useful commands you can use with this bot. The commands listed here are only avaliable using ~> and not via mentioning the bot. CleverBot talk is avaliable via @Mantarobot talk *message here*```");
		evt.getAuthor().getPrivateChannel().sendMessage("```\n"+listString1.toString()+"```");
		if(evt.getAuthor().getId().equals("155867458203287552")){
			StringBuilder listString2 = new StringBuilder();

			for (String help2 : adminInfo)
			{
			    listString2.append(help2+"\r\n");
			}
			
			evt.getAuthor().getPrivateChannel().sendMessage("```\n"+listString2.toString()+"```");
		}				
		
		evt.getChannel().deleteMessageById(msgId);

	}

	@Override
	public void actionResult(boolean result, MessageReceivedEvent evt) {
		System.out.println("Command executed " + this.getClass().getName() + ", in channel " + evt.getChannel().toString() + " (" + evt.getMessage().toString() + " )");
	}

}
