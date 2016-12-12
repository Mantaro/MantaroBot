package net.kodehawa.mantarobot.cmd.servertools;

import java.io.File;
import java.util.HashMap;

import org.json.JSONObject;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.util.JSONUtils;

public class Parameters extends Command {

	private static HashMap<String, String> prefixes = new HashMap<>();
	private static HashMap<String, String> logs = new HashMap<>();
	private static HashMap<String, String> nsfw = new HashMap<>();
	private JSONObject logObject = new JSONObject();
	private JSONObject prefixObject = new JSONObject();
	private JSONObject nsfwObject = new JSONObject();
	private File logFile;
	private File prefixFile;
	private File nsfwFile;

	public Parameters(){
		logObject.put("version", "1.0");
		
		setName("params");
		setCommandType("servertool");
		setDescription("Enables and disables logs, sets a custom prefix, etc. Check ~>help params");
		setExtendedHelp(
				"This command sets specific parameters in your server.\n"
				+ "**Parameters:**\n"
				+ "~>params logs set enable channel\n"
				+ "~>params logs set disable\n"
				+ "~>params prefix set prefix\n"
				+ "~>params prefix disable\n"
				+ "~>params nsfw set channel\n"
				+ "~>params nsfw disable\n"
				+ "**Parameter explanation:**\n"
				+ "*channel*: The channel name to action in."
				+ "*prefix*: The prefix to set."
						);
		if(Mantaro.instance().isWindows()){ this.logFile = new File("C:/mantaro/config/logconf.json"); }
		else if(Mantaro.instance().isUnix()){ this.logFile = new File("/home/mantaro/config/logconf.json"); }
		if(!logFile.exists()){
			JSONUtils.instance().createFile(logFile);
			JSONUtils.instance().write(logFile, logObject);
		}
		
		logObject = JSONUtils.instance().getJSONObject(logFile);
		JSONUtils.instance().read(logs, logObject);
		prefixObject.put("default", "~>");
		if(Mantaro.instance().isWindows()){ this.prefixFile = new File("C:/mantaro/config/prefix.json"); }
		else if(Mantaro.instance().isUnix()){ this.prefixFile = new File("/home/mantaro/config/prefix.json"); }
		if(!prefixFile.exists()){
			JSONUtils.instance().createFile(prefixFile);
			JSONUtils.instance().write(prefixFile, prefixObject);
		}
		
		prefixObject = JSONUtils.instance().getJSONObject(prefixFile);
		JSONUtils.instance().read(prefixes, prefixObject);
		nsfwObject.put("213468583252983809", "nsfw");
		if(Mantaro.instance().isWindows()){ this.nsfwFile = new File("C:/mantaro/config/nsfw.json"); }
		else if(Mantaro.instance().isUnix()){ this.nsfwFile = new File("/home/mantaro/config/nsfw.json"); }
		
		if(!nsfwFile.exists()){
			JSONUtils.instance().createFile(nsfwFile);
			JSONUtils.instance().write(nsfwFile, nsfwObject);
		}
		
		nsfwObject = JSONUtils.instance().getJSONObject(nsfwFile);
		JSONUtils.instance().read(nsfw, nsfwObject);
	}
	
	@Override
	public void onCommand(String[] split, String content, MessageReceivedEvent event) {

		guild = event.getGuild();
        author = event.getAuthor();
        channel = event.getChannel();
        receivedMessage = event.getMessage();
        
		String noArgs = split[0];
		String mainArgs = split[1];
		switch(noArgs){
		case "logs":
			System.out.println(mainArgs);
			switch(mainArgs){
			case "set":
				if(guild.getMember(author).hasPermission(Permission.ADMINISTRATOR))
				{
					TextChannel logChannel = guild.getTextChannelsByName(split[2], true).get(0);
					logObject.put(guild.getId(), logChannel.getName());
					JSONUtils.instance().write(logFile, logObject);
					JSONUtils.instance().read(logs, logObject);
					channel.sendMessage("Log channel set to " + "#" + logChannel.getName()).queue();
				} else {
					channel.sendMessage(":heavy_multiplication_x: You have no permissions to do this.");
				}
				break;
			case "disable":
				if(guild.getMember(author).hasPermission(Permission.ADMINISTRATOR))
				{
					logObject.remove(guild.getId());
					JSONUtils.instance().write(logFile, logObject);
					JSONUtils.instance().read(logs, logObject);
					channel.sendMessage("Removed server from logging.").queue();
				} else {
					channel.sendMessage(":heavy_multiplication_x: You have no permissions to do this.");
				}
				break;
			}
			break;
		case "prefix":
			switch(mainArgs){
            case "set":
            	if(guild.getMember(author).isOwner())
				{
                	prefixObject.put(guild.getId(), split[2]);
        			JSONUtils.instance().write(prefixFile, prefixObject);
        			JSONUtils.instance().read(prefixes, prefixObject);
        			channel.sendMessage("Channel bot prefix set to " + split[2]).queue();
        			break;
				}
            case "remove":
            	if(guild.getMember(author).isOwner())
				{
            		prefixObject.remove(guild.getId());
        			JSONUtils.instance().write(prefixFile, prefixObject);
        			JSONUtils.instance().read(prefixes, prefixObject);
        			channel.sendMessage("Channel bot prefix defaulted to ~>").queue();
        			break;
				}
            }
			break;
		case "nsfw":
			switch(mainArgs){
            case "set":
            	System.out.println("hi");
            	if(guild.getMember(author).hasPermission(Permission.ADMINISTRATOR))
				{
            		nsfwObject.put(guild.getId(), split[2]);
        			JSONUtils.instance().write(nsfwFile, nsfwObject);
        			JSONUtils.instance().read(nsfw, nsfwObject);
        			channel.sendMessage("NSFW channel set to #" + split[2]).queue();
				}
    			break;
            case "remove":
            	if(guild.getMember(author).hasPermission(Permission.ADMINISTRATOR))
				{
            		nsfwObject.remove(guild.getId());
        			JSONUtils.instance().write(nsfwFile, nsfwObject);
        			JSONUtils.instance().read(nsfw, nsfwObject);
        			channel.sendMessage("NSFW channel removed").queue();
        			break;
				}
            }
			break;
		}
	}
		
	public static String getPrefixForServer(String guildId){
		if(prefixes.get(guildId) == null){
			return prefixes.get("default");
		}
		return prefixes.get(guildId);
	}

	public static String getNSFWChannelForServer(String guildId){
		return nsfw.get(guildId);
	}
	
	public static String getLogChannelForServer(String serverid){
		return logs.get(serverid);
	}
	
	public static HashMap<String, String> getLogHash(){
		return logs;
	}
}