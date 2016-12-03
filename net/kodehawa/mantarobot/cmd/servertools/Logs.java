package net.kodehawa.mantarobot.cmd.servertools;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONObject;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.util.JSONUtils;

public class Logs extends Command {

	private static HashMap<String, String> logs = new HashMap<String, String>();
	JSONObject logObject = new JSONObject();
	File file;
	
	public Logs(){
		logObject.put("version", "1.0");
		
		setName("logs");
		setCommandType("servertool");
		setExtendedHelp(
				"This command enables or disables logs in your server.\n"
				+ "**Parameters:**\n"
				+ "~>logs set enable logchannel\n"
				+ "~>logs set disable\n"
				+ "**Parameter explanation:**\n"
				+ "*logchannel*: The channel name to log in. If the channel is #logs, use logs."
						);
		if(Mantaro.instance().isWindows()){
			this.file = new File("C:/mantaro/config/logconf.json");
		}
		else if(Mantaro.instance().isUnix()){
			this.file = new File("/home/mantaro/config/logconf.json");
		}
		if(!file.exists()){
			this.createFile();
			JSONUtils.instance().write(file, logObject);
		}
		
		logObject = JSONUtils.instance().getJSONObject(file);
		read(logs, logObject);
	}
	
	@Override
	public void onCommand(String[] split, String content, MessageReceivedEvent event) {

		guild = event.getGuild();
        author = event.getAuthor();
        channel = event.getChannel();
        receivedMessage = event.getMessage();
        
		String noArgs = content.split(" ")[0];
		switch(noArgs){
		case "set":
			if(split[1].equals("enable")){
				if(guild.getMember(author).hasPermission(Permission.ADMINISTRATOR))
				{
					TextChannel logChannel = guild.getTextChannelsByName(split[2], true).get(0);
					logObject.put(guild.getId(), logChannel.getName());
					JSONUtils.instance().write(file, logObject);
					read(logs, logObject);
					channel.sendMessage("Log channel set to " + "#" + logChannel.getName()).queue();
				}
			}
			else if(split[1].equals("disable")){
				if(guild.getMember(author).hasPermission(Permission.ADMINISTRATOR))
				{
					logObject.remove(guild.getId());
					JSONUtils.instance().write(file, logObject);
					read(logs, logObject);
					channel.sendMessage("Removed server from logging.").queue();
				}
			}		
		}
	}
	
	private void read(HashMap<String, String> hash, JSONObject data){
		try{
			Iterator<?> datakeys = data.keys();

	        while(datakeys.hasNext()){
	            String key = (String)datakeys.next();
	            String value = data.getString(key); 
	            hash.put(key, value);
	        }
		} catch (Exception e){
			System.out.println("Error reading for HashMap.");
			e.printStackTrace();
		}
	}
	
	private void createFile()
	{
		if(!file.exists()){
			file.getParentFile().mkdirs();
			try{
				file.createNewFile();
			}
			catch(Exception e){}
		}
	}
		
	public static String getLogChannelForServer(String serverid){
		return logs.get(serverid);
	}
	
	public static HashMap<String, String> getLogHash(){
		return logs;
	}
}
