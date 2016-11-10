package net.kodehawa.discord.Mantaro.listeners;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.bot.MantaroBot;
import net.kodehawa.discord.Mantaro.commands.admin.perms.Permissions;
import net.kodehawa.discord.Mantaro.commands.storm.Birthday;
import net.kodehawa.discord.Mantaro.utils.HashMapUtils;
import net.kodehawa.discord.Mantaro.utils.LogType;
import net.kodehawa.discord.Mantaro.utils.Logger;
import net.kodehawa.discord.Mantaro.utils.Values;

public class Listener extends ListenerAdapter
{
	DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
	Calendar cal = Calendar.getInstance();
	ArrayList<String> userCommands = new ArrayList<String>();
	ArrayList<String> adminCommands = new ArrayList<String>();
	ArrayList<String> ownerCommands = new ArrayList<String>();

	public static boolean isMenction = false;
	private int count = 0;
	
	@Override
	public void onMessageReceived(MessageReceivedEvent evt)
	{
	    count = count + 1;
		boolean isPrivate = evt.isPrivate();
		
		//Permission checker.
		//In code is as follows
		//master = owner | user = user | admin = admin
		for(@SuppressWarnings("rawtypes") Class c : MantaroBot.getInstance().classes)
		{
			Method[] methods = c.getMethods();
			int n = -1;
			for (Method m : methods)
			{
				
			    if (m.isAnnotationPresent(ModuleProperties.class) && n == 0)
			    {
			        ModuleProperties ta = m.getAnnotation(ModuleProperties.class);
			        if(ta.level().equals("admin"))
			        {
			        	adminCommands.add(ta.name());
			        }
			        if(ta.level().equals("user"))
			        {
			        	userCommands.add(ta.name());
			        }
			        if(ta.level().equals("owner"))
			        {
			        	ownerCommands.add(ta.name());
			        }
			        break;
			    }
			}
			break;
		}
		
		/**for(String s : userCommands)
		{
			
		}
		
		for(String s1 : adminCommands)
		{
			
		}
		
		for(String s2 : ownerCommands)
		{
			
		}**/
		
		if(!isPrivate && !Values.disabledServers.contains(evt.getGuild().getId()))
		{
			
			if(evt.getMessage().getContent().startsWith("@MantaroBot") || evt.getMessage().getContent().startsWith(MantaroBot.getInstance().getBotPrefix()) && evt.getMessage().getAuthor().getId() != evt.getJDA().getSelfInfo().getId())
			{
				if(evt.getMessage().getContent().startsWith("@MantaroBot")){ isMenction = true; } else { isMenction = false; }
				MantaroBot.onCommand(MantaroBot.getInstance().getParser().parse(evt.getMessage().getContent(), evt));
				if(MantaroBot.getInstance().debugMode){ Logger.instance().print("Listened to: '" + evt.getMessage().getContent().replace(MantaroBot.getInstance().getBotPrefix(), "") + "' command.", LogType.INFO); }

			}
		}
		else if(evt.getMessage().getContent().startsWith("~>bot.status "))
		{
			MantaroBot.onCommand(MantaroBot.getInstance().getParser().parse(evt.getMessage().getContent(), evt));
		}

		/**
		 * "I am listening/looping" prints are for simple debugging to see if the method is called in case anything changes.
		 */
		Thread t = new Thread() {
			
            @Override 
            public void run() {
            	//Storm's server birthday looker. Also portable to other servers
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
        						int n = -1;
        						List<User> user = evt.getGuild().getUsers();
        						List<Role> roles = evt.getGuild().getRoles();
        						
        						User userToAssign = null;
        						Role birthdayRole = null;
        						for(@SuppressWarnings("unused") User users : user)
        						{
        							//System.out.println("I am looping.");
        							n = n + 1;
        							if(user.get(n).getId() == evt.getAuthor().getId())
        							{
        								//who to assign the role
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
        									Logger.instance().print("Woah, someone just gained a year today, role " + birthdayRole.getName() + " assigned.", LogType.INFO);
        									evt.getGuild().getManager().addRoleToUser(userToAssign, birthdayRole);
        									evt.getGuild().getManager().update();
        									evt.getTextChannel().sendMessage(userToAssign.getAsMention() + " is in his/her birthday now!");
        								}
        								break;
        							}
        						}
        						
        					}
        					else
        					{
    							int n1 = -1;
        						List<User> user1 = evt.getGuild().getUsers();
        						List<Role> roles1 = evt.getGuild().getRoles();
        						
        						User userToAssign1 = null;
        						Role birthdayRole1 = null;
        						for(@SuppressWarnings("unused") User users1 : user1)
        						{
        							
        							n1 = n1 + 1;
        							if(user1.get(n1).getId() == evt.getAuthor().getId())
        							{
        								//who to assign the role
        								userToAssign1 = user1.get(n1);
        								n1 = -1;
        								for(Role role : roles1){
        									n1 = n1 + 1;
        									if(role.getName().contains("Birthday"))
        									{
        										//which role
        										birthdayRole1 = roles1.get(n1);
        										break;
        									}
        								}
        								
        								if(count == 1)
        								{
        									Logger.instance().print("A day passed since someone had his/her birthday.", LogType.INFO);
        									evt.getGuild().getManager().removeRoleFromUser(userToAssign1, birthdayRole1);
        									evt.getGuild().getManager().update();
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
        			Logger.instance().print("Birthday thread has encountered a critical error.", LogType.CRITICAL);
        			Logger.instance().print(e.getMessage(), LogType.CRITICAL);
        			//Prints stacktrace if it's running in debug mode.
        			if(MantaroBot.getInstance().debugMode){ 
        				e.printStackTrace();
        			}
        		}
            }
		};
		t.setName("Birthday looker.");
		t.start();
		
		if(evt.getMessage().getContent().contains("you broke")  || evt.getMessage().getContent().contains("You broke") || evt.getMessage().getContent().contains("it's broken") || evt.getMessage().getContent().contains("I broke the"))
		{
			evt.getChannel().sendMessageAsync("It's not broken, it's a feature.", null);
		}
				if(evt.getMessage().getContent().contains("awoo") || evt.getMessage().getContent().contains("Awoo"))

		{
			evt.getChannel().sendMessageAsync("https://pbs.twimg.com/profile_images/578805576701870080/jr1_XDbp.jpeg", null);
		}
				
		t.interrupt();	
		
		
		Thread permissionsThread = new Thread() {
			
            @Override 
            public void run() {
            	for(User user : evt.getGuild().getUsers())
        		{
            		if(!Permissions.perms.containsKey(user.getId())){
						Permissions.perms.put(user.getId(), "user" + ":" + evt.getGuild().getId());
						System.out.println("Automatically set permission level " + "user" + " in server " + evt.getGuild().getName() + " to user" + user.getUsername());
						new HashMapUtils("mantaro", "perms", Permissions.perms, Permissions.FILE_SIGN, true);
            		}
        		}
            }
		};
		
		permissionsThread.setName("Permission set thread");
		permissionsThread.start();
		
		
		permissionsThread.interrupt();
	}
	
	//soon (tm)
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {}
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {}
}

