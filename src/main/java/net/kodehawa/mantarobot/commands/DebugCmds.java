/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
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
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.utils.cache.SnowflakeCacheView;
import net.kodehawa.lib.imageboards.ImageBoard;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.commands.currency.RateLimiter;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.MantaroListener;
import net.kodehawa.mantarobot.core.listeners.command.CommandListener;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.processor.DefaultCommandProcessor;
import net.kodehawa.mantarobot.core.shard.MantaroShard;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static net.kodehawa.mantarobot.commands.info.AsyncInfoMonitor.*;
import static net.kodehawa.mantarobot.utils.Utils.handleDefaultRatelimit;

@Module
public class DebugCmds {
    @Subscribe
    public void info(CommandRegistry cr) {
        cr.register("info", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                SnowflakeCacheView<Guild> guilds = MantaroBot.getInstance().getGuildCache();
                SnowflakeCacheView<VoiceChannel> vc = MantaroBot.getInstance().getVoiceChannelCache();
                SnowflakeCacheView<User> users = MantaroBot.getInstance().getUserCache();

                event.getChannel().sendMessage("```prolog\n"
                        + " --------- Technical Information --------- \n\n"
                        + "Commands: " + DefaultCommandProcessor.REGISTRY.commands().values().stream().filter(command -> command.category() != null).count() + "\n"
                        + "Bot Version: " + MantaroInfo.VERSION + "\n"
                        + "JDA Version: " + JDAInfo.VERSION + "\n"
                        + "Lavaplayer Version: " + PlayerLibrary.VERSION + "\n"
                        + "Image API Version: " + ImageBoard.VERSION + "\n"
                        + "API Responses: " + MantaroBot.getInstance().getResponseTotal() + "\n"
                        + "CPU Usage: " + String.format("%.2f", getVpsCPUUsage()) + "%" + "\n"
                        + "CPU Cores: " + getAvailableProcessors() + "\n"
                        + "Shard Info: " + event.getJDA().getShardInfo()
                        + "\n\n --------- Mantaro Information --------- \n\n"
                        + "Guilds: " + guilds.size() + "\n"
                        + "Users: " + users.size() + "\n"
                        + "Shards: " + MantaroBot.getInstance().getShardedMantaro().getTotalShards() + " (Current: " + (MantaroBot.getInstance().getShardForGuild(event.getGuild().getId()).getId()) + ")" + "\n"
                        + "Threads: " + Thread.activeCount() + "\n"
                        + "Executed Commands: " + CommandListener.getCommandTotal() + "\n"
                        + "Logs: " + MantaroListener.getLogTotal() + "\n"
                        + "Memory: " + (getTotalMemory() - getFreeMemory()) + "MB / " + getMaxMemory() + "MB" + "\n"
                        + "Music Connections: " + (int) vc.stream().filter(voiceChannel -> voiceChannel.getMembers().contains(voiceChannel.getGuild().getSelfMember())).count() + "\n"
                        + "Active Connections: " + (int) vc.stream().filter(voiceChannel ->
                                voiceChannel.getMembers().contains(voiceChannel.getGuild().getSelfMember()) && voiceChannel.getMembers().size() > 1).count() + "\n"
                        + "Queue Size: " + MantaroBot.getInstance().getAudioManager().getTotalQueueSize()
                        + "```").queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return baseEmbed(event, "Info")
                        .setDescription("**Gets the bot technical information**")
                        .build();
            }
        });
    }

    @Subscribe
    public void shard(CommandRegistry cr) {
        cr.register("shard", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                event.getChannel().sendMessage("I'm in shard " + (event.getJDA().getShardInfo() == null ? 0 : event.getJDA().getShardInfo().getShardId()) + "!").queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Shard")
                        .setDescription("**Returns in what shard I am**")
                        .build();
            }
        });
    }

    @Subscribe
    public void ping(CommandRegistry cr) {
        final RateLimiter rateLimiter = new RateLimiter(TimeUnit.SECONDS, 5, true);
        final Random r = new Random();
        final String[] pingQuotes = {
                "W-Was I fast enough?", "What are you doing?", "W-What are you looking at?!", "Huh.", "Did I do well?", "What do you think?",
                "Does this happen often?", "Am I performing p-properly?", "<3", "*pats*", "Pong.", "Pang.", "Pung.", "Peng.", "Ping-pong? Yay!"
        };

        cr.register("ping", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                if(!handleDefaultRatelimit(rateLimiter, event.getAuthor(), event)) return;

                long start = System.currentTimeMillis();
                event.getChannel().sendTyping().queue(v -> {
                    long ping = System.currentTimeMillis() - start;
                    event.getChannel().sendMessage(EmoteReference.MEGA + "*" + pingQuotes[r.nextInt(pingQuotes.length)] + "* - My ping: " + ping + " ms (" + ratePing(ping) + ")  `Websocket:" + event.getJDA().getPing() + "ms`").queue();
                    TextChannelGround.of(event).dropItemWithChance(5, 5);
                });
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Ping Command")
                        .setDescription("**Plays Ping-Pong with Discord and prints out the result.**")
                        .build();
            }
        });
    }

    @Subscribe
    public void shardinfo(CommandRegistry cr) {
        cr.register("shardinfo", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                StringBuilder builder = new StringBuilder();
                for(MantaroShard shard : MantaroBot.getInstance().getShardList()) {
                    JDA jda = shard.getJDA();
                    builder.append(String.format(
                            "%-15s | %-9s | U: %-6d | G: %-4d | EV: %-8s | P: %-6s | VC: %-2d",
                            jda.getShardInfo() == null ? "Shard [0 / 1]" : jda.getShardInfo(),
                            jda.getStatus(),
                            jda.getUserCache().size(),
                            jda.getGuildCache().size(),
                            shard.getEventManager().getLastJDAEventTimeDiff() + " ms",
                            jda.getPing(),
                            jda.getVoiceChannelCache().stream().filter(voiceChannel -> voiceChannel.getMembers().contains(voiceChannel.getGuild().getSelfMember())).count()
                    ));

                    if(shard.getJDA().getShardInfo() != null && shard.getJDA().getShardInfo().equals(event.getJDA().getShardInfo())) {
                        builder.append(" <- CURRENT");
                    }

                    builder.append("\n");
                }

                List<String> m = DiscordUtils.divideString(builder);
                List<String> messages = new LinkedList<>();

                for(String s1 : m) {
                    messages.add(String.format("%s\n```prolog\n%s```", "**Mantaro's Shard Information. Use &p >> and &p << to move pages, &cancel to exit.**", s1));
                }

                DiscordUtils.listText(event, 45, false, messages);
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Shard info")
                        .setDescription("**Returns information about shards**")
                        .build();
            }
        });
    }

    private String ratePing(long ping) {
        if(ping == 69) return "l-lewd! <:MantaroGasm:318869352851963904>";
        if(ping <= 1) return "supersonic speed! :upside_down:"; //just in case...
        if(ping <= 10) return "faster than Sonic! :smiley:";
        if(ping <= 100) return "great! :smiley:";
        if(ping <= 200) return "nice! :slight_smile:";
        if(ping <= 300) return "decent. :neutral_face:";
        if(ping <= 400) return "average... :confused:";
        if(ping <= 500) return "slightly slow. :slight_frown:";
        if(ping <= 600) return "kinda slow.. :frowning2:";
        if(ping <= 700) return "slow.. :worried:";
        if(ping <= 800) return "too slow. :disappointed:";
        if(ping <= 900) return "bad. :sob: (helpme)";
        if(ping <= 1600) return "#BlameDiscord. :angry:";
        if(ping <= 10000) return "this makes no sense :thinking: #BlameSteven";
        return "slow af. :dizzy_face: ";
    }
}
