/*
 * Copyright (C) 2016-2021 David Rubio Escares / Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.kodehawa.mantarobot.commands.utils.UrbanData;
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
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.JsonDataManager;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;

import java.awt.Color;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Module
public class UtilsCmds {
    private static final Pattern rawTimePattern = Pattern.compile("^[(\\d)((?d|?h|(?m|(?s)]+$");
    private static final Pattern timePattern = Pattern.compile("-time [(\\d+)((?:h(?:our(?:s)?)?)|(?:m(?:in(?:ute(?:s)?)?)?)|(?:s(?:ec(?:ond(?:s)?)?)?))]+");

    @Subscribe
    public void remindme(CommandRegistry registry) {
        ITreeCommand remindme = registry.register("remindme", new TreeCommand(CommandCategory.UTILS) {
            @Override
            public Command defaultTrigger(Context context, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, I18nContext languageContext, String content) {
                        var optionalArguments = ctx.getOptionalArguments();
                        var args = ctx.getArguments();
                        long time = 0L;

                        if (args.length == 0) {
                            ctx.sendLocalized("commands.remindme.no_time", EmoteReference.ERROR);
                            return;
                        }

                        final var maybeTime = args[0];
                        final var matchTime = rawTimePattern.matcher(maybeTime).matches();
                        if (matchTime) {
                            content = content.replaceFirst(maybeTime, "").trim();
                            time = Utils.parseTime(maybeTime);
                        }

                        // Old format compatiblity.
                        if (!matchTime) {
                            if (optionalArguments.get("time") != null) {
                                time = Utils.parseTime(optionalArguments.get("time"));
                            }
                        }

                        if (time == 0) {
                            ctx.sendLocalized("commands.remindme.no_time", EmoteReference.ERROR);
                            return;
                        }

                        var toRemind = timePattern.matcher(content).replaceAll("").trim();
                        var user = ctx.getUser();
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

                        var displayRemind = toRemind
                                .replaceAll(Utils.DISCORD_INVITE.pattern(), "discord invite link")
                                .replaceAll(Utils.DISCORD_INVITE_2.pattern(), "discord invite link")
                                .replaceAll(Utils.HTTP_URL.pattern(), "url")
                                .trim();

                        if (displayRemind.isEmpty()) {
                            displayRemind = "something";
                        }

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
                        .setUsage("`~>remindme <time> <reminder>`\n" +
                                "Check subcommands for more. Append the subcommand after the main command.")
                        .addParameter("time",
                              """
                              How much time until I remind you of it. Time is in this format: 1h20m (1 hour and 20m). 
                              You can use h, m and s (hour, minute, second). 
                              """
                        )
                        .addParameter("reminder", "What to remind you of.")
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
                var rms = getReminders(reminders).stream()
                        .sorted(Comparator.comparingLong(ReminderObject::getScheduledAtMillis))
                        .collect(Collectors.toList());

                if (rms.isEmpty()) {
                    ctx.sendLocalized("commands.remindme.no_reminders", EmoteReference.ERROR);
                    return;
                }

                var builder = new StringBuilder();
                var i = new AtomicInteger();
                for (var rems : rms) {
                    builder.append("**").append(i.incrementAndGet()).append(".-**")
                            .append("Content: *")
                            .append(rems.getReminder())
                            .append("*, Due in: **")
                            .append(Utils.formatDuration(rems.getTime() - System.currentTimeMillis()))
                            .append("**").append("\n");
                }

                var toSend = new MessageBuilder().append(builder.toString())
                        .buildAll(MessageBuilder.SplitPolicy.NEWLINE);

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

        registry.registerAlias("remindme", "remind");
        registry.registerAlias("remindme", "reminder");
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

                var dateformat = "";
                try {
                    dateformat = Utils.formatDate(LocalDateTime.now(Utils.timezoneToZoneID(timezone)), userData.getLang());
                } catch (DateTimeException e) {
                    ctx.sendLocalized("commands.time.invalid_timezone", EmoteReference.ERROR);
                    return;
                }

                ctx.sendLocalized("commands.time.success", EmoteReference.CLOCK, dateformat, timezone);
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
                UrbanData data;

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
                        .addField(EmoteReference.PENCIL.toHeaderString() + languageContext.get("general.definition"),
                                StringUtils.limit(definition, 1000), false
                        )
                        .addField(EmoteReference.ZAP.toHeaderString() + languageContext.get("general.example"),
                                StringUtils.limit(urbanData.getExample(), 800), false
                        )
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
                                "https://github.com/Mantaro/MantaroBot/wiki/Custom-Commands-101"))
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
                .addSubCommand("items", (ctx, s) ->
                        ctx.send(EmoteReference.OK + "**For a list of items, please visit:**" +
                                " https://github.com/Mantaro/MantaroBot/wiki/Item-Documentation"))
                .addSubCommand("overview", (ctx, s) ->
                        ctx.send(EmoteReference.OK + "**For a feature overview, check:** https://mantaro.site/features.html"))
                .addSubCommand("birthday", (ctx, s) ->
                        ctx.send(EmoteReference.OK + "**For a guide on the birthday system, please visit:**" +
                                " https://github.com/Mantaro/MantaroBot/wiki/Birthday-101"))
        );
    }
}
