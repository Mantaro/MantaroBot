package net.kodehawa.discord.Mantaro.listeners;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.kodehawa.discord.Mantaro.bot.MantaroBot;
import net.kodehawa.discord.Mantaro.commands.storm.Birthday;
import net.kodehawa.discord.Mantaro.utils.LogTypes;
import net.kodehawa.discord.Mantaro.utils.Logging;
import net.kodehawa.discord.Mantaro.utils.Values;

public class Listener extends ListenerAdapter 
{
	DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
	Calendar cal = Calendar.getInstance();

	public static boolean isMenction = false;
	private int count = 0;
	
	@Override
	public void onMessageReceived(MessageReceivedEvent evt)
	{
	    count = count + 1;
		boolean isPrivate = evt.isPrivate();
		
		if(!isPrivate && !Values.disabledServers.contains(evt.getGuild().getId()))
		{
			if(evt.getMessage().getContent().startsWith("@MantaroBot") || evt.getMessage().getContent().startsWith(MantaroBot.getInstance().getBotPrefix()) && evt.getMessage().getAuthor().getId() != evt.getJDA().getSelfInfo().getId())
			{
				if(evt.getMessage().getContent().startsWith("@MantaroBot")){ isMenction = true; } else { isMenction = false; }
				MantaroBot.onCommand(MantaroBot.getInstance().getParser().parse(evt.getMessage().getContent(), evt));
				if(MantaroBot.getInstance().debugMode){ Logging.instance().print("Listened to: '" + evt.getMessage().getContent().replace(MantaroBot.getInstance().getBotPrefix(), "") + "' command.", LogTypes.INFO); }

			}
		}
		else if(evt.getMessage().getContent().startsWith("~>bot.status "))
		{
			MantaroBot.onCommand(MantaroBot.getInstance().getParser().parse(evt.getMessage().getContent(), evt));
		}
				
		//Storm's server birthday thingy
		try{
			if(evt.getGuild().getId().equals("147276654014758914"))
			{
				//System.out.println("Am I listening?");
				if(Birthday.bd.containsKey(evt.getAuthor().getId()))
				{
					//System.out.println("Am I listening?");
					//dd-MM-yyyy with yyyy trimmed
					//System.out.println(Birthday.bd.get(evt.getAuthor().getId()).substring(0, 5));
					//System.out.println(dateFormat.format(cal.getTime()).substring(0, 5));
					if(Birthday.bd.get(evt.getAuthor().getId()).substring(0, 5).equals(dateFormat.format(cal.getTime()).substring(0, 5)))
					{
						List<User> user = evt.getGuild().getUsers();
						List<Role> roles = evt.getGuild().getRoles();
						
						User userToAssign = null;
						Role birthdayRole = null;
						
						int n = -1;
						for(@SuppressWarnings("unused") User users : user)
						{
							//System.out.println("I am looping.");
							n = n + 1;
							if(user.get(n).getId() == evt.getAuthor().getId())
							{
								//which to assign the role
								userToAssign = user.get(n);
								n = -1;
								for(Role role : roles){
									n = n + 1;
									if(role.getName().contains("Birthday"))
									{
										//which role
										birthdayRole = roles.get(n);
										break;
									}
								}
								
								if(count == 1)
								{
									Logging.instance().print("Woah, someone just gained a year today, role " + birthdayRole.getName() + " assigned.", LogTypes.INFO);
									evt.getGuild().getManager().addRoleToUser(userToAssign, birthdayRole);
									evt.getGuild().getManager().update();
									evt.getTextChannel().sendMessage(userToAssign.getAsMention() + " is in his/her birthday now!");
								}
								break;
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			//e.printStackTrace();
		}
		
		if(evt.getMessage().getContent().contains("you broke")  || evt.getMessage().getContent().contains("You broke") || evt.getMessage().getContent().contains("it's broken") || evt.getMessage().getContent().contains("I broke"))
		{
			evt.getChannel().sendMessageAsync("It's not broken, it's a feature.", null);
		}
				if(evt.getMessage().getContent().contains("awoo") || evt.getMessage().getContent().contains("Awoo"))

		{
			evt.getChannel().sendMessageAsync("https://pbs.twimg.com/profile_images/578805576701870080/jr1_XDbp.jpeg", null);
		}
	}
	
	//soon (tm)
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {}
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {}
}

