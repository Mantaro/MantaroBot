/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.utils.UrbanData;
import net.kodehawa.mantarobot.commands.utils.birthday.BirthdayCacher;
import net.kodehawa.mantarobot.commands.utils.reminders.Reminder;
import net.kodehawa.mantarobot.commands.utils.reminders.ReminderObject;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.base.ITreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.awt.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Module
@SuppressWarnings("unused")
public class UtilsCmds {
    private static final Logger log = LoggerFactory.getLogger(UtilsCmds.class);
    private static final Pattern timePattern = Pattern.compile(" -time [(\\d+)((?:h(?:our(?:s)?)?)|(?:m(?:in(?:ute(?:s)?)?)?)|(?:s(?:ec(?:ond(?:s)?)?)?))]+");
    private static final Random random = new Random();

    protected static String dateGMT(Guild guild, String tz) {
        DateFormat format = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        Date date = new Date();

        DBGuild dbGuild = MantaroData.db().getGuild(guild.getId());
        GuildData guildData = dbGuild.getData();

        if (guildData.getTimeDisplay() == 1) {
            format = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a");
        }

        format.setTimeZone(TimeZone.getTimeZone(tz));
        return format.format(date);
    }

    @Subscribe
    public void birthday(CommandRegistry registry) {
        TreeCommand birthdayCommand = (TreeCommand) registry.register("birthday", new TreeCommand(Category.UTILS) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, String content) {
                        if (content.isEmpty()) {
                            ctx.sendLocalized("commands.birthday.no_content", EmoteReference.ERROR);
                            return;
                        }

                        String[] args = ctx.getArguments();
                        SimpleDateFormat format1 = new SimpleDateFormat("dd-MM-yyyy");
                        Date bd1;

                        try {
                            String bd;
                            bd = content.replace("/", "-");
                            String[] parts = bd.split("-");
                            if (Integer.parseInt(parts[0]) > 31 || Integer.parseInt(parts[1]) > 12 || Integer.parseInt(parts[2]) > 3000) {
                                ctx.sendLocalized("commands.birthday.invalid_date", EmoteReference.ERROR);
                                return;
                            }

                            bd1 = format1.parse(bd);
                        } catch (Exception e) {
                            Optional.ofNullable(args[0]).ifPresent(s ->
                                    ctx.sendStrippedLocalized("commands.birthday.error_date", "\u274C", args[0])
                            );
                            return;
                        }

                        String birthdayFormat = format1.format(bd1);

                        DBUser dbUser = ctx.getDBUser();
                        dbUser.getData().setBirthday(birthdayFormat);
                        dbUser.save();

                        ctx.sendLocalized("commands.birthday.added_birthdate", EmoteReference.CORRECT, birthdayFormat);
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Sets your birthday date. Only useful if the server has enabled this functionality")
                        .setUsage("`~>birthday <date>`")
                        .addParameter("date", "A date in dd-mm-yyyy format (13-02-1998 for example). Check subcommands for more options.")
                        .build();
            }
        });

        birthdayCommand.addSubCommand("remove", new SubCommand() {
            @Override
            public String description() {
                return "Removes your set birthday date.";
            }

            @Override
            protected void call(Context ctx, String content) {
                DBUser user = ctx.getDBUser();
                user.getData().setBirthday(null);
                user.save();

                ctx.sendLocalized("commands.birthday.reset", EmoteReference.CORRECT);
            }
        });

        birthdayCommand.addSubCommand("list", new SubCommand() {
            @Override
            public String description() {
                return "Gives all of the birthdays for this server.";
            }

            @Override
            protected void call(Context ctx, String content) {
                BirthdayCacher cacher = MantaroBot.getInstance().getBirthdayCacher();

                try {
                    //Why would this happen is out of my understanding.
                    if (cacher != null) {
                        //same as above unless testing?
                        if (cacher.cachedBirthdays.isEmpty()) {
                            ctx.sendLocalized("commands.birthday.no_global_birthdays", EmoteReference.SAD);
                            return;
                        }

                        //O(1) lookups. Probably.
                        Guild guild = ctx.getGuild();
                        HashSet<String> ids = guild.getMemberCache().stream().map(m -> m.getUser().getId()).collect(Collectors.toCollection(HashSet::new));
                        Map<String, BirthdayCacher.BirthdayData> guildCurrentBirthdays = cacher.cachedBirthdays;

                        //No birthdays to be seen here? (This month)
                        if (guildCurrentBirthdays.isEmpty()) {
                            ctx.sendLocalized("commands.birthday.no_guild_birthdays", EmoteReference.ERROR);
                            return;
                        }

                        //Build the message. This is duplicated on birthday month with a lil different.
                        String birthdays = guildCurrentBirthdays.entrySet().stream()
                                .sorted(Comparator.comparingInt(i -> Integer.parseInt(i.getValue().day)))
                                .filter(entry -> guild.getMemberById(entry.getKey()) != null)
                                .map((entry) -> String.format("+ %-20s : %s ", guild.getMemberById(entry.getKey()).getEffectiveName(), entry.getValue().getBirthday()))
                                .collect(Collectors.joining("\n"));

                        List<String> parts = DiscordUtils.divideString(1000, birthdays);
                        boolean hasReactionPerms = ctx.hasReactionPerms();

                        List<String> messages = new LinkedList<>();
                        I18nContext languageContext = ctx.getLanguageContext();
                        for (String s1 : parts) {
                            messages.add(String.format(languageContext.get("commands.birthday.full_header"), guild.getName(),
                                    (parts.size() > 1 ? (hasReactionPerms ? languageContext.get("general.arrow_react") : languageContext.get("general.text_menu")) : "") +
                                            String.format("```diff\n%s```", s1)));
                        }

                        //Show the message.
                        //Probably a p big one tbh.
                        if (hasReactionPerms)
                            DiscordUtils.list(ctx.getEvent(), 45, false, messages);
                        else
                            DiscordUtils.listText(ctx.getEvent(), 45, false, messages);
                    } else {
                        ctx.sendLocalized("commands.birthday.cache_not_running", EmoteReference.SAD);
                    }
                } catch (Exception e) {
                    ctx.sendLocalized("commands.birthday.error", EmoteReference.SAD);
                    log.error("Error on birthday list display!", e);
                }
            }
        });

        birthdayCommand.addSubCommand("month", new SubCommand() {
            @Override
            public String description() {
                return "Checks the current birthday date for the specified month. Example: `~>birthday month 1`";
            }

            @Override
            protected void call(Context ctx, String content) {
                String[] args = ctx.getArguments();
                BirthdayCacher cacher = MantaroBot.getInstance().getBirthdayCacher();
                Calendar calendar = Calendar.getInstance();
                int month = calendar.get(Calendar.MONTH);

                String m1 = "";
                if (args.length == 1)
                    m1 = args[0];

                if (!m1.isEmpty()) {
                    try {
                        month = Integer.parseInt(m1);
                        if (month < 1 || month > 12) {
                            ctx.sendLocalized("commands.birthday.invalid_month", EmoteReference.ERROR);
                            return;
                        }

                        //Substract here so we can do the check properly up there.
                        month = month - 1;
                    } catch (NumberFormatException e) {
                        ctx.sendLocalized("commands.birthday.invalid_month", EmoteReference.ERROR);
                        return;
                    }
                }

                //Inspection excluded below not needed, I'm passing a proper value.
                calendar.set(calendar.get(Calendar.YEAR), month, Calendar.MONDAY);

                try {
                    //Why would this happen is out of my understanding.
                    if (cacher != null) {
                        //same as above unless testing?
                        if (cacher.cachedBirthdays.isEmpty()) {
                            ctx.sendLocalized("commands.birthday.no_global_birthdays", EmoteReference.SAD);
                            return;
                        }

                        //O(1) lookups. Probably.
                        HashSet<String> ids = ctx.getGuild().getMemberCache().stream().map(m -> m.getUser().getId()).collect(Collectors.toCollection(HashSet::new));
                        Map<String, BirthdayCacher.BirthdayData> guildCurrentBirthdays = new HashMap<>();

                        //Try not to die. I mean get calendar month and sum 1.
                        String calendarMonth = String.valueOf(calendar.get(Calendar.MONTH) + 1);
                        String currentMonth = (calendarMonth.length() == 1 ? 0 : "") + calendarMonth;

                        //~100k repetitions rip
                        for (Map.Entry<String, BirthdayCacher.BirthdayData> birthdays : cacher.cachedBirthdays.entrySet()) {
                            //Why was the birthday saved on this outdated format again?
                            //Check if this guild contains x user and that the month matches.
                            if (ids.contains(birthdays.getKey()) && birthdays.getValue().month.equals(currentMonth)) {
                                //Insert into current month bds.
                                guildCurrentBirthdays.put(birthdays.getKey(), birthdays.getValue());
                            }
                        }

                        //No birthdays to be seen here? (This month)
                        if (guildCurrentBirthdays.isEmpty()) {
                            ctx.sendLocalized("commands.birthday.no_guild_month_birthdays", EmoteReference.ERROR, month + 1, EmoteReference.BLUE_SMALL_MARKER);
                            return;
                        }

                        //Build the message.
                        String birthdays = guildCurrentBirthdays.entrySet().stream()
                                .sorted(Comparator.comparingInt(i -> Integer.parseInt(i.getValue().day)))
                                .map((entry) -> String.format("+ %-20s : %s ",
                                        ctx.getGuild().getMemberById(entry.getKey()).getEffectiveName(),
                                        entry.getValue().getBirthday())
                                ).collect(Collectors.joining("\n"));

                        List<String> parts = DiscordUtils.divideString(1000, birthdays);
                        I18nContext languageContext = ctx.getLanguageContext();
                        List<String> messages = new LinkedList<>();
                        for (String s1 : parts) {
                            messages.add(String.format(languageContext.get("commands.birthday.header"), ctx.getGuild().getName(),
                                    Utils.capitalize(calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH))) +
                                    (parts.size() > 1 ? (ctx.hasReactionPerms() ? languageContext.get("general.arrow_react") : languageContext.get("general.text_menu")) : "") +
                                    String.format("```diff\n%s```", s1));
                        }

                        //Show the message.
                        //Probably a p big one tbh.
                        if (ctx.hasReactionPerms())
                            DiscordUtils.list(ctx.getEvent(), 45, false, messages);
                        else
                            DiscordUtils.listText(ctx.getEvent(), 45, false, messages);
                    } else {
                        ctx.sendLocalized("commands.birthday.cache_not_running", EmoteReference.SAD);
                    }
                } catch (Exception e) {
                    ctx.sendLocalized("commands.birthday.error", EmoteReference.SAD);
                    log.error("Error on birthday month display!", e);
                }
            }
        });
    }

    @Subscribe
    public void choose(CommandRegistry registry) {
        registry.register("choose", new SimpleCommand(Category.UTILS) {
            @Override
            public void call(Context ctx, String content, String[] args) {
                if (args.length < 1) {
                    ctx.sendLocalized("commands.choose.nothing_to", EmoteReference.ERROR);
                    return;
                }

                String send = Utils.DISCORD_INVITE.matcher(args[random.nextInt(args.length)]).replaceAll("-inv link-");
                send = Utils.DISCORD_INVITE_2.matcher(send).replaceAll("-inv link-");
                ctx.sendStrippedLocalized("commands.choose.success", EmoteReference.EYES, send);
            }

            @Override
            public String[] splitArgs(String content) {
                return StringUtils.advancedSplitArgs(content, -1);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Choose between 2 or more things.")
                        .setUsage("`~>choose <parameters>`")
                        .addParameter("parameters", "The parameters. Example `pat hello \"go watch the movies\"`.")
                        .build();
            }
        });
    }

    @Subscribe
    public void remindme(CommandRegistry registry) {
        ITreeCommand remindme = (ITreeCommand) registry.register("remindme", new TreeCommand(Category.UTILS) {
            @Override
            public Command defaultTrigger(Context context, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, String content) {
                        Map<String, String> optionalArguments = ctx.getOptionalArguments();

                        if (!optionalArguments.containsKey("time")) {
                            ctx.sendLocalized("commands.remindme.no_time", EmoteReference.ERROR);
                            return;
                        }

                        if (optionalArguments.get("time") == null) {
                            ctx.sendLocalized("commands.remindme.no_time", EmoteReference.ERROR);
                            return;
                        }

                        String toRemind = timePattern.matcher(content).replaceAll("");
                        User user = ctx.getUser();
                        long time = Utils.parseTime(optionalArguments.get("time"));
                        DBUser dbUser = ctx.getDBUser();

                        if (dbUser.getData().getReminders().size() > 25) {
                            //Max amount of reminders reached
                            ctx.sendLocalized("commands.remindme.too_many_reminders", EmoteReference.ERROR);
                            return;
                        }

                        if (time < 60000) {
                            ctx.sendLocalized("commands.remindme.too_little_time", EmoteReference.ERROR);
                            return;
                        }

                        if (System.currentTimeMillis() + time > System.currentTimeMillis() + TimeUnit.DAYS.toMillis(180)) {
                            ctx.sendLocalized("commands.remindme.too_long", EmoteReference.ERROR);
                            return;
                        }

                        String displayRemind = Utils.DISCORD_INVITE.matcher(toRemind).replaceAll("discord invite link");
                        displayRemind = Utils.DISCORD_INVITE_2.matcher(displayRemind).replaceAll("discord invite link");

                        ctx.sendStrippedLocalized("commands.remindme.success", EmoteReference.CORRECT, ctx.getUser().getName(),
                                ctx.getUser().getDiscriminator(), displayRemind, Utils.formatDuration(time));

                        new Reminder.Builder()
                                .id(user.getId())
                                .guild(ctx.getGuild().getId())
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
            protected void call(Context ctx, String content) {
                List<String> reminders = ctx.getDBUser().getData().getReminders();
                List<ReminderObject> rms = getReminders(reminders);

                if (reminders.isEmpty()) {
                    ctx.sendLocalized("commands.remindme.no_reminders", EmoteReference.ERROR);
                    return;
                }

                StringBuilder builder = new StringBuilder();
                AtomicInteger i = new AtomicInteger();
                for (ReminderObject rems : rms) {
                    builder.append("**").append(i.incrementAndGet()).append(".-**").append("R: *").append(rems.getReminder()).append("*, Due in: **")
                            .append(Utils.formatDuration(rems.getTime() - System.currentTimeMillis())).append("**").append("\n");
                }

                Queue<Message> toSend = new MessageBuilder().append(builder.toString()).buildAll(MessageBuilder.SplitPolicy.NEWLINE);
                toSend.forEach(ctx::send);
            }
        });

        remindme.createSubCommandAlias("list", "ls");
        remindme.createSubCommandAlias("list", "Is");

        remindme.addSubCommand("cancel", new SubCommand() {
            @Override
            public String description() {
                return "Cancel a reminder. You'll be given a list if you have more than one.";
            }

            @Override
            protected void call(Context ctx, String content) {
                try {
                    List<String> reminders = ctx.getDBUser().getData().getReminders();

                    if (reminders.isEmpty()) {
                        ctx.sendLocalized("commands.remindme.no_reminders", EmoteReference.ERROR);
                        return;
                    }

                    if (reminders.size() == 1) {
                        Reminder.cancel(ctx.getUser().getId(), reminders.get(0)); //Cancel first reminder.
                        ctx.sendLocalized("commands.remindme.cancel.success", EmoteReference.CORRECT);
                    } else {
                        List<ReminderObject> rems = getReminders(reminders);
                        rems = rems.stream().filter(reminder -> reminder.time - System.currentTimeMillis() > 3).collect(Collectors.toList());
                        DiscordUtils.selectList(ctx.getEvent(), rems,
                                r -> String.format("%s, Due in: %s", r.reminder, Utils.formatDuration(r.time - System.currentTimeMillis())),
                                r1 -> new EmbedBuilder().setColor(Color.CYAN).setTitle(ctx.getLanguageContext().get("commands.remindme.cancel.select"), null)
                                        .setDescription(r1)
                                        .setFooter(String.format(ctx.getLanguageContext().get("general.timeout"), 10), null).build(),
                                sr -> {
                                    Reminder.cancel(ctx.getUser().getId(), sr.id + ":" + sr.getUserId());
                                    ctx.send(EmoteReference.CORRECT + "Cancelled your reminder");
                                });
                    }
                } catch (Exception e) {
                    ctx.sendLocalized("commands.remindme.no_reminders", EmoteReference.ERROR);
                }
            }
        });
    }

    private List<ReminderObject> getReminders(List<String> reminders) {
        try (Jedis j = MantaroData.getDefaultJedisPool().getResource()) {
            List<ReminderObject> rems = new ArrayList<>();
            for (String s : reminders) {
                String rem = j.hget("reminder", s);
                if (rem != null) {
                    JSONObject json = new JSONObject(rem);
                    rems.add(ReminderObject.builder()
                            .id(s.split(":")[0])
                            .userId(json.getString("user"))
                            .guildId(json.getString("guild"))
                            .scheduledAtMillis(json.getLong("scheduledAt"))
                            .time(json.getLong("at"))
                            .reminder(json.getString("reminder"))
                            .build());
                }
            }

            return rems;
        }
    }

    @Subscribe
    public void time(CommandRegistry registry) {
        registry.register("time", new SimpleCommand(Category.UTILS) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                try {
                    content = content.replace("UTC", "GMT").toUpperCase();
                    DBUser dbUser = ctx.getDBUser();
                    String timezone = dbUser.getData().getTimezone() != null ? (content.isEmpty() ? dbUser.getData().getTimezone() : content) : content;

                    if (!Utils.isValidTimeZone(timezone)) {
                        ctx.sendLocalized("commands.time.invalid_timezone", EmoteReference.ERROR);
                        return;
                    }

                    ctx.sendLocalized("commands.time.success", EmoteReference.MEGA, dateGMT(ctx.getGuild(), timezone), timezone);
                } catch (Exception e) {
                    ctx.sendLocalized("commands.time.error", EmoteReference.ERROR);
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
            protected void call(Context ctx, String content, String[] args) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.urban.no_args", EmoteReference.ERROR);
                    return;
                }

                if (!ctx.getChannel().isNSFW()) {
                    ctx.sendLocalized("commands.urban.nsfw_notice", EmoteReference.ERROR);
                    return;
                }

                String[] commandArguments = content.split("->");
                var languageContext = ctx.getLanguageContext();

                var url = "http://api.urbandictionary.com/v0/define?term=" + URLEncoder.encode(commandArguments[0], StandardCharsets.UTF_8);
                var json = Utils.wget(url);
                var data = GsonDataManager.GSON_PRETTY.fromJson(json, UrbanData.class);

                if (commandArguments.length > 2) {
                    ctx.sendLocalized("commands.urban.too_many_args", EmoteReference.ERROR);
                    return;
                }

                if (data == null || data.getList() == null || data.getList().isEmpty()) {
                    ctx.send(EmoteReference.ERROR + languageContext.get("general.no_results"));
                    return;
                }

                var definitionNumber = commandArguments.length > 1 ? (Integer.parseInt(commandArguments[1]) - 1) : 0;
                var header = commandArguments[0];

                UrbanData.List urbanData = data.getList().get(definitionNumber);
                var definition = urbanData.getDefinition();

                ctx.send(new EmbedBuilder()
                        .setAuthor(String.format(languageContext.get("commands.urban.header"), commandArguments[0]), urbanData.getPermalink(), null)
                        .setThumbnail("https://everythingfat.files.wordpress.com/2013/01/ud-logo.jpg")
                        .setDescription(languageContext.get("general.definition") + " " + (definitionNumber + 1))
                        .setColor(Color.GREEN)
                        .addField(languageContext.get("general.definition"), StringUtils.limit(definition, 1000), false)
                        .addField(languageContext.get("general.example"), StringUtils.limit(urbanData.getExample(), 800), false)
                        .addField(":thumbsup:", urbanData.thumbs_up, true)
                        .addField(":thumbsdown:", urbanData.thumbs_down, true)
                        .setFooter(languageContext.get("commands.urban.footer"), null)
                        .build()
                );
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

    //Won't translate
    @Subscribe
    public void wiki(CommandRegistry registry) {
        registry.register("wiki", new TreeCommand(Category.UTILS) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, String content) {
                        ctx.send(EmoteReference.OK + "**For Mantaro's documentation please visit:** https://github.com/Mantaro/MantaroBot/wiki/Home");
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
        }.addSubCommand("opts", (ctx, s) -> ctx.send(EmoteReference.OK + "**For Mantaro's documentation on `~>opts` and general bot options please visit:** https://github.com/Mantaro/MantaroBot/wiki/Configuration"))
                .addSubCommand("custom", (ctx, s) -> ctx.send(EmoteReference.OK + "**For Mantaro's documentation on custom commands please visit:** https://github.com/Mantaro/MantaroBot/wiki/Custom-Command-%22v3%22"))
                .addSubCommand("modifiers", (ctx, s) -> ctx.send(EmoteReference.OK + "**For Mantaro's documentation in custom commands modifiers please visit:** https://github.com/Mantaro/MantaroBot/wiki/Custom-Command-Modifiers"))
                .addSubCommand("commands", (ctx, s) -> ctx.send(EmoteReference.OK + "**For Mantaro's documentation on commands and usage please visit:** https://github.com/Mantaro/MantaroBot/wiki/Command-reference-and-documentation"))
                .addSubCommand("faq", (ctx, s) -> ctx.send(EmoteReference.OK + "**For Mantaro's FAQ please visit:** https://github.com/Mantaro/MantaroBot/wiki/FAQ"))
                .addSubCommand("badges", (ctx, s) -> ctx.send(EmoteReference.OK + "**For Mantaro's badge documentation please visit:** https://github.com/Mantaro/MantaroBot/wiki/Badge-reference-and-documentation"))
                .addSubCommand("tos", (ctx, s) -> ctx.send(EmoteReference.OK + "**For Mantaro's ToS please visit:** https://github.com/Mantaro/MantaroBot/wiki/Terms-of-Service"))
                .addSubCommand("usermessage", (ctx, s) -> ctx.send(EmoteReference.OK + "**For Mantaro's Welcome and Leave message tutorial please visit:** https://github.com/Mantaro/MantaroBot/wiki/Welcome-and-Leave-Messages-tutorial"))
                .addSubCommand("premium", (ctx, s) -> ctx.send(EmoteReference.OK + "**To see what Mantaro's Premium features offer please visit:** https://github.com/Mantaro/MantaroBot/wiki/Premium-Perks"))
                .addSubCommand("items", (ctx, s) -> ctx.send(EmoteReference.OK + "**For a list of all collectable (non-purchaseable) items please visit:** https://github.com/Mantaro/MantaroBot/wiki/Collectable-Items")));
    }
}
