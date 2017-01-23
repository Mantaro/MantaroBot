package net.kodehawa.mantarobot.cmd;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.guild.Parameters;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.log.Log;
import net.kodehawa.mantarobot.log.Type;
import net.kodehawa.mantarobot.module.Category;
import net.kodehawa.mantarobot.module.Command;
import net.kodehawa.mantarobot.module.CommandType;
import net.kodehawa.mantarobot.module.Module;
import net.kodehawa.mantarobot.util.GeneralUtils;
import net.kodehawa.mantarobot.util.JSONUtils;
import org.json.JSONObject;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Random;

public class Quote extends Module {

	private static LinkedHashMap<String, LinkedHashMap<String, List<String>>> fromJson(String json) {
		JsonElement element = new JsonParser().parse(json);

		if (!element.isJsonObject()) throw new IllegalStateException("\"ROOT\" element MUST BE a JsonObject");

		LinkedHashMap<String, LinkedHashMap<String, List<String>>> result = new LinkedHashMap<>();

		element.getAsJsonObject().entrySet().forEach(entry -> {
			if (!entry.getValue().isJsonObject())
				throw new IllegalStateException("\"ROOT -> *\" Element MUST BE a JsonObject");

			LinkedHashMap<String, List<String>> map = new LinkedHashMap<>();

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

	private static String toJson(Map<String, LinkedHashMap<String, List<String>>> map) {
		return new Gson().toJson(map);
	}

	private File file;
	private LinkedHashMap<String, LinkedHashMap<String, List<String>>> quotesMap = new LinkedHashMap<>();

	public Quote() {
		if (Mantaro.instance().isWindows()) {
			this.file = new File("C:/mantaro/quotes.json");
		} else if (Mantaro.instance().isUnix()) {
			this.file = new File("/home/mantaro/quotes.json");
		}

		super.setCategory(Category.MISC);
		read();
		this.registerCommands();
	}

	@SuppressWarnings({"unused", "unchecked"})
	@Override
	public void registerCommands() {
		super.register("quote", "Adds or retrieves quotes.", new Command() {
			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public String help() {
				return "> Usage:\n"
					+ "~>quote add [number]: Adds a quote with content defined by the number. For example 1 will quote the last message.\n"
					+ "~>quote random: Gets a random quote. \n"
					+ "~>quote read [number]: Gets a quote matching the number. \n"
					+ "~>quote addfrom [phrase] Adds a quote based in text search criteria.\n"
					+ "~>quote getfrom [phrase]: Searches for the first quote which matches your search criteria and prints it.\n"
					+ "> Parameters:\n"
					+ "[number]: Message number to quote. For example 1 will quote the last message.\n"
					+ "[phrase]: A part of the quote phrase.";
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				Random rand = new Random();
				Guild guild = event.getGuild();
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				Message receivedMessage = event.getMessage();
				List<Message> messageHistory = null;
				try {
					messageHistory = channel.getHistory().retrievePast(100).complete();
				} catch (Exception e) {
					e.printStackTrace();
				}

				String noArgs = content.split(" ")[0];
				String phrase = content.replace(noArgs + " ", "");
				SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				switch (noArgs) {
					default:
						channel.sendMessage(help()).queue();
						break;
					case "add":
						try {
							int i = Integer.parseInt(phrase);
							Message m = messageHistory.get(i);

							String[] sContent = {
								m.getAuthor().getName(),
								m.getAuthor().getAvatarUrl(), m.getChannel().getName(),
								m.getGuild().getName(), String.valueOf(System.currentTimeMillis())
							};

							if (quotesMap.containsKey(guild.getId())) {
								LinkedHashMap<String, List<String>> temp = new LinkedHashMap<>();
								quotesMap.get(
									guild.getId()).put(m.getContent(), Arrays.asList(sContent)
								);

							} else {
								LinkedHashMap<String, List<String>> temp = new LinkedHashMap<>();
								temp.put(
									m.getContent(), Arrays.asList(sContent)
								);
								quotesMap.put(guild.getId(), temp);
							}

							JSONObject object = new JSONObject(toJson(quotesMap));
							JSONUtils.instance().write(file, object);
							read();

							Date quoteDate = new Date(System.currentTimeMillis());
							EmbedBuilder builder = new EmbedBuilder();
							builder.setAuthor(m.getAuthor().getName() + " said:", null, m.getAuthor().getEffectiveAvatarUrl())
								.setThumbnail(m.getAuthor().getEffectiveAvatarUrl())
								.setColor(m.getGuild().getMember(m.getAuthor()).getColor())
								.setDescription("Quote made on server " + m.getGuild().getName()
									+ " in channel " + "#" + m.getChannel().getName())
								.addField("Content", m.getContent(), false)
								.setFooter("Date: " + dateFormat.format(quoteDate), null);
							channel.sendMessage(builder.build()).queue();
							break;
						} catch (Exception e) {
							channel.sendMessage(":heavy_multiplication_x: Error while adding quote: " + e.getCause() + e.getMessage()).queue();
							e.printStackTrace();
							break;
						}
					case "random":
						List keys = new ArrayList(quotesMap.get(event.getGuild().getId()).keySet());
						int quoteN = rand.nextInt(keys.size());
						List<String> quoteElements = quotesMap.get(event.getGuild().getId()).get(keys.get(quoteN));

						EmbedBuilder embedBuilder = new EmbedBuilder();
						Date dat = new Date(Long.parseLong(quoteElements.get(4)));
						embedBuilder.setAuthor(quoteElements.get(0) + " said:", null, quoteElements.get(1))
							.setThumbnail(quoteElements.get(1))
							.setColor(Color.CYAN)
							.setDescription("Quote made on server " + quoteElements.get(3)
								+ " in channel " + "#" + quoteElements.get(2))
							.addField("Content", keys.get(quoteN).toString(), false)
							.setFooter("Date: " + dateFormat.format(dat), null);
						channel.sendMessage(embedBuilder.build()).queue();
						break;
					case "read":
						int i = Integer.parseInt(phrase);
						List keys1 = new ArrayList(quotesMap.get(event.getGuild().getId()).keySet());
						List<String> quoteElements2 = quotesMap.get(event.getGuild().getId()).get(keys1.get(i));
						EmbedBuilder embedBuilder2 = new EmbedBuilder();
						Date date1 = new Date(Long.parseLong(quoteElements2.get(4)));
						embedBuilder2.setAuthor(quoteElements2.get(0) + " said:", null, quoteElements2.get(1))
							.setThumbnail(quoteElements2.get(1))
							.setColor(Color.CYAN)
							.setDescription("Quote made on server " + quoteElements2.get(3)
								+ " in channel " + "#" + quoteElements2.get(2))
							.addField("Content", keys1.get(i).toString(), false)
							.setFooter("Date: " + dateFormat.format(date1), null);
						channel.sendMessage(embedBuilder2.build()).queue();
						break;
					case "addfrom":
						int i1 = -1;
						Message m = event.getMessage();
						for (Message m1 : messageHistory) {
							i1++;
							if (m1.getContent().contains(phrase) && !m1.getContent().startsWith(Parameters.getPrefixForServer("default")) && !m1.getContent().startsWith(Parameters.getPrefixForServer(m1.getGuild().getId()))) {
								m = messageHistory.get(i1);
								break;
							}
						}

						String[] sContent = {
							m.getAuthor().getName(),
							m.getAuthor().getAvatarUrl(), m.getChannel().getName(),
							m.getGuild().getName(), String.valueOf(System.currentTimeMillis())
						};

						if (quotesMap.containsKey(guild.getId())) {
							LinkedHashMap<String, List<String>> temp = new LinkedHashMap<>();
							quotesMap.get(
								guild.getId()).put(m.getContent(), Arrays.asList(sContent)
							);

						} else {
							LinkedHashMap<String, List<String>> temp = new LinkedHashMap<>();
							temp.put(
								m.getContent(), Arrays.asList(sContent)
							);
							quotesMap.put(guild.getId(), temp);
						}

						JSONObject object = new JSONObject(toJson(quotesMap));
						JSONUtils.instance().write(file, object);
						read();

						Date quoteDate = new Date(System.currentTimeMillis());
						EmbedBuilder builder = new EmbedBuilder();
						builder.setAuthor(m.getAuthor().getName() + " said:", null, m.getAuthor().getEffectiveAvatarUrl())
							.setThumbnail(m.getAuthor().getEffectiveAvatarUrl())
							.setColor(m.getGuild().getMember(m.getAuthor()).getColor())
							.setDescription("Quote made on server " + m.getGuild().getName()
								+ " in channel " + "#" + m.getChannel().getName())
							.addField("Content", m.getContent(), false)
							.setFooter("Date: " + dateFormat.format(quoteDate), null);
						channel.sendMessage(builder.build()).queue();
						break;
					case "getfrom":
						List<String> quotes = new ArrayList(quotesMap.get(event.getGuild().getId()).keySet());
						for (int i2 = 0; i2 < quotes.size() - 1; i2++) {
							if (quotes.get(i2).contains(phrase)) {
								List<String> quoteE = quotesMap.get(event.getGuild().getId()).get(quotes.get(i2));
								Date date = new Date(Long.parseLong(quoteE.get(4)));
								EmbedBuilder builder2 = new EmbedBuilder();
								builder2.setAuthor(quoteE.get(0) + " said:", null, quoteE.get(1))
									.setThumbnail(quoteE.get(1))
									.setColor(Color.CYAN)
									.setDescription("Quote made on server " + quoteE.get(3)
										+ " in channel " + "#" + quoteE.get(2))
									.addField("Content", quotes.get(i2), false)
									.setFooter("Date: " + dateFormat.format(date), null);
								channel.sendMessage(builder2.build()).queue();
								break;
							}
						}
					case "debug":
						if (event.getAuthor().getId().equals(Mantaro.OWNER_ID))
							event.getChannel().sendMessage(GeneralUtils.instance().paste(GeneralUtils.instance().toPrettyJson(toJson(quotesMap)))).queue();
						else
							event.getChannel().sendMessage("What are you trying to do, silly.").queue();
						break;
				}
			}
		});
	}

	private void read() {
		try {
			Log.instance().print("Loading quotes...", this.getClass(), Type.INFO);
			BufferedReader br = new BufferedReader(new FileReader(file));
			JsonParser parser = new JsonParser();
			JsonObject object = parser.parse(br).getAsJsonObject();
			quotesMap = fromJson(object.toString());
		} catch (FileNotFoundException | UnsupportedOperationException e) {
			e.printStackTrace();
			Log.instance().print("Cannot load quotes!", this.getClass(), Type.WARNING, e);
		}
	}

}