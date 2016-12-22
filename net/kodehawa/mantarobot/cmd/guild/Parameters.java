package net.kodehawa.mantarobot.cmd.guild;

import java.io.File;
import java.util.HashMap;

import net.kodehawa.mantarobot.module.Callback;
import net.kodehawa.mantarobot.module.CommandType;
import net.kodehawa.mantarobot.module.Module;
import net.kodehawa.mantarobot.util.HashMapUtils;
import org.json.JSONObject;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.util.JSONUtils;

public class Parameters extends Module {

	private static HashMap<String, String> prefixes = new HashMap<>();
	private static HashMap<String, String> logs = new HashMap<>();
	private static HashMap<String, String> nsfw = new HashMap<>();
	private static HashMap<String, String> bd_data = new HashMap<>();
	private String FILE_SIGN = "d41d8cd98f00b204e9800998ecf8427e";

	private JSONObject logObject = new JSONObject();
	private JSONObject prefixObject = new JSONObject();
	private JSONObject nsfwObject = new JSONObject();
	private File logFile;
	private File prefixFile;
	private File nsfwFile;

	public Parameters(){
		this.registerCommands();
		logObject.put("version", "1.0");
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
		new HashMapUtils("mantaro", "bd_data", bd_data, FILE_SIGN, false);
	}
	
	@Override
	public void registerCommands(){
		super.register("params", "Defines bot parameters for the server", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {

				guild = event.getGuild();
				author = event.getAuthor();
				channel = event.getChannel();
				receivedMessage = event.getMessage();
				String noArgs = "";
				String mainArgs = "";
				try{ noArgs = args[0]; } catch (Exception ignored){}
				try{ mainArgs = args[1]; } catch (Exception ignored){}
				switch(noArgs) {
					case "logs":
						switch (mainArgs) {
							case "set":
								if (guild.getMember(author).hasPermission(Permission.ADMINISTRATOR)) {
									TextChannel logChannel = guild.getTextChannelsByName(args[2], true).get(0);
									logObject.put(guild.getId(), logChannel.getName());
									JSONUtils.instance().write(logFile, logObject);
									JSONUtils.instance().read(logs, logObject);
									channel.sendMessage(":mega: Log channel set to " + "#" + logChannel.getName()).queue();
								} else {
									channel.sendMessage(":heavy_multiplication_x: You have no permissions to do this.");
								}
								break;
							case "disable":
								if (guild.getMember(author).hasPermission(Permission.ADMINISTRATOR)) {
									logObject.remove(guild.getId());
									JSONUtils.instance().write(logFile, logObject);
									JSONUtils.instance().read(logs, logObject);
									channel.sendMessage(":mega: Removed server from logging.").queue();
								} else {
									channel.sendMessage(":heavy_multiplication_x: You have no permissions to do this.");
								}
								break;
						}
						break;
					case "prefix":
						switch (mainArgs) {
							case "set":
								if (guild.getMember(author).isOwner()) {
									prefixObject.put(guild.getId(), args[2]);
									JSONUtils.instance().write(prefixFile, prefixObject);
									JSONUtils.instance().read(prefixes, prefixObject);
									channel.sendMessage(":mega: Channel bot prefix set to " + args[2]).queue();
									break;
								}
							case "remove":
								if (guild.getMember(author).isOwner()) {
									prefixObject.remove(guild.getId());
									JSONUtils.instance().write(prefixFile, prefixObject);
									JSONUtils.instance().read(prefixes, prefixObject);
									channel.sendMessage(":mega: Channel bot prefix defaulted to ~>").queue();
									break;
								}
							default:
								channel.sendMessage(help()).queue();
								break;
						}
						break;
					case "nsfw":
						switch (mainArgs) {
							case "set":
								System.out.println("hi");
								if (guild.getMember(author).hasPermission(Permission.ADMINISTRATOR)) {
									nsfwObject.put(guild.getId(), args[2]);
									JSONUtils.instance().write(nsfwFile, nsfwObject);
									JSONUtils.instance().read(nsfw, nsfwObject);
									channel.sendMessage(":mega: NSFW channel set to #" + args[2]).queue();
								}
								break;
							case "remove":
								if (guild.getMember(author).hasPermission(Permission.ADMINISTRATOR)) {
									nsfwObject.remove(guild.getId());
									JSONUtils.instance().write(nsfwFile, nsfwObject);
									JSONUtils.instance().read(nsfw, nsfwObject);
									channel.sendMessage(":mega: NSFW channel removed").queue();
									break;
								}
							default:
								channel.sendMessage(help()).queue();
								break;
						}
					case "birthday":
						switch(mainArgs) {
							case "set":
								TextChannel birthdayChannel = guild.getTextChannelsByName(args[2], true).get(0);
								String birthdayRoleName = args[3];
								bd_data.put(guild.getId(), birthdayChannel.getId() + ":" + birthdayRoleName);
								new HashMapUtils("mantaro", "bd_data", bd_data, FILE_SIGN, true);
								channel.sendMessage(":mega: Birthday channel set to #" + birthdayChannel.getName()
										+ " with role " + birthdayRoleName + ".").queue();
								break;
							case "disable":
								bd_data.remove(guild.getId());
								new HashMapUtils("mantaro", "bd_data", bd_data, FILE_SIGN, true);
								channel.sendMessage(":mega: Removed birthday monitoring.").queue();
								break;
							default:
								channel.sendMessage(help()).queue();
								break;
						}
				}
			}

			@Override
			public String help() {
				return "This command sets specific parameters in your server.\n"
						+ "**Parameters:**\n"
						+ "~>params logs set enable [channel]\n"
						+ "~>params logs set disable\n"
						+ "~>params prefix set [prefix]\n"
						+ "~>params prefix disable\n"
						+ "~>params nsfw set [channel]\n"
						+ "~>params nsfw disable\n"
						+ "~>params birthday set [channel]\n"
						+ "~>params birthday disable\n"
						+ "**Parameter explanation:**\n"
						+ "[channel]: The channel name to action in."
						+ "[prefix]: The prefix to set.";
			}

			@Override
			public CommandType commandType() {
				return CommandType.ADMIN;
			}
		});
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

	public static String getBirthdayChannelForServer(String guildId){
		return bd_data.get(guildId).split(":")[0];
	}

	public static String getBirthdayRoleForServer(String guildId){
		return bd_data.get(guildId).split(":")[1];
	}
	public static String getLogChannelForServer(String serverid){
		return logs.get(serverid);
	}
	
	public static HashMap<String, String> getLogHash(){
		return logs;
	}

	public static HashMap<String, String> getBirthdayHash(){
		return bd_data;
	}
}