package net.kodehawa.discord.Mantaro.commands.admin.perms;

import java.util.HashMap;
import java.util.List;

import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;
import net.kodehawa.discord.Mantaro.utils.HashMapUtils;
import net.kodehawa.discord.Mantaro.utils.LogType;
import net.kodehawa.discord.Mantaro.utils.Logger;

/**
 * Permissions are stored on a HashMap with the following output userid=permissionlevel:serverid and checked when someone performs a command.
 * Permission levels are as follows:
 * -> User: can use all common/util/special commands.
 * -> Admin: can use all common/util/special/server management commands.
 * -> Owner: can shutdown the bot, etc (only Kodehawa and Kyrex)
 * It's responsability of the server admin to add the corresponding permissions. Server owner will always have permission level manage and by default the default permission level is User.
 * Permission level owner can only be set in code (hardcoded) or by Kodehawa in Mantaro test server.
 * @author Yomura
 *
 */

public class Permissions implements Command {
	
	public static HashMap<String, String> perms = new HashMap<String, String>();
	public static String FILE_SIGN = "ada9cd5864c1365284072e5a7f39ce58";

	public Permissions()
	{
		Logger.instance().print("Permission module call recieved.", LogType.INFO);
		new HashMapUtils("mantaro", "perms", perms, FILE_SIGN, false);
	}

	@Override
	@ModuleProperties(level = "admin", name = "permission", type = "manage", description = "Changes permissions.", takesArgs = true)
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		if(!evt.getAuthor().getId().equals("155867458203287552"))
		{
			List<User> mentions = evt.getMessage().getMentionedUsers();
			String userId = null;
			for(User s : mentions)
			{
				userId = s.getId(); 
			}

			String userPermissionLevel = msg[1];
			String ownerId = evt.getGuild().getOwnerId();
			String serverId = evt.getGuild().getId();
			
			if(msg[1].equals("user") || msg[1].equals("admin") && evt.getAuthor().getId().equals(ownerId) && userId != null)
			{
				if(perms.containsKey(userId))
				{
					if(!perms.get(userId).contains(serverId))
					{
						perms.put(userId, userPermissionLevel + ":" + serverId);
						evt.getChannel().sendMessageAsync("Manually set permission level " + userPermissionLevel + " in server with id " + serverId, null);
						new HashMapUtils("mantaro", "perms", perms, FILE_SIGN, true);
					}
					else
					{
						perms.remove(userId);
						perms.put(userId, userPermissionLevel + ":" + serverId);
						evt.getChannel().sendMessageAsync("Manually changed permission level " + userPermissionLevel + " in server with id " + serverId, null);
						new HashMapUtils("mantaro", "perms", perms, FILE_SIGN, true);

					}
				}
				else
				{
					perms.put(userId, userPermissionLevel + ":" + serverId);
					evt.getChannel().sendMessageAsync("Manually set permission level of" + userPermissionLevel + " in server with id " + serverId, null);
					new HashMapUtils("mantaro", "perms", perms, FILE_SIGN, true);
				}
			}
			else
			{
				evt.getChannel().sendMessageAsync("Permission level specified isn't valid or you mentioned no user.\r\n Valid levels: user, admin", null);
			}
		}
		else
		{
			List<User> mentions = evt.getMessage().getMentionedUsers();
			String userId = null;
			for(User s : mentions)
			{
				userId = s.getId(); 
			}
			
			String userPermissionLevel = msg[1];
			String serverId = evt.getGuild().getId();
			
			if(msg[1].equals("user") || msg[1].equals("admin") && userId != null)
			{
				if(perms.containsKey(userId))
				{
					if(!perms.get(userId).contains(serverId))
					{
						perms.put(userId, userPermissionLevel + ":" + serverId);
						evt.getChannel().sendMessageAsync("Manually set permission level " + userPermissionLevel + " in server with id " + serverId, null);
						new HashMapUtils("mantaro", "perms", perms, FILE_SIGN, true);
					}
					else
					{
						perms.remove(userId);
						perms.put(userId, userPermissionLevel + ":" + serverId);
						evt.getChannel().sendMessageAsync("Manually changed permission level " + userPermissionLevel + " in server with id " + serverId, null);
						new HashMapUtils("mantaro", "perms", perms, FILE_SIGN, true);
					}
				}
				else
				{
					perms.put(userId, userPermissionLevel + ":" + serverId);
					evt.getChannel().sendMessageAsync("Manually set permission level " + userPermissionLevel + " in server with id " + serverId, null);
					new HashMapUtils("mantaro", "perms", perms, FILE_SIGN, true);
				}
			}
			else
			{
				evt.getChannel().sendMessageAsync("Permission level specified isn't valid or you mentioned no user.\r\n Valid levels: user, admin, owner", null);
			}
		}
	}

	public static String getPermissionLevel(String user)
	{
		String whole = Permissions.perms.get(user);
		String permissionLevel = whole.split(":")[0];
		return permissionLevel;
	}
	
	public static String getServerId(String user)
	{
		String whole = Permissions.perms.get(user);
		String serverId = whole.split(":")[1];
		return serverId;
	}
	
	public static int getPermissionId(String user)
	{
		int permissionLevel = 0;
		if(Permissions.getPermissionLevel(user).equals("user")){ permissionLevel = 0; }
		if(Permissions.getPermissionLevel(user).equals("admin")){ permissionLevel = 1; }
		if(Permissions.getPermissionLevel(user).equals("owner")){ permissionLevel = 2; }
		return permissionLevel;
	}
}
