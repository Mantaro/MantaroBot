/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import com.rethinkdb.gen.ast.ReqlFunction1;
import com.rethinkdb.model.OptArgs;
import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Cursor;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.RateLimiter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.utils.Utils.handleDefaultRatelimit;

@Module
public class LeaderboardCmd {
    @Subscribe
    public void richest(CommandRegistry cr) {
        final RateLimiter rateLimiter = new RateLimiter(TimeUnit.SECONDS, 5);
        final String pattern = ":g$";

        TreeCommand leaderboards = (TreeCommand) cr.register("leaderboard", new TreeCommand(Category.CURRENCY) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                        List<Map> lb1 = getLeaderboard("playerstats", "gambleWinAmount",
                                stats -> stats.pluck("id", "gambleWinAmount"), 5
                        );

                        List<Map> lb2 = getLeaderboard("playerstats", "slotsWinAmount",
                                stats -> stats.pluck("id", "slotsWinAmount"), 5
                        );

                        event.getChannel().sendMessage(
                                baseEmbed(event, languageContext.get("commands.leaderboard.header"))
                                        .setDescription(EmoteReference.DICE + "**Main Leaderboard page.**\n" +
                                                "To check what leaderboards we have avaliable, please run `~>help leaderboard`.\n\n" +
                                                EmoteReference.TALKING + "This page shows the top 5 in slots and gamble wins, both in amount and quantity. The old money leaderboard is avaliable on `~>leaderboard money`")
                                        .setThumbnail(event.getAuthor().getEffectiveAvatarUrl())
                                        .addField("Gamble", lb1.stream()
                                                .map(map -> Pair.of(MantaroBot.getInstance().getUserById(map.get("id").toString().split(":")[0]), map.get("gambleWinAmount").toString()))
                                                .filter(p -> Objects.nonNull(p.getKey()))
                                                .map(p -> String.format("%s**%s#%s** - $%,d", EmoteReference.BLUE_SMALL_MARKER, p.getKey().getName(), p.getKey().getDiscriminator(), Long.parseLong(p.getValue())))
                                                .collect(Collectors.joining("\n")), true)
                                        .addField("Slots", lb2.stream()
                                                .map(map -> Pair.of(MantaroBot.getInstance().getUserById(map.get("id").toString().split(":")[0]), map.get("slotsWinAmount").toString()))
                                                .filter(p -> Objects.nonNull(p.getKey()))
                                                .map(p -> String.format("%s**%s#%s** - $%,d", EmoteReference.BLUE_SMALL_MARKER, p.getKey().getName(), p.getKey().getDiscriminator(), Long.parseLong(p.getValue())))
                                                .collect(Collectors.joining("\n")), true)
                                        .setFooter(String.format(languageContext.get("general.requested_by"), event.getAuthor().getName()), null)
                                        .build()
                        ).queue();
                    }
                };
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Leaderboard")
                        .setDescription("**Returns the leaderboard.**")
                        .addField("Usage", "`~>leaderboard` - **Returns the main leaderboard.**\n" +
                                "`~>leaderboard rep` - **Returns the reputation leaderboard.**\n" +
                                "`~>leaderboard lvl` - **Returns the level leaderboard.**\n" +
                                "`~>leaderboard gamble` - **Returns the gamble (times) leaderboard.**\n" +
                                "`~>leaderboard slots` - **Returns the slots (times) leaderboard.**\n" +
                                "`~>leaderboard money` - **Returns the money leaderboard.**\n" +
                                "`~>leaderboard waifu` - **Returns the waifu value leaderboard.**\n" +
                                "`~>leaderboard claim` - **Returns the waifu claims leaderboard.**\n" +
                                //"`~>leaderboard games`  - **Returns the games leaderboard.**\n" +
                                "`~>leaderboard streak` - **Returns the daily streak leaderboard.**", false)
                        .build();
            }
        });

        leaderboards.setPredicate(event -> handleDefaultRatelimit(rateLimiter, event.getAuthor(), event));

        leaderboards.addSubCommand("gamble", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                List<Map> c = getLeaderboard("playerstats", "gambleWins",
                        player -> player.pluck("id", "gambleWins"), 10
                );

                event.getChannel().sendMessage(generateLeaderboardEmbed(event, languageContext,
                        String.format(languageContext.get("commands.leaderboard.inner.gamble"), EmoteReference.MONEY),"commands.leaderboard.gamble", c,
                        map -> Pair.of(MantaroBot.getInstance().getUserById(map.get("id").toString().split(":")[0]),
                                map.get("gambleWins").toString()), "%s**%s#%s** - %,d")
                        .build()
                ).queue();
            }
        });

        leaderboards.addSubCommand("slots", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                List<Map> c = getLeaderboard("playerstats", "slotsWins",
                        player -> player.pluck("id", "slotsWins"), 10
                );

                event.getChannel().sendMessage(generateLeaderboardEmbed(event, languageContext,
                        String.format(languageContext.get("commands.leaderboard.inner.slots"), EmoteReference.MONEY),"commands.leaderboard.slots", c,
                        map -> Pair.of(MantaroBot.getInstance().getUserById(map.get("id").toString().split(":")[0]),
                                map.get("slotsWins").toString()), "%s**%s#%s** - %,d")
                        .build()
                ).queue();
            }
        });

        leaderboards.addSubCommand("money", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                List<Map> c = getLeaderboard("players", "money",
                        player -> player.g("id").match(pattern),
                        player -> player.pluck("id", "money"), 10
                );

                event.getChannel().sendMessage(generateLeaderboardEmbed(event, languageContext,
                        String.format(languageContext.get("commands.leaderboard.inner.money"), EmoteReference.MONEY),"commands.leaderboard.money", c,
                        map -> Pair.of(MantaroBot.getInstance().getUserById(map.get("id").toString().split(":")[0]),
                                map.get("money").toString()), "%s**%s#%s** - $%,d")
                        .build()
                ).queue();
            }
        });

        leaderboards.addSubCommand("lvl", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                List<Map> c = getLeaderboard("players", "level",
                        player -> player.g("id").match(pattern),
                        player -> player.pluck("id", "level", r.hashMap("data", "experience")), 10
                );

                event.getChannel().sendMessage(generateLeaderboardEmbed(event, languageContext,
                        String.format(languageContext.get("commands.leaderboard.inner.lvl"), EmoteReference.ZAP),"commands.leaderboard.level", c,
                        map -> Pair.of(MantaroBot.getInstance().getUserById(map.get("id").toString().split(":")[0]), map.get("level").toString() +
                                "\n -" + languageContext.get("commands.leaderboard.inner.experience")  + ":** " +
                                ((Map)map.get("data")).get("experience") + "**"), "%s**%s#%s** - %s")
                        .build()
                ).queue();
            }
        });

        leaderboards.addSubCommand("rep", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                List<Map> c = getLeaderboard("players", "reputation",
                        player -> player.g("id").match(pattern),
                        player -> player.pluck("id", "reputation"), 10
                );

                event.getChannel().sendMessage(generateLeaderboardEmbed(event, languageContext,
                        String.format(languageContext.get("commands.leaderboard.inner.rep"), EmoteReference.REP),"commands.leaderboard.reputation", c,
                        map -> Pair.of(MantaroBot.getInstance().getUserById(map.get("id").toString().split(":")[0]),
                                map.get("reputation").toString()), "%s**%s#%s** - %,d")
                        .build()
                ).queue();
            }
        });

        leaderboards.addSubCommand("streak", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                List<Map> c = getLeaderboard("players", "userDailyStreak",
                        player -> player.g("id").match(pattern),
                        player -> player.pluck("id", r.hashMap("data", "dailyStrike")), 10
                );

                event.getChannel().sendMessage(generateLeaderboardEmbed(event, languageContext,
                        String.format(languageContext.get("commands.leaderboard.inner.streak"), EmoteReference.POPPER),"commands.leaderboard.daily", c,
                        map -> Pair.of(MantaroBot.getInstance().getUserById(map.get("id").toString().split(":")[0]),
                                ((HashMap)(map.get("data"))).get("dailyStrike").toString()), "%s**%s#%s** - %sx")
                        .build()
                ).queue();
            }
        });

        leaderboards.addSubCommand("waifuvalue", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                List<Map> c = getLeaderboard("players", "waifuCachedValue",
                        player -> player.g("id").match(pattern),
                        player -> player.pluck("id", r.hashMap("data", "waifuCachedValue")), 10
                );

                event.getChannel().sendMessage(generateLeaderboardEmbed(event, languageContext,
                        String.format(languageContext.get("commands.leaderboard.inner.waifu"), EmoteReference.MONEY),"commands.leaderboard.waifu", c,
                        map -> Pair.of(MantaroBot.getInstance().getUserById(map.get("id").toString().split(":")[0]),
                                ((HashMap)(map.get("data"))).get("waifuCachedValue").toString()), "%s**%s#%s** - $%,d")
                        .build()
                ).queue();
            }
        });

        leaderboards.addSubCommand("claim", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                List<Map> c = getLeaderboard("users", "timesClaimed",
                        player -> player.pluck("id", r.hashMap("data", "timesClaimed")), 10
                );

                event.getChannel().sendMessage(generateLeaderboardEmbed(event, languageContext,
                        String.format(languageContext.get("commands.leaderboard.inner.claim"), EmoteReference.HEART),"commands.leaderboard.claim", c,
                        map -> Pair.of(MantaroBot.getInstance().getUserById(map.get("id").toString().split(":")[0]),
                                ((HashMap)(map.get("data"))).get("timesClaimed").toString()), "%s**%s#%s** - %,d")
                        .build()
                ).queue();
            }
        });

        leaderboards.addSubCommand("games", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                List<Map> c = getLeaderboard("players", "gameWins",
                        player -> player.g("id").match(pattern),
                        player -> player.pluck("id", r.hashMap("data", "gamesWon")), 10
                );

                event.getChannel().sendMessage(generateLeaderboardEmbed(event, languageContext,
                        String.format(languageContext.get("commands.leaderboard.inner.game"), EmoteReference.ZAP),"commands.leaderboard.game", c,
                        map -> Pair.of(MantaroBot.getInstance().getUserById(map.get("id").toString().split(":")[0]),
                                ((HashMap)(map.get("data"))).get("gamesWon").toString()), "%s**%s#%s** - %,d")
                        .build()
                ).queue();
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

    private List<Map> getLeaderboard(String table, String index, ReqlFunction1 mapFunction, int limit) {
        return getLeaderboard(table, index, m -> true, mapFunction, limit);
    }

    private List<Map> getLeaderboard(String table, String index, ReqlFunction1 filterFunction, ReqlFunction1 mapFunction, int limit) {
        Cursor<Map> m;
        try(Connection conn = Utils.newDbConnection()) {
            m = r.table(table)
                    .orderBy()
                    .optArg("index", r.desc(index))
                    .filter(filterFunction)
                    .map(mapFunction)
                    .limit(limit)
                    .run(conn, OptArgs.of("read_mode", "outdated"));
        }

        List<Map> c = m.toList();
        m.close();

        return c;
    }

    private EmbedBuilder generateLeaderboardEmbed(GuildMessageReceivedEvent event, I18nContext languageContext, String description, String leaderboardKey, List<Map> lb, Function<Map, Pair<User, String>> mapFunction, String format) {
        return new EmbedBuilder().setAuthor(languageContext.get("commands.leaderboard.header"), null, event.getJDA().getSelfUser().getEffectiveAvatarUrl())
                .setDescription(description)
                .addField(languageContext.get(leaderboardKey), lb.stream()
                        .map(mapFunction)
                        .filter(p -> Objects.nonNull(p.getKey()))
                        .map(p -> String.format(format, EmoteReference.BLUE_SMALL_MARKER, p.getKey().getName(),
                                p.getKey().getDiscriminator(), StringUtils.isNumeric(p.getValue()) ? Long.parseLong(p.getValue()) : p.getValue())
                        )
                        .collect(Collectors.joining("\n")), false)
                .setFooter(String.format(languageContext.get("general.requested_by"), event.getAuthor().getName()), null)
                .setThumbnail(event.getAuthor().getEffectiveAvatarUrl());
    }
}
