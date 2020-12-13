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
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import lavalink.client.io.LavalinkSocket;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDAInfo;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.processor.CommandProcessor;
import net.kodehawa.mantarobot.core.listeners.command.CommandListener;
import net.kodehawa.mantarobot.core.listeners.events.PreLoadEvent;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.APIUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;
import org.json.JSONException;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static net.kodehawa.mantarobot.commands.info.AsyncInfoMonitor.*;

@Module
public class DebugCmds {
    @Subscribe
    public void info(CommandRegistry cr) {
        cr.register("info", new SimpleCommand(CommandCategory.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var config = ctx.getConfig();
                var bot = ctx.getBot();
                var guilds = 0L;
                var users = 0L;
                var clusterTotal = 0L;
                var players = 0L;
                var totalMemory = 0L;
                var queueSize = 0L;
                var totalThreadCount = 0L;
                var totalCommandCount = 0L;

                String nodeData;
                try (Jedis jedis = ctx.getJedisPool().getResource()) {
                    nodeData = jedis.hget("node-stats-" + config.getClientId(), "node-" + bot.getNodeNumber());
                }

                try(Jedis jedis = ctx.getJedisPool().getResource()) {
                    var stats = jedis.hgetAll("shardstats-" + config.getClientId());
                    for (var shards : stats.entrySet()) {
                        var json = new JSONObject(shards.getValue());
                        guilds += json.getLong("guild_count");
                        users += json.getLong("cached_users");
                    }

                    var clusters = jedis.hgetAll("node-stats-" + config.getClientId());
                    for (var cluster : clusters.entrySet()) {
                        var json = new JSONObject(cluster.getValue());
                        totalMemory += json.getLong("used_memory");
                        queueSize += json.getLong("queue_size");
                        totalThreadCount += json.getLong("thread_count");
                        totalCommandCount += json.getLong("commands_ran");
                    }

                    clusterTotal = clusters.size();
                }

                // We don't need to account for node stats delay here
                if (config.isPremiumBot()) {
                    queueSize = ctx.getBot().getAudioManager().getTotalQueueSize();
                }

                List<LavalinkSocket> lavaLinkSockets = ctx.getBot().getLavaLink().getNodes();
                for (var lavaLink : lavaLinkSockets) {
                    if (lavaLink.isAvailable() && lavaLink.getStats() != null) {
                        players += lavaLink.getStats().getPlayingPlayers();
                    }
                }

                var responseTotal = bot.getShardManager().getShardCache()
                        .stream()
                        .mapToLong(JDA::getResponseTotal)
                        .sum();

                var mApiRequests = 0;
                try {
                    mApiRequests = new JSONObject(APIUtils.getFrom("/mantaroapi/ping")).getInt("requests_served");
                } catch (IOException | JSONException ignored) { }

                // Get the master node.
                var node = new JSONObject(nodeData);
                var shardManager = ctx.getShardManager();
                var jda = ctx.getJDA();

                ctx.send("```prolog\n"
                        + " --------- Technical Information --------- \n\n"
                        + "Uptime: " + Utils.formatDuration(node.getLong("uptime")) + "\n"
                        + "Version: " + MantaroInfo.VERSION + " (Git: " + MantaroInfo.GIT_REVISION + ")\n"
                        + "Libraries: " + "[ JDA: %s, LP: %s ]".formatted(JDAInfo.VERSION, PlayerLibrary.VERSION) + "\n"
                        + "Commands: " +
                        CommandProcessor.REGISTRY.commands()
                                .values()
                                .stream()
                                .filter(command -> command.category() != null)
                                .count()
                        + "\n\n --------- Debug Information --------- \n\n"
                        + "Replies: " + "[ Discord: %,d, MAPI: %,d ]".formatted(responseTotal, mApiRequests) + "\n"
                        + "Nodes: " + "%,d (Current: %,d)".formatted(clusterTotal, ctx.getBot().getNodeNumber()) + "\n"
                        + "CPU: " + "%.2f%% (Cores: %,d)".formatted(getInstanceCPUUsage() * 100, getAvailableProcessors()) + "\n"
                        + "Memory: " +  Utils.formatMemoryAmount(totalMemory) +
                        " [Node: " + Utils.formatMemoryAmount(getTotalMemory() - getFreeMemory())  + "]"
                        + "\n\n --------- Mantaro Information --------- \n\n"
                        + "Guilds: " + "%,d (Node: %,d)".formatted(guilds, shardManager.getGuildCache().size()) + "\n"
                        + "User Cache: " + "%,d (Node: %,d)".formatted(users, shardManager.getUserCache().size()) + "\n"
                        + "Shards: " + bot.getShardManager().getShardsTotal() + " (This: " + jda.getShardInfo().getShardString() + ")" + "\n"
                        + "Threads: " + "%,d (Node: %,d)".formatted(totalThreadCount, Thread.activeCount()) + "\n"
                        + "Commands Used: " + "%,d (Node: %,d)".formatted(totalCommandCount, CommandListener.getCommandTotal()) + "\n"
                        + "Overall: " + "[ Players: %,d, Queue: %,d ]".formatted(players, queueSize) + "\n"
                        + "```"
                );
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Gets the bot technical information. Nothing all that interesting, but shows cute stats.")
                        .build();
            }
        });

        cr.registerAlias("info", "status");
    }

    @Subscribe
    public void shard(CommandRegistry cr) {
        cr.register("shard", new SimpleCommand(CommandCategory.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                long nodeAmount;
                try(Jedis jedis = MantaroData.getDefaultJedisPool().getResource()) {
                    nodeAmount = jedis.hlen("node-stats-" + ctx.getConfig().getClientId());
                }

                final var jda = ctx.getJDA();
                final var guildCache = jda.getGuildCache();

                ctx.sendLocalized("commands.shard.info",
                        jda.getShardInfo().getShardId(),
                        ctx.getBot().getShardManager().getShardsTotal(),
                        ctx.getBot().getNodeNumber(), nodeAmount,
                        guildCache.size(), jda.getUserCache().size(),
                        guildCache.stream().mapToLong(guild -> guild.getMemberCache().size()).sum()
                );
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Returns in what shard I am.")
                        .build();
            }
        });
    }

    @Subscribe
    public void ping(CommandRegistry cr) {
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                .limit(1)
                .spamTolerance(2)
                .cooldown(2, TimeUnit.SECONDS)
                .maxCooldown(30, TimeUnit.SECONDS)
                .randomIncrement(true)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("ping")
                .build();

        cr.register("ping", new SimpleCommand(CommandCategory.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                I18nContext languageContext = ctx.getLanguageContext();
                if (!RatelimitUtils.ratelimit(rateLimiter, ctx, false))
                    return;

                long start = System.currentTimeMillis();
                ctx.getChannel().sendMessage("Pinging...").queue(v -> {
                    long ping = System.currentTimeMillis() - start;
                    //display: show a random quote, translated.
                    v.editMessageFormat(
                            languageContext.get("commands.ping.text"), EmoteReference.MEGA,
                            languageContext.get("commands.ping.display"), ping, ctx.getJDA().getGatewayPing()
                    ).queue();
                });
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Plays Ping-Pong with Discord and prints out the result.")
                        .build();
            }
        });
    }

    @Subscribe
    public void shardinfo(CommandRegistry cr) {
        cr.register("shardinfo", new SimpleCommand(CommandCategory.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                StringBuilder builder = new StringBuilder();
                Map<String, String> stats;

                try(Jedis jedis = ctx.getJedisPool().getResource()) {
                    stats = jedis.hgetAll("shardstats-" + ctx.getConfig().getClientId());
                }

                //id, shard_status, cached_users, guild_count, last_ping_diff, gateway_ping
                stats.entrySet().stream().sorted(
                        Comparator.comparingInt(e -> Integer.parseInt(e.getKey()))
                ).forEach(shard -> {
                    var jsonData = new JSONObject(shard.getValue());
                    var shardId = Integer.parseInt(shard.getKey());
    
                    builder.append("%-7s | %-9s | U: %-6d | G: %-4d | EV: %-8s | P: %-6s".formatted(
                            shardId + " / " + ctx.getBot().getShardManager().getShardsTotal(),
                            jsonData.getString("shard_status"),
                            jsonData.getLong("cached_users"),
                            jsonData.getLong("guild_count"),
                            jsonData.getLong("last_ping_diff") + " ms",
                            jsonData.getLong("gateway_ping")
                    ));
    
                    if (shardId == ctx.getJDA().getShardInfo().getShardId()) {
                        builder.append(" <- CURRENT");
                    }
    
                    builder.append("\n");
                });

                List<String> m = DiscordUtils.divideString(builder);
                List<String> messages = new LinkedList<>();

                for (String shard : m) {
                    messages.add("%s\n\n```prolog\n%s```"
                            .formatted("**Mantaro's Shard Information**", shard)
                    );
                }

                DiscordUtils.list(ctx.getEvent(), 150, false, messages);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Returns information about shards.")
                        .build();
            }
        });
    }

    @Subscribe
    public void onPreLoad(PreLoadEvent e) {
        start();
    }
}
