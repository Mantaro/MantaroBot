package net.kodehawa.mantarobot.web;

import br.com.brjdevs.java.utils.async.Async;
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.core.CommandProcessorAndRegistry;
import net.kodehawa.mantarobot.core.listeners.MantaroListener;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.shard.MantaroShard;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static net.kodehawa.mantarobot.commands.info.AsyncInfoMonitor.*;
import static net.kodehawa.mantarobot.commands.info.CommandStatsManager.*;
import static net.kodehawa.mantarobot.commands.info.GuildStatsManager.*;
import static net.kodehawa.mantarobot.web.MantaroAPI.sessionToken;

@Slf4j
public class MantaroAPISender {

    private static OkHttpClient httpClient = new OkHttpClient();

    public static void startService() {
        Runnable postStats = () -> {
            //Ignore API calls if the api did a boom.
            if(MantaroBot.getInstance().getMantaroAPI().STATUS == APIStatus.OFFLINE) {
                return;
            }

            MantaroBot bot = MantaroBot.getInstance();

            List<VoiceChannel> vc = MantaroBot.getInstance().getVoiceChannels();
            int c = (int) vc.stream().filter(voiceChannel -> voiceChannel.getMembers().contains(
                    voiceChannel.getGuild().getSelfMember())).count();
            List<Integer> memoryUsage = new ArrayList<>();
            memoryUsage.add((int) (getTotalMemory() - getFreeMemory())); //used
            memoryUsage.add((int) getMaxMemory()); //total

            JSONObject mainStats = new JSONObject();
            mainStats.put("jdaVersion", JDAInfo.VERSION)
                    .put("lpVersion", PlayerLibrary.VERSION)
                    .put("botVersion", MantaroInfo.VERSION)
                    .put("guilds", bot.getGuilds().size())
                    .put("users", bot.getUsers().size())
                    .put("shardsTotal", bot.getShardAmount())
                    .put("executedCommands", CommandProcessorAndRegistry.getCommandTotal())
                    .put("logTotal", Integer.parseInt(MantaroListener.getLogTotal()))
                    .put("musicConnections", c)
                    .put("parsedCpuUsage", getVpsCPUUsage())
                    .put("cores", getAvailableProcessors())
                    .put("queueSize", bot.getAudioManager().getTotalQueueSize())
                    .put("memoryUsage", memoryUsage);

            List<Integer> ids = new ArrayList<>();
            List<JDA.Status> statuses = new ArrayList<>();
            List<Integer> users = new ArrayList<>();
            List<Integer> guilds = new ArrayList<>();
            List<Long> musicConnections = new ArrayList<>();
            List<Long> lastEventTimes = new ArrayList<>();

            for(MantaroShard shard : MantaroBot.getInstance().getShardList()) {
                ids.add(shard.getId());
                statuses.add(shard.getStatus());
                users.add(shard.getUsers().size());
                guilds.add(shard.getGuilds().size());
                musicConnections.add(
                        shard.getJDA().getVoiceChannels().stream().filter(voiceChannel -> voiceChannel.getMembers().contains(voiceChannel.getGuild().getSelfMember()))
                                .count()
                );
                lastEventTimes.add(shard.getEventManager().getLastJDAEventTimeDiff());
            }

            JSONObject shardInfo = new JSONObject();
            shardInfo.put("ids", ids)
                    .put("statuses", statuses)
                    .put("users", users)
                    .put("guilds", guilds)
                    .put("musicConnections", musicConnections)
                    .put("lastEventTimes", lastEventTimes);

            Map<Integer, Map<String, AtomicInteger>> total = new HashMap<>();
            Map<Integer, Map<String, AtomicInteger>> today = new HashMap<>();
            Map<Integer, Map<String, AtomicInteger>> hourly = new HashMap<>();
            Map<Integer, Map<String, AtomicInteger>> now = new HashMap<>();

            total.put(getTotalValueFor(TOTAL_CMDS), TOTAL_CMDS);
            today.put(getTotalValueFor(DAY_CMDS), DAY_CMDS);
            hourly.put(getTotalValueFor(HOUR_CMDS), HOUR_CMDS);
            now.put(getTotalValueFor(MINUTE_CMDS), MINUTE_CMDS);
            JSONObject commands = new JSONObject()
                    .put("total", total)
                    .put("today", today)
                    .put("hourly", hourly)
                    .put("now", now);

            JSONObject guildsS = new JSONObject()
                    .put("total", TOTAL_EVENTS)
                    .put("today", DAY_EVENTS)
                    .put("hourly", HOUR_EVENTS)
                    .put("now", MINUTE_EVENTS);

            JSONObject toPost = new JSONObject()
                    .put("nodeid", bot.getMantaroAPI().nodeId)
                    .put("stats", mainStats)
                    .put("guildstats", guildsS)
                    .put("commandstats", commands)
                    .put("shardinfo", shardInfo);

            try {
                RequestBody body = RequestBody.create(MediaType.parse("application/json"),
                        toPost.toString());

                Request identify = new Request.Builder()
                        .url(String.format("http://%s/api/nodev1/identify", MantaroData.config().get().apiUrl))
                        .header("Authorization", sessionToken)
                        .post(body)
                        .build();
                httpClient.newCall(identify).execute().close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        };

        Async.task("Mantaro API POST Worker", postStats, 15, TimeUnit.SECONDS);
    }
}