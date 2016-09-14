package net.kodehawa.discord.Mantaro.commands;

import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;

public class CBrainPower implements Command {

	private List<String> lyrics = new ArrayList<String>();
	
	public CBrainPower()
	{
		lyrics.add("Are you ready?");
		lyrics.add("O-oooooooooo AAAAE-A-A-I-A-U-");
		lyrics.add("E-eee-ee-eee AAAAE-A-E-I-E-A-");
		lyrics.add("JO-ooo-oo-oo-oo EEEEO-A-AAA-AAAA");
		lyrics.add("O-oooooooooo AAAAE-A-A-I-A-U-");
		lyrics.add("JO-oooooooooooo AAE-O-A-A-U-U-A-");
		lyrics.add("E-eee-ee-eee AAAAE-A-E-I-E-A-");
		lyrics.add("JO-ooo-oo-oo-oo EEEEO-A-AAA-AAAA");
		lyrics.add("O-oooooooooo AAAAE-A-A-I-A-U-");
		lyrics.add("E-eee-ee-eee AAAAE-A-E-I-E-A-");
		lyrics.add("JO-ooo-oo-oo-oo EEEEO-A-AAA-AAAA-");
	}
	
	@Override
	@ModuleProperties(level = "user", name = "brainpower", type = "common", description = "NOMA - Brain Power lyrics.")
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		StringBuilder finalMessage = new StringBuilder();

		for (String help : lyrics)
		{
			finalMessage.append(help+"\r\n");
		}
		
		evt.getChannel().sendMessage(finalMessage.toString());
	}
}
