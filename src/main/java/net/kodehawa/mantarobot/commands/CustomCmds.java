package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.audio.MantaroAudioManager;
import net.kodehawa.mantarobot.core.CommandProcessor.Arguments;
import net.kodehawa.mantarobot.data.Data.GuildData;
import net.kodehawa.mantarobot.data.DataManager;
import net.kodehawa.mantarobot.modules.*;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static net.kodehawa.mantarobot.commands.custom.Mapifier.dynamicResolve;
import static net.kodehawa.mantarobot.commands.custom.Mapifier.map;

public class CustomCmds extends Module {
	private static Logger LOGGER = LoggerFactory.getLogger("UserCommands");

	private Command customCommand = new Command() {

		private Random r = new Random();

		@Override
		public CommandType commandType() {
			return CommandType.USER;
		}

		private void handle(GuildMessageReceivedEvent event, String cmdName, String rawArgs, String[] args) {
			Map<String, GuildData> guilds = DataManager.getData().get().guilds;
			if (!guilds.containsKey(event.getGuild().getId()) || !guilds.get(event.getGuild().getId()).customCommands.containsKey(cmdName))
				return;
			List<String> responses = guilds.get(event.getGuild().getId()).customCommands.get(cmdName);
			String response = responses.get(r.nextInt(responses.size()));

			Map<String, String> dynamicMap = new HashMap<>();
			map("event", dynamicMap, event);
			for (int i = 0; i < args.length; i++) dynamicMap.put("event.args" + i, args[i]);
			response = dynamicResolve(response, dynamicMap);

			if (response.startsWith("play:")) {
				String toSend = response.substring(5);
				try {
					new URL(toSend);
				} catch (Exception e) {
					toSend = "ytsearch: " + toSend;
				}

				MantaroAudioManager.loadAndPlay(event, toSend);
				return;
			}

			if (response.startsWith("embed:")) {
				event.getChannel().sendMessage(new EmbedBuilder().setDescription(response.substring(6)).setTitle(cmdName).setColor(event.getMember().getColor()).build()).queue();
				return;
			}

			if (response.startsWith("imgembed:")) {
				event.getChannel().sendMessage(new EmbedBuilder().setImage(response.substring(9)).setTitle(cmdName).setColor(event.getMember().getColor()).build()).queue();
				return;
			}

			event.getChannel().sendMessage(response).queue();
		}

		@Override
		public MessageEmbed help(GuildMessageReceivedEvent event) {
			return null;
		}

		@Override
		public void invoke(Arguments args) {
			handle(args.event, args.cmdName, args.rawCommand, args.args);
		}

		@Override
		public boolean isHiddenFromHelp() {
			return true;
		}

	};

	public CustomCmds() {
		super(Category.CUSTOM);

		super.register("custom", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				if (args.length < 1) {
					onHelp(event);
					return;
				}

				String action = args[0];

				if (action.equals("list") || action.equals("ls")) {

					return;
				}

				if (action.equals("debug")) {

					return;
				}

				if (args.length < 2) {
					onHelp(event);
					return;
				}

				String cmd = args[1];

				if (action.equals("add") || action.equals("make")) {

					return;
				}
				if (action.equals("remove") || action.equals("rm")) {

					return;
				}
				if (action.equals("clear")) {

					return;
				}

				//TODO ADD a HELP
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return null;
			}
		});
	}

	@Override
	public void onPostLoad() {
		Set<String> invalidCmds = new HashSet<>();
		DataManager.getData().get().guilds.values().forEach(guildData -> guildData.customCommands.keySet().forEach(cmd -> {
			if (!Manager.commands.containsKey(cmd)) Manager.commands.put(cmd, Pair.of(customCommand, null));
			else invalidCmds.add(cmd);
		}));
		DataManager.getData().get().guilds.values().forEach(d -> d.customCommands.keySet().removeAll(invalidCmds));
		try {
			DataManager.getData().update();
		} catch (IOException e) {
			LOGGER.error("Error happened while dealing with Data saving.", e);
		}
	}
}
