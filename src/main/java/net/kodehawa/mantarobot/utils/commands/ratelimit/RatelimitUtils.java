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

package net.kodehawa.mantarobot.utils.commands.ratelimit;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.LogUtils;
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

    private static boolean ratelimit(IncreasingRateLimiter rateLimiter, String u,
                                    GuildMessageReceivedEvent event, I18nContext i18nContext, boolean spamAware) {
        if (i18nContext == null) {
            //en_US
            i18nContext = new I18nContext();
        }

        if (!config.isHandleRatelimits()) {
            return true;
        }

        RateLimit rateLimit = rateLimiter.limit(u);
        if (rateLimit.getTriesLeft() < 1) {
            event.getChannel().sendMessage(
                    String.format(i18nContext.get("general.ratelimit.header"),
                            EmoteReference.STOPWATCH, i18nContext.get("general.ratelimit_quotes"),
                            Utils.formatDuration(rateLimit.getCooldown()))
                            + ((rateLimit.getSpamAttempts() > 2 && spamAware) ?
                            "\n\n" + EmoteReference.STOP + i18nContext.get("general.ratelimit.spam_1") : "")
                            + ((rateLimit.getSpamAttempts() > 4 && spamAware) ?
                            i18nContext.get("general.ratelimit.spam_2") : "")
                            + ((rateLimit.getSpamAttempts() > 10 && spamAware) ?
                            i18nContext.get("general.ratelimit.spam_3") : "")
                            + ((rateLimit.getSpamAttempts() > 15 && spamAware) ?
                            i18nContext.get("general.ratelimit.spam_4") : "")
            ).queue();

            // Assuming it's an user RL if it can parse a long since we use UUIDs for other RLs.
            try {
                //noinspection ResultOfMethodCallIgnored
                Long.parseUnsignedLong(u);
                User user;

                try {
                    var member = event.getGuild().retrieveMemberById(u, false).complete();
                    user = member.getUser();
                } catch (Exception e) {
                    log.error("Got a exception while trying to fetch a user that was just spamming?", e);
                    return false;
                }

                var guildId = event.getGuild().getId();
                var channelId = event.getChannel().getId();
                var messageId = event.getMessage().getId();

                // If they go over 50 in one attempt, flag as blatant.
                if (rateLimit.getSpamAttempts() > 50 && spamAware && !loggedAttemptUsers.contains(user.getId())) {
                    loggedAttemptUsers.add(user.getId());
                    LogUtils.spambot(user, guildId, channelId, messageId, LogUtils.SpamType.BLATANT);
                }

                onRateLimit(user, guildId, channelId, messageId);
            } catch (Exception e) {
                return false;
            }

            return false;
        }

        return true;
    }

    public static boolean ratelimit(IncreasingRateLimiter rateLimiter, Context ctx) {
        return ratelimit(rateLimiter, ctx.getAuthor().getId(), ctx.getEvent(), ctx.getLanguageContext(), false);
    }

    public static boolean ratelimit(IncreasingRateLimiter rateLimiter, Context ctx, I18nContext languageContext) {
        return ratelimit(rateLimiter, ctx.getAuthor().getId(), ctx.getEvent(), languageContext, false);
    }

    public static boolean ratelimit(IncreasingRateLimiter rateLimiter, Context ctx, boolean spamAware) {
        return ratelimit(rateLimiter, ctx.getAuthor().getId(), ctx.getEvent(), ctx.getLanguageContext(), spamAware);
    }

    public static boolean ratelimit(IncreasingRateLimiter rateLimiter, Context ctx, I18nContext languageContext, boolean spamAware) {
        return ratelimit(rateLimiter, ctx.getAuthor().getId(), ctx.getEvent(), languageContext, spamAware);
    }

    private static void onRateLimit(User user, String guildId, String channelId, String messageId) {
        var ratelimitedTimes = ratelimitedUsers.computeIfAbsent(user.getIdLong(), __ -> new AtomicInteger()).incrementAndGet();

        // Remember to update this if you make a command that has rls
        if (ratelimitedTimes > 1450 && !loggedSpambotUsers.contains(user.getId())) {
            loggedSpambotUsers.add(user.getId());
            LogUtils.spambot(user, guildId, channelId, messageId, LogUtils.SpamType.BLATANT);
        }
    }
}
