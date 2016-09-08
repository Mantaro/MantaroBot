package net.kodehawa.discord.Mantaro.commands;


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
		evt.getChannel().sendMessage("Bot made by Kodehawa. Latest build is: " + MantaroBot.getInstance().getBuild() + " " +  "With date: " + MantaroBot.getInstance().getBuildDate()+ ", running on JDA.");
    }

	@Override
	public void actionResult(boolean result, MessageReceivedEvent evt) {
		System.out.println("Command executed " + this.getClass().getName() + ", in channel " + evt.getChannel().toString() + " (" + evt.getMessage().toString() + " )");
	}

}
