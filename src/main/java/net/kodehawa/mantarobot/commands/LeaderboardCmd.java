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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.eventbus.Subscribe;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Sorts;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.utils.leaderboards.CachedLeaderboardMember;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Defer;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.slash.IContext;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.MongoUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.PlayerStats;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;
import net.kodehawa.mantarobot.utils.data.JsonDataManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.conversions.Bson;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Module
public class LeaderboardCmd {
    private static final Config config = MantaroData.config().get();
    private static final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
            .spamTolerance(3)
            .limit(1)
            .cooldown(2, TimeUnit.SECONDS)
            .cooldownPenaltyIncrease(20, TimeUnit.SECONDS)
            .maxCooldown(5, TimeUnit.MINUTES)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("leaderboard")
            .build();

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(Leaderboard.class);
    }

    @Description("Shows various leaderboards.")
    @Category(CommandCategory.CURRENCY)
    public static class Leaderboard extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {}

        @Override
        public Predicate<SlashContext> getPredicate() {
            return ctx -> RatelimitUtils.ratelimit(rateLimiter, ctx, null);
        }

        @Description("Sends the money leaderboard.")
        public static class Money extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                if (config.premiumBot) {
                    var tableName = "players";
                    var moneyLeaderboard = getLeaderboard(tableName, Player.class, Sorts.descending("oldMoney"));
                    send(ctx,
                            generateLeaderboardEmbed(ctx,
                                    ctx.getLanguageContext().get("commands.leaderboard.inner.money_old").formatted(EmoteReference.MONEY),
                                    "commands.leaderboard.money", moneyLeaderboard,
                                    player -> Pair.of(getMember(ctx, player.getId()),
                                            String.valueOf(player.getOldMoney())), "%s**%s#%s** - $%,d"
                            ).build()
                    );
                    return;
                }

                var tableName = "players";
                var moneyLeaderboard = getLeaderboard(tableName, Player.class, Sorts.descending("newMoney"));
                send(ctx,
                        generateLeaderboardEmbed(
                                ctx, ctx.getLanguageContext().get("commands.leaderboard.inner.money").formatted(EmoteReference.MONEY),
                                "commands.leaderboard.money", moneyLeaderboard,
                                player -> {
                                    var money = player.getNewMoney();
                                    return Pair.of(getMember(ctx, player.getId()), String.valueOf(money));
                                }, "%s**%s#%s** - $%,d"
                        ).build()
                );
            }
        }

        @Description("Sends the gamble leaderboard.")
        @Defer
        public static class Gamble extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var gambleLeaderboard = getLeaderboard("playerstats", PlayerStats.class, Sorts.descending("gambleWins"));
                send(ctx,
                        generateLeaderboardEmbed(ctx,
                                ctx.getLanguageContext().get("commands.leaderboard.inner.gamble").formatted(EmoteReference.MONEY),
                                "commands.leaderboard.gamble", gambleLeaderboard,
                                stats -> Pair.of(getMember(ctx, stats.getId()), String.valueOf(stats.getGambleWins())), "%s**%s#%s** - %,d"
                        ).build()
                );
            }
        }

        @Description("Sends the slots leaderboard.")
        @Defer
        public static class Slots extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var slotsLeaderboard = getLeaderboard("playerstats", PlayerStats.class, Sorts.descending("slotsWins"));
                send(ctx,
                        generateLeaderboardEmbed(ctx,
                                ctx.getLanguageContext().get("commands.leaderboard.inner.slots").formatted(EmoteReference.MONEY),
                                "commands.leaderboard.slots", slotsLeaderboard,
                                stats -> Pair.of(getMember(ctx, stats.getId()),
                                        String.valueOf(stats.getSlotsWins())), "%s**%s#%s** - %,d"
                        ).build()
                );
            }
        }

        @Description("Sends the reputation leaderboard.")
        @Defer
        public static class Reputation extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var tableName = "players";
                var reputationLeaderboard = getLeaderboard(tableName, Player.class, Sorts.descending("reputation"));
                send(ctx,
                        generateLeaderboardEmbed(ctx,
                                ctx.getLanguageContext().get("commands.leaderboard.inner.rep").formatted(EmoteReference.REP),
                                "commands.leaderboard.reputation", reputationLeaderboard,
                                player -> Pair.of(getMember(ctx, player.getId()), String.valueOf(player.getReputation())), "%s**%s#%s** - %,d")
                                .build()
                );
            }
        }

        @Description("Sends the daily streak leaderboard.")
        @Defer
        public static class Daily extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var dailyLeaderboard = getLeaderboard("players", Player.class, Sorts.descending("dailyStreak"));
                send(ctx,
                        generateLeaderboardEmbed(ctx,
                                ctx.getLanguageContext().get("commands.leaderboard.inner.streak")
                                        .formatted(EmoteReference.POPPER), "commands.leaderboard.daily", dailyLeaderboard,
                                player -> {
                                    var streak = player.getDailyStreak();
                                    return Pair.of(getMember(ctx, player.getId()), String.valueOf(streak));
                                }, "%s**%s#%s** - %sx")
                                .build()
                );
            }
        }

        @Description("Sends the waifu claim leaderboard.")
        @Defer
        public static class Claim extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var claimLeaderboard = getLeaderboard("users", MongoUser.class, Sorts.descending("timesClaimed"));
                send(ctx,
                        generateLeaderboardEmbed(ctx,
                                ctx.getLanguageContext().get("commands.leaderboard.inner.claim").formatted(EmoteReference.HEART),
                                "commands.leaderboard.claim", claimLeaderboard,
                                user -> {
                                    var timesClaimed = user.getTimesClaimed();
                                    return Pair.of(getMember(ctx, user.getId()), String.valueOf(timesClaimed));
                                }, "%s**%s#%s** - %,d")
                                .build()
                );

            }
        }

        @Description("Sends the game wins leaderboard.")
        @Defer
        public static class Games extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var tableName = "players";
                var gameLeaderboard = getLeaderboard(tableName, Player.class, Sorts.descending("gamesWon"));
                send(ctx,
                        generateLeaderboardEmbed(ctx,
                                ctx.getLanguageContext().get("commands.leaderboard.inner.game").formatted(EmoteReference.ZAP),
                                "commands.leaderboard.game", gameLeaderboard,
                                player -> {
                                    var gamesWon = player.getGamesWon();
                                    return Pair.of(getMember(ctx, player.getId()), String.valueOf(gamesWon));
                                }, "%s**%s#%s** - %,d")
                                .build()
                );
            }
        }
    }

    private static <T> AggregateIterable<T> getLeaderboard(String table, Class<T> deserialize, Bson sortFunction) {
        // Somehow using an index is automatic?

        return MantaroData.db().dbMantaro().getCollection(table, deserialize)
                .aggregate(List.of(
                        Aggregates.sort(sortFunction),
                        Aggregates.limit(15))
                );
    }

    private static <T> EmbedBuilder generateLeaderboardEmbed(IContext ctx, String description, String leaderboardKey,
                                                         AggregateIterable<T> lbObject,
                                                         Function<T, Pair<CachedLeaderboardMember, String>> mapFunction,
                                                         String format) {
        var languageContext = ctx.getLanguageContext();
        return new EmbedBuilder()
                .setAuthor(languageContext.get("commands.leaderboard.header"),
                        null,
                        ctx.getGuild().getSelfMember().getEffectiveAvatarUrl()
                ).setDescription(description)
                .addField(
                        languageContext.get(leaderboardKey),
                        StreamSupport.stream(lbObject.spliterator(), false)
                                .map(mapFunction)
                                .filter(p -> Objects.nonNull(p.getKey()))
                                .map(p -> {
                                    final var lbMember = p.getKey();
                                    //This is... an interesting place to do it lol
                                    if (lbMember.getId() == ctx.getAuthor().getIdLong()) {
                                        var player = MantaroData.db().getPlayer(ctx.getAuthor());
                                        if (player.addBadgeIfAbsent(Badge.CHAMPION))
                                            player.updateAllChanged();
                                    }

                                    return format.formatted(
                                            EmoteReference.BLUE_SMALL_MARKER,
                                            lbMember.getName(),
                                            config.isOwner(ctx.getAuthor()) ? "(" + lbMember.getId() + ")" : "",
                                            StringUtils.isNumeric(p.getValue()) ? Long.parseLong(p.getValue()) : p.getValue()
                                    );
                                })
                                .collect(Collectors.joining("\n")),
                        false
                ).setFooter(
                        languageContext.get("general.requested_by").formatted(ctx.getAuthor().getName()),
                        null)
                .setThumbnail(ctx.getAuthor().getEffectiveAvatarUrl());
    }

    /**
     * Caches a user in redis if they're in the leaderboard. This speeds up User lookup times tenfold.
     * The key will expire after 48 hours in the set, then we will just re-cache it as needed.
     * This should also take care of username changes.
     * <p>
     * This value is saved in Redis, so it can be used cross-node.
     * This also fixes leaderboards being incomplete in some nodes.
     * <p>
     * This method is necessary to avoid calling Discord every single time we call a leaderboard,
     * since this might create hundreds of API requests in a few seconds, causing some nice 429s.
     *
     * @param id The id of the user.
     * @return A instance of CachedLeaderboardMember.
     * This can either be retrieved from Redis or cached on the spot if the cache didn't exist for it.
     */
    private static CachedLeaderboardMember getMember(IContext ctx, String id) {
        try(Jedis jedis = MantaroData.getDefaultJedisPool().getResource()) {
            var savedTo = "cachedlbuser:" + id;
            var missed = "lbmiss:" + id;

            var json = jedis.get(savedTo);
            if (json == null) {
                // No need to keep trying missed entries for a while. Entry should have a TTL of 12 hours.
                if (jedis.get(missed) != null) {
                    return null;
                }

                // Sadly a .complete() call for a User won't fill the internal cache, as JDA has no way to TTL it, instead, we will add it
                // to our own cache in Redis, and expire it in 48 hours to avoid it filling up endlessly.
                // This is to avoid having to do calls to discord all the time a leaderboard is retrieved, and only do the calls whenever
                // it's absolutely needed, or when we need to re-populate the cache.
                var user = ctx.getShardManager().retrieveUserById(id).complete();

                // If no user was found, we need to return null. This is later handled on generateLeaderboardEmbed.
                if (user == null) {
                    jedis.set(missed, "1");
                    jedis.expire(missed, TimeUnit.HOURS.toSeconds(12));
                    return null;
                }

                CachedLeaderboardMember cached = new CachedLeaderboardMember(
                        user.getIdLong(), user.getName(), user.getDiscriminator(), System.currentTimeMillis()
                );
                jedis.set(savedTo, JsonDataManager.toJson(cached));

                // Set the value to expire in 48 hours.
                jedis.expire(savedTo, TimeUnit.HOURS.toSeconds(48));
                return cached;
            } else {
                return JsonDataManager.fromJson(json, CachedLeaderboardMember.class);
            }
        } catch (JsonProcessingException e) { // This would be odd, really.
            e.printStackTrace();
            return null;
        }
    }

    private static void send(IContext ctx, MessageEmbed embed) {
        ctx.send(
                embed,
                ActionRow.of(
                        Button.link("https://www.mantaro.site/mantaro-wiki/legal/terms-of-service", "Terms of Service"),
                        Button.link("https://www.mantaro.site/mantaro-wiki", "Wiki"),
                        Button.link("https://www.mantaro.site/mantaro-wiki/currency/101", "Currency Guide")
                )
        );
    }
}
