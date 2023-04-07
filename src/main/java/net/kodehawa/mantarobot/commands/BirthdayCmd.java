/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.utils.birthday.BirthdayCacher;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Defer;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.meta.Help;
import net.kodehawa.mantarobot.core.command.meta.Name;
import net.kodehawa.mantarobot.core.command.meta.Options;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Module
public class BirthdayCmd {
    private static final Logger log = LoggerFactory.getLogger(BirthdayCmd.class);

    // This will get invalidated as a whole every 23 hours, we don't really *need* to expire entries here
    // because BirthdayCacher will make it so it refreshes at the same period as the BirthdayCacher gets refreshed.
    // Therefore the birthday list here can be kept up-to-date.
    private static final Cache<Long, ConcurrentHashMap<Long, BirthdayCacher.BirthdayData>> guildBirthdayCache = CacheBuilder.newBuilder()
            .maximumSize(2500)
            .build();

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(Birthday.class);
    }

    @Name("birthday")
    @Description("The hub for birthday-related commands.")
    @Category(CommandCategory.UTILS)
    public static class Birthday extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {}

        @Name("set")
        @Description("Sets your birthday date. Only useful if the server has enabled this functionality.")
        @Options({
                @Options.Option(type = OptionType.STRING, name = "date", description = "The date to add. The format is dd-MM (30-01 for January 30th)", required = true)
        })
        @Help(
                description = "Sets your birthday date. Only useful if the server has enabled this functionality.",
                usage = "`/birthday set date:[date format]` - Sets your birthday to the specified date in dd-MM-yyyy format.",
                parameters = {@Help.Parameter(name = "date", description = "A date in dd-MM format (13-02 for February 13th for example).")}
        )
        public static class Set extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                // Twice. Yep.
                var parseFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                var displayFormat = DateTimeFormatter.ofPattern("dd-MM");
                MonthDay birthdayDate;
                String date;
                var extra = "";

                try {
                    String birthday = ctx.getOptionAsString("date");
                    birthday = birthday.replace("/", "-");
                    var parts = new ArrayList<>(Arrays.asList(birthday.split("-")));

                    if (Integer.parseInt(parts.get(0)) > 31 || Integer.parseInt(parts.get(1)) > 12) {
                        ctx.replyEphemeral("commands.birthday.error_date", EmoteReference.ERROR);
                        return;
                    }

                    if (parts.size() > 2) {
                        ctx.replyEphemeral("commands.birthday.new_format", EmoteReference.ERROR);
                        return;
                    }

                    //Add a year so it parses and saves using the old format. Yes, this is also cursed.
                    parts.add("2037");
                    date = String.join("-", parts);
                    birthdayDate = MonthDay.parse(birthday, displayFormat);
                } catch (Exception e) {
                    ctx.replyEphemeral("commands.birthday.error_date", EmoteReference.ERROR);
                    return;
                }

                final var display = displayFormat.format(birthdayDate);
                // This whole leap year stuff is cursed when you work with dates using raw strings tbh.
                // Only I could come up with such an idea like this on 2016. Now I regret it with pain... peko.
                var leap = display.equals("29-02");
                if (leap) {
                    extra += "\n" + ctx.getLanguageContext().get("commands.birthday.leap");
                    date = date.replace("2037", "2036"); // Cursed workaround since 2036 is a leap.
                }

                final var birthdayFormat = parseFormat.format(parseFormat.parse(date));

                //Actually save it to the user's profile.
                DBUser dbUser = ctx.getDBUser();
                dbUser.getData().setBirthday(birthdayFormat);
                dbUser.saveUpdating();

                ctx.replyEphemeral("commands.birthday.added_birthdate", EmoteReference.CORRECT, display, extra);
            }
        }

        @Name("allowserver")
        @Description("Allows the server where you send this command to announce your birthday.")
        @Help(description = "Allows the server where you send this command to announce your birthday.")
        public static class AllowServer extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var dbGuild = ctx.getDBGuild();
                var author = ctx.getAuthor();
                if (dbGuild.getAllowedBirthdays().contains(author.getId())) {
                    ctx.replyEphemeral("commands.birthday.already_allowed", EmoteReference.ERROR);
                    return;
                }

                dbGuild.getAllowedBirthdays().add(author.getId());
                dbGuild.save();

                var cached = guildBirthdayCache.getIfPresent(ctx.getGuild().getIdLong());
                var cachedBirthday = ctx.getBot().getBirthdayCacher().getCachedBirthdays().get(author.getIdLong());
                if (cached != null && cachedBirthday != null) {
                    cached.put(author.getIdLong(), cachedBirthday);
                }

                ctx.replyEphemeral("commands.birthday.allowed_server", EmoteReference.CORRECT);
            }
        }

        @Name("denyserver")
        @Description("Denies the server where you send this command from announcing your birthday.")
        @Help(description = "Denies the server where you send this command from announcing your birthday.")
        public static class RemoveServer extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var dbGuild = ctx.getDBGuild();
                var author = ctx.getAuthor();
                if (!dbGuild.getAllowedBirthdays().contains(author.getId())) {
                    ctx.replyEphemeral("commands.birthday.already_denied", EmoteReference.CORRECT);
                    return;
                }

                dbGuild.getAllowedBirthdays().remove(author.getId());
                dbGuild.save();

                var cached = guildBirthdayCache.getIfPresent(ctx.getGuild().getIdLong());
                if (cached != null) {
                    cached.remove(author.getIdLong());
                }

                ctx.replyEphemeral("commands.birthday.denied", EmoteReference.CORRECT);
            }
        }

        @Name("remove")
        @Description("Removes your set birthday date.")
        @Help(description = "Removes your set birthday date.")
        public static class Remove extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var user = ctx.getDBUser();
                user.getData().setBirthday(null);
                user.save();

                ctx.replyEphemeral("commands.birthday.reset", EmoteReference.CORRECT);
            }
        }

        @Name("list")
        @Defer
        @Description("Gives all of the birthdays for this server.")
        @Help(description = "Gives all of the birthdays for this server.")
        public static class List extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                BirthdayCacher cacher = MantaroBot.getInstance().getBirthdayCacher();
                try {
                    if (cacher != null) {
                        var globalBirthdays = cacher.getCachedBirthdays();
                        if (globalBirthdays.isEmpty()) {
                            ctx.reply("commands.birthday.no_global_birthdays", EmoteReference.SAD);
                            return;
                        }

                        var guild = ctx.getGuild();
                        var data = ctx.getDBGuild();
                        var ids = data.getAllowedBirthdays().stream().map(Long::parseUnsignedLong).collect(Collectors.toList());

                        if (ids.isEmpty()) {
                            ctx.reply("commands.birthday.no_guild_birthdays", EmoteReference.ERROR);
                            return;
                        }

                        var guildCurrentBirthdays = getBirthdayMap(ctx.getGuild().getIdLong(), ids);
                        if (guildCurrentBirthdays.isEmpty()) {
                            ctx.reply("commands.birthday.no_guild_birthdays", EmoteReference.ERROR);
                            return;
                        }

                        var birthdays = guildCurrentBirthdays.entrySet().stream()
                                .sorted(Comparator.comparingLong(i -> i.getValue().day()))
                                .filter(birthday -> ids.contains(birthday.getKey()))
                                .limit(100).toList();


                        var bdIds = birthdays.stream().map(Map.Entry::getKey).collect(Collectors.toList());
                        guild.retrieveMembersByIds(bdIds).onSuccess(members ->
                                sendBirthdayList(ctx, members, guildCurrentBirthdays, null, false)
                        );
                    } else {
                        ctx.reply("commands.birthday.cache_not_running", EmoteReference.SAD);
                    }
                } catch (Exception e) {
                    ctx.reply("commands.birthday.error", EmoteReference.SAD);
                    log.error("Error on birthday list display!", e);
                }
            }
        }

        @Name("month")
        @Defer
        @Description("Checks the current birthday date for the specified month.")
        @Options({@Options.Option(type = OptionType.INTEGER, name = "month", description = "The month, in number format (January is 1, etc)", maxValue = 12)})
        @Help(description = "Checks the current birthday date for the specified month. Example: `/birthday month 1`")
        public static class Month extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var cacher = MantaroBot.getInstance().getBirthdayCacher();
                var calendar = Calendar.getInstance();
                long month = calendar.get(Calendar.MONTH);
                long specifiedMonth = ctx.getOptionAsLong("month", 1); // There's no month zero so...

                if (specifiedMonth != 0) {
                    // Subtract here, so we can do the check properly up there.
                    month = specifiedMonth - 1;
                }

                //noinspection MagicConstant
                calendar.set(calendar.get(Calendar.YEAR), (int) month, Calendar.MONDAY);
                try {
                    if (cacher != null) {
                        final var cachedBirthdays = cacher.getCachedBirthdays();
                        if (cachedBirthdays.isEmpty()) {
                            ctx.reply("commands.birthday.no_global_birthdays", EmoteReference.SAD);
                            return;
                        }

                        var data = ctx.getDBGuild();
                        var ids = data.getAllowedBirthdays().stream().map(Long::parseUnsignedLong).collect(Collectors.toList());
                        var guildCurrentBirthdays = getBirthdayMap(ctx.getGuild().getIdLong(), ids);

                        if (ids.isEmpty()) {
                            ctx.reply("commands.birthday.no_guild_birthdays", EmoteReference.ERROR);
                            return;
                        }

                        var calendarMonth = calendar.get(Calendar.MONTH) + 1;
                        var birthdays = guildCurrentBirthdays.entrySet().stream()
                                .filter(bds -> bds.getValue().month() == calendarMonth)
                                .sorted(Comparator.comparingLong(i -> i.getValue().day()))
                                .limit(100).toList();

                        if (birthdays.isEmpty()) {
                            ctx.reply("commands.birthday.no_guild_month_birthdays", EmoteReference.ERROR, month + 1, EmoteReference.BLUE_SMALL_MARKER);
                            return;
                        }

                        var bdIds = birthdays.stream().map(Map.Entry::getKey).collect(Collectors.toList());
                        ctx.getGuild().retrieveMembersByIds(bdIds).onSuccess(members ->
                                sendBirthdayList(ctx, members, guildCurrentBirthdays, calendar, true)
                        );
                    } else {
                        ctx.reply("commands.birthday.cache_not_running", EmoteReference.SAD);
                    }
                } catch (Exception e) {
                    ctx.reply("commands.birthday.error", EmoteReference.SAD);
                    log.error("Error on birthday month display!", e);
                }
            }
        }
    }

    private static void sendBirthdayList(SlashContext ctx, List<Member> members, Map<Long, BirthdayCacher.BirthdayData> guildCurrentBirthdays,
                                         Calendar calendar, boolean month) {
        StringBuilder builder = new StringBuilder();
        var languageContext = ctx.getLanguageContext();
        var guild = ctx.getGuild();
        var memberSort = members.stream()
                .sorted(Comparator.comparingInt(i -> {
                    var bd = guildCurrentBirthdays.get(i.getIdLong());
                    // So I don't forget later: this is equivalent to day + (month * 31)
                    // And this is so we get a stable sort of day/month
                    return (int) (bd.day() + (bd.month() << 5));
                })).toList();

        for (Member member : memberSort) {
            var birthday = guildCurrentBirthdays.get(member.getIdLong());
            builder.append("+ %-20s : %s ".formatted(StringUtils.limit(member.getEffectiveName(), 20), birthday.day() + "-" + birthday.month()));
            builder.append("\n");
        }

        var parts = DiscordUtils.divideString(1000, '\n', builder);

        List<String> messages = new LinkedList<>();
        for (String part : parts) {
            var help = languageContext.get("general.button_react");

            if (month && calendar != null) {
                messages.add(
                        languageContext.get("commands.birthday.header").formatted(
                                ctx.getGuild().getName(),
                                Utils.capitalize(calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH))
                        ) + "```diff\n%s```".formatted(part)
                );
            } else {
                messages.add(languageContext.get("commands.birthday.full_header")
                        .formatted(guild.getName(), (parts.size() > 1 ? help : "") + "```diff\n%s```".formatted(part))
                );
            }
        }

        if (parts.isEmpty()) {
            ctx.reply("commands.birthday.no_guild_birthdays", EmoteReference.ERROR);
            return;
        }

        DiscordUtils.listButtons(ctx.getUtilsContext(), 45, messages);
    }

    public static Cache<Long, ConcurrentHashMap<Long, BirthdayCacher.BirthdayData>> getGuildBirthdayCache() {
        return guildBirthdayCache;
    }

    private static ConcurrentHashMap<Long, BirthdayCacher.BirthdayData> getBirthdayMap(long guildId, List<Long> allowed) {
        ConcurrentHashMap<Long, BirthdayCacher.BirthdayData> guildCurrentBirthdays = new ConcurrentHashMap<>();
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
