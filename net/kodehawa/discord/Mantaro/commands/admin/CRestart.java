package net.kodehawa.discord.Mantaro.commands.admin;

import java.io.File;
import java.util.ArrayList;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.bot.MantaroBot;
import net.kodehawa.discord.Mantaro.main.Command;

public class CRestart implements Command{

	
	@Override
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	@ModuleProperties(level = "master", name = "restart", type = "control", description = "Restarts the bot.")
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		if(evt.getAuthor().getId().equals("155867458203287552") || evt.getAuthor().getId().equals("155035543984537600"))
		{
			evt.getChannel().sendMessageAsync("Restarting..", null);
			restartApplication();
		}
		else
		{
			evt.getChannel().sendMessage("You cannot do that, silly.");
		}
	}

	public void restartApplication()
	{
		try
		{
			final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
			final File currentJar = new File(MantaroBot.class.getProtectionDomain().getCodeSource().getLocation().toURI());

			if(!currentJar.getName().endsWith(".jar"))
			return;

			final ArrayList<String> command = new ArrayList<String>();
			command.add(javaBin);
			command.add("-jar");
			command.add(currentJar.getPath());

			final ProcessBuilder builder = new ProcessBuilder(command);
			builder.start();
			System.exit(0);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
