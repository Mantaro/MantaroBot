package net.kodehawa.mantarobot.cmd;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.listeners.Listener;
import net.kodehawa.mantarobot.listeners.LogListener;
import net.kodehawa.mantarobot.module.Callback;
import net.kodehawa.mantarobot.module.Category;
import net.kodehawa.mantarobot.module.CommandType;
import net.kodehawa.mantarobot.module.Module;

import java.awt.*;
import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class Random extends Module {

	public Random(){
		super.setCategory(Category.FUN);
		this.registerCommands();
	}
	
	@Override
	public void registerCommands(){
		super.register("lewd", "T-Too lewd!", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				channel = event.getChannel();
				channel.sendMessage("Y-You lewdie!" + "\r\n"
						+ "http://puu.sh/rzVEe/c8272e7c84.png").queue();
			}

			@Override
			public String help() {
				return "Returns a image of a girl holding a paper that says lewd";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("time", "Retrieves time in a specific timezone.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				channel = event.getChannel();

				try{
					if(content.startsWith("GMT")){
						channel.sendMessage(":mega: The time is: " + dateGMT(content) + " in " + content).queue();
					} else{
						channel.sendMessage(":heavy_multiplication_x: " + "You didn't specify a valid timezone").queue();
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public String help() {
				return "Retrieves time in a specific timezone.\n"
						+"~>time [timezone]. Retrieves the time in the specified timezone.\n"
						+"**Parameter specification**\n"
						+ "[timezone] A valid timezone between GMT-12 and GMT+14";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});
		super.register("about", "Displays information about the bot.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				channel = event.getChannel();
				author = event.getAuthor();

				int online = 0;
				for(Guild g : Mantaro.instance().getSelf().getGuilds()){
					for(Member u : g.getMembers()){
						if(!u.getOnlineStatus().equals(OnlineStatus.OFFLINE)){
							online++;
						}
					}
				}

				long millis = ManagementFactory.getRuntimeMXBean().getUptime();
				String uptime = String.format("%02d hrs, %02d min, %02d sec", TimeUnit.MILLISECONDS.toHours(millis), TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toHours(millis)), TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
				EmbedBuilder embed = new EmbedBuilder();
				channel.sendTyping().queue();
				embed.setColor(Color.PINK)
						.setAuthor("About Mantaro", "https://github.com/Kodehawa/MantaroBot/", "https://puu.sh/suxQf/e7625cd3cd.png")
						.setThumbnail("https://puu.sh/suxQf/e7625cd3cd.png")
						.setDescription("This is **MantaroBot** and I'm here to make your life a little easier. Remember to get commands from `~>help`\n"
								+ "Some of my features include:\n \u2713 Moderation made easy (Mass kick/ban, prune commands, logs, nsfw and more!)\n"
								+ "\u2713 Funny and useful commands see `~>help anime` or `~>help action` for examples.	\n"
								+ "\u2713 Extensive support!")
						.addField("Latest Build", Mantaro.instance().getMetadata("build") + '.' + Mantaro.instance().getMetadata("date"), true)
						.addField("JDA Version", JDAInfo.VERSION, true)
						.addField("Uptime", uptime, true)
						.addField("Threads", String.valueOf(Thread.activeCount()), true)
						.addField("Guilds", String.valueOf(Mantaro.instance().getSelf().getGuilds().size()), true)
						.addField("Users (Online/Unique)", online + "/" + Mantaro.instance().getSelf().getUsers().size(), true)
						.addField("Channels", String.valueOf(Mantaro.instance().getSelf().getTextChannels().size()), true)
						.addField("Voice Channels", String.valueOf(Mantaro.instance().getSelf().getVoiceChannels().size()), true)
						.setFooter("Invite link: http://goo.gl/ei1C5j (Commands this session: " + Listener.getCommandTotal() + " | Logs this session: " + LogListener.getLogTotal() + ")", null);

				channel.sendMessage(embed.build()).queue();
			}

			@Override
			public String help() {
				return "Displays info about the bot status.";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});
	}

	private String dateGMT(String timezone) throws ParseException, NullPointerException
	{
		SimpleDateFormat dateGMT = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
		dateGMT.setTimeZone(TimeZone.getTimeZone(timezone));
		SimpleDateFormat dateLocal = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
		Date date1 = dateLocal.parse(dateGMT.format(new Date()));
		return DateFormat.getInstance().format(date1);
	}
}