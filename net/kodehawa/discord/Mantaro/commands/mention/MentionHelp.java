package net.kodehawa.discord.Mantaro.commands.mention;

import java.lang.reflect.Method;
import java.util.ArrayList;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.bot.MantaroBot;
import net.kodehawa.discord.Mantaro.main.Command;

public class MentionHelp implements Command {

	private ArrayList<String> info = new ArrayList<String>();
	private ArrayList<String> adminInfo = new ArrayList<String>();

	public MentionHelp()
	{
		for(@SuppressWarnings("rawtypes") Class c : MantaroBot.getInstance().classes)
		{
			Method[] methods = c.getMethods();
			for (Method m : methods)
			{
			     if (m.isAnnotationPresent(ModuleProperties.class))
			    {
			        ModuleProperties ta = m.getAnnotation(ModuleProperties.class);
			        if(ta.level().equals("user") && ta.type().equals("mention"))
			        {
				       info.add("| @MantaroBot " + ta.name()+ ", " +ta.description() + " " +ta.additionalInfo());
			        }
			        if(ta.level().equals("master") && ta.type().equals("mention"))
			        {
				       adminInfo.add("@MantaroBot " + ta.name()+ ", " +ta.description() + " " +ta.additionalInfo());
			        }
			    }
			}
		}
	}
	
	@Override
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	@ModuleProperties(level = "user", name = "help", type = "mention", description = "Menction command help.")
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		StringBuilder listString = new StringBuilder();		StringBuilder listString2 = new StringBuilder();

		for (String help : info)
		{
		    listString.append(help+"\r\n");
		}
		
		for (String help2 : adminInfo)
		{
		    listString2.append(help2+"\r\n");
		}
			
		evt.getAuthor().getPrivateChannel().sendMessage("```" + listString.toString() + "``` \n");
		if(!adminInfo.isEmpty())
		{
			evt.getAuthor().getPrivateChannel().sendMessage("```" + listString2.toString() + "``` \n");
		}
	}

	@Override
	public void actionResult(boolean result, MessageReceivedEvent evt) {
		System.out.println("Command executed " + this.getClass().getName() + ".");
	}

}
