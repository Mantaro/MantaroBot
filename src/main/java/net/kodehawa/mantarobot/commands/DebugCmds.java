package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.commands.currency.RateLimiter;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.core.processor.DefaultCommandProcessor;
import net.kodehawa.mantarobot.core.listeners.MantaroListener;
import net.kodehawa.mantarobot.core.listeners.command.CommandListener;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.shard.MantaroShard;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import static net.kodehawa.mantarobot.commands.info.AsyncInfoMonitor.*;
import static net.kodehawa.mantarobot.commands.info.StatsHelper.sendStatsMessageAndThen;

@Module
public class DebugCmds {
    @Subscribe
    public void info(CommandRegistry cr) {
        cr.register("info", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                List<Guild> guilds = MantaroBot.getInstance().getGuilds();
                List<VoiceChannel> vc = MantaroBot.getInstance().getVoiceChannels();

                sendStatsMessageAndThen(event, "```prolog\n"
                        + " --------- Technical Information --------- \n\n"
                        + "Commands: " + DefaultCommandProcessor.REGISTRY.commands().values().stream().filter(command -> command.category() != null).count() + "\n"
                        + "Bot Version: " + MantaroInfo.VERSION + "\n"
                        + "JDA Version: " + JDAInfo.VERSION + "\n"
                        + "Lavaplayer Version: " + PlayerLibrary.VERSION + "\n"
                        + "API Responses: " + MantaroBot.getInstance().getResponseTotal() + "\n"
                        + "CPU Usage: " + String.format("%.2f", getVpsCPUUsage()) + "%" + "\n"
                        + "CPU Cores: " + getAvailableProcessors() + "\n"
                        + "Shard Info: " + event.getJDA().getShardInfo() + "\n"
                        //+ "API Status: " + MantaroBot.getInstance().getMantaroAPI().STATUS + "\n"
                        + "DAPI Ping: " + MantaroBot.getInstance().getPing() + "ms"
                        + "\n\n --------- Mantaro Information --------- \n\n"
                        + "Guilds: " + guilds.size() + "\n"
                        + "Users: " + guilds.stream().flatMap(guild -> guild.getMembers().stream()).map(user -> user.getUser().getId()).distinct().count() + "\n"
                        + "Shards: " + MantaroBot.getInstance().getShardedMantaro().getTotalShards() + " (Current: " + (MantaroBot.getInstance().getShardForGuild(event.getGuild()
                                .getId()).getId() + 1) + ")" + "\n"
                        + "Threads: " + Thread.activeCount() + "\n"
                        + "Executed Commands: " + CommandListener.getCommandTotal() + "\n"
                        + "Logs: " + MantaroListener.getLogTotal() + "\n"
                        + "Memory: " + (getTotalMemory() - getFreeMemory()) + "MB / " + getMaxMemory() + "MB" + "\n"
                        + "Music Connections: " + (int) vc.stream().filter(voiceChannel -> voiceChannel.getMembers().contains(voiceChannel.getGuild().getSelfMember())).count() + "\n"
                        + "Queue Size: " + MantaroBot.getInstance().getAudioManager().getTotalQueueSize()
                        + "```");
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
    public void ping(CommandRegistry cr) {
        RateLimiter rateLimiter = new RateLimiter(TimeUnit.SECONDS, 5, true);

        cr.register("ping", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                if (!rateLimiter.process(event.getMember())) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Yikes! Seems like you're going too fast.").queue();
                    return;
                }

                long start = System.currentTimeMillis();
                event.getChannel().sendTyping().queue(v -> {
                    long ping = System.currentTimeMillis() - start;
                    event.getChannel().sendMessage(EmoteReference.MEGA + "My ping: " + ping + " ms - " + ratePing(ping) + "  `Websocket:" + event.getJDA().getPing() + "ms`").queue();
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
    public void shard(CommandRegistry cr) {
        cr.register("shardinfo", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                StringBuilder builder = new StringBuilder();
                for (MantaroShard shard : MantaroBot.getInstance().getShardList()) {
                    JDA jda = shard.getJDA();
                    builder.append(String.format(
                            "%-15s | %-9s | U: %-6d | G: %-4d | L: %-7s | VC: %-2d",
                            jda.getShardInfo() == null ? "Shard [0 / 1]" : jda.getShardInfo(),
                            jda.getStatus(),
                            jda.getUsers().size(),
                            jda.getGuilds().size(),
                            shard.getEventManager().getLastJDAEventTimeDiff() + " ms",
                            jda.getVoiceChannels().stream().filter(voiceChannel -> voiceChannel.getMembers().contains(voiceChannel.getGuild().getSelfMember())).count()
                    ));

                    if (shard.getJDA().getShardInfo() != null && shard.getJDA().getShardInfo().equals(event.getJDA().getShardInfo())) {
                        builder.append(" <- CURRENT");
                    }

                    builder.append("\n");
                }
                Queue<String> m = new LinkedList<>();
                String s = builder.toString().trim();
                StringBuilder sb = new StringBuilder();
                while(s.length() > 0) {
                    int idx = s.indexOf('\n');
                    String line = idx == -1 ? s : s.substring(0, idx+1);
                    s = s.substring(line.length());
                    if(s.equals("\n")) s = "";
                    if(sb.length() + line.length() > 1800) {
                        m.add(sb.toString());
                        sb = new StringBuilder();
                    }
                    sb.append(line);
                }
                if(sb.length() != 0) m.add(sb.toString());

                m.forEach(message -> event.getChannel().sendMessage(String.format("```prolog\n%s```", message)).queue());
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
        if (ping <= 1) return "supersonic speed! :upside_down:"; //just in case...
        if (ping <= 10) return "faster than Sonic! :smiley:";
        if (ping <= 100) return "great! :smiley:";
        if (ping <= 200) return "nice! :slight_smile:";
        if (ping <= 300) return "decent. :neutral_face:";
        if (ping <= 400) return "average... :confused:";
        if (ping <= 500) return "slightly slow. :slight_frown:";
        if (ping <= 600) return "kinda slow.. :frowning2:";
        if (ping <= 700) return "slow.. :worried:";
        if (ping <= 800) return "too slow. :disappointed:";
        if (ping <= 900) return "bad. :sob: (helpme)";
        if (ping <= 1600) return "#BlameDiscord. :angry:";
        if (ping <= 10000) return "this makes no sense :thinking: #BlameSteven";
        return "slow af. :dizzy_face: ";
    }
}
