package net.kodehawa.mantarobot.commands;

import br.com.brjdevs.java.utils.texts.StringUtils;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.lib.google.Crawler;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.utils.UrbanData;
import net.kodehawa.mantarobot.commands.utils.WeatherData;
import net.kodehawa.mantarobot.commands.utils.YoutubeMp3Info;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.modules.Command;
import net.kodehawa.mantarobot.modules.CommandRegistry;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.modules.events.PostLoadEvent;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import org.json.JSONArray;
import org.json.JSONObject;
import us.monoid.web.Resty;

import java.awt.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.*;
import java.util.List;

import static br.com.brjdevs.java.utils.collections.CollectionUtils.random;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Module
public class UtilsCmds {
	private static final Resty resty = new Resty();

	@Command
	public static void birthday(CommandRegistry registry) {
		registry.register("birthday", new SimpleCommand(Category.UTILS) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				SimpleDateFormat format1 = new SimpleDateFormat("dd-MM-yyyy");
				DBUser user = MantaroData.db().getUser(event.getAuthor());
				if (content.isEmpty()) {
					onError(event);
					return;
				}

				if (content.startsWith("remove")) {
					user.getData().setBirthday(null);
					user.save();
					event.getChannel().sendMessage(EmoteReference.CORRECT + "Correctly resetted birthday date.")
						.queue();
					return;
				}

				if (content.startsWith("month")) {
					Map<String, String> closeBirthdays = new HashMap<>();
					final int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;

					event.getGuild().getMembers().forEach(member -> {
						try {
							if (MantaroData.db().getUser(member.getUser()) != null &&
								MantaroData.db().getUser(event.getMember()).getData().getBirthday() != null) {
								Date date = format1.parse(MantaroData.db().getUser(member).getData().getBirthday());
								int month = date.toInstant().atOffset(ZoneOffset.UTC).getMonthValue();
								if (currentMonth == month) {
									closeBirthdays.put(
										member.getEffectiveName() + "#" + member.getUser().getDiscriminator(),
										MantaroData
											.db().getUser(member.getUser()).getData().getBirthday()
									);
								}
							}
						} catch (Exception e) {
							if (!(e instanceof NullPointerException))
								log.warn("Error while retrieving close birthdays", e);
						}
					});

					if (closeBirthdays.isEmpty()) {
						event.getChannel().sendMessage(
							"No one has a birthday this month! " + EmoteReference.SAD.getDiscordNotation())
							.queue();
						return;
					}

					StringBuilder builder = new StringBuilder();
					closeBirthdays.forEach((name, birthday) -> {
						if (name != null && birthday != null) {
							builder.append(name).append(": ").append(birthday.substring(0, 5)).append("\n");
						}
					});

					event.getChannel().sendMessage(
						"```md\n" + "--Birthdays this month--\n\n" + builder.toString() + "```"
					).queue();

					return;
				}

				Date bd1;
				try {
					String bd;
					bd = content.replace("/", "-");
					String[] parts = bd.split("-");
					if (Integer.parseInt(parts[0]) > 31 || Integer.parseInt(parts[1]) > 12 || Integer.parseInt(
						parts[2]) > 3000) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "Not a valid date.").queue();
						return;
					}

					bd1 = format1.parse(bd);
				} catch (Exception e) {
					Optional.ofNullable(args[0]).ifPresent((s -> event.getChannel().sendMessage(
						"\u274C" + args[0] + " is either not a " +
							"valid date or not parseable. Please try with the correct formatting. Remember to include the year (though you can put any year)")
						.queue()));
					return;
				}

				user.getData().setBirthday(format1.format(bd1));
				user.save();
				event.getChannel().sendMessage(EmoteReference.CORRECT + "Added birthday date.").queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Birthday")
					.setDescription("**Sets your birthday date.**\n")
					.addField(
						"Usage",
						"~>birthday <date>. Set your birthday date using this. Only useful if the server has " +
							"enabled this functionality\n"
							+ "**Parameter explanation:**\n"
							+ "date. A date in dd-mm-yyyy format (13-02-1998 for example)", false
					)
					.addField(
						"Tip",
						"To see whose birthdays are this month, type ~>birthday month\nTo remove your birthday date do " +
							"~>birthday " +
							"remove", false
					)
					.setColor(Color.DARK_GRAY)
					.build();
			}
		});
	}

	@Command
	public static void choose(CommandRegistry registry) {
		registry.register("choose", new SimpleCommand(Category.UTILS) {
			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (args.length < 1) {
					onHelp(event);
					return;
				}

				event.getChannel().sendMessage("I choose ``" + random(args) + "``").queue();
			}

			@Override
			public String[] splitArgs(String content) {
				return StringUtils.efficientSplitArgs(content, -1);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Choose Command")
					.setDescription("**Choose between 1 or more things\n" +
						"It accepts all parameters it gives (Also in quotes to account for spaces if used) and chooses a random one.**")
					.build();
			}
		});
	}

	@Command
	public static void dictionary(CommandRegistry registry) {
		registry.register("dictionary", new SimpleCommand(Category.UTILS) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (args.length == 0) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify a word.").queue();
					return;
				}

				String word = content;

				JSONObject main;
				String definition, part_of_speech, headword, example;

				try {
					main = new JSONObject(Utils.wgetResty("http://api.pearson.com/v2/dictionaries/laes/entries?headword=" + word, event));
					JSONArray results = main.getJSONArray("results");
					JSONObject result = results.getJSONObject(0);
					JSONArray senses = result.getJSONArray("senses");

					headword = result.getString("headword");

					if (result.has("part_of_speech")) part_of_speech = result.getString("part_of_speech");
					else part_of_speech = "Not found.";

					if (senses.getJSONObject(0).get("definition") instanceof JSONArray)
						definition = senses.getJSONObject(0).getJSONArray("definition").getString(0);
					else
						definition = senses.getJSONObject(0).getString("definition");

					try {
						if (senses.getJSONObject(0).getJSONArray("translations").getJSONObject(0).get(
							"example") instanceof JSONArray) {
							example = senses.getJSONObject(0)
								.getJSONArray("translations")
								.getJSONObject(0)
								.getJSONArray("example")
								.getJSONObject(0)
								.getString("text");
						} else {
							example = senses.getJSONObject(0)
								.getJSONArray("translations")
								.getJSONObject(0)
								.getJSONObject("example")
								.getString("text");
						}
					} catch (Exception e) {
						example = "Not found";
					}

				} catch (Exception e) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "No results.").queue();
					return;
				}

				EmbedBuilder eb = new EmbedBuilder();
				eb.setAuthor("Definition for " + word, null, event.getAuthor().getAvatarUrl())
					.setThumbnail(
						"https://upload.wikimedia.org/wikipedia/commons/thumb/5/5a/Wikt_dynamic_dictionary_logo.svg/1000px-Wikt_dynamic_dictionary_logo.svg.png")
					.addField("Definition", "**" + definition + "**", false)
					.addField("Example", "**" + example + "**", false)
					.setDescription(
						String.format("**Part of speech:** `%s`\n" + "**Headword:** `%s`\n", part_of_speech, headword));

				event.getChannel().sendMessage(eb.build()).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Dictionary command")
					.setDescription("**Looks up a word in the dictionary.**")
					.addField("Usage", "`~>dictionary <word>` - Searches a word in the dictionary.", false)
					.addField("Parameters", "`word` - The word to look for", false)
					.build();
			}
		});
	}

	@Command
	public static void google(CommandRegistry registry) {
		registry.register("google", new SimpleCommand(Category.UTILS) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				StringBuilder b = new StringBuilder();
				EmbedBuilder builder = new EmbedBuilder();
				List<Crawler.SearchResult> result = Crawler.get(content);
				for (int i = 0; i < 5 && i < result.size(); i++) {
					Crawler.SearchResult data = result.get(i);
					if (data != null) {
						String title = data.getTitle();
						if (title.length() > 40) title = title.substring(0, 40) + "...";
						b.append(i + 1)
							.append(". **[")
							.append(title)
							.append("](")
							.append(data.getUrl())
							.append(")**\n");
					}
				}

				event.getChannel().sendMessage(
					builder.
						setDescription(b.toString())
						.setThumbnail(
							"https://cdn.pixabay.com/photo/2015/12/08/17/38/magnifying-glass-1083373_960_720.png")
						.setFooter("Click on the blue text to go to the URL.", null)
						.build())
					.queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Google search")
					.setDescription("**Searches on google.**")
					.addField("Usage", "`~>google <query>` - **Makes a search**", false)
					.addField("Parameters", "`query` - **The search query**", false)
					.build();
			}
		});
	}

	@Command
	public static void onPostLoad(PostLoadEvent e) {
		OptsCmd.registerOption("birthday:enable", (event, args) -> {
			if (args.length < 2) {
				OptsCmd.onHelp(event);
				return;
			}

			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();

			try {
				String channel = args[0];
				String role = args[1];

				boolean isId = channel.matches("^[0-9]*$");
				String channelId = isId ? channel : event.getGuild().getTextChannelsByName(channel, true).get(0)
					.getId();
				String roleId = event.getGuild().getRolesByName(role.replace(channelId, ""), true).get(0).getId();
				guildData.setBirthdayChannel(channelId);
				guildData.setBirthdayRole(roleId);
				dbGuild.save();
				event.getChannel().sendMessage(
					String.format(EmoteReference.MEGA + "Birthday logging enabled on this server with parameters -> " +
							"Channel: ``#%s (%s)`` and role: ``%s (%s)``",
						channel, channelId, role, roleId
					)).queue();
			} catch (Exception ex) {
				if (ex instanceof IndexOutOfBoundsException) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "I didn't find a channel or role!\n " +
						"**Remember, you don't have to mention neither the role or the channel, rather just type its " +
						"name, order is <channel> <role>, without the leading \"<>\".**")
						.queue();
					return;
				}
				event.getChannel().sendMessage(
					EmoteReference.ERROR + "You supplied invalid arguments for this command " +
						EmoteReference.SAD).queue();
				OptsCmd.onHelp(event);
			}
		});

		OptsCmd.registerOption("birthday:disable", (event) -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			guildData.setBirthdayChannel(null);
			guildData.setBirthdayRole(null);
			dbGuild.save();
			event.getChannel().sendMessage(EmoteReference.MEGA + "Birthday logging has been disabled on this server")
				.queue();
		});
	}

	@Command
	public static void time(CommandRegistry registry) {
		registry.register("time", new SimpleCommand(Category.UTILS) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				try {
					content = content.replace("UTC", "GMT");

					DBUser user = MantaroData.db().getUser(event.getMember());
					if (user.getData().getTimezone() != null && args.length == 0) {
						event.getChannel().sendMessage(
							EmoteReference.MEGA + "It's " + dateGMT(event.getGuild(), user.getData().getTimezone()) +
								" in the " + user.getData().getTimezone() + " " +
								"timezone").queue();
						return;
					}
					event.getChannel().sendMessage(
						EmoteReference.MEGA + "It's " + dateGMT(
							event.getGuild(), content) + " in the " + content + " " +
							"timezone").queue();

				} catch (Exception e) {
					event.getChannel().sendMessage(
						EmoteReference.ERROR + "Error while retrieving timezone or it's not valid").queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Time")
					.setDescription("**Get the time in a specific timezone**.\n")
					.addField(
						"Usage",
						"`~>time <timezone>` - **Retrieves the time in the specified timezone [Don't write a country!]**.",
						false
					)
					.addField(
						"Parameters", "`timezone` - **A valid timezone [no countries!] between GMT-12 and GMT+14**",
						false
					)
					.build();
			}
		});
	}

	@Command
	public static void translate(CommandRegistry registry) {
		registry.register("translate", new SimpleCommand(Category.UTILS) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				try {
					TextChannel channel = event.getChannel();

					if (!content.isEmpty()) {
						String sourceLang = args[0];
						String targetLang = args[1];
						String textToEncode = content.replace(args[0] + " " + args[1] + " ", "");
						String textEncoded = "";
						String translatorUrl2;

						try {
							textEncoded = URLEncoder.encode(textToEncode, "UTF-8");
						} catch (UnsupportedEncodingException ignored) {}

						String translatorUrl = String.format("https://translate.google.com/translate_a/" +
								"single?client=at&dt=t&dt=ld&dt=qca&dt=rm&dt=bd&dj=1&hl=es-ES&ie=UTF-8&oe=UTF-8&inputm=2" +
								"&otf=2&iid=1dd3b944-fa62-4b55-b330-74909a99969e&sl=%1s&tl=%2s&dt=t&q=%3s", sourceLang,
							targetLang,
							textEncoded
						);

						try {
							resty.identifyAsMozilla();
							translatorUrl2 = resty.text(translatorUrl).toString();
							JSONArray data = new JSONObject(translatorUrl2).getJSONArray("sentences");

							for (int i = 0; i < data.length(); i++) {
								JSONObject entry = data.getJSONObject(i);
								channel.sendMessage(
									":speech_balloon: " + "Translation for " + textToEncode + ": " + entry.getString
										("trans")).queue();
							}
						} catch (IOException e) {
							event.getChannel().sendMessage(
								"I got an error while translating, Google doesn't like me " + EmoteReference.SAD
									.getDiscordNotation())
								.queue();
						}
					} else {
						onHelp(event);
					}
				} catch (Exception e) {
					event.getChannel().sendMessage(
						"Error while fetching results. Google doesn't like us " + EmoteReference.SAD
							.getDiscordNotation())
						.queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Translation command")
					.setDescription("**Translates the given sentence**.")
					.addField(
						"Usage",
						"`~>translate <sourcelang> <outputlang> <sentence>` - **Translates the specified sentence.**.",
						false
					)
					.addField(
						"Parameters",
						"`sourcelang` - **The language the sentence is written in. Use codes (english = en)**\n"
							+ "`outputlang` - **The language you want to translate to (french = fr, for example)**\n"
							+ "`sentence` - **The sentence to translate.**", false
					)
					.setColor(Color.BLUE)
					.build();
			}
		});
	}

	@Command
	public static void urban(CommandRegistry registry) {
		registry.register("urban", new SimpleCommand(Category.UTILS) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				String beheadedSplit[] = content.split("->");
				EmbedBuilder embed = new EmbedBuilder();

				if (!content.isEmpty()) {
					long start = System.currentTimeMillis();
					String url = null;
					try {
						url = "http://api.urbandictionary.com/v0/define?term=" + URLEncoder.encode(
							beheadedSplit[0], "UTF-8");
					} catch (UnsupportedEncodingException ignored) {}
					String json = Utils.wgetResty(url, event);
					UrbanData data = GsonDataManager.GSON_PRETTY.fromJson(json, UrbanData.class);

					long end = System.currentTimeMillis() - start;
					//This shouldn't happen, but it fucking happened.
					if (beheadedSplit.length < 1) {
						return;
					}

					if (data.list.isEmpty()) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "No results.").queue();
						return;
					}

					switch (beheadedSplit.length) {
						case 1:
							embed.setAuthor(
								"Urban Dictionary definition for " + content, data.list.get(0).permalink, null)
								.setDescription("Main definition.")
								.setThumbnail("https://everythingfat.files.wordpress.com/2013/01/ud-logo.jpg")
								.setColor(Color.GREEN)
								.addField("Definition", data.list.get(0).definition.length() > 1000 ? data.list.get(0).definition + "..." : data.list.get(0).definition, false)
								.addField("Example", data.list.get(0).example, false)
								.addField(":thumbsup:", data.list.get(0).thumbs_up, true)
								.addField(":thumbsdown:", data.list.get(0).thumbs_down, true)
								.setFooter("Information by Urban Dictionary (Process time: " + end + "ms)", null);
							event.getChannel().sendMessage(embed.build()).queue();
							break;
						case 2:
							int defn = Integer.parseInt(beheadedSplit[1]) - 1;
							String defns = String.valueOf(defn + 1);
							String definition = data.list.get(defn).definition;
							embed.setAuthor(
								"Urban Dictionary definition for " + beheadedSplit[0], data.list.get(defn).permalink,
								null
							)
								.setThumbnail("https://everythingfat.files.wordpress.com/2013/01/ud-logo.jpg")
								.setDescription("Definition " + defns)
								.setColor(Color.GREEN)
								.addField("Definition", definition.length() > 1000 ? definition.substring(0, 1000) + "..." : definition, false)
								.addField("Example", data.list.get(defn).example.length() > 1000 ? data.list.get(defn).example.substring(0, 1000) + "..." : data.list.get(defn).example, false)
								.addField(":thumbsup:", data.list.get(defn).thumbs_up, true)
								.addField(":thumbsdown:", data.list.get(defn).thumbs_down, true)
								.setFooter("Information by Urban Dictionary (Process time: " + end + "ms)", null);
							event.getChannel().sendMessage(embed.build()).queue();
							break;
						default:
							onHelp(event);
							break;
					}
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Urban dictionary")
					.setColor(Color.CYAN)
					.setDescription("Retrieves definitions from **Urban Dictionary**.")
					.addField(
						"Usage",
						"`~>urban <term>-><number>` - **Retrieve a definition based on the given parameters.**", false
					)
					.addField("Parameters", "term - **The term you want to look up**\n"
						+ "number - **(OPTIONAL) Parameter defined with the modifier '->' after the term. You don't need to use it.**\n"
						+ "e.g. putting 2 will fetch the second result on Urban Dictionary", false)
					.build();
			}
		});
	}

	@Command
	public static void weather(CommandRegistry registry) {
		registry.register("weather", new SimpleCommand(Category.UTILS) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (content.isEmpty()) {
					onError(event);
					return;
				}

				EmbedBuilder embed = new EmbedBuilder();
				try {
					long start = System.currentTimeMillis();
					WeatherData data = GsonDataManager.GSON_PRETTY.fromJson(
						Utils.wget(
							String.format(
								"http://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s",
								URLEncoder.encode(content, "UTF-8"),
								MantaroData.config().get().weatherAppId
							), event
						),
						WeatherData.class
					);

					String countryCode = data.sys.country;
					String status = data.getWeather().get(0).main;
					Double temp = data.getMain().getTemp();
					double pressure = data.getMain().getPressure();
					int hum = data.getMain().getHumidity();
					Double ws = data.getWind().speed;
					int clness = data.getClouds().all;

					Double finalTemperatureCelcius = temp - 273.15;
					Double finalTemperatureFarnheit = temp * 9 / 5 - 459.67;
					Double finalWindSpeedMetric = ws * 3.6;
					Double finalWindSpeedImperial = ws / 0.447046;
					long end = System.currentTimeMillis() - start;

					embed.setColor(Color.CYAN)
						.setTitle(
							":flag_" + countryCode.toLowerCase() + ":" + " Forecast information for " + content, null)
						.setDescription(status + " (" + clness + "% cloudiness)")
						.addField(":thermometer: Temperature", finalTemperatureCelcius.intValue() + "°C | " +
							finalTemperatureFarnheit.intValue() + "°F", true)
						.addField(":droplet: Humidity", hum + "%", true)
						.addBlankField(true)
						.addField(":wind_blowing_face: Wind Speed", finalWindSpeedMetric.intValue() + "km/h | " +
							finalWindSpeedImperial.intValue() + "mph", true)
						.addField("Pressure", pressure + "kPA", true)
						.addBlankField(true)
						.setFooter("Information provided by OpenWeatherMap (Process time: " + end + "ms)", null);
					event.getChannel().sendMessage(embed.build()).queue();
				} catch (Exception e) {
					event.getChannel().sendMessage("Error while fetching results.").queue();
					if (!(e instanceof NullPointerException))
						log.warn(
							"Exception caught while trying to fetch weather data, maybe the API changed something?", e);
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Weather command")
					.setDescription(
						"This command retrieves information from OpenWeatherMap. Used to check **forecast information.**")
					.addField(
						"Usage",
						"`~>weather <city>,<countrycode>` - **Retrieves the forecast information for the given location.**",
						false
					)
					.addField(
						"Parameters", "`city` - **Your city name, e.g. New York**\n"
							+ "`countrycode` - **(OPTIONAL) The abbreviation for your country, for example US (USA) or MX (Mexico).**",
						false
					)
					.build();
			}
		});
	}

	@Command
	public static void ytmp3(CommandRegistry registry) {
		registry.register("ytmp3", new SimpleCommand(Category.UTILS) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				YoutubeMp3Info info = YoutubeMp3Info.forLink(content);

				if (info == null) {
					event.getChannel().sendMessage(":heavy_multiplication_x: Your link seems to be invalid.").queue();
					return;
				}

				if (info.error != null) {
					event.getChannel().sendMessage(
						":heavy_multiplication_x: I got an error while fetching that link``" + info.error
							+ "``").queue();
					return;
				}

				EmbedBuilder builder = new EmbedBuilder()
					.setAuthor(info.title, info.link, event.getAuthor().getEffectiveAvatarUrl())
					.setFooter("Powered by the youtubeinmp3.com API", null);

				try {
					int length = Integer.parseInt(info.length);
					builder.addField(
						"Length",
						String.format(
							"%02d minutes, %02d seconds",
							SECONDS.toMinutes(length),
							length - MINUTES.toSeconds(SECONDS.toMinutes(length))
						),
						false
					);
				} catch (Exception ignored) {}

				event.getChannel().sendMessage(builder
					.addField("Download Link", "[Click Here!](" + info.link + ")", false)
					.build()
				).queue();
				TextChannelGround.of(event).dropItemWithChance(7, 5);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Youtube MP3 command")
					.setDescription("**Youtube video to MP3 converter**")
					.addField("Usage", "`~>ytmp3 <youtube link>`", true)
					.addField("Parameters", "`youtube link` - **The link of the video to convert to MP3**", true)
					.build();
			}
		});
	}

	static String dateGMT(Guild guild, String tz) {
		DateFormat format = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
		Date date = new Date();

		DBGuild dbGuild = MantaroData.db().getGuild(guild.getId());
		GuildData guildData = dbGuild.getData();

		if(guildData.getTimeDisplay() == 1){
			format = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a");
		}

		format.setTimeZone(TimeZone.getTimeZone(tz));
		return format.format(date);
	}
}
