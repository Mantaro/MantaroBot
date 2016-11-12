package net.kodehawa.discord.Mantaro.listeners;

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
import net.kodehawa.discord.Mantaro.bot.MantaroBot;
import net.kodehawa.discord.Mantaro.commands.admin.perms.Permissions;
import net.kodehawa.discord.Mantaro.commands.storm.Birthday;
import net.kodehawa.discord.Mantaro.manager.CommandManager;
import net.kodehawa.discord.Mantaro.utils.HashMapUtils;
import net.kodehawa.discord.Mantaro.utils.LogType;
import net.kodehawa.discord.Mantaro.utils.Logger;
import net.kodehawa.discord.Mantaro.utils.Values;

/**
 * Listens to messages in any servers. 
 * Servers as a multipropose class for modules or threads that require a periodical check.
 * This is probably the class with most debugging things too...
 * @author Yomura
 *
 */
public class Listener extends ListenerAdapter
{
	DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
	Calendar cal = Calendar.getInstance();
	ArrayList<String> commands = new ArrayList<String>();
	public static boolean isMenction = false;
	boolean hi = false;
	
	public Listener()
	{
		
	}
		
	@Override
	public void onMessageReceived(MessageReceivedEvent evt)
	{
		boolean isPrivate = evt.isPrivate();
		String command = "";
		String author = evt.getAuthor().getId();
		
		//Permission checker.
		//In code is as follows
		MantaroBot.getInstance().addPermissionValues();
		
		if(!isPrivate && !Values.disabledServers.contains(evt.getGuild().getId()))
		{			
			if(evt.getMessage().getContent().startsWith("@MantaroBot") || evt.getMessage().getContent().startsWith(MantaroBot.getInstance().getBotPrefix()) && evt.getMessage().getAuthor().getId() != evt.getJDA().getSelfInfo().getId())
			{
				System.out.println("print check");
				System.out.println(MantaroBot.getInstance().permissions.size());
				command = evt.getMessage().getContent().replace(MantaroBot.getInstance().getBotPrefix(), "");
				if(evt.getMessage().getContent().startsWith("@MantaroBot")){ isMenction = true; } else { isMenction = false; }
				int n = -1;
				for(@SuppressWarnings("unused") String s : MantaroBot.getInstance().permissions)
				{
					n = n + 1;
					System.out.println(MantaroBot.getInstance().permissions.get(n).split(":")[0]);
					System.out.println(command.split(" ")[0]);
					System.out.println(MantaroBot.getInstance().permissions.get(n).split(":")[0].equals(command.split(" ")[0]));

					if(MantaroBot.getInstance().permissions.get(n).split(":")[0].equals(command.split(" ")[0]))
					{
						String commandPermissionLevel = MantaroBot.getInstance().permissions.get(n).split(":")[1];
						int requiredPermissionId = 0;
						if(commandPermissionLevel.equals("user")){ requiredPermissionId = 0; }
						if(commandPermissionLevel.equals("admin")){ requiredPermissionId = 1; }
						if(commandPermissionLevel.equals("owner")){ requiredPermissionId = 2; }
						
						System.out.println(Permissions.getPermissionId(author) >= requiredPermissionId);
						System.out.println("Server id check (AND)" +  Permissions.getServerId(author).equals(evt.getGuild().getId()));
						System.out.println(Permissions.getServerId(author));
						System.out.println(evt.getGuild().getId());
						System.out.println("Owner check (OR)" + evt.getAuthor().getId().equals("155867458203287552"));
						if(Permissions.getPermissionId(author) >= requiredPermissionId && Permissions.getServerId(author).equals(evt.getGuild().getId()) || evt.getAuthor().getId().equals("155867458203287552"))
						{
							System.out.println("do i have permission");
							MantaroBot.onCommand(MantaroBot.getInstance().getParser().parse(evt.getMessage().getContent(), evt));
							break;
						}

						else
						{
							evt.getChannel().sendMessageAsync("You have no permission to execute that command", null);
						}
						break;
					}
				}
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
        						for(User users : user)
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
        								//hi = evt.getGuild().getRolesForUser(users).contains(birthdayRole);

        								
        								if(!evt.getGuild().getRolesForUser(users).contains(birthdayRole))
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
        						for(User users1 : user1)
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
        								
        								//hi = evt.getGuild().getRolesForUser(users1).contains(birthdayRole1);
        								
	        								
        								if(evt.getGuild().getRolesForUser(users1).contains(birthdayRole1))
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
        			
        			//if(MantaroBot.getInstance().debugMode){ System.out.println("Is birthday role on this user? " + hi); }
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
            		if(!Permissions.perms.containsKey(user.getId()) && Permissions.perms.size() > 0 /*Prevents this to actually start writing before reading the file tbh*/){
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

