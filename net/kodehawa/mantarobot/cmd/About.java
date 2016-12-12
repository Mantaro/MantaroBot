package net.kodehawa.mantarobot.cmd;

import java.awt.Color;
import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.core.Mantaro;

public class About extends Command {

	public About()
	{
		setName("about");
		setDescription("Displays information about the bot.");
		setCommandType("user");
	}
	
	@Override
	public void onCommand(String[] message, String content, MessageReceivedEvent event)
	{
        channel = event.getChannel();
        author = event.getAuthor();
        
		long millis = ManagementFactory.getRuntimeMXBean().getUptime();
		String uptime = String.format("%02d hrs, %02d min, %02d sec",TimeUnit.MILLISECONDS.toHours(millis), TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toHours(millis)), TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
		EmbedBuilder embed = new EmbedBuilder();
		channel.sendTyping().queue();
		embed.setColor(Color.PINK)
			.setAuthor("About Mantaro", "https://github.com/Kodehawa/MantaroBot/", "https://puu.sh/suxQf/e7625cd3cd.png")
			.setThumbnail("https://puu.sh/suxQf/e7625cd3cd.png")
			.setDescription("This is **MantaroBot** and I'm here to make your life a little easier. Remember to get commands from `~>help`\n"
					+ "Some of my features include:\n \u2713 Moderation made easy (Mass kick/ban, prune commands, logs, nsfw and more!)\n"
					+ "\u2713 Funny and useful commands see `~>help anime` or `~>help action` for examples.	\n"
					+ "\u2713 Extensive support,!")
			.addField("Latest Build", Mantaro.instance().getMetadata("build") + '.' + Mantaro.instance().getMetadata("date"), true)
			.addField("JDA Version", JDAInfo.VERSION, true)
			.addField("Uptime", uptime, true)
			.addField("Threads", String.valueOf(Thread.activeCount()), true)
			.addField("Guilds", String.valueOf(Mantaro.instance().getSelf().getGuilds().size()), true)
			.addField("Users (Online/Unique)", Mantaro.instance().getSelf().getUsers().size() + "/" + "", true)
			.addField("Channels", String.valueOf(Mantaro.instance().getSelf().getTextChannels().size()), true)
			.addField("Voice Channels", String.valueOf(Mantaro.instance().getSelf().getVoiceChannels().size()), true)
			.setFooter("Invite link: http://goo.gl/ei1C5j", null);
		
		channel.sendMessage(embed.build()).queue();
    }

}
