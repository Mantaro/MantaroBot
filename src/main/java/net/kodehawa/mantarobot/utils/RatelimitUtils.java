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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.utils;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RateLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RatelimitUtils {
    private static final Logger log = LoggerFactory.getLogger(RatelimitUtils.class);

    public static final Map<Long, AtomicInteger> ratelimitedUsers = new ConcurrentHashMap<>();
    private static final Set<String> loggedSpambotUsers = ConcurrentHashMap.newKeySet();
    private static final Set<String> loggedAttemptUsers = ConcurrentHashMap.newKeySet();
    private static final Config config = MantaroData.config().get();

    public static boolean handleIncreasingRatelimit(IncreasingRateLimiter rateLimiter, String u,
                                                    GuildMessageReceivedEvent event, I18nContext context, boolean spamAware) {
        if (context == null) {
            //en_US
            context = new I18nContext();
        }

        if(!config.isHandleRatelimits()) {
            return true;
        }

        RateLimit rateLimit = rateLimiter.limit(u);
        if (rateLimit.getTriesLeft() < 1) {
            event.getChannel().sendMessage(
                    String.format(context.get("general.ratelimit.header"),
                            EmoteReference.STOPWATCH, context.get("general.ratelimit_quotes"),
                            Utils.formatDuration(rateLimit.getCooldown()))
                            + ((rateLimit.getSpamAttempts() > 2 && spamAware) ?
                            "\n\n" + EmoteReference.STOP + context.get("general.ratelimit.spam_1") : "")
                            + ((rateLimit.getSpamAttempts() > 4 && spamAware) ?
                            context.get("general.ratelimit.spam_2") : "")
                            + ((rateLimit.getSpamAttempts() > 10 && spamAware) ?
                            context.get("general.ratelimit.spam_3") : "")
                            + ((rateLimit.getSpamAttempts() > 15 && spamAware) ?
                            context.get("general.ratelimit.spam_4") : "")
            ).queue();

            //Assuming it's an user RL if it can parse a long since we use UUIDs for other RLs.
            try {
                //noinspection ResultOfMethodCallIgnored
                Long.parseUnsignedLong(u);
                User user;

                try {
                    Member member = event.getGuild().retrieveMemberById(u, false).complete();
                    user = member.getUser();
                } catch (Exception e) {
                    log.error("Got a exception while trying to fetch a user that was just spamming?", e);
                    return false;
                }

                String guildId = event.getGuild().getId();
                String channelId = event.getChannel().getId();
                String messageId = event.getMessage().getId();

                // If they go over 50 in one attempt, flag as blatant.
                if (rateLimit.getSpamAttempts() > 50 && spamAware && !loggedAttemptUsers.contains(user.getId())) {
                    loggedAttemptUsers.add(user.getId());
                    LogUtils.spambot(user, guildId, channelId, messageId, LogUtils.SpamType.BLATANT);
                }

                onRateLimit(user, guildId, channelId, messageId);
            } catch (Exception ignored) {}

            return false;
        }

        return true;
    }

    public static boolean handleIncreasingRatelimit(IncreasingRateLimiter rateLimiter, User u,
                                                    GuildMessageReceivedEvent event, I18nContext context, boolean spamAware) {
        return handleIncreasingRatelimit(rateLimiter, u.getId(), event, context, spamAware);
    }

    public static boolean handleIncreasingRatelimit(IncreasingRateLimiter rateLimiter, User u,
                                                    GuildMessageReceivedEvent event, I18nContext context) {
        return handleIncreasingRatelimit(rateLimiter, u.getId(), event, context, true);
    }

    public static boolean handleIncreasingRatelimit(IncreasingRateLimiter rateLimiter, User u, Context ctx) {
        return handleIncreasingRatelimit(rateLimiter, u.getId(), ctx.getEvent(), ctx.getLanguageContext(), true);
    }

    public static boolean handleIncreasingRatelimit(IncreasingRateLimiter rateLimiter, User u, Context ctx, boolean spamAware) {
        return handleIncreasingRatelimit(rateLimiter, u.getId(), ctx.getEvent(), ctx.getLanguageContext(), spamAware);
    }

    private static void onRateLimit(User user, String guildId, String channelId, String messageId) {
        int ratelimitedTimes = ratelimitedUsers.computeIfAbsent(user.getIdLong(), __ -> new AtomicInteger()).incrementAndGet();
        if (ratelimitedTimes > 700 && !loggedSpambotUsers.contains(user.getId())) {
            loggedSpambotUsers.add(user.getId());
            LogUtils.spambot(user, guildId, channelId, messageId, LogUtils.SpamType.BLATANT);
        }
    }
}
