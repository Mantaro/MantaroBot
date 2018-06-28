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
import net.dv8tion.jda.core.entities.MessageEmbed;
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
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.utils.Utils.handleDefaultRatelimit;

@Module
public class LeaderboardCmd {
    @Subscribe
    public void richest(CommandRegistry cr) {
        final RateLimiter rateLimiter = new RateLimiter(TimeUnit.SECONDS, 10);
        final String pattern = ":g$";

        TreeCommand leaderboards = (TreeCommand) cr.register("leaderboard", new TreeCommand(Category.CURRENCY) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {

                    }
                };
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Leaderboard")
                        .setDescription("**Returns the leaderboard.**")
                        .addField("Usage", "`~>leaderboard` - **Returns the money leaderboard.**\n" +
                                "`~>leaderboard rep` - **Returns the reputation leaderboard.**\n" +
                                "`~>leaderboard lvl` - **Returns the level leaderboard.**\n" +
                                "`~>leaderboard streak` - **Returns the daily streak leaderboard.**", false)
                        .build();
            }
        });

        leaderboards.setPredicate(event -> handleDefaultRatelimit(rateLimiter, event.getAuthor(), event));

        leaderboards.addSubCommand("money", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                List<Map> c = getLeaderboard("players", "money",
                        player -> player.g("id").match(pattern),
                        player -> player.pluck("id", "money"), 10
                );

                event.getChannel().sendMessage(
                        baseEmbed(event, languageContext.get("commands.leaderboard.money"), event.getJDA().getSelfUser().getEffectiveAvatarUrl()
                        ).setDescription(c.stream()
                                .map(map -> Pair.of(MantaroBot.getInstance().getUserById(map.get("id").toString().split(":")[0]), map.get("money").toString()))
                                .filter(p -> Objects.nonNull(p.getKey()))
                                .map(p -> String.format("%s**%s#%s** - $%s", EmoteReference.MARKER, p.getKey().getName(), p
                                        .getKey().getDiscriminator(), p.getValue()))
                                .collect(Collectors.joining("\n"))
                        ).build()
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

                event.getChannel().sendMessage(
                        baseEmbed(event, languageContext.get("commands.leaderboard.level"), event.getJDA().getSelfUser().getEffectiveAvatarUrl()
                        ).setDescription(c.stream()
                                .map(map -> Pair.of(MantaroBot.getInstance().getUserById(map.get("id").toString().split(":")[0]), map.get("level").toString() +
                                        "\n -" + languageContext.get("commands.leaderboard.inner.experience")  + ":** " + ((Map)map.get("data")).get("experience") + "**"))
                                .filter(p -> Objects.nonNull(p.getKey()))
                                .map(p -> String.format("%s**%s#%s** - %s", EmoteReference.MARKER, p.getKey().getName(), p
                                        .getKey().getDiscriminator(), p.getValue()))
                                .collect(Collectors.joining("\n"))
                        ).build()
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

                event.getChannel().sendMessage(
                        baseEmbed(event, languageContext.get("commands.leaderboard.reputation"), event.getJDA().getSelfUser().getEffectiveAvatarUrl()
                        ).setDescription(c.stream()
                                .map(map -> Pair.of(MantaroBot.getInstance().getUserById(map.get("id").toString().split(":")[0]), map.get("reputation").toString()))
                                .filter(p -> Objects.nonNull(p.getKey()))
                                .map(p -> String.format("%s**%s#%s** - %s", EmoteReference.MARKER, p.getKey().getName(), p
                                        .getKey().getDiscriminator(), p.getValue()))
                                .collect(Collectors.joining("\n"))
                        ).build()
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

                event.getChannel().sendMessage(
                        baseEmbed(event, languageContext.get("commands.leaderboard.daily"), event.getJDA().getSelfUser().getEffectiveAvatarUrl()
                        ).setDescription(c.stream()
                                .map(map -> Pair.of(MantaroBot.getInstance().getUserById(map.get("id").toString().split(":")[0]), ((HashMap)(map.get("data"))).get("dailyStrike").toString()))
                                .filter(p -> Objects.nonNull(p.getKey()))
                                .map(p -> String.format("%s**%s#%s** - %sx", EmoteReference.MARKER, p.getKey().getName(), p
                                        .getKey().getDiscriminator(), p.getValue()))
                                .collect(Collectors.joining("\n"))
                        ).build()
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

                event.getChannel().sendMessage(
                        baseEmbed(event, languageContext.get("commands.leaderboard.waifu"), event.getJDA().getSelfUser().getEffectiveAvatarUrl()
                        ).setDescription(c.stream()
                                .map(map -> Pair.of(MantaroBot.getInstance().getUserById(map.get("id").toString().split(":")[0]), ((HashMap)(map.get("data"))).get("waifuCachedValue").toString()))
                                .filter(p -> Objects.nonNull(p.getKey()))
                                .map(p -> String.format("%s**%s#%s** - $%s", EmoteReference.MARKER, p.getKey().getName(), p
                                        .getKey().getDiscriminator(), p.getValue()))
                                .collect(Collectors.joining("\n"))
                        ).build()
                ).queue();
            }
        });

        leaderboards.addSubCommand("claim", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                List<Map> c = getLeaderboard("users", "timesClaimed",
                        user -> true,
                        player -> player.pluck("id", r.hashMap("data", "timesClaimed")), 10
                );

                event.getChannel().sendMessage(
                        baseEmbed(event, languageContext.get("commands.leaderboard.claim"), event.getJDA().getSelfUser().getEffectiveAvatarUrl()
                        ).setDescription(c.stream()
                                .map(map -> Pair.of(MantaroBot.getInstance().getUserById(map.get("id").toString().split(":")[0]), ((HashMap)(map.get("data"))).get("timesClaimed").toString()))
                                .filter(p -> Objects.nonNull(p.getKey()))
                                .map(p -> String.format("%s**%s#%s** - %s", EmoteReference.MARKER, p.getKey().getName(), p
                                        .getKey().getDiscriminator(), p.getValue()))
                                .collect(Collectors.joining("\n"))
                        ).build()
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

                event.getChannel().sendMessage(
                        baseEmbed(event, languageContext.get("commands.leaderboard.game"), event.getJDA().getSelfUser().getEffectiveAvatarUrl()
                        ).setDescription(c.stream()
                                .map(map -> Pair.of(MantaroBot.getInstance().getUserById(map.get("id").toString().split(":")[0]), ((HashMap)(map.get("data"))).get("gamesWon").toString()))
                                .filter(p -> Objects.nonNull(p.getKey()))
                                .map(p -> String.format("%s**%s#%s** - %s", EmoteReference.MARKER, p.getKey().getName(), p
                                        .getKey().getDiscriminator(), p.getValue()))
                                .collect(Collectors.joining("\n"))
                        ).build()
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
}
