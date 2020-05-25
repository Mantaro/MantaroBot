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
import com.rethinkdb.gen.ast.ReqlFunction1;
import com.rethinkdb.model.OptArgs;
import com.rethinkdb.net.Connection;
import com.rethinkdb.utils.Types;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.utils.Utils.handleIncreasingRatelimit;

@Module
public class LeaderboardCmd {
    private final Config config = MantaroData.config().get();
    private final Connection leaderboardConnection = Utils.newDbConnection();

    @Subscribe
    public void richest(CommandRegistry cr) {
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                .spamTolerance(3)
                .limit(1)
                .cooldown(6, TimeUnit.SECONDS)
                .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
                .maxCooldown(5, TimeUnit.MINUTES)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("leaderboard")
                .build();

        TreeCommand leaderboards = (TreeCommand) cr.register("leaderboard", new TreeCommand(Category.CURRENCY) {
            @Override
            public Command defaultTrigger(Context context, String commandName, String content) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, String content) {
                        List<Map<String, Object>> lb1 = getLeaderboard("playerstats", "gambleWinAmount",
                                stats -> stats.pluck("id", "gambleWinAmount"), 5
                        );

                        List<Map<String, Object>> lb2 = getLeaderboard("playerstats", "slotsWinAmount",
                                stats -> stats.pluck("id", "slotsWinAmount"), 5
                        );

                        I18nContext languageContext = ctx.getLanguageContext();

                        ctx.send(
                                baseEmbed(ctx, languageContext.get("commands.leaderboard.header"))
                                        .setDescription(EmoteReference.DICE + "**Main Leaderboard page.**\n" +
                                                "To check what leaderboards we have avaliable, please run `~>help leaderboard`.\n\n" +
                                                EmoteReference.TALKING + "This page shows the top 5 in slots and gamble wins, both in amount and quantity. " +
                                                "The old money leaderboard is avaliable on `~>leaderboard money`")
                                        .setThumbnail(ctx.getAuthor().getEffectiveAvatarUrl())
                                        .addField("Gamble", lb1.stream()
                                                .map(map -> Pair.of(MantaroBot.getInstance().getShardManager().
                                                        getUserById(map.get("id").toString().split(":")[0]),
                                                        map.get("gambleWinAmount").toString())
                                                ).filter(p -> Objects.nonNull(p.getKey()))
                                                .map(p -> String.format("%s**%s#%s** - $%,d",
                                                        EmoteReference.BLUE_SMALL_MARKER, p.getKey().getName(), p.getKey().getDiscriminator(),
                                                        Long.parseLong(p.getValue()))
                                                ).collect(Collectors.joining("\n")), true)
                                        .addField("Slots", lb2.stream()
                                                .map(map -> Pair.of(MantaroBot.getInstance().getShardManager()
                                                        .getUserById(map.get("id").toString().split(":")[0]), map.get("slotsWinAmount").toString())
                                                ).filter(p -> Objects.nonNull(p.getKey()))
                                                .map(p -> String.format("%s**%s#%s** - $%,d",
                                                        EmoteReference.BLUE_SMALL_MARKER, p.getKey().getName(), p.getKey().getDiscriminator(),
                                                        Long.parseLong(p.getValue()))
                                                ).collect(Collectors.joining("\n")), true)
                                        .setFooter(String.format(languageContext.get("general.requested_by"), ctx.getAuthor().getName()), null)
                                        .build()
                        );
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Returns the currency leaderboard.")
                        .build();
            }
        });

        leaderboards.setPredicate(ctx -> Utils.handleIncreasingRatelimit(rateLimiter, ctx.getAuthor(), ctx.getEvent(), null));

        leaderboards.addSubCommand("gamble", new SubCommand() {
            @Override
            public String description() {
                return "Returns the gamble (times) leaderboard";
            }

            @Override
            protected void call(Context ctx, String content) {
                List<Map<String, Object>> c = getLeaderboard("playerstats", "gambleWins",
                        player -> player.pluck("id", "gambleWins"), 10
                );

                I18nContext languageContext = ctx.getLanguageContext();

                ctx.send(
                        generateLeaderboardEmbed(ctx,
                                String.format(languageContext.get("commands.leaderboard.inner.gamble"), EmoteReference.MONEY), "commands.leaderboard.gamble", c,
                                map -> Pair.of(MantaroBot.getInstance().getShardManager().getUserById(map.get("id").toString().split(":")[0]),
                                        map.get("gambleWins").toString()), "%s**%s#%s** - %,d", false)
                                .build()
                );
            }
        });

        leaderboards.addSubCommand("slots", new SubCommand() {
            @Override
            public String description() {
                return "Returns the slots (times) leaderboard";
            }

            @Override
            protected void call(Context ctx, String content) {
                List<Map<String, Object>> c = getLeaderboard("playerstats", "slotsWins",
                        player -> player.pluck("id", "slotsWins"), 10
                );

                I18nContext languageContext = ctx.getLanguageContext();

                ctx.send(
                        generateLeaderboardEmbed(ctx,
                        String.format(languageContext.get("commands.leaderboard.inner.slots"), EmoteReference.MONEY), "commands.leaderboard.slots", c,
                        map -> Pair.of(MantaroBot.getInstance().getShardManager().getUserById(map.get("id").toString().split(":")[0]),
                                map.get("slotsWins").toString()), "%s**%s#%s** - %,d", false)
                        .build()
                );
            }
        });

        leaderboards.addSubCommand("money", new SubCommand() {
            @Override
            public String description() {
                return "Returns the money leaderboard";
            }

            @Override
            protected void call(Context ctx, String content) {
                boolean seasonal = ctx.isSeasonal();
                String tableName = seasonal ? "seasonalplayers" : "players";

                List<Map<String, Object>> c = getLeaderboard(tableName, "money",
                        player -> player.g("id"),
                        player -> player.pluck("id", "money"), 10
                );

                I18nContext languageContext = ctx.getLanguageContext();

                ctx.send(
                        generateLeaderboardEmbed(ctx,
                        String.format((seasonal ? languageContext.get("commands.leaderboard.inner.seasonal_money") : languageContext.get("commands.leaderboard.inner.money")), EmoteReference.MONEY), "commands.leaderboard.money", c,
                        map -> Pair.of(MantaroBot.getInstance().getShardManager().getUserById(map.get("id").toString().split(":")[0]),
                                map.get("money").toString()), "%s**%s#%s** - $%,d", seasonal)
                        .build()
                );
            }
        });

        leaderboards.addSubCommand("lvl", new SubCommand() {
            @Override
            public String description() {
                return "Returns the level leaderboard";
            }

            @Override
            protected void call(Context ctx, String content) {
                List<Map<String, Object>> c = getLeaderboard("players", "level",
                        player -> player.g("id"),
                        player -> player.pluck("id", "level", r.hashMap("data", "experience")), 10
                );

                I18nContext languageContext = ctx.getLanguageContext();

                ctx.send(
                        generateLeaderboardEmbed(ctx,
                        String.format(languageContext.get("commands.leaderboard.inner.lvl"), EmoteReference.ZAP), "commands.leaderboard.level", c,
                        map -> {
                            @SuppressWarnings("unchecked")
                            var experience = ((Map<String, Object>) map.get("data")).get("experience");
                            return Pair.of(
                                    MantaroBot.getInstance().getShardManager().getUserById(map.get("id").toString().split(":")[0]),
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
            protected void call(Context ctx, String content) {
                boolean seasonal = ctx.isSeasonal();
                String tableName = seasonal ? "seasonalplayers" : "players";

                List<Map<String, Object>> c = getLeaderboard(tableName, "reputation",
                        player -> player.g("id"),
                        player -> player.pluck("id", "reputation"), 10
                );

                I18nContext languageContext = ctx.getLanguageContext();

                ctx.send(
                        generateLeaderboardEmbed(ctx,
                        String.format(languageContext.get("commands.leaderboard.inner.rep"), EmoteReference.REP), "commands.leaderboard.reputation", c,
                        map -> Pair.of(MantaroBot.getInstance().getShardManager().getUserById(map.get("id").toString().split(":")[0]),
                                map.get("reputation").toString()), "%s**%s#%s** - %,d", seasonal)
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
            protected void call(Context ctx, String content) {
                List<Map<String, Object>> c = getLeaderboard("players", "userDailyStreak",
                        player -> player.g("id"),
                        player -> player.pluck("id", r.hashMap("data", "dailyStrike")), 10
                );

                I18nContext languageContext = ctx.getLanguageContext();

                ctx.send(
                        generateLeaderboardEmbed(ctx,
                        String.format(languageContext.get("commands.leaderboard.inner.streak"), EmoteReference.POPPER), "commands.leaderboard.daily", c,
                        map -> {
                            @SuppressWarnings("unchecked")
                            var strike = ((Map<String, Object>) (map.get("data"))).get("dailyStrike").toString();
                            return Pair.of(
                                    MantaroBot.getInstance().getShardManager().getUserById(map.get("id").toString().split(":")[0]),
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
            protected void call(Context ctx, String content) {
                boolean seasonal = ctx.isSeasonal();
                String tableName = seasonal ? "seasonalplayers" : "players";

                List<Map<String, Object>> c = getLeaderboard(tableName, "waifuCachedValue",
                        player -> player.g("id"),
                        player -> player.pluck("id", r.hashMap("data", "waifuCachedValue")), 10
                );

                I18nContext languageContext = ctx.getLanguageContext();

                ctx.send(
                        generateLeaderboardEmbed(ctx,
                        String.format(languageContext.get("commands.leaderboard.inner.waifu"), EmoteReference.MONEY), "commands.leaderboard.waifu", c,
                        map -> {
                            @SuppressWarnings("unchecked")
                            var waifuValue = ((Map<String, Object>) (map.get("data"))).get("waifuCachedValue").toString();
                            return Pair.of(
                                    MantaroBot.getInstance().getShardManager().getUserById(map.get("id").toString().split(":")[0]),
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
            protected void call(Context ctx, String content) {
                List<Map<String, Object>> c = getLeaderboard("users", "timesClaimed",
                        player -> player.pluck("id", r.hashMap("data", "timesClaimed")), 10
                );

                I18nContext languageContext = ctx.getLanguageContext();

                ctx.send(
                        generateLeaderboardEmbed(ctx,
                        String.format(languageContext.get("commands.leaderboard.inner.claim"), EmoteReference.HEART), "commands.leaderboard.claim", c,
                        map -> {
                            @SuppressWarnings("unchecked")
                            var timesClaimed = ((Map<String, Object>) (map.get("data"))).get("timesClaimed").toString();
                            return Pair.of(
                                    MantaroBot.getInstance().getShardManager().getUserById(map.get("id").toString().split(":")[0]),
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
            protected void call(Context ctx, String content) {
                boolean seasonal = ctx.isSeasonal();
                String tableName = seasonal ? "seasonalplayers" : "players";

                List<Map<String, Object>> c = getLeaderboard(tableName, "gameWins",
                        player -> player.g("id"),
                        player -> player.pluck("id", r.hashMap("data", "gamesWon")), 10
                );

                I18nContext languageContext = ctx.getLanguageContext();

                ctx.send(
                        generateLeaderboardEmbed(ctx,
                        String.format(languageContext.get("commands.leaderboard.inner.game"), EmoteReference.ZAP), "commands.leaderboard.game", c,
                        map -> {
                            @SuppressWarnings("unchecked")
                            var gamesWon = ((Map<String, Object>) (map.get("data"))).get("gamesWon").toString();
                            return Pair.of(
                                    MantaroBot.getInstance().getShardManager().getUserById(map.get("id").toString().split(":")[0]),
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

    private List<Map<String, Object>> getLeaderboard(String table, String index, ReqlFunction1 mapFunction, int limit) {
        return getLeaderboard(table, index, m -> true, mapFunction, limit);
    }

    private List<Map<String, Object>> getLeaderboard(String table, String index, ReqlFunction1 filterFunction, ReqlFunction1 mapFunction, int limit) {
        return r.table(table)
                .orderBy()
                .optArg("index", r.desc(index))
                .filter(filterFunction)
                .map(mapFunction)
                .limit(limit)
                .run(leaderboardConnection, OptArgs.of("read_mode", "outdated"), Types.mapOf(String.class, Object.class))
                .toList();
    }

    private EmbedBuilder generateLeaderboardEmbed(Context ctx, String description, String leaderboardKey, List<Map<String, Object>> lb, Function<Map<?, ?>, Pair<User, String>> mapFunction, String format, boolean isSeasonal) {
        I18nContext languageContext = ctx.getLanguageContext();
        return new EmbedBuilder()
                .setAuthor(isSeasonal ?
                        String.format(languageContext.get("commands.leaderboard.header_seasonal"),
                        config.getCurrentSeason().getDisplay()) : languageContext.get("commands.leaderboard.header"), null, ctx.getSelfUser().getEffectiveAvatarUrl()
                ).setDescription(description)
                .addField(languageContext.get(leaderboardKey), lb.stream()
                        .map(mapFunction)
                        .filter(p -> Objects.nonNull(p.getKey()))
                        .map(p -> {
                            //This is... an interesting place to do it lol
                            if(p.getKey().getIdLong() == ctx.getAuthor().getIdLong()) {
                                var player = MantaroData.db().getPlayer(ctx.getAuthor());
                                if(player.getData().addBadgeIfAbsent(Badge.CHAMPION))
                                    player.saveAsync();
                            }

                            return String.format(
                                    format, EmoteReference.BLUE_SMALL_MARKER, p.getKey().getName(), p.getKey().getDiscriminator(),
                                    StringUtils.isNumeric(p.getValue()) ? Long.parseLong(p.getValue()) : p.getValue()
                            );
                        }).collect(Collectors.joining("\n")), false)
                .setFooter(String.format(languageContext.get("general.requested_by"), ctx.getAuthor().getName()), null)
                .setThumbnail(ctx.getAuthor().getEffectiveAvatarUrl());
    }
}
