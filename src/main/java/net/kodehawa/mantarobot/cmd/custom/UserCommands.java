package net.kodehawa.mantarobot.cmd.custom;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.Audio;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.log.Log;
import net.kodehawa.mantarobot.log.Type;
import net.kodehawa.mantarobot.module.Category;
import net.kodehawa.mantarobot.module.Command;
import net.kodehawa.mantarobot.module.CommandType;
import net.kodehawa.mantarobot.module.Module;
import net.kodehawa.mantarobot.module.Parser.CommandArguments;
import net.kodehawa.mantarobot.util.GeneralUtils;
import net.kodehawa.mantarobot.util.JSONUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;
import java.util.*;

public class UserCommands extends Module {
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
		private Random r = new Random();

		@Override
		public CommandType commandType() {
			return CommandType.USER;
		}

		@Override
		public void onCommand(String[] args, String commandName, GuildMessageReceivedEvent event) {
			if (!custom.containsKey(event.getGuild().getId()) || custom.get(event.getGuild().getId()).containsKey(commandName))
				return;

			List<String> responses = custom.get(event.getGuild().getId()).get(commandName);

			String response = responses.get(r.nextInt(responses.size()));
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
				event.getChannel().sendMessage(new EmbedBuilder().setDescription(response.substring(6)).setTitle(commandName).setColor(event.getMember().getColor()).build()).queue();
				return;
			}

			event.getChannel().sendMessage(response).queue();
		}

		@Override
		public void invoke(CommandArguments args) {
			onCommand(args.args, args.invoke, args.event);
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
		if (Mantaro.instance().isWindows()) {
			this.file = new File("C:/mantaro/cc.json");
		} else if (Mantaro.instance().isUnix()) {
			this.file = new File("/home/mantaro/cc.json");
		}
		read();
		this.setCategory(Category.CUSTOM);
		this.registerCommands();
		Mantaro.instance().schedule(() -> {
			Set<String> invalidCmds = new HashSet<>();
			custom.keySet().forEach(cmd -> {
				if (!modules.containsKey(cmd)) modules.put(cmd, customCommand);
				else invalidCmds.add(cmd);
			});
			custom.keySet().removeAll(invalidCmds);
		});
	}

	@Override
	public void registerCommands() {
		super.register("addcustom", "Adds a custom command", new Command() {
			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

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
			public String help() {
				return "Creates a custom command. Only works on the guild where it was created.";
			}

		});

		super.register("deletecustom", "Deletes a custom command", new Command() {
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
				return "Deletes a custom command.";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("listcustom", "Lists the custom commands of the guild", new Command() {
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
				Map<String, List<String>> guildCommands = custom.get(event.getGuild().getId());
				StringBuilder customBuilder = new StringBuilder();
				if(content.isEmpty()){
					guildCommands.forEach((name, responses) -> customBuilder.append("``").append(name).append("``").append(" "));
					EmbedBuilder toSend = new EmbedBuilder();
					toSend.setAuthor("Commands for this guild", null, event.getGuild().getIconUrl())
							.setDescription(customBuilder.toString());
					event.getChannel().sendMessage(toSend.build()).queue();
				} else if(args[0].equals("detailed")){
					guildCommands.forEach((name, responses) -> customBuilder.append("``").append(name).append(" -> With responses: ").append(responses).append("\n"));
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
			Log.instance().print("Loading custom commands...", this.getClass(), Type.INFO);
			BufferedReader br = new BufferedReader(new FileReader(file));
			JsonParser parser = new JsonParser();
			JsonObject object = parser.parse(br).getAsJsonObject();
			custom = fromJson(object.toString());
		} catch (FileNotFoundException | UnsupportedOperationException e) {
			e.printStackTrace();
			Log.instance().print("Cannot load custom commands!", this.getClass(), Type.WARNING, e);
		}
	}
}
