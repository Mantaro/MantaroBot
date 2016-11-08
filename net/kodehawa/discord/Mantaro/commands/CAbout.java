package net.kodehawa.discord.Mantaro.commands;


import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.bot.MantaroBot;
import net.kodehawa.discord.Mantaro.main.Command;

public class CAbout implements Command {

	@Override
	@ModuleProperties(level = "user", name = "about", type = "common", description = "Tells info about the bot.")
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		long millis = ManagementFactory.getRuntimeMXBean().getUptime();
		String uptime = String.format("%02d hrs, %02d min, %02d sec",TimeUnit.MILLISECONDS.toHours(millis), TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.MILLISECONDS.toHours(millis), TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
		String mention = evt.getAuthor().getAsMention();
		evt.getChannel().sendMessage("Hello" + mention + " You're talking to MantaroBot! \r" + "**Latest build is:** " + MantaroBot.getInstance().getBuild() + ", " +  "**With date:** " + MantaroBot.getInstance().getBuildDate()+ ", running on JDA. \r**The uptime is:** " + uptime + "\r" + 
		"I'm currently serving " + MantaroBot.getInstance().getSelf().getGuilds().size() + " servers, " + "having fun with " + MantaroBot.getInstance().getSelf().getUsers().size() + " people " +
		"and looking over " + MantaroBot.getInstance().getSelf().getTextChannels().size() + " channels.\r"
		+ "My GitHub page can be found at: https://github.com/Kodehawa/MantaroBot/" + ".");
    }
}
