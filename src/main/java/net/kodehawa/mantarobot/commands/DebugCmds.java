/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import lavalink.client.io.LavalinkSocket;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDAInfo;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.MantaroListener;
import net.kodehawa.mantarobot.core.listeners.events.PreLoadEvent;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.core.command.processor.CommandProcessor;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.APIUtils;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
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

                try(Jedis jedis = ctx.getJedisPool().getResource()) {
                    var stats = jedis.hgetAll("shardstats-" + config.getClientId());
                    for (var shards : stats.entrySet()) {
                        var json = new JSONObject(shards.getValue());
                        guilds += json.getLong("guild_count");
                        users += json.getLong("cached_users");
                    }

                    var clusters = jedis.hgetAll("node-stats-" + config.getClientId());
                    for(var cluster : clusters.entrySet()) {
                        var json = new JSONObject(cluster.getValue());
                        totalMemory += json.getLong("used_memory");
                        queueSize += json.getLong("queue_size");
                        totalThreadCount += json.getLong("thread_count");
                        totalCommandCount += json.getLong("commands_ran");
                    }

                    clusterTotal = clusters.size();
                }

                List<LavalinkSocket> lavaLinkSockets = ctx.getBot().getLavaLink().getNodes();
                for(var lavaLink : lavaLinkSockets) {
                    if(lavaLink.isAvailable())
                        players += lavaLink.getStats().getPlayers();
                }

                var responseTotal = bot.getShardManager().getShardCache()
                        .stream()
                        .mapToLong(JDA::getResponseTotal)
                        .sum();

                var mApiRequests = 0;
                try {
                    mApiRequests = new JSONObject(APIUtils.getFrom("/mantaroapi/ping")).getInt("requests_served");
                } catch (IOException ignored) { }

                ctx.send("```prolog\n"
                        + " --------- Technical Information --------- \n\n"
                        + "Commands: " + CommandProcessor.REGISTRY.commands().values().stream().filter(command -> command.category() != null).count() + "\n"
                        + "Bot Version: " + MantaroInfo.VERSION + " [" + MantaroInfo.GIT_REVISION + "]\n"
                        + "JDA Version: " + JDAInfo.VERSION + "\n"
                        + "Lavaplayer Version: " + PlayerLibrary.VERSION + "\n"
                        + "API Responses: " + String.format("%,d [MAPI: %,d]", responseTotal, mApiRequests) + "\n"
                        + "Nodes: " + String.format("%,d [Current: %,d]", clusterTotal, ctx.getBot().getNodeNumber()) + "\n"
                        + "CPU Usage: " + String.format("%.2f", getInstanceCPUUsage() * 100) + "%" + "\n"
                        + "CPU Cores: " + getAvailableProcessors() + "\n"
                        + "Shard Info: " + ctx.getJDA().getShardInfo()
                        + "\n\n --------- Mantaro Information --------- \n\n"
                        + "Guilds: " + String.format("%,d [Local: %,d]", guilds, ctx.getShardManager().getGuildCache().size()) + "\n"
                        + "User Cache: " + String.format("%,d [Local: %,d]", users, ctx.getShardManager().getUserCache().size()) + "\n"
                        + "Shards: " + bot.getShardManager().getShardsTotal() + " [Current: " + ctx.getJDA().getShardInfo().getShardId() + "]" + "\n"
                        + "Threads: " + String.format("%,d [Local: %,d]", totalThreadCount, Thread.activeCount()) + "\n"
                        + "Executed Commands: " + String.format("%,d", totalCommandCount) + "\n"
                        + "Music Players: " + players + "\n"
                        + "Logs: " + String.format("%,d", MantaroListener.getLogTotalInt()) + "\n"
                        + "Memory: " + Utils.formatMemoryAmount(getTotalMemory() - getFreeMemory()) + " (Total: " +  Utils.formatMemoryAmount(totalMemory) + ")\n"
                        + "Queue Size: " + String.format("%,d", queueSize)
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

                var jda = ctx.getJDA();

                ctx.sendLocalized("commands.shard.info",
                        jda.getShardInfo().getShardId(), MantaroBot.getInstance().getShardManager().getShardsTotal(),
                        ctx.getBot().getNodeNumber(), nodeAmount,
                        jda.getUserCache().size(), jda.getGuildCache().size()
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
                .cooldown(10, TimeUnit.SECONDS)
                .maxCooldown(10, TimeUnit.SECONDS)
                .randomIncrement(true)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("ping")
                .build();

        cr.register("ping", new SimpleCommand(CommandCategory.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                I18nContext languageContext = ctx.getLanguageContext();
                if (!Utils.handleIncreasingRatelimit(rateLimiter, ctx.getAuthor(), ctx.getEvent(), languageContext, false))
                    return;

                long start = System.currentTimeMillis();
                ctx.getChannel().sendMessage("Pinging...").queue(v -> {
                    long ping = System.currentTimeMillis() - start;
                    //display: show a random quote, translated.
                    v.editMessageFormat(
                            languageContext.get("commands.ping.text"), EmoteReference.MEGA,
                            languageContext.get("commands.ping.display"), ping, ratePing(ping, languageContext),
                            ctx.getJDA().getGatewayPing()
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
    
                    builder.append(String.format(
                            "%-7s | %-9s | U: %-6d | G: %-4d | EV: %-8s | P: %-6s",
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

                for (String s1 : m)
                    messages.add(String.format("%s\n```prolog\n%s```", "**Mantaro's Shard Information. Use &p >> and &p << to move pages, &cancel to exit.**", s1));

                DiscordUtils.listText(ctx.getEvent(), 45, false, messages);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Returns information about shards.")
                        .build();
            }
        });
    }

    private String ratePing(long ping, I18nContext languageContext) {
        if (ping == 69)
            return languageContext.get("commands.ping.quotes.69");
        if (ping <= 1)
            return languageContext.get("commands.ping.quotes.1"); //just in case...
        if (ping <= 10)
            return languageContext.get("commands.ping.quotes.10");
        if (ping <= 100)
            return languageContext.get("commands.ping.quotes.100");
        if (ping <= 200)
            return languageContext.get("commands.ping.quotes.200");
        if (ping <= 300)
            return languageContext.get("commands.ping.quotes.300");
        if (ping <= 400)
            return languageContext.get("commands.ping.quotes.400");
        if (ping <= 500)
            return languageContext.get("commands.ping.quotes.500");
        if (ping <= 600)
            return languageContext.get("commands.ping.quotes.600");
        if (ping <= 700)
            return languageContext.get("commands.ping.quotes.700");
        if (ping <= 800)
            return languageContext.get("commands.ping.quotes.800");
        if (ping <= 900)
            return languageContext.get("commands.ping.quotes.900");
        if (ping <= 1600)
            return languageContext.get("commands.ping.quotes.1600");
        if (ping <= 10000)
            return languageContext.get("commands.ping.quotes.10000");

        return languageContext.get("commands.ping.quotes.default");
    }

    @Subscribe
    public void onPreLoad(PreLoadEvent e) {
        start();
    }
}
