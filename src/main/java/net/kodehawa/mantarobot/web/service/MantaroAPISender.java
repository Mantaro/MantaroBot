package net.kodehawa.mantarobot.web.service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
//TODO use /api/nodev1/stats
public class MantaroAPISender {
    public static void startService(){
        Runnable postStats = () -> {
            //Ignore API calls if the api did a boom.
            /*if(MantaroBot.getInstance().getMantaroAPI().STATUS == APIStatus.OFFLINE){
                return;
            }

            List<VoiceChannel> vc = MantaroBot.getInstance().getVoiceChannels();
            int c = (int) vc.stream().filter(voiceChannel -> voiceChannel.getMembers().contains(
                    voiceChannel.getGuild().getSelfMember())).count();
            List<Integer> memoryUsage = new ArrayList<>();
            memoryUsage.add((int)(getTotalMemory() - getFreeMemory())); //used
            memoryUsage.add((int)getMaxMemory()); //total

            StatsEntity statsEntity = new StatsEntity(
                    JDAInfo.VERSION, PlayerLibrary.VERSION, MantaroInfo.VERSION, MantaroBot.getInstance().getGuilds().size(),
                    MantaroBot.getInstance().getUsers().size(), MantaroBot.getInstance().getShardAmount(), Integer.parseInt(CommandListener.getCommandTotal()),
                    Integer.parseInt(MantaroListener.getLogTotal()), c, (int)getVpsCPUUsage(), getAvailableProcessors(),
                    MantaroBot.getInstance().getAudioManager().getTotalQueueSize(), memoryUsage
            );

            try{
                Unirest.post(String.format("http://%s/api/stats", MantaroData.config().get().apiUrl))
                        .header("Content-Type", "application/json")
                        .body(GsonDataManager.GSON_PRETTY.toJson(statsEntity))
                        .asString().getBody();
            } catch (UnirestException e){
                log.warn("Cannot post stats to Mantaro API, maybe it's down?");
            }


            List<Integer> ids = new ArrayList<>();
            List<JDA.Status> statuses = new ArrayList<>();
            List<Integer> users = new ArrayList<>();
            List<Integer> guilds = new ArrayList<>();
            List<Long> musicConnections = new ArrayList<>();
            List<Long> lastUnifiedJDALastEventTimes = new ArrayList<>();

            for(MantaroShard shard : MantaroBot.getInstance().getShardList()){
                ids.add(shard.getId());
                statuses.add(shard.getStatus());
                users.add(shard.getUsers().size());
                guilds.add(shard.getGuilds().size());
                musicConnections.add(
                        shard.getJDA().getVoiceChannels().stream().filter(voiceChannel -> voiceChannel.getMembers().contains(voiceChannel.getGuild().getSelfMember()))
                                .count()
                );
                lastUnifiedJDALastEventTimes.add(shard.getEventManager().getLastJDAEventTimeDiff());
            }

            ShardInfo shardInfo = new ShardInfo(ids, statuses, users, guilds, musicConnections, lastUnifiedJDALastEventTimes);

            try{
                Unirest.post(String.format("http://%s/api/sinfo", MantaroData.config().get().apiUrl))
                        .header("Content-Type", "application/json")
                        .body(GsonDataManager.GSON_PRETTY.toJson(shardInfo))
                        .asString().getBody();
            }  catch (UnirestException e){
                log.warn("Cannot post shard info to Mantaro API, maybe it's down?");
            }

            Map<Integer, Map<String, AtomicInteger>> total = new HashMap<>();
            Map<Integer, Map<String, AtomicInteger>> today = new HashMap<>();
            Map<Integer, Map<String, AtomicInteger>> hourly = new HashMap<>();
            Map<Integer, Map<String, AtomicInteger>> now = new HashMap<>();

            total.put(getTotalValueFor(TOTAL_CMDS), TOTAL_CMDS);
            today.put(getTotalValueFor(DAY_CMDS), DAY_CMDS);
            hourly.put(getTotalValueFor(HOUR_CMDS), HOUR_CMDS);
            now.put(getTotalValueFor(MINUTE_CMDS), MINUTE_CMDS);
            CommandsEntity commandsEntity = new CommandsEntity(total, today, hourly, now);

            try{
                Unirest.post(String.format("http://%s/api/cstats", MantaroData.config().get().apiUrl))
                        .header("Content-Type", "application/json")
                        .body(GsonDataManager.GSON_PRETTY.toJson(commandsEntity))
                        .asString().getBody();
            }  catch (UnirestException e){
                log.warn("Cannot post command info to Mantaro API, maybe it's down?");
            }

            GuildsEntity guildsEntity = new GuildsEntity(TOTAL_EVENTS, DAY_EVENTS, HOUR_EVENTS, MINUTE_EVENTS);
            try{
                Unirest.post(String.format("http://%s/api/gstats", MantaroData.config().get().apiUrl))
                        .header("Content-Type", "application/json")
                        .body(GsonDataManager.GSON_PRETTY.toJson(guildsEntity))
                        .asString().getBody();
            }  catch (UnirestException e){
                log.warn("Cannot post command info to Mantaro API, maybe it's down?");
            }
        };

        Async.task("Mantaro API POST Worker", postStats, 30, TimeUnit.SECONDS);*/
        };
    }
}