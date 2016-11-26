package net.kodehawa.mantarobot.cmd;

import java.util.HashMap;
import java.util.List;

import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.util.HashMapUtils;
import net.kodehawa.mantarobot.util.LogType;
import net.kodehawa.mantarobot.util.Logger;

/**
 * I need to redo this so I can make it work. I don't know how though, might need databases.
 * @author Yomura
 */
public class Permission extends Command {

	public static HashMap<String, String> perms = new HashMap<String, String>();
	public static String FILE_SIGN = "ada9cd5864c1365284072e5a7f39ce58";

	public Permission()
	{
		setName("perm");
		setDescription("");
		Logger.instance().print("Permission module call recieved.", LogType.INFO);
		new HashMapUtils("mantaro", "perms", perms, FILE_SIGN, false);
	}
	
	@Override
	public void onCommand(String[] split, String content, MessageReceivedEvent event) {
		if(!event.getAuthor().getId().equals(Mantaro.OWNER_ID))
		{
			List<User> mentions = event.getMessage().getMentionedUsers();
			String userId = null;
			for(User s : mentions)
			{
				userId = s.getId(); 
			}

			String userPermissionLevel = split[1];
			String ownerId = event.getGuild().getOwner().getUser().getId();
			String serverId = event.getGuild().getId();
			
			if(split[1].equals("user") || split[1].equals("admin") && event.getAuthor().getId().equals(ownerId) && userId != null)
			{
				if(perms.containsKey(userId))
				{
					if(!perms.get(userId).contains(serverId))
					{
						perms.put(userId, userPermissionLevel + ":" + serverId);
						event.getChannel().sendMessage("Manually set permission level " + userPermissionLevel + " in server with id " + serverId).queue();
						new HashMapUtils("mantaro", "perms", perms, FILE_SIGN, true);
					}
					else
					{
						perms.remove(userId);
						perms.put(userId, userPermissionLevel + ":" + serverId);
						event.getChannel().sendMessage("Manually changed permission level " + userPermissionLevel + " in server with id " + serverId).queue();
						new HashMapUtils("mantaro", "perms", perms, FILE_SIGN, true);

					}
				}
				else
				{
					perms.put(userId, userPermissionLevel + ":" + serverId);
					event.getChannel().sendMessage("Manually set permission level of" + userPermissionLevel + " in server with id " + serverId).queue();
					new HashMapUtils("mantaro", "perms", perms, FILE_SIGN, true);
				}
			}
			else
			{
				event.getChannel().sendMessage("Permission level specified isn't valid or you mentioned no user.\r\n Valid levels: user, admin").queue();
			}
		}
		else
		{
			List<User> mentions = event.getMessage().getMentionedUsers();
			String userId = null;
			for(User s : mentions)
			{
				userId = s.getId(); 
			}
			
			String userPermissionLevel = split[1];
			String serverId = event.getGuild().getId();
			
			if(split[1].equals("user") || split[1].equals("admin") && userId != null)
			{
				if(perms.containsKey(userId))
				{
					if(!perms.get(userId).contains(serverId))
					{
						perms.put(userId, userPermissionLevel + ":" + serverId);
						event.getChannel().sendMessage("Manually set permission level " + userPermissionLevel + " in server with id " + serverId).queue();
						new HashMapUtils("mantaro", "perms", perms, FILE_SIGN, true);
					}
					else
					{
						perms.remove(userId);
						perms.put(userId, userPermissionLevel + ":" + serverId);
						event.getChannel().sendMessage("Manually changed permission level " + userPermissionLevel + " in server with id " + serverId).queue();
						new HashMapUtils("mantaro", "perms", perms, FILE_SIGN, true);
					}
				}
				else
				{
					perms.put(userId, userPermissionLevel + ":" + serverId);
					event.getChannel().sendMessage("Manually set permission level " + userPermissionLevel + " in server with id " + serverId).queue();
					new HashMapUtils("mantaro", "perms", perms, FILE_SIGN, true);
				}
			}
			else
			{
				event.getChannel().sendMessage("Permission level specified isn't valid or you mentioned no user.\r\n Valid levels: user, admin, owner").queue();
			}
		}
	}
	
	public static String getPermissionLevel(String user)
	{
		String whole = perms.get(user);
		String permissionLevel = whole.split(":")[0];
		return permissionLevel;
	}
	
	public static String getServerId(String user)
	{
		String whole = perms.get(user);
		String serverId = whole.split(":")[1];
		return serverId;
	}
	
	public static int getPermissionId(String user)
	{
		int permissionLevel = 0;
		if(getPermissionLevel(user).equals("user")){ permissionLevel = 0; }
		if(getPermissionLevel(user).equals("admin")){ permissionLevel = 1; }
		if(getPermissionLevel(user).equals("owner")){ permissionLevel = 2; }
		return permissionLevel;
	}
}
