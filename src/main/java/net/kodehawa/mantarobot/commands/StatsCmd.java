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

package net.kodehawa.mantarobot.commands;

import com.github.natanbc.usagetracker.DefaultBucket;
import com.google.common.eventbus.Subscribe;
import lavalink.client.io.LavalinkSocket;
import lavalink.client.io.RemoteStats;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.kodehawa.mantarobot.commands.info.stats.CategoryStatsManager;
import net.kodehawa.mantarobot.commands.info.stats.CommandStatsManager;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleTreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;

import java.awt.Color;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Module
public class StatsCmd {
    @Subscribe
    public void stats(CommandRegistry cr) {
        SimpleTreeCommand statsCommand = cr.register("stats", new SimpleTreeCommand(CommandCategory.INFO) {
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("See the bot, usage or vps statistics.")
                        .setUsage("~>stats <option>` - Returns statistical information.")
                        .addParameter("option", "What to check for. See subcommands")
                        .build();
            }
        });

        statsCommand.addSubCommand("nodes", new SubCommand() {
            @Override
            public String description() {
                return "Mantaro node statistics.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                Map<String, String> nodeMap;
                try (Jedis jedis = ctx.getJedisPool().getResource()) {
                    nodeMap = jedis.hgetAll("node-stats-" + ctx.getConfig().getClientId());
                }

                var embed = new EmbedBuilder().setTitle("Mantaro Node Statistics")
                        .setDescription("This shows the current status of the online nodes. " +
                                "Every node contains a set amount of shards.")
                        .setThumbnail(ctx.getSelfUser().getAvatarUrl())
                        .setColor(Color.PINK)
                        .setFooter("Available Nodes: " + nodeMap.size());

                java.util.List<MessageEmbed.Field> fields = new LinkedList<>();
                nodeMap.entrySet().stream().sorted(
                        Comparator.comparingInt(e -> Integer.parseInt(e.getKey().split("-")[1]))
                ).forEach(node -> {
                    var nodeData = new JSONObject(node.getValue());
                    fields.add(new MessageEmbed.Field("Node " + node.getKey(),
                            """
                               **Uptime**: %s
                               **CPU Cores**: %s
                               **CPU Usage**: %s
                               **Memory**: %s
                               **Threads**: %,d
                               **Shards**: %s
                               **Guilds**: %,d
                               **User Cache**: %,d
                               **Machine Memory**: %s
                               """.formatted(
                                    Utils.formatDuration(nodeData.getLong("uptime")),
                                    nodeData.getLong("available_processors"),
                                    "%.2f%%".formatted(nodeData.getDouble("cpu_usage")),
                                    Utils.formatMemoryUsage(nodeData.getLong("used_memory"), nodeData.getLong("total_memory")),
                                    nodeData.getLong("thread_count"),
                                    nodeData.getString("shard_slice"),
                                    nodeData.getLong("guild_count"),
                                    nodeData.getLong("user_count"),
                                    Utils.formatMemoryAmount(nodeData.getLong("machine_total_memory"))
                            ), false
                    ));
                });

                DiscordUtils.sendPaginatedEmbed(ctx, embed, DiscordUtils.divideFields(3, fields));
            }
        });

        statsCommand.addSubCommand("lavalink", new SubCommand() {
            @Override
            public String description() {
                return "Lavalink node statistics.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                java.util.List<LavalinkSocket> nodes = ctx.getBot().getLavaLink().getNodes();
                var embed = new EmbedBuilder();
                embed.setTitle("Lavalink Node Statistics")
                        .setDescription("This shows the current status of the online Lavalink nodes. " +
                                "Every node contains a dynamic amount of players. This is for balancing music processes " +
                                "outside of the main bot nodes.")
                        .setThumbnail(ctx.getSelfUser().getAvatarUrl())
                        .setColor(Color.PINK)
                        .setFooter("Available Nodes: " + nodes.size());

                List<MessageEmbed.Field> fields = new LinkedList<>();

                for (LavalinkSocket node : nodes) {
                    if (!node.isAvailable()) {
                        continue;
                    }

                    RemoteStats stats = node.getStats();
                    if (stats == null) {
                        continue;
                    }

                    fields.add(new MessageEmbed.Field(node.getName(),
                            """
                            **Uptime:** %s
                            **Used Memory:** %s
                            **Free Memory:** %s
                            **Players:** %s
                            **Players Playing**: %,d
                            """.formatted(
                                    Utils.formatDuration(stats.getUptime()),
                                    Utils.formatMemoryAmount(stats.getMemUsed()),
                                    Utils.formatMemoryAmount(stats.getMemFree()),
                                    stats.getPlayers(),
                                    stats.getPlayingPlayers()
                            ), false
                    ));
                }

                DiscordUtils.sendPaginatedEmbed(ctx, embed, DiscordUtils.divideFields(3, fields));
            }
        });

        statsCommand.addSubCommand("cmds", new SubCommand() {
            @Override
            public String description() {
                return "The bot's command usage";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                if (content.equalsIgnoreCase("total")) {
                    ctx.send(CommandStatsManager.fillEmbed(DefaultBucket.TOTAL, baseEmbed(ctx, "Command Stats | Total")).build());
                    return;
                }
                ctx.send(
                        baseEmbed(ctx, "Command Stats")
                                .addField(
                                        languageContext.get("general.now"),
                                        CommandStatsManager.resume(DefaultBucket.MINUTE), false
                                )
                                .addField(
                                        languageContext.get("general.hourly"),
                                        CommandStatsManager.resume(DefaultBucket.HOUR), false
                                )
                                .addField(
                                        languageContext.get("general.daily"),
                                        CommandStatsManager.resume(DefaultBucket.DAY), false
                                )
                                .addField(
                                        languageContext.get("general.total"),
                                        CommandStatsManager.resume(DefaultBucket.TOTAL), false
                                ).build()
                );
            }
        });

        statsCommand.addSubCommand("category", new SubCommand() {
            private final CategoryStatsManager categoryStatsManager = new CategoryStatsManager();

            @Override
            public String description() {
                return "The bot's category usage";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                if (content.equalsIgnoreCase("total")) {
                    ctx.send(categoryStatsManager.fillEmbed(CategoryStatsManager.TOTAL_CATS, baseEmbed(ctx, "Category Stats | Total")).build());
                    return;
                }

                ctx.send(
                        baseEmbed(ctx, "Category Stats")
                                .addField(
                                        languageContext.get("general.now"),
                                        categoryStatsManager.resume(CategoryStatsManager.MINUTE_CATS), false
                                )
                                .addField(
                                        languageContext.get("general.hourly"),
                                        categoryStatsManager.resume(CategoryStatsManager.HOUR_CATS), false
                                )
                                .addField(
                                        languageContext.get("general.daily"),
                                        categoryStatsManager.resume(CategoryStatsManager.DAY_CATS), false
                                )
                                .addField(
                                        languageContext.get("general.total"),
                                        categoryStatsManager.resume(CategoryStatsManager.TOTAL_CATS), false
                                ).build()
                );
            }
        });
    }
}
