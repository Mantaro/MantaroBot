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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
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
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.base.ITreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.JsonDataManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.awt.Color;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Module
public class UtilsCmds {
    private static final Logger log = LoggerFactory.getLogger(UtilsCmds.class);
    private static final Pattern timePattern = Pattern.compile(" -time [(\\d+)((?:h(?:our(?:s)?)?)|(?:m(?:in(?:ute(?:s)?)?)?)|(?:s(?:ec(?:ond(?:s)?)?)?))]+");
    private static final Random random = new Random();
    private static final Cache<String, ConcurrentHashMap<String, BirthdayCacher.BirthdayData>> guildBirthdayCache = CacheBuilder.newBuilder()
            .maximumSize(2500)
            .expireAfterWrite(Duration.ofHours(6))
            .build();

    protected static String dateGMT(Guild guild, String tz) {
        var format = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        var date = new Date();

        var dbGuild = MantaroData.db().getGuild(guild.getId());
        var guildData = dbGuild.getData();

        if (guildData.getTimeDisplay() == 1) {
            format = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a");
        }

        format.setTimeZone(TimeZone.getTimeZone(tz));
        return format.format(date);
    }

    @Subscribe
    public void birthday(CommandRegistry registry) {
        TreeCommand birthdayCommand = registry.register("birthday", new TreeCommand(CommandCategory.UTILS) {
            @Override
            public Command defaultTrigger(Context context, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, I18nContext languageContext, String content) {
                        if (content.isEmpty()) {
                            ctx.sendLocalized("commands.birthday.no_content", EmoteReference.ERROR);
                            return;
                        }

                        //Twice. Yep.
                        var parseFormat = new SimpleDateFormat("dd-MM-yyyy");
                        var displayFormat = new SimpleDateFormat("dd-MM");
                        Date birthdayDate;

                        //This code hurts to read, lol.
                        try {
                            String birthday;
                            birthday = content.replace("/", "-");
                            var parts = new ArrayList<>(Arrays.asList(birthday.split("-"))); //Cursed.

                            if (Integer.parseInt(parts.get(0)) > 31 || Integer.parseInt(parts.get(1)) > 12) {
                                ctx.sendLocalized("commands.birthday.invalid_date", EmoteReference.ERROR);
                                return;
                            }

                            if (parts.size() > 2) {
                                ctx.sendLocalized("commands.birthday.new_format", EmoteReference.ERROR);
                                return;
                            }

                            //Add a year so it parses and saves using the old format. Yes, this is also cursed.
                            parts.add("2037");

                            var date = String.join("-", parts);
                            birthdayDate = parseFormat.parse(date);
                        } catch (Exception e) {
                            ctx.sendStrippedLocalized("commands.birthday.error_date", "\u274C", content);
                            return;
                        }

                        var birthdayFormat = parseFormat.format(birthdayDate);

                        //Actually save it to the user's profile.
                        DBUser dbUser = ctx.getDBUser();
                        dbUser.getData().setBirthday(birthdayFormat);
                        dbUser.save();

                        //Yes, very.
                        ctx.sendLocalized("commands.birthday.added_birthdate", EmoteReference.CORRECT, displayFormat.format(birthdayDate));
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Sets your birthday date. Only useful if the server has enabled this functionality")
                        .setUsage("`~>birthday <date>`")
                        .addParameter("date", "A date in dd-mm format (13-02 for example). Check subcommands for more options.")
                        .build();
            }
        });

        birthdayCommand.addSubCommand("allowserver", new SubCommand() {
            public String description() {
                return "Allows the server where you send this command to announce your birthday.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var dbGuild = ctx.getDBGuild();
                var guildData = dbGuild.getData();

                if (guildData.getAllowedBirthdays().contains(ctx.getAuthor().getId())) {
                    ctx.sendLocalized("commands.birthday.already_allowed", EmoteReference.ERROR);
                    return;
                }

                guildData.getAllowedBirthdays().add(ctx.getAuthor().getId());
                dbGuild.save();

                var cached = guildBirthdayCache.getIfPresent(ctx.getGuild().getId());
                var cachedBirthday = ctx.getBot().getBirthdayCacher().getCachedBirthdays().get(ctx.getUser());
                if (cached != null && cachedBirthday != null) {
                    cached.put(ctx.getUser().getId(), cachedBirthday);
                }

                ctx.sendLocalized("commands.birthday.allowed_server", EmoteReference.CORRECT);
            }
        });

        birthdayCommand.addSubCommand("denyserver", new SubCommand() {
            public String description() {
                return "Denies the server where you send this command to announce your birthday.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var dbGuild = ctx.getDBGuild();
                var guildData = dbGuild.getData();

                if (!guildData.getAllowedBirthdays().contains(ctx.getAuthor().getId())) {
                    ctx.sendLocalized("commands.birthday.already_denied", EmoteReference.CORRECT);
                    return;
                }

                guildData.getAllowedBirthdays().remove(ctx.getAuthor().getId());
                dbGuild.save();

                var cached = guildBirthdayCache.getIfPresent(ctx.getGuild().getId());
                if (cached != null) {
                    cached.remove(ctx.getUser().getId());
                }

                ctx.sendLocalized("commands.birthday.denied_server", EmoteReference.CORRECT);
            }
        });

        birthdayCommand.addSubCommand("remove", new SubCommand() {
            @Override
            public String description() {
                return "Removes your set birthday date.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var user = ctx.getDBUser();
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
            protected void call(Context ctx, I18nContext languageContext, String content) {
                BirthdayCacher cacher = MantaroBot.getInstance().getBirthdayCacher();

                try {
                    // Why would this happen is out of my understanding.
                    if (cacher != null) {
                        var globalBirthdays = cacher.getCachedBirthdays();
                        if (globalBirthdays.isEmpty()) {
                            ctx.sendLocalized("commands.birthday.no_global_birthdays", EmoteReference.SAD);
                            return;
                        }

                        var guild = ctx.getGuild();
                        var data = ctx.getDBGuild().getData();
                        var ids = data.getAllowedBirthdays();

                        if (ids.isEmpty()) {
                            ctx.sendLocalized("commands.birthday.no_guild_birthdays", EmoteReference.ERROR);
                            return;
                        }

                        var guildCurrentBirthdays = getBirthdayMap(ctx.getGuild().getId(), ids);
                        if (guildCurrentBirthdays.isEmpty()) {
                            ctx.sendLocalized("commands.birthday.no_guild_birthdays", EmoteReference.ERROR);
                            return;
                        }

                        // Build the message. This is duplicated on birthday month with a lil different.
                        var birthdays = guildCurrentBirthdays.entrySet().stream()
                                .sorted(Comparator.comparingInt(i -> Integer.parseInt(i.getValue().day)))
                                .filter(birthday -> ids.contains(birthday.getKey()))
                                .map((entry) -> {
                                    var birthday = entry.getValue().getBirthday().split("-");
                                    Member member = null;
                                    try {
                                        member = guild.retrieveMemberById(entry.getKey(), false).complete();
                                    } catch (Exception e) {
                                        return "Unknown Member : " + birthday[0] + "-" + birthday[1];
                                    }

                                    return "+ %-20s : %s ".formatted(
                                            member.getEffectiveName(),
                                            birthday[0] + "-" + birthday[1]
                                    );
                                })
                                .collect(Collectors.joining("\n"));

                        var parts = DiscordUtils.divideString(1000, birthdays);
                        var hasReactionPerms = ctx.hasReactionPerms();

                        List<String> messages = new LinkedList<>();
                        for (String part : parts) {
                            messages.add(languageContext.get("commands.birthday.full_header").formatted(guild.getName(),
                                    (parts.size() > 1 ?
                                            (hasReactionPerms ? languageContext.get("general.arrow_react") :
                                            languageContext.get("general.text_menu")) : "") + "```diff\n%s```".formatted(part))
                            );
                        }

                        if (parts.isEmpty()) {
                            ctx.sendLocalized("commands.birthday.no_guild_birthdays", EmoteReference.ERROR);
                            return;
                        }

                        // Show the message.
                        // Probably a p big one tbh.
                        if (hasReactionPerms) {
                            DiscordUtils.list(ctx.getEvent(), 45, false, messages);
                        } else {
                            DiscordUtils.listText(ctx.getEvent(), 45, false, messages);
                        }
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
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var args = ctx.getArguments();
                var cacher = MantaroBot.getInstance().getBirthdayCacher();
                var calendar = Calendar.getInstance();
                var month = calendar.get(Calendar.MONTH);

                var msg = "";
                if (args.length == 1) {
                    msg = args[0];
                }

                if (!msg.isEmpty()) {
                    try {
                        month = Integer.parseInt(msg);
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
                        final var cachedBirthdays = cacher.getCachedBirthdays();
                        if (cachedBirthdays.isEmpty()) {
                            ctx.sendLocalized("commands.birthday.no_global_birthdays", EmoteReference.SAD);
                            return;
                        }

                        var data = ctx.getDBGuild().getData();
                        var ids = data.getAllowedBirthdays();
                        var guildCurrentBirthdays = getBirthdayMap(ctx.getGuild().getId(), ids);

                        if (ids.isEmpty()) {
                            ctx.sendLocalized("commands.birthday.no_guild_birthdays", EmoteReference.ERROR);
                            return;
                        }

                        //Try not to die. I mean get calendar month and sum 1.
                        var calendarMonth = String.valueOf(calendar.get(Calendar.MONTH) + 1);
                        var currentMonth = (calendarMonth.length() == 1 ? 0 : "") + calendarMonth;

                        //Build the message.
                        var birthdays = guildCurrentBirthdays.entrySet().stream()
                                .filter(bds -> bds.getValue().month.equals(currentMonth))
                                .sorted(Comparator.comparingInt(i -> Integer.parseInt(i.getValue().day)))
                                .map((entry) -> {
                                    Guild guild = ctx.getGuild();
                                    var birthday = entry.getValue().getBirthday().split("-");

                                    Member member = null;
                                    try {
                                        member = guild.retrieveMemberById(entry.getKey(), false).complete();
                                    } catch (Exception e) {
                                        return "Unknown Member : " + birthday[0] + "-" + birthday[1];
                                    }

                                    return "+ %-20s : %s ".formatted(
                                            member.getEffectiveName(),
                                            birthday[0] + "-" + birthday[1]
                                    );
                                }).collect(Collectors.joining("\n"));

                        //No birthdays to be seen here? (This month)
                        if (birthdays.trim().isEmpty()) {
                            ctx.sendLocalized("commands.birthday.no_guild_month_birthdays",
                                    EmoteReference.ERROR, month + 1, EmoteReference.BLUE_SMALL_MARKER
                            );

                            return;
                        }

                        var parts = DiscordUtils.divideString(1000, birthdays);
                        List<String> messages = new LinkedList<>();

                        for (var part : parts) {
                            messages.add(languageContext.get("commands.birthday.header").formatted(ctx.getGuild().getName(),
                                    Utils.capitalize(calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH))) +
                                    (parts.size() > 1 ? (ctx.hasReactionPerms() ?
                                            languageContext.get("general.arrow_react") :
                                            languageContext.get("general.text_menu")) : "") + "```diff\n%s```".formatted(part)
                            );
                        }

                        if (ctx.hasReactionPerms()) {
                            DiscordUtils.list(ctx.getEvent(), 45, false, messages);
                        } else {
                            DiscordUtils.listText(ctx.getEvent(), 45, false, messages);
                        }
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
        registry.register("choose", new SimpleCommand(CommandCategory.UTILS) {
            @Override
            public void call(Context ctx, String content, String[] args) {
                if (args.length < 1) {
                    ctx.sendLocalized("commands.choose.nothing_to", EmoteReference.ERROR);
                    return;
                }

                var send = Utils.DISCORD_INVITE.matcher(args[random.nextInt(args.length)]).replaceAll("-inv link-");
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
        ITreeCommand remindme = registry.register("remindme", new TreeCommand(CommandCategory.UTILS) {
            @Override
            public Command defaultTrigger(Context context, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, I18nContext languageContext, String content) {
                        var optionalArguments = ctx.getOptionalArguments();

                        if (!optionalArguments.containsKey("time")) {
                            ctx.sendLocalized("commands.remindme.no_time", EmoteReference.ERROR);
                            return;
                        }

                        if (optionalArguments.get("time") == null) {
                            ctx.sendLocalized("commands.remindme.no_time", EmoteReference.ERROR);
                            return;
                        }

                        var toRemind = timePattern.matcher(content).replaceAll("");
                        var user = ctx.getUser();
                        var time = Utils.parseTime(optionalArguments.get("time"));
                        var dbUser = ctx.getDBUser();
                        var rems = getReminders(dbUser.getData().getReminders());

                        if (rems.size() > 25) {
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

                        var displayRemind = Utils.DISCORD_INVITE.matcher(toRemind).replaceAll("discord invite link");
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
                        .addParameter("-time",
                                "How much time until I remind you of it. Time is in this format: 1h20m (1 hour and 20m). " +
                                        "You can use h, m and s (hour, minute, second)")
                        .build();
            }
        });

        remindme.addSubCommand("list", new SubCommand() {
            @Override
            public String description() {
                return "Lists current reminders.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var reminders = ctx.getDBUser().getData().getReminders();
                var rms = getReminders(reminders);

                if (rms.isEmpty()) {
                    ctx.sendLocalized("commands.remindme.no_reminders", EmoteReference.ERROR);
                    return;
                }

                var builder = new StringBuilder();
                var i = new AtomicInteger();
                for (var rems : rms) {
                    builder.append("**").append(i.incrementAndGet()).append(".-**").append("R: *").append(rems.getReminder()).append("*, Due in: **")
                            .append(Utils.formatDuration(rems.getTime() - System.currentTimeMillis())).append("**").append("\n");
                }

                var toSend = new MessageBuilder().append(builder.toString()).buildAll(MessageBuilder.SplitPolicy.NEWLINE);
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
            protected void call(Context ctx, I18nContext languageContext, String content) {
                try {
                    var reminders = ctx.getDBUser().getData().getReminders();

                    if (reminders.isEmpty()) {
                        ctx.sendLocalized("commands.remindme.no_reminders", EmoteReference.ERROR);
                        return;
                    }


                    if (reminders.size() == 1) {
                        Reminder.cancel(ctx.getUser().getId(), reminders.get(0), Reminder.CancelReason.CANCEL); //Cancel first reminder.
                        ctx.sendLocalized("commands.remindme.cancel.success", EmoteReference.CORRECT);
                    } else {
                        List<ReminderObject> rems = getReminders(reminders);
                        rems = rems.stream().filter(reminder -> reminder.time - System.currentTimeMillis() > 3).collect(Collectors.toList());
                        DiscordUtils.selectList(ctx.getEvent(), rems,
                                r -> "%s, Due in: %s".formatted(r.reminder, Utils.formatDuration(r.time - System.currentTimeMillis())),
                                r1 -> new EmbedBuilder().setColor(Color.CYAN).setTitle(ctx.getLanguageContext().get("commands.remindme.cancel.select"), null)
                                        .setDescription(r1)
                                        .setFooter(ctx.getLanguageContext().get("general.timeout").formatted(10), null).build(),
                                sr -> {
                                    Reminder.cancel(ctx.getUser().getId(), sr.id + ":" + sr.getUserId(), Reminder.CancelReason.CANCEL);
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
                var rem = j.hget("reminder", s);
                if (rem != null) {
                    var json = new JSONObject(rem);
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
        final Pattern offsetRegex = Pattern.compile("(?:UTC|GMT)[+-][0-9]{1,2}(:[0-9]{1,2})?", Pattern.CASE_INSENSITIVE);
        registry.register("time", new SimpleCommand(CommandCategory.UTILS) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var mentions = ctx.getMentionedMembers();
                var isMention = !mentions.isEmpty();
                var timezone = content.isEmpty() ? "" : args[0]; // Array out of bounds lol

                if (offsetRegex.matcher(timezone).matches()) {
                    timezone = timezone.toUpperCase().replace("UTC", "GMT");
                }

                var dbUser = !isMention ? ctx.getDBUser() : ctx.getDBUser(mentions.get(0));
                var userData = dbUser.getData();

                if (isMention && userData.getTimezone() == null) {
                    ctx.sendLocalized("commands.time.user_no_timezone", EmoteReference.ERROR);
                    return;
                }

                if (userData.getTimezone() != null && (content.isEmpty() || isMention)) {
                    timezone = userData.getTimezone();
                }

                if (!Utils.isValidTimeZone(timezone)) {
                    ctx.sendLocalized("commands.time.invalid_timezone", EmoteReference.ERROR);
                    return;
                }

                ctx.sendLocalized("commands.time.success", EmoteReference.MEGA, dateGMT(ctx.getGuild(), timezone), timezone);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Get the time in a specific timezone (GMT).")
                        .setUsage("`~>time <timezone> [@user]`")
                        .addParameter("timezone",
                                "The timezone in GMT or UTC offset (Example: GMT-3) or a ZoneId (such as Europe/London)")
                        .addParameter("@user", "The user to see the timezone of. Has to be a mention.")
                        .build();
            }
        });
    }

    @Subscribe
    public void urban(CommandRegistry registry) {
        registry.register("urban", new SimpleCommand(CommandCategory.UTILS) {
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

                var commandArguments = content.split("->");
                var languageContext = ctx.getLanguageContext();

                var url = "http://api.urbandictionary.com/v0/define?term=" + URLEncoder.encode(commandArguments[0], StandardCharsets.UTF_8);
                var json = Utils.httpRequest(url);
                UrbanData data = null;

                try {
                    data = JsonDataManager.fromJson(json, UrbanData.class);
                } catch (JsonProcessingException e) {
                    ctx.sendLocalized("commands.urban.error", EmoteReference.ERROR);
                    e.printStackTrace();
                    return;
                }

                if (commandArguments.length > 2) {
                    ctx.sendLocalized("commands.urban.too_many_args", EmoteReference.ERROR);
                    return;
                }

                if (data == null || data.getList() == null || data.getList().isEmpty()) {
                    ctx.send(EmoteReference.ERROR + languageContext.get("general.no_results"));
                    return;
                }

                var definitionNumber = commandArguments.length > 1 ? (Integer.parseInt(commandArguments[1]) - 1) : 0;
                var urbanData = data.getList().get(definitionNumber);
                var definition = urbanData.getDefinition();

                ctx.send(new EmbedBuilder()
                        .setAuthor(languageContext.get("commands.urban.header").formatted(
                                commandArguments[0]), urbanData.getPermalink(),
                                ctx.getAuthor().getEffectiveAvatarUrl()
                        )
                        .setThumbnail("https://i.imgur.com/PbXqLrS.png")
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

    @Subscribe
    public void wiki(CommandRegistry registry) {
        registry.register("wiki", new TreeCommand(CommandCategory.UTILS) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, I18nContext languageContext, String content) {
                        ctx.send(EmoteReference.OK + "**For Mantaro's documentation please visit:** https://github.com/Mantaro/MantaroBot/wiki/Home");
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Shows a bunch of things related to Mantaro's wiki.")
                        .build();
            } // addSubCommand meme incoming...
        }.addSubCommand("opts", (ctx, s) ->
                ctx.send(EmoteReference.OK + "**For Mantaro's documentation on `~>opts` and general bot options please visit:** " +
                        "https://github.com/Mantaro/MantaroBot/wiki/Configuration"))
                .addSubCommand("custom", (ctx, s) ->
                        ctx.send(EmoteReference.OK + "**For Mantaro's documentation on custom commands please visit:** " +
                                "https://github.com/Mantaro/MantaroBot/wiki/Custom-Command-%22v3%22"))
                .addSubCommand("modifiers", (ctx, s) ->
                        ctx.send(EmoteReference.OK + "**For Mantaro's documentation in custom commands modifiers please visit:** " +
                                "https://github.com/Mantaro/MantaroBot/wiki/Custom-Command-Modifiers"))
                .addSubCommand("commands", (ctx, s) ->
                        ctx.send(EmoteReference.OK + "**For Mantaro's documentation on commands and usage please visit:** " +
                                "https://github.com/Mantaro/MantaroBot/wiki/Command-reference-and-documentation"))
                .addSubCommand("faq", (ctx, s) ->
                        ctx.send(EmoteReference.OK + "**For Mantaro's FAQ please visit:** " +
                                "https://github.com/Mantaro/MantaroBot/wiki/FAQ"))
                .addSubCommand("badges", (ctx, s) ->
                        ctx.send(EmoteReference.OK + "**For Mantaro's badge documentation please visit:**" +
                                " https://github.com/Mantaro/MantaroBot/wiki/Badge-reference-and-documentation"))
                .addSubCommand("tos", (ctx, s) ->
                        ctx.send(EmoteReference.OK + "**For Mantaro's ToS please visit:** " +
                                "https://github.com/Mantaro/MantaroBot/wiki/Terms-of-Service"))
                .addSubCommand("usermessage", (ctx, s) ->
                        ctx.send(EmoteReference.OK + "**For Mantaro's Welcome and Leave message tutorial please visit:** " +
                                "https://github.com/Mantaro/MantaroBot/wiki/Welcome-and-Leave-Messages-tutorial"))
                .addSubCommand("premium", (ctx, s) ->
                        ctx.send(EmoteReference.OK + "**To see what Mantaro's Premium features offer please visit:** " +
                                "https://github.com/Mantaro/MantaroBot/wiki/Premium-Perks"))
                .addSubCommand("currency", (ctx, s) ->
                        ctx.send(EmoteReference.OK + "**For a Currency guide, please visit:** " +
                                "https://github.com/Mantaro/MantaroBot/wiki/Currency-101"))
                .addSubCommand("collectibles", (ctx, s) ->
                        ctx.send(EmoteReference.OK + "**For a list of collectables, please visit:**" +
                                " https://github.com/Mantaro/MantaroBot/wiki/Collectable-Items"))
                .addSubCommand("birthday", (ctx, s) ->
                        ctx.send(EmoteReference.OK + "**For a guide on the birthday system, please visit:**" +
                                " https://github.com/Mantaro/MantaroBot/wiki/Birthday-101"))
        );
    }

    public static Cache<String, ConcurrentHashMap<String, BirthdayCacher.BirthdayData>> getGuildBirthdayCache() {
        return guildBirthdayCache;
    }

    private ConcurrentHashMap<String, BirthdayCacher.BirthdayData> getBirthdayMap(String guildId, List<String> allowed) {
        ConcurrentHashMap<String, BirthdayCacher.BirthdayData> guildCurrentBirthdays = new ConcurrentHashMap<>();
        final var cachedBirthdays = MantaroBot.getInstance().getBirthdayCacher().getCachedBirthdays();

        var cached = guildBirthdayCache.getIfPresent(guildId);
        if (cached != null && cached.size() >= 1) {
            guildCurrentBirthdays = cached;
        } else {
            for (var birthdays : cachedBirthdays.entrySet()) {
                if (allowed.contains(birthdays.getKey())) {
                    guildCurrentBirthdays.put(birthdays.getKey(), birthdays.getValue());
                }
            }

            // Populate guild cache
            if (guildCurrentBirthdays.size() >= 1) {
                guildBirthdayCache.put(guildId, guildCurrentBirthdays);
            }
        }

        return guildCurrentBirthdays;
    }
}
