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
			.setUrl("https://github.com/Kodehawa/MantaroBot/")
			.setTitle("About Mantaro")
			.setThumbnail("https://puu.sh/suxQf/e7625cd3cd.png")
			.addField("Latest Build: ", Mantaro.instance().getMetadata("build") + Mantaro.instance().getMetadata("date") + "_J" + JDAInfo.VERSION, false)
			.addField("Uptime", uptime, false)
			.addField("Playing with", Mantaro.instance().getSelf().getUsers().size() + " users.", true)
			.addField("Looking over at", Mantaro.instance().getSelf().getTextChannels().size() + " channels.", true)
			.setFooter("Invite link: http://goo.gl/ei1C5j", null);
		
		channel.sendMessage(embed.build()).queue();;
	}

}
