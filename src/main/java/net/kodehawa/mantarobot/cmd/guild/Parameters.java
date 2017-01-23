package net.kodehawa.mantarobot.cmd.guild;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.module.Category;
import net.kodehawa.mantarobot.module.Command;
import net.kodehawa.mantarobot.module.CommandType;
import net.kodehawa.mantarobot.module.Module;
import net.kodehawa.mantarobot.util.HashMapUtils;
import net.kodehawa.mantarobot.util.JSONUtils;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;

public class Parameters extends Module {

	private static HashMap<String, String> bd_data = new HashMap<>();
	private static HashMap<String, String> logs = new HashMap<>();
	private static HashMap<String, String> music_data = new HashMap<>();
	private static HashMap<String, String> nsfw = new HashMap<>();
	private static HashMap<String, String> prefixes = new HashMap<>();

	public static String getBirthdayChannelForServer(String guildId) {
		return bd_data.get(guildId).split(":")[0];
	}

	public static HashMap<String, String> getBirthdayHash() {
		return bd_data;
	}

	public static String getBirthdayRoleForServer(String guildId) {
		return bd_data.get(guildId).split(":")[1];
	}

	public static String getLogChannelForServer(String serverId) {
		return logs.get(serverId);
	}

	public static HashMap<String, String> getLogHash() {
		return logs;
	}

	public static String getMusicVChannelForServer(String guildId) {
		if (music_data.get(guildId) == null) {
			return "";
		}
		return music_data.get(guildId);
	}

	public static String getNSFWChannelForServer(String guildId) {
		return nsfw.get(guildId);
	}

	public static String getPrefixForServer(String guildId) {
		if (prefixes.get(guildId) == null) {
			return prefixes.get("default");
		}
		return prefixes.get(guildId);
	}

	private String FILE_SIGN = "d41d8cd98f00b204e9800998ecf8427e";
	private File logFile;
	private JSONObject logObject = new JSONObject();
	private File nsfwFile;
	private JSONObject nsfwObject = new JSONObject();
	private File prefixFile;
	private JSONObject prefixObject = new JSONObject();

	public Parameters() {
		super.setCategory(Category.MODERATION);
		this.registerCommands();
		logObject.put("version", "1.0");
		if (Mantaro.instance().isWindows()) {
			this.logFile = new File("C:/mantaro/config/logconf.json");
		} else if (Mantaro.instance().isUnix()) {
			this.logFile = new File("/home/mantaro/config/logconf.json");
		}
		if (!logFile.exists()) {
			JSONUtils.instance().createFile(logFile);
			JSONUtils.instance().write(logFile, logObject);
		}

		logObject = JSONUtils.instance().getJSONObject(logFile);
		JSONUtils.instance().read(logs, logObject);
		prefixObject.put("default", "~>");
		if (Mantaro.instance().isWindows()) {
			this.prefixFile = new File("C:/mantaro/config/prefix.json");
		} else if (Mantaro.instance().isUnix()) {
			this.prefixFile = new File("/home/mantaro/config/prefix.json");
		}
		if (!prefixFile.exists()) {
			JSONUtils.instance().createFile(prefixFile);
			JSONUtils.instance().write(prefixFile, prefixObject);
		}

		prefixObject = JSONUtils.instance().getJSONObject(prefixFile);
		JSONUtils.instance().read(prefixes, prefixObject);
		nsfwObject.put("213468583252983809", "nsfw");
		if (Mantaro.instance().isWindows()) {
			this.nsfwFile = new File("C:/mantaro/config/nsfw.json");
		} else if (Mantaro.instance().isUnix()) {
			this.nsfwFile = new File("/home/mantaro/config/nsfw.json");
		}

		if (!nsfwFile.exists()) {
			JSONUtils.instance().createFile(nsfwFile);
			JSONUtils.instance().write(nsfwFile, nsfwObject);
		}

		nsfwObject = JSONUtils.instance().getJSONObject(nsfwFile);
		JSONUtils.instance().read(nsfw, nsfwObject);

		new HashMapUtils("mantaro", "bd_data", bd_data, FILE_SIGN, false);
		new HashMapUtils("mantaro", "music_data", music_data, FILE_SIGN, false);
	}

	@Override
	public void registerCommands() {
		super.register("params", "Defines bot parameters for the server", new Command() {
			@Override
			public CommandType commandType() {
				return CommandType.ADMIN;
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
					+ "~>params birthday enable [channel] [rolename]\n"
					+ "~>params birthday disable\n"
					+ "~>params music [voicechannel]\n"
					+ "**Parameter explanation:**\n"
					+ "[channel]: The channel name to action in.\n"
					+ "[voicechannel]: The voice channel to connect to.\n"
					+ "[rolename]: The name of the role to assign.\n"
					+ "[prefix]: The prefix to set.";
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				Guild guild = event.getGuild();
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				Message receivedMessage = event.getMessage();
				String noArgs = "";
				String mainArgs = "";
				try {
					noArgs = args[0];
				} catch (Exception ignored) {
				}
				try {
					mainArgs = args[1];
				} catch (Exception ignored) {
				}
				switch (noArgs) {
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
									channel.sendMessage(":heavy_multiplication_x: You have no permissions to do this. -> **Missing: ADMINISTRATOR**");
									break;
								}
								break;
							case "disable":
								if (guild.getMember(author).hasPermission(Permission.ADMINISTRATOR)) {
									logObject.remove(guild.getId());
									JSONUtils.instance().write(logFile, logObject);
									JSONUtils.instance().read(logs, logObject);
									channel.sendMessage(":mega: Removed server from logging.").queue();
									break;
								} else {
									channel.sendMessage(":heavy_multiplication_x: You have no permissions to do this. -> **Missing: ADMINISTRATOR**");
									break;
								}
						}
						break;
					case "prefix":
						switch (mainArgs) {
							case "set":
								if (guild.getMember(author).isOwner() || author.getId().equals("155867458203287552")) {
									prefixObject.put(guild.getId(), args[2]);
									JSONUtils.instance().write(prefixFile, prefixObject);
									JSONUtils.instance().read(prefixes, prefixObject);
									channel.sendMessage(":mega: Channel bot prefix set to " + args[2]).queue();
									break;
								} else {
									channel.sendMessage(":heavy_multiplication_x: You have no permissions to do this. -> **You're not the server owner.**").queue();
									break;
								}
							case "remove":
								if (guild.getMember(author).isOwner() || author.getId().equals("155867458203287552")) {
									prefixObject.remove(guild.getId());
									JSONUtils.instance().write(prefixFile, prefixObject);
									JSONUtils.instance().read(prefixes, prefixObject);
									channel.sendMessage(":mega: Channel bot prefix defaulted to ~>").queue();
									break;
								} else {
									channel.sendMessage(":heavy_multiplication_x: You have no permissions to do this -> **You're not the server owner.**").queue();
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
									break;
								} else {
									channel.sendMessage(":heavy_multiplication_x: You have no permissions to do this -> **Missing: ADMINISTRATOR**").queue();
									break;
								}
							case "remove":
								if (guild.getMember(author).hasPermission(Permission.ADMINISTRATOR)) {
									nsfwObject.remove(guild.getId());
									JSONUtils.instance().write(nsfwFile, nsfwObject);
									JSONUtils.instance().read(nsfw, nsfwObject);
									channel.sendMessage(":mega: NSFW channel removed").queue();
									break;
								} else {
									channel.sendMessage(":heavy_multiplication_x: You have no permissions to do this -> **Missing: ADMINISTRATOR**").queue();
									break;
								}
							default:
								channel.sendMessage(help()).queue();
								break;
						}
					case "birthday":
						switch (mainArgs) {
							case "enable":
								if (event.getGuild().getMember(Mantaro.instance().getSelf().getSelfUser())
									.hasPermission(Permission.MANAGE_ROLES)) {
									TextChannel birthdayChannel = guild.getTextChannelsByName(args[2], true).get(0);
									String birthdayRoleName = args[3];
									Role birthdayRole = guild.getRolesByName(args[3], true).get(0);
									bd_data.put(guild.getId(), birthdayChannel.getId() + ":" + birthdayRole.getId());
									new HashMapUtils(
										"mantaro", "bd_data", bd_data, FILE_SIGN, true
									);
									channel.sendMessage(
										":mega: Birthday channel set to **#" + birthdayChannel.getName()
											+ "** with role **" + birthdayRoleName + "**." + " (" + birthdayRole.getId() + ")"
									).queue();
									break;
								} else {
									channel.sendMessage(
										":heavy_muliplication_x: " +
											"``Cannot enable birthday role on this guild because of missing permissions -> Missing: MANAGE_ROLES``)"
									).queue();
									return;
								}
							case "disable":
								bd_data.remove(guild.getId());
								new HashMapUtils(
									"mantaro", "bd_data", bd_data, FILE_SIGN, true
								);

								channel.sendMessage(
									":mega: Removed birthday monitoring."
								).queue();
								break;
							default:
								channel.sendMessage(help()).queue();
								break;
						}
					case "music":
						String musicChannel = content.replace("music ", "");
						if (musicChannel.matches("^([A-Za-z])\\w+") || musicChannel.matches("^(?=.*[a-zA-Z])(?=.*[0-9])\\w+")) {
							music_data.put(event.getGuild().getId(), event.getGuild().getVoiceChannelsByName(musicChannel, true).get(0).getId());
							new HashMapUtils("mantaro", "music_data", music_data, FILE_SIGN, true);
							channel.sendMessage(":mega: Music channel set to (Name): **" + musicChannel + "** -> Guild: " + event.getGuild().getId()).queue();
						} else if (musicChannel.matches("^[0-9]*$")) {
							//Assuming you enter a ID
							music_data.put(event.getGuild().getId(), musicChannel);
							new HashMapUtils("mantaro", "music_data", music_data, FILE_SIGN, true);
							channel.sendMessage(":mega: Music channel set to (ID): **" + musicChannel + "** -> Guild: " + event.getGuild().getId()).queue();
						} else {
							channel.sendMessage(":heavy_multiplication_x: " +
								"Not a valid result? Not complaint with ``([a-zA-Z]) [Example: Hello] / ^(?=.*[a-zA-Z])(?=.*[0-9]) " +
								"[Example: Hello 1/Hello1] / ^[0-9]*$ [Example: 28754478217527]``. Shouldn't happen. " +
								"Maybe try using the channel id if you didn't already?").queue();
						}
				}
			}
		});
	}
}