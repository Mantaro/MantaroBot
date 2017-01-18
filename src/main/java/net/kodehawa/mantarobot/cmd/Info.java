package net.kodehawa.mantarobot.cmd;

import java.awt.Color;
import java.lang.management.ManagementFactory;
import java.net.URLEncoder;
import java.time.format.DateTimeFormatter;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.module.Callback;
import net.kodehawa.mantarobot.module.Category;
import net.kodehawa.mantarobot.module.CommandType;
import net.kodehawa.mantarobot.module.Module;
import net.kodehawa.mantarobot.util.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Information module.
 * @author Yomura
 *
 */
public class Info extends Module {
	
	public Info()
	{
		super.setCategory(Category.INFO);
		this.registerCommands();
	}

	@Override
	public void registerCommands(){
		super.register("ping", "Pong.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				channel = event.getChannel();

				event.getTextChannel().sendMessage(":mega: Pinging...").queue(sentMessage ->
				{
					long start = System.currentTimeMillis();
					try {
						channel.sendTyping().complete();
					} catch (Exception e) {
						e.printStackTrace();
					}
					long end = System.currentTimeMillis() - start;
					sentMessage.editMessage(":mega: The ping is " + end + " ms, " + ratePing(end)).queue();
				});
			}

			@Override
			public String help() {
				return "Just to see if the bot it's alive. Also reports the time it takes for the response to process and bounce back.";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("serverinfo", "Retrieves guild/server information.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				guild = event.getGuild();
				channel = event.getChannel();
				EmbedBuilder embed = new EmbedBuilder();
				StringBuilder sb = new StringBuilder();
				int i = 0;
				for(Role tc : guild.getRoles()) {
					i++;
					if (i <= 79) {
						if (!tc.getName().contains("everyone") && i < guild.getRoles().size() - 1) {
							sb.append(tc.getName()).append(", ");
						} else if (i == guild.getRoles().size() - 1 || i == 79) {
							sb.append(tc.getName()).append(".");
							break;
						}
					}
				}
				int online = 0;
				for(Member u : guild.getMembers()){
					if(!u.getOnlineStatus().equals(OnlineStatus.OFFLINE)){
						online++;
					}
				}
				embed.setColor(guild.getOwner().getColor())
						.setAuthor("Guild Information", null, guild.getIconUrl())
						.setColor(Color.orange)
						.setDescription("Guild information for server " + guild.getName())
						.setThumbnail(guild.getIconUrl())
						.addField("Users (Online/Unique)", online + "/" + guild.getMembers().size(), true)
						.addField("Main Channel", "#" + guild.getPublicChannel().getName(), true)
						.addField("Creation Date", guild.getCreationTime().format(DateTimeFormatter.ISO_DATE_TIME).replaceAll("[^0-9.:-]", " "), true)
						.addField("Voice/Text Channels", guild.getVoiceChannels().size() + "/" + guild.getTextChannels().size() , true)
						.addField("Owner", guild.getOwner().getUser().getName() + "#" + guild.getOwner().getUser().getDiscriminator(), true)
						.addField("Region", guild.getRegion().getName(), true)
						.addField("Roles ("+guild.getRoles().size() + ")", sb.toString(), false)
						.setFooter("Server ID: " + String.valueOf(guild.getId()), null);
				channel.sendMessage(embed.build()).queue();
			}

			@Override
			public String help() {
				return "Retrieves guild/server information. No need to use arguments.";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("usageinfo", "Displays bot hardware information.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				guild = event.getGuild();
				author = event.getAuthor();
				channel = event.getChannel();
				receivedMessage = event.getMessage();
				long totalMemory = Runtime.getRuntime().totalMemory()/(1024)/1024;
				long freeMemory = Runtime.getRuntime().freeMemory()/(1024)/1024;
				long maxMemory = Runtime.getRuntime().maxMemory()/(1024)/1024;
				int avaliableProcessors = Runtime.getRuntime().availableProcessors();
				int cpuUsage = Utils.pm.getCpuUsage().intValue();
				EmbedBuilder embed = new EmbedBuilder();
				embed.setAuthor("MantaroBot information", null, "https://puu.sh/sMsVC/576856f52b.png")
						.setDescription("Hardware and usage information.")
						.setThumbnail("https://puu.sh/suxQf/e7625cd3cd.png")
						.addField("Threads", ManagementFactory.getThreadMXBean().getThreadCount()+"T", true)
						.addField("Memory Usage", totalMemory - freeMemory  + "MB/" + maxMemory +"MB", true)
						.addField("CPU Cores", String.valueOf(avaliableProcessors), true)
						.addField("CPU Usage", cpuUsage + "%", true)
						.addField("Assigned Memory", totalMemory  + "MB", true)
						.addField("Remaining from assigned", freeMemory  + "MB", true);
				channel.sendMessage(embed.build()).queue();
			}

			@Override
			public String help() {
				return "Gives extended information about the bot hardware usage.";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("userinfo", "Retrieves user information.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				EmbedBuilder embed = new EmbedBuilder();
				guild = event.getGuild();
				channel = event.getChannel();
				receivedMessage = event.getMessage();
				author = event.getAuthor();
				if(!content.isEmpty())
				{
					User user1 = null;
					//Which user to get the info for?
					if(receivedMessage.getMentionedUsers() != null){
						user1 = receivedMessage.getMentionedUsers().get(0);
					}
					//Member gives way, way more info than User.
					Member member1 = guild.getMember(user1);

					if(user1 != null && member1 != null){
						//This is all done using embeds. It looks nicer and cleaner.
						embed.setColor(member1.getColor());
						embed.setAuthor("User info for " + user1.getName() + "#" + user1.getDiscriminator(), null, author.getEffectiveAvatarUrl());
						embed.setThumbnail(user1.getAvatarUrl())
								//Only get the date from the Join Date. Also replace that random Z because I'm not using time.
								.addField("Join Date", member1.getJoinDate().format(DateTimeFormatter.ISO_DATE_TIME).replaceAll("[^0-9.:-]", " "), false)
								.addField("Account Created", author.getCreationTime().format(DateTimeFormatter.ISO_DATE_TIME).replaceAll("[^0-9.:-]", " "), false);
						if(member1.getVoiceState().getChannel() != null){
							embed.addField("Voice channel: ", member1.getVoiceState().getChannel().getName(), false);
						}
						if(guild.getMember(user1).getGame() != null){
							embed.addField("Playing: ", guild.getMember(user1).getGame().getName(), false);
						}
						//Getting the hex value of the RGB color assuming no alpha that is >16 in value is required.
						if(member1.getColor() != null){
							embed.addField("Color", "#"+Integer.toHexString(member1.getColor().getRGB()).substring(2).toUpperCase(), true);
						}
						embed.addField("Status", guild.getMember(author).getOnlineStatus().getKey().toLowerCase(), true);
						StringBuilder sb = new StringBuilder();
						int i = -1;
						for(Role roles : member1.getRoles()){
							i++;
							if(i < member1.getRoles().size() - 1)
								sb.append(roles.getName()).append(", ");
							if(i == member1.getRoles().size() - 1)
								sb.append(roles.getName()).append(".");
						}
						embed.addField("Roles [" + String.valueOf(member1.getRoles().size()) + "]", sb.toString(), true);
						embed.setFooter("User ID: " + user1.getId(), null);
						channel.sendMessage(embed.build()).queue();
					}

				}
				else {
					//If the author wants to get self info.
					User user1 = author;
					//From author id, get the Member, so I can fetch the info.
					Member member1 = guild.getMemberById(author.getId());

					//This is all done using embeds. It looks nicer and cleaner.
					embed.setColor(member1.getColor());
					embed.setAuthor("Self user info for " + user1.getName() + "#" + user1.getDiscriminator(), null, author.getEffectiveAvatarUrl());
					embed.setThumbnail(user1.getAvatarUrl())
							//Only get the date from the Join Date. Also replace that random Z because I'm not using time.
							.addField("Join Date", member1.getJoinDate().format(DateTimeFormatter.ISO_DATE_TIME).replaceAll("[^0-9.:-]", " "), false)
							.addField("Account Created", author.getCreationTime().format(DateTimeFormatter.ISO_DATE_TIME).replaceAll("[^0-9.:-]", " "), false);
					if(member1.getVoiceState().getChannel() != null){
						embed.addField("Voice channel: ", member1.getVoiceState().getChannel().getName(), false);
					}
					if(guild.getMember(user1).getGame() != null){
						embed.addField("Playing: ", guild.getMember(user1).getGame().getName(), false);
					}
					//Getting the hex value of the RGB color assuming no alpha that is >16 in value is required.
					if(!String.valueOf(member1.getColor().getRGB()).isEmpty()){
						embed.addField("Color", "#"+Integer.toHexString(member1.getColor().getRGB()).substring(2).toUpperCase(), true);
					}
					embed.addField("Status", member1.getOnlineStatus().getKey().toLowerCase(), true);
					StringBuilder sb = new StringBuilder();
					int i = -1;
					for(Role roles : member1.getRoles()){
						i++;
						if(i < member1.getRoles().size() - 1)
							sb.append(roles.getName()).append(", ");
						if(i == member1.getRoles().size() - 1)
							sb.append(roles.getName()).append(".");
					}
					embed.addField("Roles [" + String.valueOf(member1.getRoles().size()) + "]", sb.toString(), true);
					embed.setFooter("User ID: " + user1.getId(), null);
					channel.sendMessage(embed.build()).queue();
				}
			}

			@Override
			public String help() {
				return "Retrieves user information."
						+ "Usage: \n"
						+ "~>info user [@user]: Retrieves the specified user information.\n"
						+ "~>info user: Retrieves self user information.\n";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("weather", "Displays forecast information", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				channel = event.getChannel();
				EmbedBuilder embed = new EmbedBuilder();

				if(!content.isEmpty()){
					try {
						long start = System.currentTimeMillis();
						//Get a parsed JSON.
						String APP_ID = Mantaro.instance().getConfig().values().get("weatherappid").toString();
						JSONObject jObject = new JSONObject(
								Utils.instance().getObjectFromUrl("http://api.openweathermap.org/data/2.5/weather?q=" + URLEncoder.encode(
										content, "UTF-8") + "&appid="+ APP_ID, event));
						//Get the object as a array.
						JSONArray data = jObject.getJSONArray("weather");
						String status = null;
						for(int i = 0; i < data.length(); i++) {
							JSONObject entry = data.getJSONObject(i);
							status = entry.getString("main"); //Used for weather status.
						}

						JSONObject jMain = jObject.getJSONObject("main"); //Used for temperature and humidity.
						JSONObject jWind = jObject.getJSONObject("wind"); //Used for wind speed.
						JSONObject jClouds = jObject.getJSONObject("clouds"); //Used for cloudiness.
						JSONObject jsys = jObject.getJSONObject("sys"); //Used for countrycode.

						long end = System.currentTimeMillis() - start;

						String countryCode = jsys.getString("country").toLowerCase();

						Double temp = (double)jMain.get("temp"); //Temperature in Kelvin.
						int pressure = (int) jMain.get("pressure"); //Pressure in kPA.
						int hum = (int) jMain.get("humidity"); //Humidity in percentage.
						Double ws = (double) jWind.get("speed"); //Speed in m/h.
						int clness = (int) jClouds.get("all"); //Cloudiness in percentage.

						//Simple math formulas to convert from universal to metric and imperial.
						Double finalTemperatureCelcius = temp - 273.15; //Temperature in Celcius degrees.
						Double finalTemperatureFarnheit = temp * 9/5 - 459.67; //Temperature in Farnheit degrees.
						Double finalWindSpeedMetric = ws * 3.6; //wind speed in km/h.
						Double finalWindSpeedImperial = ws / 0.447046; //wind speed in mph.

						embed.setColor(Color.CYAN)
								.setTitle(":flag_" + countryCode + ":" + " Forecast information for " + content) //For which city
								.setDescription(":fallen_leaf: " + status + " (" + clness + "% cloudiness)") //Clouds, sunny, etc and cloudiness.
								.addField(":thermometer: Temperature", finalTemperatureCelcius.intValue() + "°C/" + finalTemperatureFarnheit.intValue() + "°F", true)
								.addField(":droplet: Humidity", hum + "%" , true)
								.addBlankField(true)
								.addField(":wind_blowing_face: Wind Speed", finalWindSpeedMetric.intValue() + "kmh / " + finalWindSpeedImperial.intValue() + "mph" , true)
								.addField(":chart_with_downwards_trend: Pressure", pressure + "kPA" , true)
								.addBlankField(true)
								.setFooter("Information provided by OpenWeatherMap (Process time: " + end + "ms)", null);
						//Build the embed message and send it.
						channel.sendMessage(embed.build()).queue();
					}
					catch(Exception e){
						e.printStackTrace();
					}
				} else {
					channel.sendMessage(help()).queue();
				}
			}

			@Override
			public String help() {
				return  "This command retrieves information from OpenWeatherMap. Used to check **forecast information.**\n"
						+ "> Usage:\n"
						+ "~>weather [city],[countrycode]: Retrieves the forecast information for such location.\n"
						+ "> Parameters:\n"
						+ "[city]: Your city name, for example New York\n"
						+ "[countrycode]: (OPTIONAL) The code for your country, for example US (USA) or MX (Mexico).";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});
	}

	private static String ratePing(long ping) {
		if (ping <= 0) return "which doesn't even make any sense at all.";
		if (ping <= 10) return "which is faster than Sonic.";
		if (ping <= 100) return "which is great!";
		if (ping <= 200) return "which is nice!";
		if (ping <= 300) return "which is decent";
		if (ping <= 400) return "which is average...";
		if (ping <= 500) return "which is not that bad.";
		if (ping <= 600) return "which is kinda slow..";
		if (ping <= 700) return "which is slow..";
		if (ping <= 800) return "which is too slow.";
		if (ping <= 800) return "which is awful.";
		if (ping <= 900) return "which is bad.";
		if (ping <= 1600) return "which is because discord is lagging";
		if (ping <= 10000) return "which makes less sense than 0 ping";
		return "which is slow af";
	}
}