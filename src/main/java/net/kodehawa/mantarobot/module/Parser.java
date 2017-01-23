package net.kodehawa.mantarobot.module;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.Collections;

public final class Parser {
	public class CommandArguments {
		public final String[] args;
		public final String beheadedMain;
		public final GuildMessageReceivedEvent event;
		public final String invoke;
		public final String rawCommand;
		public final String[] splitBeheaded;
		public String content = "";

		public CommandArguments(String raw, String beheaded, String[] splitBeheaded, String invoke, String[] args, GuildMessageReceivedEvent evt) {
			rawCommand = raw;
			beheadedMain = beheaded;
			this.splitBeheaded = splitBeheaded;
			if (beheadedMain.contains(" ")) {
				content = beheaded.replace(splitBeheaded[0] + " ", "");
			} else {
				content = beheaded.replace(splitBeheaded[0], "");
			}
			this.invoke = invoke;
			this.args = args;
			event = evt;
		}
	}

	public CommandArguments parse(String prefix, String rw, GuildMessageReceivedEvent evt) {
		if (rw.startsWith(prefix)) {
			ArrayList<String> split = new ArrayList<>();
			String beheaded = rw.replaceFirst(prefix, "");
			String[] splitBeheaded = beheaded.split(" ");
			Collections.addAll(split, splitBeheaded);

			String invoke = split.get(0);
			String[] args = new String[split.size() - 1];
			split.subList(1, split.size()).toArray(args);
			return new CommandArguments(rw, beheaded, splitBeheaded, invoke, args, evt);
		}

		return null;
	}
}
