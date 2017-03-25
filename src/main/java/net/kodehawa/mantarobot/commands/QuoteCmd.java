package net.kodehawa.mantarobot.commands;

import com.google.gson.Gson;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class QuoteCmd extends Module {
	private static final Logger LOGGER = LoggerFactory.getLogger("QuoteCmd");

	private static String toJson(Map<String, LinkedHashMap<String, List<String>>> map) {
		return new Gson().toJson(map);
	}

	private final Random rand = new Random();

	public QuoteCmd() {
		super(Category.UTILS);
		quote();
	}

	@SuppressWarnings({"unused", "unchecked"})
	private void quote() {
		super.register("quote", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				//TODO rewrite this shit. Old code https://hastebin.com/afazuhunuz.cs
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Quote command")
					.setDescription("> Usage:\n"
						+ "~>quote add <number>: Adds a quote with content defined by the number. For example 1 will quote the last message.\n"
						+ "~>quote random: Gets a random quote. \n"
						+ "~>quote read <number>: Gets a quote matching the number. \n"
						+ "~>quote addfrom <phrase>: Adds a quote based in text search criteria.\n"
						+ "~>quote removefrom <phrase>: Removes a quote based in text search criteria.\n"
						+ "~>quote getfrom <phrase>: Searches for the first quote which matches your search criteria and prints it.\n"
						+ "> Parameters:\n"
						+ "number: Message number to quote. For example 1 will quote the last message.\n"
						+ "phrase: A part of the quote phrase.")
					.setColor(Color.DARK_GRAY)
					.build();
			}
		});
	}
}
