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

package net.kodehawa.mantarobot.utils.commands.ratelimit;

import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.core.command.slash.IContext;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.log.LogUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
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

    private static boolean ratelimit(IncreasingRateLimiter rateLimiter, String u, RateLimitContext ctx,
                                     I18nContext i18nContext, String extraMessage, boolean spamAware) {
        if (i18nContext == null) {
            //en_US
            i18nContext = new I18nContext();
        }

        if (!config.isHandleRatelimits()) {
            return true;
        }

        RateLimit rateLimit = rateLimiter.limit(u);
        if (rateLimit.getTriesLeft() < 1) {
            ctx.send(
                    String.format(i18nContext.get("general.ratelimit.header"),
                            EmoteReference.STOPWATCH, i18nContext.get("general.ratelimit_quotes"),
                            Utils.formatDuration(i18nContext, rateLimit.getCooldown()))
                            + (extraMessage == null ? "" : "\n " + extraMessage)
                            + ((rateLimit.getSpamAttempts() > 2 && spamAware) ?
                            "\n\n" + EmoteReference.STOP + i18nContext.get("general.ratelimit.spam_1") : "")
                            + ((rateLimit.getSpamAttempts() > 4 && spamAware) ?
                            i18nContext.get("general.ratelimit.spam_2") : "")
                            + ((rateLimit.getSpamAttempts() > 10 && spamAware) ?
                            i18nContext.get("general.ratelimit.spam_3") : "")
                            + ((rateLimit.getSpamAttempts() > 15 && spamAware) ?
                            i18nContext.get("general.ratelimit.spam_4") : "")
            );

            // Assuming it's a user RL if it can parse a long since we use UUIDs for other RLs.
            try {
                //noinspection ResultOfMethodCallIgnored
                Long.parseUnsignedLong(u);
                User user;

                try {
                    var member = ctx.guild().retrieveMemberById(u).useCache(true).complete();
                    user = member.getUser();
                } catch (Exception e) {
                    log.error("Got a exception while trying to fetch a user that was just spamming?", e);
                    return false;
                }

                var guildId = ctx.guild().getId();
                var channelId = ctx.channel().getId();

                String messageId;
                if (ctx.message() != null) {
                    messageId = ctx.message().getId();
                } else {
                    messageId = "slash";
                }

                // If they go over 60 in one attempt, flag.
                if (rateLimit.getSpamAttempts() > 60 && spamAware && !loggedAttemptUsers.contains(user.getId())) {
                    loggedAttemptUsers.add(user.getId());
                    LogUtils.spambot(user, guildId, channelId, messageId, LogUtils.SpamType.OVER_SPAM_LIMIT);
                }

                onRateLimit(user, guildId, channelId, messageId);
            } catch (Exception e) {
                return false;
            }

            return false;
        }

        return true;
    }

    // Overloads
    public static boolean ratelimit(IncreasingRateLimiter rateLimiter, IContext ctx) {
        return ratelimit(rateLimiter, ctx.getAuthor().getId(), ctx.ratelimitContext(), ctx.getLanguageContext(), null, false);
    }

    public static boolean ratelimit(IncreasingRateLimiter rateLimiter, IContext ctx, I18nContext languageContext) {
        return ratelimit(rateLimiter, ctx.getAuthor().getId(), ctx.ratelimitContext(), languageContext, null, false);
    }

    public static boolean ratelimit(IncreasingRateLimiter rateLimiter, IContext ctx,
                                    I18nContext languageContext, String extra) {
        return ratelimit(rateLimiter, ctx.getAuthor().getId(), ctx.ratelimitContext(), languageContext, extra, false);
    }

    public static boolean ratelimit(IncreasingRateLimiter rateLimiter, IContext ctx, boolean spamAware) {
        return ratelimit(rateLimiter, ctx.getAuthor().getId(), ctx.ratelimitContext(), ctx.getLanguageContext(), null, spamAware);
    }

    public static boolean ratelimit(IncreasingRateLimiter rateLimiter, IContext ctx, String extra, boolean spamAware) {
        return ratelimit(rateLimiter, ctx.getAuthor().getId(), ctx.ratelimitContext(), ctx.getLanguageContext(), extra, spamAware);
    }

    public static boolean ratelimit(IncreasingRateLimiter rateLimiter, IContext ctx,
                                    I18nContext languageContext, boolean spamAware) {
        return ratelimit(rateLimiter, ctx.getAuthor().getId(), ctx.ratelimitContext(), languageContext, null, spamAware);
    }

    public static boolean ratelimit(IncreasingRateLimiter rateLimiter, IContext ctx,
                                    I18nContext languageContext, String extra, boolean spamAware) {
        return ratelimit(rateLimiter, ctx.getAuthor().getId(), ctx.ratelimitContext(), languageContext, extra, spamAware);
    }

    private static void onRateLimit(User user, String guildId, String channelId, String messageId) {
        var ratelimitedTimes = ratelimitedUsers.computeIfAbsent(user.getIdLong(), __ -> new AtomicInteger()).incrementAndGet();

        // Remember to update this if you make a command that has rls
        if (ratelimitedTimes > 1750 && !loggedSpambotUsers.contains(user.getId())) {
            loggedSpambotUsers.add(user.getId());
            LogUtils.spambot(user, guildId, channelId, messageId, LogUtils.SpamType.BLATANT);
        }
    }
}
