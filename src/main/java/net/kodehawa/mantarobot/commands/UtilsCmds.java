package net.kodehawa.mantarobot.commands;

import com.udojava.evalex.Expression;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.lib.google.Crawler;
import net.kodehawa.mantarobot.commands.currency.inventory.TextChannelGround;
import net.kodehawa.mantarobot.commands.utils.data.UrbanData;
import net.kodehawa.mantarobot.commands.utils.data.WeatherData;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.YoutubeMp3Info;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.monoid.web.Resty;

import java.awt.Color;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.IntConsumer;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class UtilsCmds extends Module {
	private static final Logger LOGGER = LoggerFactory.getLogger("UtilsCmds");
	private final Resty resty = new Resty();

	public UtilsCmds() {
		super(Category.MISC);
		translate();
		birthday();
		ytmp3();
		weather();
		urban();
		math();
		googleSearch();
	}

	private void birthday() {
		super.register("birthday", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				SimpleDateFormat format1 = new SimpleDateFormat("dd-MM-yyyy");

				if (content.startsWith("month")) {
					Map<String, String> closeBirthdays = new HashMap<>();
					final int currentMonth = Integer.parseInt(String.format("%02d", Calendar.MONTH));
					event.getGuild().getMembers().forEach(member -> {
						try {
							Date date = format1.parse(MantaroData.getData().get().getUser(event.getAuthor(), false).birthdayDate);
							LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
							if (currentMonth == Integer.parseInt(String.format("%02d", localDate.getMonth().getValue()))) {
								closeBirthdays.put(member.getEffectiveName() + "#" + member.getUser().getDiscriminator(), MantaroData.getData().get().getUser(event.getAuthor(), false).birthdayDate);
							}
						} catch (Exception e) {
							LOGGER.debug("Error while retrieving close birthdays", e);
						}
					});

					if (closeBirthdays.isEmpty()) {
						event.getChannel().sendMessage("No one has a birthday this month.").queue();
						return;
					}

					StringBuilder builder = new StringBuilder();

					closeBirthdays.forEach((name, birthday) -> builder.append(name).append(": ").append(birthday.substring(0, 5)).append("\n"));

					event.getChannel().sendMessage("```md\n" + "--Birthdays this month--\n\n" + builder.toString() + "```").queue();

					return;
				}

				TextChannel channel = event.getChannel();
				String userId = event.getMessage().getAuthor().getId();
				Date bd1;
				//So they don't input something that isn't a date...
				try {
					bd1 = format1.parse(args[0]);
				} catch (Exception e) {
					Optional.ofNullable(args[0]).ifPresent((s -> channel.sendMessage("\u274C" + args[0] + " is not a valid date or I cannot parse it.").queue()));
					return;
				}

				MantaroData.getData().get().getUser(event.getAuthor(), true).birthdayDate = format1.format(bd1);
				MantaroData.getData().save();
				channel.sendMessage("\uD83D\uDCE3 Added birthday date.").queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Birthday")
					.setDescription("Sets your birthday date.\n")
					.addField("Usage", "~>birthday <date>. Sets your birthday date. Only useful if the server enabled this functionality"
						+ "**Parameter explanation:**\n"
						+ "date. A date in dd-mm-yyyy format (13-02-1998 for example)", false)
					.addField("Tip", "To see birthdays this month do ~>birthday month", false)
					.setColor(Color.DARK_GRAY)
					.build();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

		});
	}

	private void googleSearch() {
		super.register("google", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				StringBuilder b = new StringBuilder();
				EmbedBuilder builder = new EmbedBuilder();
				List<Crawler.SearchResult> result = Crawler.get(content);
				for (int i = 0; i < 5 && i < result.size(); i++) {
					Crawler.SearchResult data = result.get(i);
					if (data != null)
						b.append('[').append(i + 1).append("] ").append(data.getTitle()).append("\n");
				}

				event.getChannel().sendMessage(builder.setDescription(b.toString()).build()).queue();

				IntConsumer selector = (c) -> {
					event.getChannel().sendMessage(EmoteReference.OK + "Result for " + content + ": " + result.get(c - 1).getUrl()).queue();

					event.getMessage().addReaction(EmoteReference.OK.getUnicode()).queue();
				};
				DiscordUtils.selectInt(event, result.size() + 1, selector);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Google search")
					.setDescription("Searches on google.")
					.addField("Usage", "~>google <query>", false)
					.addField("Parameters", "query: The search query to look for", false)
					.build();
			}
		});
	}

	private void math() {
		super.register("math", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				try {
					BigDecimal expressionResult = new Expression(content)
						.setPrecision(15)
						.setRoundingMode(RoundingMode.UP)
						.eval();

					event.getChannel().sendMessage(EmoteReference.PENCIL + "The result for your math operation is: " + expressionResult).queue();
				} catch (RuntimeException e) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "Wrong syntax: ``" + e.getMessage() + "``").queue();
				}

			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Math command")
					.setDescription("Does your math work.")
					.addField("Possible arguments", "You can find a list of possible arguments on: https://hastebin.com/ayafikamip.vbs", true)
					.addField("Warning", "The floating point precision is set to 15 with a upwards rounding", true)
					.build();
			}
		});
	}

	private void translate() {
		super.register("translate", new SimpleCommand() {
			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
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
						} catch (UnsupportedEncodingException ignored) {
						}

						String translatorUrl = String.format("https://translate.google.com/translate_a/" +
							"single?client=at&dt=t&dt=ld&dt=qca&dt=rm&dt=bd&dj=1&hl=es-ES&ie=UTF-8&oe=UTF-8&inputm=2" +
							"&otf=2&iid=1dd3b944-fa62-4b55-b330-74909a99969e&sl=%1s&tl=%2s&dt=t&q=%3s", sourceLang, targetLang, textEncoded);

						try {
							resty.identifyAsMozilla();
							translatorUrl2 = resty.text(translatorUrl).toString();
							JSONArray data = new JSONObject(translatorUrl2).getJSONArray("sentences");

							for (int i = 0; i < data.length(); i++) {
								JSONObject entry = data.getJSONObject(i);
								channel.sendMessage(":speech_balloon: " + "Translation for " + textToEncode + ": " + entry.getString("trans")).queue();
							}
						} catch (IOException e) {
							LOGGER.warn("Something went wrong when translating.", e);
							channel.sendMessage(":heavy_multiplication_x:" + "Something went wrong when translating. " + e.getClass().getSimpleName()).queue();
						}
					} else {
						onHelp(event);
					}
				} catch (Exception e) {
					event.getChannel().sendMessage("Error while fetching results.").queue();
					LOGGER.warn("Something went wrong while processing translation elements.", e);
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Translation command")
					.setDescription("Translates any given sentence.\n"
						+ "**Usage example:**\n"
						+ "~>translate <sourcelang> <outputlang> <sentence>.\n"
						+ "**Parameter explanation**\n"
						+ "sourcelang: The language the sentence is written in. Use codes (english = en)\n"
						+ "outputlang: The language you want to translate to (french = fr, for example)\n"
						+ "sentence: The sentence to translate.")
					.setColor(Color.BLUE)
					.build();
			}

		});
	}

	private void urban() {
		super.register("urban", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				//First split is definition, second one is number. I would use space but we need the ability to fetch with spaces too.
				String beheadedSplit[] = content.split("->");
				EmbedBuilder embed = new EmbedBuilder();

				if (!content.isEmpty()) {
					long start = System.currentTimeMillis();
					String url = null;
					try {
						url = "http://api.urbandictionary.com/v0/define?term=" + URLEncoder.encode(beheadedSplit[0], "UTF-8");
					} catch (UnsupportedEncodingException ignored) {
					}
					String json = Utils.wgetResty(url, event);
					UrbanData data = GsonDataManager.GSON_PRETTY.fromJson(json, UrbanData.class);

					long end = System.currentTimeMillis() - start;
					switch (beheadedSplit.length) {
						case 1:
							embed.setAuthor("Urban Dictionary definition for " + content, data.list.get(0).permalink, null)
								.setDescription("Main definition.")
								.setThumbnail("https://everythingfat.files.wordpress.com/2013/01/ud-logo.jpg")
								.setColor(Color.GREEN)
								.addField("Definition", data.list.get(0).definition, false)
								.addField("Example", data.list.get(0).example, false)
								.addField(":thumbsup:", data.list.get(0).thumbs_up, true)
								.addField(":thumbsdown:", data.list.get(0).thumbs_down, true)
								.setFooter("Information by Urban Dictionary (Process time: " + end + "ms)", null);
							event.getChannel().sendMessage(embed.build()).queue();
							break;
						case 2:
							int defn = Integer.parseInt(beheadedSplit[1]) - 1;
							String defns = String.valueOf(defn + 1);
							embed.setAuthor("Urban Dictionary definition for " + beheadedSplit[0], data.list.get(defn).permalink, null)
								.setThumbnail("https://everythingfat.files.wordpress.com/2013/01/ud-logo.jpg")
								.setDescription("Definition " + defns)
								.setColor(Color.PINK)
								.addField("Definition", data.list.get(defn).definition, false)
								.addField("Thumbs up", data.list.get(defn).thumbs_up, true)
								.addField("Thumbs down", data.list.get(defn).thumbs_down, true)
								.addField("Example", data.list.get(defn).example, false)
								.setFooter("Information by Urban Dictionary", null);
							event.getChannel().sendMessage(embed.build()).queue();
							break;
						default:
							onHelp(event);
							break;
					}
				}
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Urban dictionary")
					.setColor(Color.CYAN)
					.setDescription("Retrieves definitions from **Urban Dictionary**.\n"
						+ "Usage: \n"
						+ "~>urban <term>-><number>: Gets a definition based on parameters.\n"
						+ "Parameter description:\n"
						+ "term: The term you want to look up the urban definition for.\n"
						+ "number: **OPTIONAL** Parameter defined with the modifier '->' after the term. You don't need to use it.\n"
						+ "For example putting 2 will fetch the second result on Urban Dictionary")
					.build();
			}
		});
	}

	private void weather() {
		super.register("weather", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (content.isEmpty()) {
					onHelp(event);
					return;
				}

				EmbedBuilder embed = new EmbedBuilder();
				try {
					long start = System.currentTimeMillis();
					String APP_ID = MantaroData.getConfig().get().weatherAppId;
					String json = Utils.wget(String.format("http://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s", URLEncoder.encode(content, "UTF-8"), APP_ID), event);
					WeatherData data = GsonDataManager.GSON_PRETTY.fromJson(json, WeatherData.class);

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
						.setTitle(":flag_" + countryCode.toLowerCase() + ":" + " Forecast information for " + content, null)
						.setDescription(status + " (" + clness + "% cloudiness)")
						.addField(":thermometer: Temperature", finalTemperatureCelcius.intValue() + "°C | " + finalTemperatureFarnheit.intValue() + "°F", true)
						.addField(":droplet: Humidity", hum + "%", true)
						.addBlankField(true)
						.addField(":wind_blowing_face: Wind Speed", finalWindSpeedMetric.intValue() + "km/h | " + finalWindSpeedImperial.intValue() + "mph", true)
						.addField("Pressure", pressure + "kPA", true)
						.addBlankField(true)
						.setFooter("Information provided by OpenWeatherMap (Process time: " + end + "ms)", null);
					event.getChannel().sendMessage(embed.build()).queue();
				} catch (Exception e) {
					event.getChannel().sendMessage("Error while fetching results.").queue();
					LOGGER.warn("Exception caught while trying to fetch weather data, maybe the API changed something?", e);
				}
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Weather command")
					.setDescription("This command retrieves information from OpenWeatherMap. Used to check **forecast information.**\n"
						+ "> Usage:\n"
						+ "~>weather <city>,<countrycode>: Retrieves the forecast information for such location.\n"
						+ "> Parameters:\n"
						+ "city: Your city name, for example New York\n"
						+ "countrycode: (OPTIONAL) The code for your country, for example US (USA) or MX (Mexico).")
					.build();
			}
		});
	}

	private void ytmp3() {
		super.register("ytmp3", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				YoutubeMp3Info info = YoutubeMp3Info.forLink(content, event);

				if (info == null) return; //I think we already logged this in the YoutubeMp3Info class
				if (info.error != null) {
					event.getChannel().sendMessage(":heavy_multiplication_x: Error while fetching link, maybe copyright? ``" + info.error + "``").queue();
					return;
				}

				EmbedBuilder builder = new EmbedBuilder()
					.setAuthor(info.title, info.link, event.getAuthor().getEffectiveAvatarUrl())
					.setFooter("Powered by youtubeinmp3.com API", null);

				try {
					int length = Integer.parseInt(info.length);
					builder.addField("Length",
						String.format(
							"%02d minutes, %02d seconds",
							SECONDS.toMinutes(length),
							length - MINUTES.toSeconds(SECONDS.toMinutes(length))
						),
						false
					);
				} catch (Exception ignored) {
				}

				event.getChannel().sendMessage(builder
					.addField("Download Link", "[Click Here!](" + info.link + ")", false)
					.build()
				).queue();
				TextChannelGround.of(event).dropItemWithChance(7, 5);
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Youtube MP3 command")
					.setDescription("Gives you a link to a MP3 download of almost any youtube video you want.")
					.addField("Usage", "~>ytmp3 <youtube link>", true)
					.addField("Parameters", "youtube link: The link of the video to translate to MP3", true)
					.build();
			}
		});
	}
}
