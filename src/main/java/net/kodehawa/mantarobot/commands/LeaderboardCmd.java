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
import com.google.common.eventbus.Subscribe;
import com.rethinkdb.gen.ast.ReqlFunction1;
import com.rethinkdb.model.OptArgs;
import com.rethinkdb.net.Connection;
import com.rethinkdb.utils.Types;
import net.dv8tion.jda.api.EmbedBuilder;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.utils.leaderboards.CachedLeaderboardMember;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleTreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;
import net.kodehawa.mantarobot.utils.data.JsonDataManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.rethinkdb.RethinkDB.r;

@Module
public class LeaderboardCmd {
    private final Config config = MantaroData.config().get();
    private final Connection leaderboardConnection = Utils.newDbConnection();

    @Subscribe
    public void richest(CommandRegistry cr) {
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                .spamTolerance(3)
                .limit(1)
                .cooldown(2, TimeUnit.SECONDS)
                .cooldownPenaltyIncrease(20, TimeUnit.SECONDS)
                .maxCooldown(5, TimeUnit.MINUTES)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("leaderboard")
                .build();

        SimpleTreeCommand leaderboards = cr.register("leaderboard", new SimpleTreeCommand(CommandCategory.CURRENCY) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                ctx.sendLocalized("commands.leaderboard.main_page_redirect", EmoteReference.PENCIL);
                return null;
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Returns the currency leaderboard. See subcommands for the available leaderboards.")
                        .build();
            }
        });

        leaderboards.setPredicate(ctx -> RatelimitUtils.ratelimit(rateLimiter, ctx, null));

        leaderboards.addSubCommand("gamble", new SubCommand() {
            @Override
            public String description() {
                return "Returns the gamble (times) leaderboard";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var gambleLeaderboard = getLeaderboard("playerstats", "gambleWins",
                        player -> player.pluck("id", "gambleWins"));

                ctx.send(
                        generateLeaderboardEmbed(ctx,
                                languageContext.get("commands.leaderboard.inner.gamble").formatted(EmoteReference.MONEY),
                                "commands.leaderboard.gamble", gambleLeaderboard,
                                map -> Pair.of(getMember(ctx, map.get("id").toString().split(":")[0]),
                                        map.get("gambleWins").toString()), "%s**%s#%s** - %,d", false
                        ).build()
                );
            }
        });

        leaderboards.addSubCommand("slots", new SubCommand() {
            @Override
            public String description() {
                return "Returns the slots (times) leaderboard";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var slotsLeaderboard = getLeaderboard("playerstats", "slotsWins",
                        player -> player.pluck("id", "slotsWins"));

                ctx.send(
                        generateLeaderboardEmbed(ctx,
                                languageContext.get("commands.leaderboard.inner.slots").formatted(EmoteReference.MONEY),
                                "commands.leaderboard.slots", slotsLeaderboard,
                                map -> Pair.of(getMember(ctx, map.get("id").toString().split(":")[0]),
                                        map.get("slotsWins").toString()), "%s**%s#%s** - %,d", false
                        ).build()
                );
            }
        });

        if (!config.isPremiumBot) {
            leaderboards.addSubCommand("money", new SubCommand() {
                @Override
                public String description() {
                    return "Returns the money leaderboard";
                }

                @Override
                @SuppressWarnings("unchecked")
                protected void call(Context ctx, I18nContext languageContext, String content) {
                    var seasonal = ctx.isSeasonal();
                    var tableName = seasonal ? "seasonalplayers" : "players";
                    var indexName = seasonal ? "money" : "newMoney";
                    var moneyLeaderboard = getLeaderboard(tableName, indexName,
                            player -> player.g("id"),
                            player ->
                            {
                                if (seasonal) {
                                    return player.pluck("id", "money");
                                } else {
                                    return player.pluck("id", "newMoney", r.hashMap("data", "newMoney"));
                                }
                            });

                    ctx.send(
                            generateLeaderboardEmbed(
                                    ctx,
                                    seasonal ?
                                            languageContext.get("commands.leaderboard.inner.seasonal_money").formatted(EmoteReference.MONEY) :
                                            languageContext.get("commands.leaderboard.inner.money").formatted(EmoteReference.MONEY),
                                    "commands.leaderboard.money", moneyLeaderboard,
                                    map -> {
                                        Object money;
                                        if (seasonal) {
                                            money = map.get("money");
                                        } else {
                                            money = ((Map<String, Object>) map.get("data")).get("newMoney");
                                        }

                                        return Pair.of(
                                                getMember(
                                                        ctx,
                                                        map.get("id").toString().split(":")[0]
                                                ), money.toString()
                                        );
                                    }, "%s**%s#%s** - $%,d", seasonal
                            ).build()
                    );
                }
            });
        }

        leaderboards.addSubCommand(config.isPremiumBot ? "money" : "oldmoney", new SubCommand() {
            @Override
            public String description() {
                if (config.isPremiumBot) {
                    return "Returns the money leaderboard";
                } else {
                    return "Returns the (old) pre-reset money leaderboard";
                }
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var tableName = "players";
                var moneyLeaderboard = getLeaderboard(tableName, "money",
                        player -> player.g("id"),
                        player -> player.pluck("id", "money"));

                ctx.send(
                        generateLeaderboardEmbed(ctx,
                                languageContext.get("commands.leaderboard.inner.money_old").formatted(EmoteReference.MONEY),
                                "commands.leaderboard.money", moneyLeaderboard,
                                map -> Pair.of(getMember(ctx, map.get("id").toString().split(":")[0]),
                                map.get("money").toString()), "%s**%s#%s** - $%,d", false
                        ).build()
                );
            }
        });


        leaderboards.addSubCommand("lvl", new SubCommand() {
            @Override
            public String description() {
                return "Returns the level leaderboard";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var levelLeaderboard = getLeaderboard("players", "level",
                        player -> player.g("id"),
                        player -> player.pluck("id", "level", r.hashMap("data", "experience")));

                ctx.send(
                        generateLeaderboardEmbed(ctx,
                        languageContext.get("commands.leaderboard.inner.lvl").formatted(EmoteReference.ZAP),
                                "commands.leaderboard.level", levelLeaderboard,
                        map -> {
                            @SuppressWarnings("unchecked")
                            var experience = ((Map<String, Object>) map.get("data")).get("experience");
                            return Pair.of(
                                    getMember(ctx, map.get("id").toString().split(":")[0]),
                                    map.get("level").toString() + "\n -" +
                                            languageContext.get("commands.leaderboard.inner.experience") + ":** " +
                                            experience + "**");
                        }, "%s**%s#%s** - %s", false).build()
                );
            }
        });

        leaderboards.addSubCommand("rep", new SubCommand() {
            @Override
            public String description() {
                return "Returns the reputation leaderboard";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var seasonal = ctx.isSeasonal();
                var tableName = seasonal ? "seasonalplayers" : "players";
                var reputationLeaderboard = getLeaderboard(tableName, "reputation",
                        player -> player.g("id"),
                        player -> player.pluck("id", "reputation"));

                ctx.send(
                        generateLeaderboardEmbed(ctx,
                        languageContext.get("commands.leaderboard.inner.rep").formatted(EmoteReference.REP),
                                "commands.leaderboard.reputation", reputationLeaderboard,
                        map -> Pair.of(
                                getMember(
                                        ctx,
                                        map.get("id").toString().split(":")[0]
                                ), map.get("reputation").toString()
                        ), "%s**%s#%s** - %,d", seasonal)
                        .build()
                );
            }
        });

        leaderboards.addSubCommand("streak", new SubCommand() {
            @Override
            public String description() {
                return "Returns the daily streak leaderboard";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var dailyLeaderboard = getLeaderboard("players", "userDailyStreak",
                        player -> player.g("id"),
                        player -> player.pluck("id", r.hashMap("data", "dailyStrike")));

                ctx.send(
                        generateLeaderboardEmbed(ctx,
                        languageContext.get("commands.leaderboard.inner.streak").formatted(EmoteReference.POPPER),
                                "commands.leaderboard.daily", dailyLeaderboard,
                        map -> {
                            @SuppressWarnings("unchecked")
                            var strike = ((Map<String, Object>) (map.get("data"))).get("dailyStrike").toString();
                            return Pair.of(
                                    getMember(ctx, map.get("id").toString().split(":")[0]),
                                    strike
                            );
                        }, "%s**%s#%s** - %sx", false)
                        .build()
                );
            }
        });

        leaderboards.addSubCommand("waifuvalue", new SubCommand() {
            @Override
            public String description() {
                return "Returns the waifu value leaderboard";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var seasonal = ctx.isSeasonal();
                var tableName = seasonal ? "seasonalplayers" : "players";

                var waifuLeaderboard = getLeaderboard(tableName, "waifuCachedValue",
                        player -> player.g("id"),
                        player -> player.pluck("id", r.hashMap("data", "waifuCachedValue")));

                ctx.send(
                        generateLeaderboardEmbed(ctx,
                        languageContext.get("commands.leaderboard.inner.waifu").formatted(EmoteReference.MONEY),
                                "commands.leaderboard.waifu", waifuLeaderboard,
                        map -> {
                            @SuppressWarnings("unchecked")
                            var waifuValue = ((Map<String, Object>) (map.get("data"))).get("waifuCachedValue").toString();
                            return Pair.of(
                                    getMember(ctx, map.get("id").toString().split(":")[0]),
                                    waifuValue
                            );
                        }, "%s**%s#%s** - $%,d", seasonal)
                        .build()
                );
            }
        });

        leaderboards.addSubCommand("claim", new SubCommand() {
            @Override
            public String description() {
                return "Returns the waifu claim leaderboard";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                List<Map<String, Object>> claimLeaderboard = getLeaderboard("users", "timesClaimed",
                        player -> player.pluck("id", r.hashMap("data", "timesClaimed")));

                ctx.send(
                        generateLeaderboardEmbed(ctx,
                        languageContext.get("commands.leaderboard.inner.claim").formatted(EmoteReference.HEART),
                                "commands.leaderboard.claim", claimLeaderboard,
                        map -> {
                            @SuppressWarnings("unchecked")
                            var timesClaimed = ((Map<String, Object>) (map.get("data"))).get("timesClaimed").toString();
                            return Pair.of(
                                    getMember(ctx, map.get("id").toString().split(":")[0]),
                                    timesClaimed
                            );
                        }, "%s**%s#%s** - %,d", false)
                        .build()
                );
            }
        });

        leaderboards.addSubCommand("games", new SubCommand() {
            @Override
            public String description() {
                return "Returns the games wins leaderboard";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var seasonal = ctx.isSeasonal();
                var tableName = seasonal ? "seasonalplayers" : "players";

                List<Map<String, Object>> gameLeaderboard = getLeaderboard(tableName, "gameWins",
                        player -> player.g("id"),
                        player -> player.pluck("id", r.hashMap("data", "gamesWon")));

                ctx.send(
                        generateLeaderboardEmbed(ctx,
                        languageContext.get("commands.leaderboard.inner.game").formatted(EmoteReference.ZAP),
                                "commands.leaderboard.game", gameLeaderboard,
                        map -> {
                            @SuppressWarnings("unchecked")
                            var gamesWon = ((Map<String, Object>) (map.get("data"))).get("gamesWon").toString();

                            return Pair.of(
                                    getMember(ctx, map.get("id").toString().split(":")[0]),
                                    gamesWon
                            );
                        }, "%s**%s#%s** - %,d", seasonal)
                        .build()
                );
            }
        });

        leaderboards.createSubCommandAlias("rep", "reputation");
        leaderboards.createSubCommandAlias("lvl", "level");
        leaderboards.createSubCommandAlias("streak", "daily");
        leaderboards.createSubCommandAlias("games", "wins");
        leaderboards.createSubCommandAlias("waifuvalue", "waifu");

        cr.registerAlias("leaderboard", "richest");
        cr.registerAlias("leaderboard", "lb");
    }

    private List<Map<String, Object>> getLeaderboard(String table, String index, ReqlFunction1 mapFunction) {
        return getLeaderboard(table, index, m -> true, mapFunction);
    }

    private List<Map<String, Object>> getLeaderboard(String table, String index, ReqlFunction1 filterFunction, ReqlFunction1 mapFunction) {
        return r.table(table)
                .orderBy()
                .optArg("index", r.desc(index))
                .filter(filterFunction)
                .limit(10)
                .map(mapFunction)
                .run(leaderboardConnection,
                        // This basically just means read from the available data
                        // Instead of trying to get the latest data available.
                        // For the purpose of leaderboards, this is actually pretty useful
                        // and lead to quite a few improvements in query times.
                        OptArgs.of("read_mode", "outdated"),
                        Types.mapOf(String.class, Object.class)
                )
                .toList();
    }

    private EmbedBuilder generateLeaderboardEmbed(Context ctx, String description, String leaderboardKey,
                                                  List<Map<String, Object>> lb,
                                                  Function<Map<?, ?>, Pair<CachedLeaderboardMember, String>> mapFunction,
                                                  String format, boolean isSeasonal) {
        var languageContext = ctx.getLanguageContext();
        return new EmbedBuilder()
                .setAuthor(isSeasonal ?
                                languageContext.get("commands.leaderboard.header_seasonal")
                                        .formatted(config.getCurrentSeason().getDisplay()) :
                                languageContext.get("commands.leaderboard.header"),
                        null,
                        ctx.getSelfUser().getEffectiveAvatarUrl()
                ).setDescription(description)
                .addField(
                        languageContext.get(leaderboardKey),
                        lb.stream()
                                .map(mapFunction)
                                .filter(p -> Objects.nonNull(p.getKey()))
                                .map(p -> {
                                    final var lbMember = p.getKey();
                                    //This is... an interesting place to do it lol
                                    if (lbMember.getId() == ctx.getAuthor().getIdLong()) {
                                        var player = MantaroData.db().getPlayer(ctx.getAuthor());
                                        if (player.getData().addBadgeIfAbsent(Badge.CHAMPION))
                                            player.saveUpdating();
                                    }

                                    return format.formatted(
                                            EmoteReference.BLUE_SMALL_MARKER,
                                            lbMember.getName(),
                                            config.isOwner(ctx.getAuthor()) ?
                                                    lbMember.getDiscriminator() + " (" + lbMember.getId() + ")" : lbMember.getDiscriminator(),
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
     * Caches an user in redis if they're in the leaderboard. This speeds up User lookup times tenfold.
     * The key will expire after 48 hours in the set, then we will just re-cache it as needed.
     * This should also take care of username changes.
     *
     * This value is saved in Redis so it can be used cross-node.
     * This also fixes leaderboards being incomplete in some nodes.
     *
     * This method is necessary to avoid calling Discord every single time we call a leaderboard,
     * since this might create hundreds of API requests in a few seconds, causing some nice 429s.
     *
     * @param id The id of the user.
     * @return A instance of CachedLeaderboardMember.
     * This can either be retrieved from Redis or cached on the spot if the cache didn't exist for it.
     */
    private CachedLeaderboardMember getMember(Context ctx, String id) {
        try(Jedis jedis = MantaroData.getDefaultJedisPool().getResource()) {
            var savedTo = "cachedlbuser:" + id;
            var missed = "lbmiss:" + id;

            var json = jedis.get(savedTo);
            if (json == null) {
                // No need to keep trying missed entries for a while. Entry should have a TTL of 12 hours.
                if (jedis.get(missed) != null) {
                    return null;
                }

                // Sadly a .complete() call for an User won't fill the internal cache, as JDA has no way to TTL it, instead, we will add it
                // to our own cache in Redis, and expire it in 48 hours to avoid it filling up endlessly.
                // This is to avoid having to do calls to discord all the time a leaderboard is retrieved, and only do the calls whenever
                // it's absolutely needed, or when we need to re-populate the cache.
                var user = ctx.retrieveUserById(id);

                // If no user was found, we need to return null. This is later handled on generateLeaderboardEmbed.
                if (user == null) {
                    jedis.set(missed, "1");
                    jedis.expire(missed, (int) TimeUnit.HOURS.toSeconds(12));
                    return null;
                }

                CachedLeaderboardMember cached = new CachedLeaderboardMember(
                        user.getIdLong(), user.getName(), user.getDiscriminator(), System.currentTimeMillis()
                );
                jedis.set(savedTo, JsonDataManager.toJson(cached));

                // Set the value to expire in 48 hours.
                jedis.expire(savedTo, (int) TimeUnit.HOURS.toSeconds(48));
                return cached;
            } else {
                return JsonDataManager.fromJson(json, CachedLeaderboardMember.class);
            }
        } catch (JsonProcessingException e) { // This would be odd, really.
            e.printStackTrace();
            return null;
        }
    }
}
