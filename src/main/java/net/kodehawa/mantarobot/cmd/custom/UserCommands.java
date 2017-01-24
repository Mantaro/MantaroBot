package net.kodehawa.mantarobot.cmd.custom;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.Audio;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.module.*;
import net.kodehawa.mantarobot.module.Parser.CommandArguments;
import net.kodehawa.mantarobot.util.GeneralUtils;
import net.kodehawa.mantarobot.util.JSONUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kodehawa.mantarobot.module.Module.Manager.modules;

public class UserCommands extends Module {
	private static Logger LOGGER = LoggerFactory.getLogger("UserCommands");
	private static Map<String, Map<String, List<String>>> fromJson(String json) {
		JsonElement element = new JsonParser().parse(json);

		if (!element.isJsonObject()) throw new IllegalStateException("\"ROOT\" element MUST BE a JsonObject");

		Map<String, Map<String, List<String>>> result = new HashMap<>();

		element.getAsJsonObject().entrySet().forEach(entry -> {
			if (!entry.getValue().isJsonObject())
				throw new IllegalStateException("\"ROOT -> *\" Element MUST BE a JsonObject");

			Map<String, List<String>> map = new HashMap<>();

			entry.getValue().getAsJsonObject().entrySet().forEach(entry2 -> {
				if (!entry2.getValue().isJsonArray())
					throw new IllegalStateException("\"ROOT -> * -> *\" Element MUST BE a JsonArray");

				List<String> list = new ArrayList<>();

				entry2.getValue().getAsJsonArray().forEach(arrayElement -> {
					if (!arrayElement.isJsonPrimitive() || !arrayElement.getAsJsonPrimitive().isString())
						throw new IllegalStateException("\"ROOT -> * -> * -> *\" Element MUST BE a String");

					list.add(arrayElement.getAsString());
				});

				map.put(entry2.getKey(), list);
			});

			result.put(entry.getKey(), map);
		});

		return result;
	}

	private static String toJson(Map<String, Map<String, List<String>>> map) {
		return new Gson().toJson(map);
	}

	private Map<String, Map<String, List<String>>> custom = new HashMap<>();
	private Command customCommand = new Command() {
		private final Pattern compiledPattern = Pattern.compile("\\$\\([A-Za-z.]+?\\)");
		private Random r = new Random();

		@Override
		public CommandType commandType() {
			return CommandType.USER;
		}

		private String dynamicResolve(String string, Map<String, String> dynamicMap) {
			if (!string.contains("$(")) return string;

			Set<String> skipIfIterated = new HashSet<>();
			for (String key : GeneralUtils.iterate(compiledPattern.matcher(string))) {
				if (skipIfIterated.contains(key)) continue;
				String mapKey = key.substring(2, key.length() - 1);
				string = string.replace(key, dynamicMap.getOrDefault(mapKey, mapKey));
				if (!string.contains("$(")) break;
				skipIfIterated.add(key);
			}

			return string;
		}

		private void handle(GuildMessageReceivedEvent event, String cmdName, String rawArgs, String[] args) {
			if (!custom.containsKey(event.getGuild().getId()) || !custom.get(event.getGuild().getId()).containsKey(cmdName))
				return;
			List<String> responses = custom.get(event.getGuild().getId()).get(cmdName);
			String response = responses.get(r.nextInt(responses.size()));

			Map<String, String> dynamicMap = new HashMap<>();
			dynamicMap.put("event.username", event.getAuthor().getName());
			dynamicMap.put("event.nickname", event.getMember().getNickname());
			dynamicMap.put("event.name", event.getMember().getEffectiveName());
			dynamicMap.put("event.mentionUser", event.getAuthor().getAsMention());
			for (int i = 0; i < args.length; i++) dynamicMap.put("event.args" + i, args[i]);
			dynamicMap.put("event.args", Stream.of(args).collect(Collectors.joining(" ")));
			dynamicMap.put("event.guild", event.getGuild().getName());
			dynamicMap.put("event.channel", event.getChannel().getAsMention());
			response = dynamicResolve(response, dynamicMap);

			if (response.startsWith("play:")) {
				String toSend = response.substring(5);
				try {
					new URL(toSend);
				} catch (Exception e) {
					toSend = "ytsearch: " + toSend;
				}

				Audio.getInstance().loadAndPlay(event, toSend);
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
		public void invoke(CommandArguments args) {
			handle(args.event, args.cmdName, args.rawCommand, args.args);
		}

		@Override
		public boolean isHiddenFromHelp() {
			return true;
		}

		@Override
		public String help() {
			return "Hmmm... a Custom Command?! I wonder what it does!";
		}
	};
	private File file;

	public UserCommands() {
		super(Category.CUSTOM);

		if (Mantaro.isWindows()) {
			this.file = new File("C:/mantaro/cc.json");
		} else if (Mantaro.isUnix()) {
			this.file = new File("/home/mantaro/cc.json");
		}

		read();

		this.registerCommands();
		Mantaro.schedule(() -> {
			Set<String> invalidCmds = new HashSet<>();
			custom.values().forEach(map -> map.keySet().forEach(cmd -> {
				if (!modules.containsKey(cmd)) modules.put(cmd, customCommand);
				else invalidCmds.add(cmd);
			}));
			custom.keySet().removeAll(invalidCmds);
		});
	}

	@Override
	public void registerCommands() {
		super.register("addcustom", "Adds a custom command", new SimpleCommand() {
			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				if (!content.startsWith("debug")) {
					String guild = event.getGuild().getId();
					String name = args[0];

					if (modules.containsKey(name) && modules.get(name) != customCommand) {
						event.getChannel().sendMessage("You cannot add a custom command with the name of a command that already exists, you silly.").queue();
						return;
					}

					String responses[] = content.replaceAll(args[0] + " ", "").split(",");

					List<String> responses1 = new ArrayList<>(Arrays.asList(responses));

					custom.computeIfAbsent(guild, s -> new HashMap<>()).put(name, responses1);

					if (!modules.containsKey(name)) modules.put(name, customCommand);

					JSONObject jsonObject = new JSONObject(toJson(custom));
					JSONUtils.instance().write(file, jsonObject);
					read();

					String sResponses = String.join(", ", responses1);
					event.getChannel().sendMessage("``Added custom command: " + name + " with responses: " + sResponses + " -> Guild: " + guild + "``").queue();
				} else {
					if (event.getMember().isOwner() || event.getAuthor().getId().equals(Mantaro.OWNER_ID))
						event.getChannel().sendMessage("```json\n" + GeneralUtils.instance().toPrettyJson(toJson(custom)) + "```").queue();
					else
						event.getChannel().sendMessage(":heavy_multiplication_x: You cannot do that, silly.").queue();
				}
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public String help() {
				return "Creates a custom command. Only works on the guild where it was created.\n"
						+ "You can add multiple responses to the command using the separator , (for example hello,hi,greetings will be 3 different replies)\n"
						+ "There are multiple dynamic modificators available, with a list of them being found on: https://hastebin.com/baxeqovese.js\n"
						+ "Also, there are multiple variables to modify how the reply will be displayed, with a list of them being found on: https://hastebin.com/usududubiv.makefile\n"
						+ "Have fun creating commands!";
			}

		});

		super.register("deletecustom", "Deletes a custom command", new SimpleCommand() {
			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				String guild = event.getGuild().getId();
				String name = args[0];

				if (custom.get(guild).get(name) != null) {
					custom.get(guild).remove(name);
					JSONObject jsonObject = new JSONObject(toJson(custom));
					JSONUtils.instance().write(file, jsonObject);
					read();

					if (custom.entrySet().stream().noneMatch(entry -> entry.getValue().containsKey(name))) {
						modules.remove(name);
					}

					event.getChannel().sendMessage("``Deleted custom command: " + name + " -> Guild: " + event.getGuild().getId() + "``").queue();
				} else {
					event.getChannel().sendMessage("``Command doesn't exist!``").queue();
				}
			}

			@Override
			public String help() {
				return "Deletes a custom command by its name.";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("listcustom", "Lists the custom commands of the guild", new SimpleCommand() {
			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public String help() {
				return "Lists the custom commands of the guild";
			}

			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				if(!custom.containsKey(event.getGuild().getId())){
					EmbedBuilder toSend = new EmbedBuilder();
					toSend.setAuthor("Commands for this guild", null, event.getGuild().getIconUrl())
							.setDescription("There is nothing here, just dust.");
					event.getChannel().sendMessage(toSend.build()).queue();
					return;
				}
				Map<String, List<String>> guildCommands = custom.get(event.getGuild().getId());
				StringBuilder customBuilder = new StringBuilder();
				if (content.isEmpty()) {
					guildCommands.forEach((name, responses) -> customBuilder.append("``").append(name).append("``").append(" "));
					EmbedBuilder toSend = new EmbedBuilder();
					toSend.setAuthor("Commands for this guild", null, event.getGuild().getIconUrl())
						.setDescription(customBuilder.toString());
					event.getChannel().sendMessage(toSend.build()).queue();
				} else if (args[0].equals("detailed")) {
					guildCommands.forEach((name, responses) -> customBuilder.append("``").append(name).append("`` -> With responses: ").append(responses).append("\n"));
					EmbedBuilder toSend = new EmbedBuilder();
					toSend.setAuthor("Commands for this guild", null, event.getGuild().getIconUrl())
						.setDescription(customBuilder.toString());
					event.getChannel().sendMessage(toSend.build()).queue();
				}
			}
		});
	}

	private Map<String, Map<String, List<String>>> getCustomCommands() {
		return custom;
	}

	private void read() {
		try {
			LOGGER.info("Loading custom commands...");
			BufferedReader br = new BufferedReader(new FileReader(file));
			JsonParser parser = new JsonParser();
			JsonObject object = parser.parse(br).getAsJsonObject();
			custom = fromJson(object.toString());
		} catch (FileNotFoundException | UnsupportedOperationException e) {
			e.printStackTrace();
			LOGGER.error("Cannot load custom commands!", e);
		}
	}
}
