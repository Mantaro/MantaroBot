/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

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
import net.kodehawa.mantarobot.commands.utils.GoogleCrawler;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.utils.Reminder;
import net.kodehawa.mantarobot.commands.utils.UrbanData;
import net.kodehawa.mantarobot.commands.utils.WeatherData;
import net.kodehawa.mantarobot.commands.utils.YoutubeMp3Info;
import net.kodehawa.mantarobot.commands.utils.birthday.BirthdayCacher;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
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
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static br.com.brjdevs.java.utils.collections.CollectionUtils.random;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Module
public class UtilsCmds {
    private static Pattern timePattern = Pattern.compile(" -time [(\\d+)((?:h(?:our(?:s)?)?)|(?:m(?:in(?:ute(?:s)?)?)?)|(?:s(?:ec(?:ond(?:s)?)?)?))]+");

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

    @Subscribe
    public void birthday(CommandRegistry registry) {
        registry.register("birthday", new SimpleCommand(Category.UTILS) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                SimpleDateFormat format1 = new SimpleDateFormat("dd-MM-yyyy");
                DBUser user = MantaroData.db().getUser(event.getAuthor());
                if(content.isEmpty()) {
                    onError(event);
                    return;
                }

                if(content.startsWith("remove")) {
                    user.getData().setBirthday(null);
                    user.save();
                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Correctly reset birthday date.")
                            .queue();
                    return;
                }

                if(content.startsWith("month")) {
                    BirthdayCacher cacher = MantaroBot.getInstance().getBirthdayCacher();

                    try {
                        if(cacher != null) {
                            if(cacher.cachedBirthdays.isEmpty()) {
                                event.getChannel().sendMessage(EmoteReference.SAD + "Things seems a bit empty here...").queue();
                                return;
                            }

                            List<String> ids = event.getGuild().getMemberCache().stream().map(m -> m.getUser().getId()).collect(Collectors.toList());
                            Map<String, String> guildCurrentBirthdays = new HashMap<>();
                            Calendar calendar = Calendar.getInstance();
                            String currentMonth = 0 + String.valueOf(calendar.get(Calendar.MONTH) + 1);

                            for(Map.Entry<String, String> birthdays : cacher.cachedBirthdays.entrySet()) {
                                if(ids.contains(birthdays.getKey()) && birthdays.getValue().split("-")[1].equals(currentMonth)) {
                                    guildCurrentBirthdays.put(birthdays.getKey(), birthdays.getValue());
                                }
                            }

                            if(guildCurrentBirthdays.isEmpty()) {
                                event.getChannel().sendMessage(EmoteReference.ERROR + "There are no birthdays for this month here :(").queue();
                                return;
                            }

                            String birthdays = guildCurrentBirthdays.entrySet().stream()
                                    .sorted(Comparator.comparingInt(entry -> Integer.parseInt(entry.getValue().split("-")[0])))
                                    .limit(10)
                                    .map((entry) -> String.format("+ %-20s : %s ", event.getGuild().getMemberById(entry.getKey()).getEffectiveName(), entry.getValue()))
                                    .collect(Collectors.joining("\n"));

                            event.getChannel().sendMessage(new MessageBuilder()
                                    .append("Birthdays for ")
                                    .append(Utils.capitalize(calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH)))
                                    .append(" in Guild: **")
                                    .append(event.getGuild().getName())
                                    .append("**\n")
                                    .appendCodeBlock(birthdays, "diff").build()).queue();
                        } else {
                            event.getChannel().sendMessage(EmoteReference.SAD + "Birthday cacher doesn't seem to be running :(").queue();
                        }
                    } catch (Exception e) {
                        event.getChannel().sendMessage(EmoteReference.SAD + "Something went wrong while getting birthdays :(").queue();
                    }

                    return;
                }

                Date bd1;
                try {
                    String bd;
                    bd = content.replace("/", "-");
                    String[] parts = bd.split("-");
                    if(Integer.parseInt(parts[0]) > 31 || Integer.parseInt(parts[1]) > 12 || Integer.parseInt(
                            parts[2]) > 3000) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Not a valid date.").queue();
                        return;
                    }

                    bd1 = format1.parse(bd);
                } catch(Exception e) {
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
                        .addField("Tip", "To remove your birthday date do ~>birthday remove", false)
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
                if(args.length < 1) {
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
                if(args.length == 0) {
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

                    if(result.has("part_of_speech")) part_of_speech = result.getString("part_of_speech");
                    else part_of_speech = "Not found.";

                    if(senses.getJSONObject(0).get("definition") instanceof JSONArray)
                        definition = senses.getJSONObject(0).getJSONArray("definition").getString(0);
                    else
                        definition = senses.getJSONObject(0).getString("definition");

                    try {
                        if(senses.getJSONObject(0).getJSONArray("translations").getJSONObject(0).get(
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
                    } catch(Exception e) {
                        example = "Not found";
                    }

                } catch(Exception e) {
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

                if(args[0].equals("list") || args[0].equals("ls")) {
                    List<Reminder> reminders = Reminder.CURRENT_REMINDERS.get(event.getAuthor().getId());

                    if(reminders == null || reminders.isEmpty()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You have no reminders set!").queue();
                        return;
                    }

                    StringBuilder builder = new StringBuilder();
                    AtomicInteger i = new AtomicInteger();
                    for(Reminder r : reminders) {
                        builder.append("**").append(i.incrementAndGet()).append(".-**").append("R: *").append(r.reminder).append("*, Due in: **")
                                .append(Utils.getHumanizedTime(r.time - System.currentTimeMillis())).append("**").append("\n");
                    }

                    Queue<Message> toSend = new MessageBuilder().append(builder.toString()).buildAll(MessageBuilder.SplitPolicy.NEWLINE);
                    toSend.forEach(message -> event.getChannel().sendMessage(message).queue());

                    return;
                }


                if(args[0].equals("cancel")) {
                    try {
                        List<Reminder> reminders = Reminder.CURRENT_REMINDERS.get(event.getAuthor().getId());

                        if(reminders.isEmpty()) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "You have no reminders set!").queue();
                            return;
                        }

                        if(reminders.size() == 1) {
                            reminders.get(0).cancel();
                            event.getChannel().sendMessage(EmoteReference.CORRECT + "Cancelled your reminder.").queue();
                        } else {
                            DiscordUtils.selectList(event, reminders,
                                    (r) -> String.format("%s, Due in: %s", r.reminder, Utils.getHumanizedTime(r.time - System.currentTimeMillis())),
                                    r1 -> new EmbedBuilder().setColor(Color.CYAN).setTitle("Select the reminder you want to cancel.", null)
                                            .setDescription(r1)
                                            .setFooter("This timeouts in 10 seconds.", null).build(),
                                    sr -> {
                                        sr.cancel();
                                        event.getChannel().sendMessage(EmoteReference.CORRECT + "Cancelled your reminder").queue();
                                    });
                        }
                    } catch (Exception e) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You have no reminders set!").queue();
                    }

                    return;
                }


                Map<String, Optional<String>> t = StringUtils.parse(args);

                if(!t.containsKey("time")) {
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

                if(System.currentTimeMillis() + time > System.currentTimeMillis() + TimeUnit.DAYS.toMillis(90)) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Too long (Maximum: 3 months)...").queue();
                    return;
                }

                event.getChannel().sendMessage(EmoteReference.CORRECT + "I'll remind you of **" + toRemind + "**" +
                        " in " + Utils.getHumanizedTime(time)).queue();

                new Reminder.Builder()
                        .id(user.getId())
                        .reminder(toRemind)
                        .current(System.currentTimeMillis())
                        .time(time + System.currentTimeMillis())
                        .build()
                        .schedule(); //automatic
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Remind me")
                        .setDescription("**Reminds you of something**")
                        .addField("Usage", "`~>remindme do the laundry -time 1h20m`\n" +
                                "`~>remindme cancel` to cancel a reminder." +
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
                List<GoogleCrawler.SearchResult> result = GoogleCrawler.get(content);
                for(int i = 0; i < 5 && i < result.size(); i++) {
                    GoogleCrawler.SearchResult data = result.get(i);
                    if(data != null) {
                        String title = data.getTitle();
                        String desc = data.getDescription();
                        if(title.length() > 40) title = title.substring(0, 40) + "...";
                        if(title.length() > 250) title = desc.substring(0, 250) + "...";
                        b.append(i + 1)
                                .append(". **[")
                                .append(title)
                                .append("](")
                                .append(data.getUrl())
                                .append(")**\n")
                                .append("- ")
                                .append(desc)
                                .append("\n\n");
                    }
                }

                event.getChannel().sendMessage(
                        builder.setTitle("Search results for " + content)
                                .setDescription(b.toString())
                                .setThumbnail(
                                        "https://cdn.pixabay.com/photo/2015/12/08/17/38/magnifying-glass-1083373_960_720.png")
                                .setFooter("Click on the blue text to go to the URL.", null)
                                .setColor(Color.pink)
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
                    if(user.getData().getTimezone() != null && args.length == 0) {
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

                } catch(Exception e) {
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

                if(!content.isEmpty()) {
                    long start = System.currentTimeMillis();
                    String url = null;
                    try {
                        url = "http://api.urbandictionary.com/v0/define?term=" + URLEncoder.encode(
                                beheadedSplit[0], "UTF-8");
                    } catch(UnsupportedEncodingException ignored) {
                    }
                    String json = Utils.wgetResty(url, event);
                    UrbanData data = GsonDataManager.GSON_PRETTY.fromJson(json, UrbanData.class);

                    long end = System.currentTimeMillis() - start;
                    //This shouldn't happen, but it fucking happened.
                    if(beheadedSplit.length < 1) {
                        return;
                    }

                    if(data == null || data.list.isEmpty()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "No results.").queue();
                        return;
                    }

                    switch(beheadedSplit.length) {
                        case 1:
                            embed.setAuthor(
                                    "Urban Dictionary definition for " + content, data.list.get(0).permalink, null)
                                    .setDescription("Main definition.")
                                    .setThumbnail("https://everythingfat.files.wordpress.com/2013/01/ud-logo.jpg")
                                    .setColor(Color.GREEN)
                                    .addField("Definition", data.list.get(0).definition.length() > 1000 ?
                                            data.list.get(0).definition.substring(0, 1000) + "..." : data.list.get(0).definition, false)
                                    .addField("Example", data.list.get(0).example.length() > 1000 ?
                                            data.list.get(0).example.substring(0, 1000) + "..." : data.list.get(0).example, false)
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
                                    .addField("Example", data.list.get(defn).example.length() > 1000 ? data.list.get(defn).example.substring(0, 1000) + "..." :
                                            data.list.get(defn).example, false)
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
                if(content.isEmpty()) {
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
                } catch(Exception e) {
                    event.getChannel().sendMessage("Error while fetching results.").queue();
                    if(!(e instanceof NullPointerException))
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
    public void wiki(CommandRegistry registry) {
        registry.register("wiki", new TreeCommand(Category.UTILS) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, String content) {
                        event.getChannel().sendMessage(EmoteReference.OK + "**For Mantaro's documentation please visit:** https://github.com/Mantaro/MantaroBot/wiki/Home").queue();
                    }
                };
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Wiki command")
                        .setDescription("**Shows a bunch of things related to mantaro's wiki.**\n" +
                                "Avaliable subcommands: `opts`, `custom`, `faq`, `commands`, `modifiers` `tos` `usermessage` `premium`")
                        .build();
            }
        }.addSubCommand("opts", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content) {
                event.getChannel().sendMessage(EmoteReference.OK + "**For Mantaro's documentation on `~>opts` and general bot options " +
                        "please visit:** https://github.com/Mantaro/MantaroBot/wiki/Configuration").queue();
            }
        }).addSubCommand("custom", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content) {
                event.getChannel().sendMessage(EmoteReference.OK + "**For Mantaro's documentation on custom commands please visit:** " +
                        "https://github.com/Mantaro/MantaroBot/wiki/Custom-Commands").queue();
            }
        }).addSubCommand("modifiers", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content) {
                event.getChannel().sendMessage(EmoteReference.OK + "**For Mantaro's documentation in custom commands modifiers please visit:** " +
                        "https://github.com/Mantaro/MantaroBot/wiki/Custom-Command-Modifiers").queue();
            }
        }).addSubCommand("commands", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content) {
                event.getChannel().sendMessage(EmoteReference.OK + "**For Mantaro's documentation on commands and usage please visit:**" +
                        " https://github.com/Mantaro/MantaroBot/wiki/Command-reference-and-documentation").queue();
            }
        }).addSubCommand("faq", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content) {
                event.getChannel().sendMessage(EmoteReference.OK + "**For Mantaro's FAQ please visit:**" +
                        " https://github.com/Mantaro/MantaroBot/wiki/FAQ").queue();
            }
        }).addSubCommand("badges", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content) {
                event.getChannel().sendMessage(EmoteReference.OK + "**For Mantaro's badge documentation please visit:**" +
                        " https://github.com/Mantaro/MantaroBot/wiki/Badge-reference-and-documentation").queue();
            }
        }).addSubCommand("tos", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content) {
                event.getChannel().sendMessage(EmoteReference.OK + "**For Mantaro's ToS please visit:**" +
                        " https://github.com/Mantaro/MantaroBot/wiki/Terms-of-Service").queue();
            }
        }).addSubCommand("usermessage", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content) {
                event.getChannel().sendMessage(EmoteReference.OK + "**For Mantaro's Welcome and Leave message tutorial please visit:**" +
                        " https://github.com/Mantaro/MantaroBot/wiki/Welcome-and-Leave-Messages-tutorial").queue();
            }
        }).addSubCommand("premium", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content) {
                event.getChannel().sendMessage(EmoteReference.OK + "**To see what Mantaro's Premium features offer please visit:**" +
                        " https://github.com/Mantaro/MantaroBot/wiki/Premium-Perks").queue();
            }
        }));
    }

    @Subscribe
    public void ytmp3(CommandRegistry registry) {
        registry.register("ytmp3", new SimpleCommand(Category.UTILS) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                YoutubeMp3Info info = YoutubeMp3Info.forLink(content);

                if(info == null) {
                    event.getChannel().sendMessage(":heavy_multiplication_x: Your link seems to be invalid.").queue();
                    return;
                }

                if(info.error != null) {
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
                } catch(Exception ignored) {
                }

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
}
