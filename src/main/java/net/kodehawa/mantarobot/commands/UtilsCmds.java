package net.kodehawa.mantarobot.commands;

import br.com.brjdevs.java.utils.texts.StringUtils;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.lib.google.Crawler;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.utils.Reminder;
import net.kodehawa.mantarobot.commands.utils.UrbanData;
import net.kodehawa.mantarobot.commands.utils.WeatherData;
import net.kodehawa.mantarobot.commands.utils.YoutubeMp3Info;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.PremiumKey;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static br.com.brjdevs.java.utils.collections.CollectionUtils.random;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Module
public class UtilsCmds {

	private static Pattern timePattern = Pattern.compile(" -time [(\\d+)((?:h(?:our(?:s)?)?)|(?:m(?:in(?:ute(?:s)?)?)?)|(?:s(?:ec(?:ond(?:s)?)?)?))]+");

	@Subscribe
	public void birthday(CommandRegistry registry) {
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
					event.getChannel().sendMessage(EmoteReference.CORRECT + "Correctly reset birthday date.")
						.queue();
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
							"valid date or not parseable. Please try with the correct formatting. Remember to include the year, although you can put any year and it won't affect anything.")
						.queue()));
					return;
				}

				user.getData().setBirthday(format1.format(bd1));
				user.save();
				event.getChannel().sendMessage(EmoteReference.CORRECT + "Added birthdate.").queue();
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

	@Subscribe
	public void choose(CommandRegistry registry) {
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

	@Subscribe
	public void dictionary(CommandRegistry registry) {
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
					.setThumbnail("https://upload.wikimedia.org/wikipedia/commons/thumb/5/5a/Wikt_dynamic_dictionary_logo.svg/1000px-Wikt_dynamic_dictionary_logo.svg.png")
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

	@Subscribe
	public void remindme(CommandRegistry registry) {
		registry.register("remindme", new SimpleCommand(Category.UTILS) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if(content.isEmpty()) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "What could I remind you of if you don't give me what to remind you? " +
						"Oh! Lemme remind you of setting a reminder!").queue();
					return;
				}

				if(args[0].equals("list") || args[0].equals("ls")){
					List<Reminder> reminders = Reminder.CURRENT_REMINDERS.get(event.getAuthor().getId());

					if(reminders.isEmpty()){
						event.getChannel().sendMessage(EmoteReference.ERROR + "You have no reminders set!").queue();
						return;
					}

					StringBuilder builder = new StringBuilder();
					AtomicInteger i = new AtomicInteger();
					for(Reminder r : reminders){
						builder.append("**").append(i.incrementAndGet()).append(".-**").append("R: *").append(r.reminder).append("*, Due in: **")
								.append(Utils.getReadableTime(r.time - currentTimeMillis())).append("**").append("\n");
					}

					Queue<Message> toSend = new MessageBuilder().append(builder.toString()).buildAll(MessageBuilder.SplitPolicy.NEWLINE);
					toSend.forEach(message -> event.getChannel().sendMessage(message).queue());

					return;
				}


				if(args[0].equals("cancel")){
					List<Reminder> reminders = Reminder.CURRENT_REMINDERS.get(event.getAuthor().getId());

					if(reminders.isEmpty()){
						event.getChannel().sendMessage(EmoteReference.ERROR + "You have no reminders set!").queue();
						return;
					}

					if(reminders.size() == 1){
						reminders.get(0).cancel();
						event.getChannel().sendMessage(EmoteReference.CORRECT + "Cancelled your reminder.").queue();
					} else {
						DiscordUtils.selectList(event, reminders,
								(r) -> String.format("%s, Due in: %s", r.reminder, Utils.getShortReadableTime(r.time - currentTimeMillis())),
								r1 -> new EmbedBuilder().setColor(Color.CYAN).setTitle("Select the reminder you want to cancel.", null)
										.setDescription(r1)
										.setFooter("This timeouts in 10 seconds.", null).build(),
								sr -> {
									sr.cancel();
									event.getChannel().sendMessage(EmoteReference.CORRECT + "Cancelled your reminder").queue();
								});
					}

					return;
				}


				Map<String, Optional<String>> t = StringUtils.parse(args);

				if(!t.containsKey("time")){
					event.getChannel().sendMessage(EmoteReference.ERROR + "You didn't give me a `-time` argument! (Example: `-time 1h`)").queue();
					return;
				}

				if(!t.get("time").isPresent()) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You didn't give me a `-time` argument! (Example: `-time 1h`)").queue();
					return;
				}

				String toRemind = timePattern.matcher(content).replaceAll("");
				User user = event.getAuthor();
				long time = Utils.parseTime(t.get("time").get());

				if(time < 10000) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "That's too little time!").queue();
					return;
				}

				event.getChannel().sendMessage(EmoteReference.CORRECT + "I'll remind you of **" + toRemind + "**" +
						" in " + Utils.getVerboseTime(time)).queue();

				new Reminder.Builder()
						.id(user.getId())
						.reminder(toRemind)
						.current(currentTimeMillis())
						.time(time + currentTimeMillis())
						.build()
						.schedule();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Remind me")
						.setDescription("**Reminds you of something**")
						.addField("Usage", "`~>remindme do the laundry -time 1h20m`" +
								"\nTime is in this format: 1h20m (1 hour and 20m). You can use h, m and s (hour, minute, second)", false)
						.build();
			}
		});
	}

	@Subscribe
	public void google(CommandRegistry registry) {
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

	@Subscribe
	public void time(CommandRegistry registry) {
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

	@Subscribe
	public void urban(CommandRegistry registry) {
		registry.register("urban", new SimpleCommand(Category.UTILS) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				String beheadedSplit[] = content.split("->");
				EmbedBuilder embed = new EmbedBuilder();

				if (!content.isEmpty()) {
					long start = currentTimeMillis();
					String url = null;
					try {
						url = "http://api.urbandictionary.com/v0/define?term=" + URLEncoder.encode(
							beheadedSplit[0], "UTF-8");
					} catch (UnsupportedEncodingException ignored) {}
					String json = Utils.wgetResty(url, event);
					UrbanData data = GsonDataManager.GSON_PRETTY.fromJson(json, UrbanData.class);

					long end = currentTimeMillis() - start;
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

	@Subscribe
	public void weather(CommandRegistry registry) {
		registry.register("weather", new SimpleCommand(Category.UTILS) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (content.isEmpty()) {
					onError(event);
					return;
				}

				EmbedBuilder embed = new EmbedBuilder();
				try {
					long start = currentTimeMillis();
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
					long end = currentTimeMillis() - start;

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

	@Subscribe
	public void ytmp3(CommandRegistry registry) {
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
						":heavy_multiplication_x: I got an error while fetching that link. ``" + info.error
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

	@Subscribe
	public void comprevip(CommandRegistry cr){
		cr.register("activatekey", new SimpleCommand(Category.UTILS) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {

				if(!(args.length == 0) && args[0].equalsIgnoreCase("check")){
					PremiumKey currentKey = MantaroData.db().getPremiumKey(MantaroData.db().getUser(event.getAuthor()).getData().getPremiumKey());

					if(currentKey != null && currentKey.isEnabled() && currentTimeMillis() < currentKey.getExpiration()){ //Should always be enabled...
						event.getChannel().sendMessage(EmoteReference.EYES + "Your key is valid for " + currentKey.validFor() + " more days :heart:").queue();
					} else {
						event.getChannel().sendMessage(EmoteReference.ERROR + "You don't have a key enabled... If you're a premium from the old system you should " +
								"still have your rewards, though!").queue();
					}
					return;
				}

				if(args.length < 2){
					event.getChannel().sendMessage(EmoteReference.ERROR + "I cannot enable a premium key if you don't give me one or if you don't give me the scope!").queue();
					return;
				}

				PremiumKey key = MantaroData.db().getPremiumKey(args[0]);

				if(key == null || (key.isEnabled() && !(key.getParsedType().equals(PremiumKey.Type.MASTER)))){
					event.getChannel().sendMessage(EmoteReference.ERROR + "You provided an invalid or already enabled key!").queue();
					return;
				}

				String scope = args[1];
				PremiumKey.Type scopeParsed = null;
				try{
					scopeParsed = PremiumKey.Type.valueOf(scope.toUpperCase()); //To get the ordinal
				} catch (IllegalArgumentException ignored){}

				if(scopeParsed == null){
					event.getChannel().sendMessage(EmoteReference.ERROR + "Invalid scope (Valid ones are: `user` or `guild`)").queue();
					return;
				}

				if(!key.getParsedType().equals(scopeParsed)){
					event.getChannel().sendMessage(EmoteReference.ERROR + "This key is for another scope (Scope: " + key.getParsedType() + ")").queue();
					return;
				}

				if (scopeParsed.equals(PremiumKey.Type.GUILD)) {
					DBGuild guild = MantaroData.db().getGuild(event.getGuild());

					PremiumKey currentKey = MantaroData.db().getPremiumKey(guild.getData().getPremiumKey());

					if(currentKey != null && currentKey.isEnabled() && currentTimeMillis() < key.getExpiration()){ //Should always be enabled...
						event.getChannel().sendMessage(EmoteReference.ERROR + "This server already has a premium subscription!").queue();
						return;
					}

					key.activate(180);
					event.getChannel().sendMessage(EmoteReference.POPPER + "This server is now **Premium** :heart: (For: " + key.getDurationDays() + " days)").queue();
					guild.getData().setPremiumKey(key.getId());
					guild.save();
					return;
				}

				if(scopeParsed.equals(PremiumKey.Type.USER)){
					DBUser user = MantaroData.db().getUser(event.getAuthor());

					PremiumKey currentUserKey = MantaroData.db().getPremiumKey(user.getData().getPremiumKey());
					if(currentUserKey != null && currentUserKey.isEnabled() && currentTimeMillis() < key.getExpiration()){ //Should always be enabled...
						event.getChannel().sendMessage(EmoteReference.ERROR + "You're already premium :heart:!").queue();
						return;
					}

					key.activate(event.getAuthor().getId().equals(key.getOwner()) ? 365 : 180);
					event.getChannel().sendMessage(EmoteReference.POPPER + "You're now **Premium** :heart: (For: " + key.getDurationDays() + " days)").queue();
					user.getData().setPremiumKey(key.getId());
					user.save();
					return;
				}

				event.getChannel().sendMessage(EmoteReference.ERROR + "This shouldn't happen...").queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Premium Key Actvation")
						.setDescription("Activates a premium key!\n" +
								"Example: `~>activatekey a4e98f07-1a32-4dcc-b53f-c540214d54ec user` or `~>activatekey 550e8400-e29b-41d4-a716-446655440000 guild`\n" +
								"No, those aren't valid keys.")
						.build();
			}
		});
	}

	protected static String dateGMT(Guild guild, String tz) {
		DateFormat format = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
		Date date = new Date();

		DBGuild dbGuild = MantaroData.db().getGuild(guild.getId());
		GuildData guildData = dbGuild.getData();

		if(guildData.getTimeDisplay() == 1) {
			format = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a");
		}

		format.setTimeZone(TimeZone.getTimeZone(tz));
		return format.format(date);
	}
}
