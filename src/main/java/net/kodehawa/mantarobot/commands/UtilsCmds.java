/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.utils.Reminder;
import net.kodehawa.mantarobot.commands.utils.UrbanData;
import net.kodehawa.mantarobot.commands.utils.WeatherData;
import net.kodehawa.mantarobot.commands.utils.birthday.BirthdayCacher;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.ITreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
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

@Slf4j
@Module
@SuppressWarnings("unused")
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
        TreeCommand birthdayCommand = (TreeCommand) registry.register("birthday", new TreeCommand(Category.UTILS) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                        if(content.isEmpty()) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.birthday.no_content"), EmoteReference.ERROR).queue();
                            return;
                        }

                        String[] args = StringUtils.splitArgs(content, 0);
                        SimpleDateFormat format1 = new SimpleDateFormat("dd-MM-yyyy");
                        DBUser user = MantaroData.db().getUser(event.getAuthor());
                        Date bd1;
                        try {
                            String bd;
                            bd = content.replace("/", "-");
                            String[] parts = bd.split("-");
                            if(Integer.parseInt(parts[0]) > 31 || Integer.parseInt(parts[1]) > 12 || Integer.parseInt(parts[2]) > 3000) {
                                event.getChannel().sendMessageFormat(languageContext.get("commands.birthday.invalid_date"), EmoteReference.ERROR).queue();
                                return;
                            }

                            bd1 = format1.parse(bd);
                        } catch(Exception e) {
                            Optional.ofNullable(args[0]).ifPresent((s -> event.getChannel().sendMessageFormat(languageContext.get("commands.birthday.error_date"), "\u274C", args[0]).queue()));
                            return;
                        }

                        String birthdayFormat = format1.format(bd1);
                        user.getData().setBirthday(birthdayFormat);
                        user.save();
                        event.getChannel().sendMessageFormat(languageContext.get("commands.birthday.added_birthdate"), EmoteReference.CORRECT, birthdayFormat).queue();
                    }
                };
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

        birthdayCommand.addSubCommand("remove", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                DBUser user = MantaroData.db().getUser(event.getAuthor());
                user.getData().setBirthday(null);
                user.save();
                event.getChannel().sendMessageFormat(languageContext.get("commands.birthday.reset"), EmoteReference.CORRECT).queue();
            }
        });

        birthdayCommand.addSubCommand("month", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                String[] args = StringUtils.splitArgs(content, 0);
                BirthdayCacher cacher = MantaroBot.getInstance().getBirthdayCacher();
                Calendar calendar = Calendar.getInstance();
                int month = calendar.get(Calendar.MONTH);

                String m1 = "";
                if(args.length == 1)
                    m1 = args[0];

                if(!m1.isEmpty()) {
                    try {
                        month = Integer.parseInt(m1);
                        if(month < 1 || month > 12) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.birthday.invalid_month"), EmoteReference.ERROR).queue();
                            return;
                        }

                        //Substract here so we can do the check properly up there.
                        month = month - 1;
                    } catch (NumberFormatException e) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.birthday.invalid_month"), EmoteReference.ERROR).queue();
                        return;
                    }
                }

                //Inspection excluded below not needed, I'm passing a proper value.
                //noinspection MagicConstant
                calendar.set(calendar.get(Calendar.YEAR), month, calendar.get(Calendar.DAY_OF_MONTH));

                try {
                    //Why would this happen is out of my understanding.
                    if(cacher != null) {
                        //same as above unless testing?
                        if(cacher.cachedBirthdays.isEmpty()) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.birthday.no_global_birthdays"), EmoteReference.SAD).queue();
                            return;
                        }

                        //O(1) lookups. Probably.
                        HashSet<String> ids = event.getGuild().getMemberCache().stream().map(m -> m.getUser().getId()).collect(Collectors.toCollection(HashSet::new));
                        Map<String, BirthdayCacher.BirthdayData> guildCurrentBirthdays = new HashMap<>();

                        //Try not to die. I mean get calendar month and sum 1.
                        String calendarMonth = String.valueOf(calendar.get(Calendar.MONTH) + 1);
                        String currentMonth = (calendarMonth.length() == 1 ? 0 : "") + calendarMonth;

                        //~100k repetitions rip
                        for(Map.Entry<String, BirthdayCacher.BirthdayData> birthdays : cacher.cachedBirthdays.entrySet()) {
                            //Why was the birthday saved on this outdated format again?
                            //Check if this guild contains x user and that the month matches.
                            if(ids.contains(birthdays.getKey()) && birthdays.getValue().month.equals(currentMonth)) {
                                //Insert into current month bds.
                                guildCurrentBirthdays.put(birthdays.getKey(), birthdays.getValue());
                            }
                        }

                        //No birthdays to be seen here? (This month)
                        if(guildCurrentBirthdays.isEmpty()) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.birthday.no_guild_month_birthdays"), EmoteReference.ERROR, month + 1, EmoteReference.BLUE_SMALL_MARKER).queue();
                            return;
                        }

                        //Build the message.
                        String birthdays = guildCurrentBirthdays.entrySet().stream()
                                .sorted(Comparator.comparingInt(i -> Integer.parseInt(i.getValue().day)))
                                .map((entry) -> String.format("+ %-20s : %s ", event.getGuild().getMemberById(entry.getKey()).getEffectiveName(), entry.getValue().getBirthday()))
                                .collect(Collectors.joining("\n"));

                        List<String> parts = DiscordUtils.divideString(1000, birthdays);
                        boolean hasReactionPerms = event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION);

                        List<String> messages = new LinkedList<>();
                        for(String s1 : parts) {
                            messages.add(String.format(languageContext.get("commands.birthday.header"), event.getGuild().getName(),
                                    Utils.capitalize(calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH))) +
                                    (parts.size() > 1 ? (hasReactionPerms ? languageContext.get("general.arrow_react") :  languageContext.get("general.text_menu")) : "") +
                                    String.format("```diff\n%s```", s1));
                        }

                        //Show the message.
                        //Probably a p big one tbh.
                        if(hasReactionPerms)
                            DiscordUtils.list(event, 45, false, messages);
                        else
                            DiscordUtils.listText(event, 45, false, messages);
                    } else {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.birthday.cache_not_running"), EmoteReference.SAD).queue();
                    }
                } catch(Exception e) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.birthday.error"), EmoteReference.SAD).queue();
                    log.error("Error on birthday month display!", e);
                }
            }
        });
    }

    @Subscribe
    public void choose(CommandRegistry registry) {
        registry.register("choose", new SimpleCommand(Category.UTILS) {
            @Override
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(args.length < 1) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.choose.nothing_to"), EmoteReference.ERROR).queue();
                    return;
                }

                String send = Utils.DISCORD_INVITE.matcher(random(args)).replaceAll("-inv link-");
                send = Utils.DISCORD_INVITE_2.matcher(send).replaceAll("-inv link-");

                new MessageBuilder().setContent(String.format(languageContext.get("commands.choose.success"), EmoteReference.EYES, send))
                        .stripMentions(event.getGuild(), Message.MentionType.HERE, Message.MentionType.EVERYONE, Message.MentionType.USER)
                        .sendTo(event.getChannel())
                        .queue();
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
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(args.length == 0) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.dictionary.no_word"), EmoteReference.ERROR).queue();
                    return;
                }

                String word = content;

                JSONObject main;
                String definition, part_of_speech, headword, example;

                try {
                    main = new JSONObject(Utils.wgetResty("http://api.pearson.com/v2/dictionaries/laes/entries?headword=" + word));
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
                        example = languageContext.get("general.not_found");
                    }

                } catch(Exception e) {
                    event.getChannel().sendMessageFormat(languageContext.get("general.no_results"), EmoteReference.ERROR).queue();
                    return;
                }

                EmbedBuilder eb = new EmbedBuilder();
                eb.setAuthor("Definition for " + word, null, event.getAuthor().getAvatarUrl())
                        .setThumbnail("https://upload.wikimedia.org/wikipedia/commons/thumb/5/5a/Wikt_dynamic_dictionary_logo.svg/1000px-Wikt_dynamic_dictionary_logo.svg.png")
                        .addField(languageContext.get("general.definition"), "**" + definition + "**", false)
                        .addField(languageContext.get("general.example"), "**" + example + "**", false)
                        .setDescription(
                                String.format(languageContext.get("commands.dictionary.description"), part_of_speech, headword));

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
        ITreeCommand remindme = (ITreeCommand) registry.register("remindme", new TreeCommand(Category.UTILS) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                        Map<String, Optional<String>> t = StringUtils.parse(StringUtils.splitArgs(content, 0));

                        if(!t.containsKey("time")) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.remindme.no_time"), EmoteReference.ERROR).queue();
                            return;
                        }

                        if(!t.get("time").isPresent()) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.remindme.no_time"), EmoteReference.ERROR).queue();
                            return;
                        }

                        String toRemind = timePattern.matcher(content).replaceAll("");
                        User user = event.getAuthor();
                        long time = Utils.parseTime(t.get("time").get());

                        if(time < 10000) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.remindme.too_little_time"), EmoteReference.ERROR).queue();
                            return;
                        }

                        if(System.currentTimeMillis() + time > System.currentTimeMillis() + TimeUnit.DAYS.toMillis(90)) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.remindme.too_long"), EmoteReference.ERROR).queue();
                            return;
                        }

                        //lol
                        String displayRemind = Utils.DISCORD_INVITE.matcher(toRemind).replaceAll("discord invite link");
                        displayRemind = Utils.DISCORD_INVITE_2.matcher(displayRemind).replaceAll("discord invite link");

                        new MessageBuilder().append(String.format(languageContext.get("commands.remindme.success"), EmoteReference.CORRECT, event.getAuthor().getName(),
                                    event.getAuthor().getDiscriminator(), displayRemind, Utils.getHumanizedTime(time)))
                                .stripMentions(event.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.ROLE, Message.MentionType.HERE)
                                .sendTo(event.getChannel()).queue();

                        //TODO save to db
                        new Reminder.Builder()
                                .id(user.getId())
                                .guild(event.getGuild().getId())
                                .reminder(toRemind)
                                .current(System.currentTimeMillis())
                                .time(time + System.currentTimeMillis())
                                .build()
                                .schedule(); //automatic
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Reminds you of something.")
                        .setUsage("`~>remindme <reminder> <-time>`\n" +
                                "Check subcommands for more. Append the subcommand after the main command.")
                        .addParameter("reminder", "What to remind you of.")
                        .addParameter("-time", "How much time until I remind you of it. Time is in this format: 1h20m (1 hour and 20m). You can use h, m and s (hour, minute, second)")
                        .build();
            }
        });

        remindme.addSubCommand("list", new SubCommand() {
            @Override
            public String description() {
                return "Lists current reminders.";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                List<Reminder> reminders = Reminder.CURRENT_REMINDERS.get(event.getAuthor().getId());

                if(reminders == null || reminders.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.remindme.no_reminders"), EmoteReference.ERROR).queue();
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
            }
        }).createSubCommandAlias("list", "ls");

        remindme.addSubCommand("cancel", new SubCommand() {
            @Override
            public String description() {
                return "Cancel a reminder. You'll be given a list if you have more than one.";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                try {
                    List<Reminder> reminders = Reminder.CURRENT_REMINDERS.get(event.getAuthor().getId());

                    if(reminders.isEmpty()) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.remindme.no_reminders"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if(reminders.size() == 1) {
                        reminders.get(0).cancel();
                        event.getChannel().sendMessageFormat(languageContext.get("commands.remindme.cancel.success"), EmoteReference.CORRECT).queue();
                    } else {
                        DiscordUtils.selectList(event, reminders,
                                (r) -> String.format("%s, Due in: %s", r.reminder, Utils.getHumanizedTime(r.time - System.currentTimeMillis())),
                                r1 -> new EmbedBuilder().setColor(Color.CYAN).setTitle(languageContext.get("commands.remindme.cancel.select"), null)
                                        .setDescription(r1)
                                        .setFooter(String.format(languageContext.get("general.timeout"), 10), null).build(),
                                sr -> {
                                    sr.cancel();
                                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Cancelled your reminder").queue();
                                });
                    }
                } catch(Exception e) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.remindme.no_reminders"), EmoteReference.ERROR).queue();
                }
            }
        });
    }

    @Subscribe
    public void time(CommandRegistry registry) {
        registry.register("time", new SimpleCommand(Category.UTILS) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                try {
                    content = content.replace("UTC", "GMT").toUpperCase();
                    DBUser user = MantaroData.db().getUser(event.getMember());
                    String timezone = user.getData().getTimezone() != null ? (content.isEmpty() ? user.getData().getTimezone() : content) : content;

                    if(!Utils.isValidTimeZone(timezone)) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.time.invalid_timezone"), EmoteReference.ERROR).queue();
                        return;
                    }

                    event.getChannel().sendMessage(String.format(languageContext.get("commands.time.success"), EmoteReference.MEGA, dateGMT(event.getGuild(), timezone), timezone)).queue();

                } catch(Exception e) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.time.error"), EmoteReference.ERROR).queue();
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Get the time in a specific timezone (GMT).")
                        .setUsage("`~>time <timezone>`")
                        .addParameter("timezone", "The timezone in GMT or UTC offset (Example: GMT-3)")
                        .build();
            }
        });
    }

    @Subscribe
    public void urban(CommandRegistry registry) {
        registry.register("urban", new SimpleCommand(Category.UTILS) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.urban.no_args"), EmoteReference.ERROR).queue();
                    return;
                }

                if(!event.getChannel().isNSFW()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.urban.nsfw_notice"), EmoteReference.ERROR).queue();
                    return;
                }

                String commandArguments[] = content.split("->");
                EmbedBuilder embed = new EmbedBuilder();

                String url = null;

                try {
                    url = "http://api.urbandictionary.com/v0/define?term=" + URLEncoder.encode(commandArguments[0], "UTF-8");
                } catch(UnsupportedEncodingException ignored) { }

                String json = Utils.wgetResty(url);
                UrbanData data = GsonDataManager.GSON_PRETTY.fromJson(json, UrbanData.class);

                if (commandArguments.length > 2) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.urban.too_many_args"), EmoteReference.ERROR).queue();
                    return;
                }

                if(data == null || data.getList() == null || data.getList().isEmpty()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + languageContext.get("general.no_results")).queue();
                    return;
                }

                if(commandArguments.length > 1) {
                    int definitionNumber = Integer.parseInt(commandArguments[1]) - 1;
                    UrbanData.List urbanData = data.getList().get(definitionNumber);
                    String definition = urbanData.getDefinition();
                    embed.setAuthor(
                            String.format(languageContext.get("commands.urban.header"), commandArguments[0]), urbanData.getPermalink(), null)
                            .setThumbnail("https://everythingfat.files.wordpress.com/2013/01/ud-logo.jpg")
                            .setDescription(languageContext.get("general.definition") + " " + String.valueOf(definitionNumber + 1))
                            .setColor(Color.GREEN)
                            .addField(languageContext.get("general.definition"), definition.length() > 1000 ? definition.substring(0, 1000) + "..." : definition, false)
                            .addField(languageContext.get("general.example"), urbanData.getExample().length() > 1000 ? urbanData.getExample().substring(0, 1000) + "..." : urbanData.getExample(), false)
                            .addField(":thumbsup:", urbanData.thumbs_up, true)
                            .addField(":thumbsdown:", urbanData.thumbs_down, true)
                            .setFooter(languageContext.get("commands.urban.footer"), null);
                    event.getChannel().sendMessage(embed.build()).queue();
                } else {
                    UrbanData.List urbanData = data.getList().get(0);
                    embed.setAuthor(
                            String.format(languageContext.get("commands.urban.header"), content), data.getList().get(0).getPermalink(), null)
                            .setDescription(languageContext.get("commands.urban.main_def"))
                            .setThumbnail("https://everythingfat.files.wordpress.com/2013/01/ud-logo.jpg")
                            .setColor(Color.GREEN)
                            .addField(languageContext.get("general.definition"), urbanData.getDefinition().length() > 1000 ? urbanData.getDefinition().substring(0, 1000) + "..." : urbanData.getDefinition(), false)
                            .addField(languageContext.get("general.example"), urbanData.getExample().length() > 1000 ? urbanData.getExample().substring(0, 1000) + "..." : urbanData.getExample(), false)
                            .addField(":thumbsup:", urbanData.thumbs_up, true)
                            .addField(":thumbsdown:", urbanData.thumbs_down, true)
                            .setFooter(languageContext.get("commands.urban.footer"), null);
                    event.getChannel().sendMessage(embed.build()).queue();
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Retrieves definitions from **Urban Dictionary.")
                        .setUsage("`~>urban <term>-><number>`. Yes, the arrow is needed if you put a number, idk why, I probably liked arrows 2 years ago.")
                        .addParameter("term", "The term to look for")
                        .addParameter("number", "The definition number to show. (Usually tops at around 5)")
                        .build();
            }
        });
    }

    @Subscribe
    public void weather(CommandRegistry registry) {
        registry.register("weather", new SimpleCommand(Category.UTILS) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.weather.no_content"), EmoteReference.ERROR).queue();
                    return;
                }

                EmbedBuilder embed = new EmbedBuilder();
                try {
                    long start = System.currentTimeMillis();
                    WeatherData data = GsonDataManager.GSON_PRETTY.fromJson(
                            Utils.wgetResty(
                                    String.format(
                                            "http://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s",
                                            URLEncoder.encode(content, "UTF-8"),
                                            MantaroData.config().get().weatherAppId
                                    )
                            ),
                            WeatherData.class
                    );

                    String countryCode = data.getSys().country;
                    String status = data.getWeather().get(0).main;
                    Double temp = data.getMain().getTemp();
                    double pressure = data.getMain().getPressure();
                    int humidity = data.getMain().getHumidity();
                    Double ws = data.getWind().speed;
                    int cloudiness = data.getClouds().all;

                    Double finalTemperatureCelsius = temp - 273.15;
                    Double finalTemperatureFahrenheit = temp * 9 / 5 - 459.67;
                    Double finalWindSpeedMetric = ws * 3.6;
                    Double finalWindSpeedImperial = ws / 0.447046;
                    long end = System.currentTimeMillis() - start;

                    embed.setColor(Color.CYAN)
                            .setTitle(String.format(languageContext.get("commands.weather.header"), ":flag_" + countryCode.toLowerCase() + ":", content), null)
                            .setDescription(status + " (" + cloudiness + "% clouds)")
                            .addField(":thermometer: " + languageContext.get("commands.weather.temperature"), String.format("%d°C | %d°F", finalTemperatureCelsius.intValue(), finalTemperatureFahrenheit.intValue()), true)
                            .addField(":droplet: " + languageContext.get("commands.weather.humidity"), humidity + "%", true)
                            .addField(":wind_blowing_face: " + languageContext.get("commands.weather.wind_speed"), String.format("%dkm/h | %dmph", finalWindSpeedMetric.intValue(), finalWindSpeedImperial.intValue()), true)
                            .addField(":wind_chime: " + languageContext.get("commands.weather.pressure"), pressure + "hPA", true)
                            .setFooter(String.format(languageContext.get("commands.weather.footer"), end), null)
                            .setThumbnail("https://cdn2.iconfinder.com/data/icons/lovely-weather-icons/32/Thermometer-50-512.png");
                    event.getChannel().sendMessage(embed.build()).queue();
                } catch (NullPointerException npe) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.weather.error"), EmoteReference.ERROR).queue();
                } catch (Exception e) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.weather.error"), EmoteReference.ERROR).queue();
                    log.warn("Exception caught while trying to fetch weather data, maybe the API changed something?", e);
                }
            }


            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("This command retrieves information from OpenWeatherMap. Used to check forecast information.")
                        .setUsage("`~>weather <city>,<country code>`")
                        .addParameter("city", "The city to look for, example: Concepción")
                        .addParameter("country code", "The country code of the country where the city is located, example: CL")
                        .build();
            }
        });
    }

    //Won't translate
    @Subscribe
    public void wiki(CommandRegistry registry) {
        registry.register("wiki", new TreeCommand(Category.UTILS) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                        event.getChannel().sendMessage(EmoteReference.OK + "**For Mantaro's documentation please visit:** https://github.com/Mantaro/MantaroBot/wiki/Home").queue();
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Shows a bunch of things related to Mantaro's wiki.\n" +
                                "Avaliable subcommands: `opts`, `custom`, `faq`, `commands`, `modifiers`, `tos`, `usermessage`, `premium`, `items`")
                        .build();
            }//addSubCommand meme incoming...
        }.addSubCommand("opts", (event, s) -> event.getChannel().sendMessage(EmoteReference.OK + "**For Mantaro's documentation on `~>opts` and general bot options please visit:** https://github.com/Mantaro/MantaroBot/wiki/Configuration").queue())
        .addSubCommand("custom", (event, s) -> event.getChannel().sendMessage(EmoteReference.OK + "**For Mantaro's documentation on custom commands please visit:** https://github.com/Mantaro/MantaroBot/wiki/Custom-Commands").queue())
        .addSubCommand("modifiers", (event, s) -> event.getChannel().sendMessage(EmoteReference.OK + "**For Mantaro's documentation in custom commands modifiers please visit:** https://github.com/Mantaro/MantaroBot/wiki/Custom-Command-Modifiers").queue())
        .addSubCommand("commands", (event, s) -> event.getChannel().sendMessage(EmoteReference.OK + "**For Mantaro's documentation on commands and usage please visit:** https://github.com/Mantaro/MantaroBot/wiki/Command-reference-and-documentation").queue())
        .addSubCommand("faq", (event, s) -> event.getChannel().sendMessage(EmoteReference.OK + "**For Mantaro's FAQ please visit:** https://github.com/Mantaro/MantaroBot/wiki/FAQ").queue())
        .addSubCommand("badges", (event, s) -> event.getChannel().sendMessage(EmoteReference.OK + "**For Mantaro's badge documentation please visit:** https://github.com/Mantaro/MantaroBot/wiki/Badge-reference-and-documentation").queue())
        .addSubCommand("tos", (event, s) -> event.getChannel().sendMessage(EmoteReference.OK + "**For Mantaro's ToS please visit:** https://github.com/Mantaro/MantaroBot/wiki/Terms-of-Service").queue())
        .addSubCommand("usermessage", (event, s) -> event.getChannel().sendMessage(EmoteReference.OK + "**For Mantaro's Welcome and Leave message tutorial please visit:** https://github.com/Mantaro/MantaroBot/wiki/Welcome-and-Leave-Messages-tutorial").queue())
        .addSubCommand("premium", (event, s) -> event.getChannel().sendMessage(EmoteReference.OK + "**To see what Mantaro's Premium features offer please visit:** https://github.com/Mantaro/MantaroBot/wiki/Premium-Perks").queue())
        .addSubCommand("items", (event, s) -> event.getChannel().sendMessage(EmoteReference.OK + "**For a list of all collectable (non-purchaseable) items please visit:** https://github.com/Mantaro/MantaroBot/wiki/Collectable-Items").queue()));
    }
}
