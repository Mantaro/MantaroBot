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

package net.kodehawa.mantarobot.core;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.SessionController;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.kodehawa.mantarobot.ExtraRuntimeOptions;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.listener.VoiceChannelListener;
import net.kodehawa.mantarobot.core.cache.EvictingCachePolicy;
import net.kodehawa.mantarobot.core.command.processor.CommandProcessor;
import net.kodehawa.mantarobot.core.listeners.MantaroListener;
import net.kodehawa.mantarobot.core.listeners.command.CommandListener;
import net.kodehawa.mantarobot.core.listeners.events.PostLoadEvent;
import net.kodehawa.mantarobot.core.listeners.events.PreLoadEvent;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.ReactionOperations;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.shard.Shard;
import net.kodehawa.mantarobot.core.shard.jda.BucketedController;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.banner.BannerPrinter;
import net.kodehawa.mantarobot.utils.exporters.Metrics;
import net.kodehawa.mantarobot.utils.external.BotListPost;
import okhttp3.Request;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.kodehawa.mantarobot.core.LoadState.*;
import static net.kodehawa.mantarobot.core.cache.EvictionStrategy.leastRecentlyUsed;
import static net.kodehawa.mantarobot.utils.ShutdownCodes.SHARD_FETCH_FAILURE;

public class MantaroCore {
    private static final Logger log = LoggerFactory.getLogger(MantaroCore.class);
    private static final VoiceChannelListener VOICE_CHANNEL_LISTENER = new VoiceChannelListener();

    private static LoadState loadState = PRELOAD;
    private final Map<Integer, Shard> shards = new ConcurrentHashMap<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("Mantaro-Thread-%d").build()
    );
    private final Config config;
    private final boolean isDebug;
    private final boolean useBanner;
    private String commandsPackage;
    private String optsPackage;
    private final CommandProcessor commandProcessor = new CommandProcessor();
    private EventBus shardEventBus;
    private ShardManager shardManager;

    public MantaroCore(Config config, boolean useBanner, boolean isDebug) {
        this.config = config;
        this.useBanner = useBanner;
        this.isDebug = isDebug;
        Metrics.THREAD_POOL_COLLECTOR.add("mantaro-executor", threadPool);
    }

    public static boolean hasLoadedCompletely() {
        return getLoadState().equals(POSTLOAD);
    }

    public static LoadState getLoadState() {
        return MantaroCore.loadState;
    }

    public static void setLoadState(LoadState loadState) {
        MantaroCore.loadState = loadState;
    }

    private static int getInstanceShards(String token) {
        if (ExtraRuntimeOptions.SHARD_SUBSET) {
            return ExtraRuntimeOptions.TO_SHARD.orElseThrow() - ExtraRuntimeOptions.FROM_SHARD.orElseThrow();
        }

        if (ExtraRuntimeOptions.SHARD_COUNT.isPresent()) {
            return ExtraRuntimeOptions.SHARD_COUNT.getAsInt();
        }

        try {
            var shards = new Request.Builder()
                    .url("https://discordapp.com/api/gateway/bot")
                    .header("Authorization", "Bot " + token)
                    .header("Content-Type", "application/json")
                    .build();

            try (var response = Utils.httpClient.newCall(shards).execute()) {
                var body = response.body();
                if (body == null) {
                    throw new IllegalStateException("Error requesting shard count: " + response.code() + " " + response.message());
                }

                var shardObject = new JSONObject(body.string());
                return shardObject.getInt("shards");
            }

        } catch (Exception e) {
            log.error("Unable to fetch shard count", e);
            System.exit(SHARD_FETCH_FAILURE);
        }
        return 1;
    }

    public MantaroCore setOptionsPackage(String optionsPackage) {
        this.optsPackage = optionsPackage;
        return this;
    }

    public MantaroCore setCommandsPackage(String commandsPackage) {
        this.commandsPackage = commandsPackage;
        return this;
    }

    private void startShardedInstance() {
        loadState = LOADING;

        SessionController controller;
        if (isDebug) {
            // Bucketed controller still prioritizes home guild and reconnecting shards.
            // Only really useful in the node that actually contains the guild, but worth keeping.
            controller = new BucketedController(1, 213468583252983809L);
        } else {
            var bucketFactor = config.getBucketFactor();
            if (bucketFactor > 1) {
                log.info("Using buckets of {} shards to start the bot! Assuming we're on big bot sharding." , bucketFactor);
                log.info("If you're self-hosting, set bucketFactor in config.json to 1 and isSelfHost to true.");
            }

            controller = new BucketedController(bucketFactor, 213468583252983809L);
        }

        var gatewayThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("GatewayThread-%d")
                .setDaemon(true)
                .setPriority(Thread.MAX_PRIORITY)
                .build();
        var requesterThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("RequesterThread-%d")
                .setDaemon(true)
                .build();

        try {
            // Don't allow mentioning @everyone, @here or @role (can be overriden in a per-command context, but we only ever re-enable role)
            EnumSet<Message.MentionType> deny = EnumSet.of(Message.MentionType.EVERYONE, Message.MentionType.HERE, Message.MentionType.ROLE);
            MessageAction.setDefaultMentions(EnumSet.complementOf(deny));

            // Gateway Intents to enable.
            // We used to have GUILD_PRESENCES here for caching before, since chunking wasn't possible, but we needed to remove it.
            // So we have no permanent cache anymore.
            GatewayIntent[] toEnable = {
                    GatewayIntent.GUILD_MESSAGES, // Recieve guild messages, needed to, well operate at all.
                    GatewayIntent.GUILD_MESSAGE_REACTIONS,  // Receive message reactions, used for reaction menus.
                    GatewayIntent.GUILD_MEMBERS, // Receive member events, needed for mod features *and* welcome/leave messages.
                    GatewayIntent.GUILD_VOICE_STATES, // Receive voice states, needed so Member#getVoiceState doesn't return null.
                    GatewayIntent.GUILD_BANS // Receive guild bans, needed for moderation stuff.
            };

            // This is used so we can fire PostLoadEvent properly.
            var shardStartListener = new ShardStartListener();

            DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.create(config.token, Arrays.asList(toEnable))
                    // Can't do chunking with Gateway Intents enabled, fun, but don't need it anymore.
                    .setChunkingFilter(ChunkingFilter.NONE)
                    .setSessionController(controller)
                    .addEventListeners(
                            VOICE_CHANNEL_LISTENER, InteractiveOperations.listener(),
                            ReactionOperations.listener(), MantaroBot.getInstance().getLavaLink(),
                            shardStartListener
                    )
                    .addEventListenerProviders(List.of(
                            id -> new CommandListener(commandProcessor, threadPool, getShard(id).getMessageCache()),
                            id -> new MantaroListener(threadPool, getShard(id).getMessageCache()),
                            id -> getShard(id).getListener()
                    ))
                    .setEventManagerProvider(id -> getShard(id).getManager())
                    // Don't spam on mass-prune.
                    .setBulkDeleteSplittingEnabled(false)
                    .setVoiceDispatchInterceptor(MantaroBot.getInstance().getLavaLink().getVoiceInterceptor())
                    // We technically don't need it, as we don't ask for either GUILD_PRESENCES nor GUILD_EMOJIS anymore.
                    .disableCache(EnumSet.of(CacheFlag.ACTIVITY, CacheFlag.EMOTE, CacheFlag.CLIENT_STATUS))
                    .setActivity(Activity.playing("Hold on to your seatbelts!"));
            
            /* only create eviction strategies that will get used */
            List<Integer> shardIds;
            int latchCount;
            if (isDebug) {
                var shardCount = 2;
                shardIds = List.of(0, 1);
                latchCount = shardCount;
                builder.setShardsTotal(shardCount)
                        .setGatewayPool(Executors.newSingleThreadScheduledExecutor(gatewayThreadFactory), true)
                        .setRateLimitPool(Executors.newScheduledThreadPool(2, requesterThreadFactory), true);
                log.info("Debug instance, using {} shards", shardCount);
            } else {
                int shardCount;
                // Count specified in config.
                if (config.totalShards != 0) {
                    shardCount = config.totalShards;
                    builder.setShardsTotal(config.totalShards);
                    log.info("Using {} shards from config (totalShards != 0)", shardCount);
                } else {
                    //Count specified on runtime options or recommended count by discord.
                    shardCount = ExtraRuntimeOptions.SHARD_COUNT.orElseGet(() -> getInstanceShards(config.token));
                    builder.setShardsTotal(shardCount);
                    if (ExtraRuntimeOptions.SHARD_COUNT.isPresent()) {
                        log.info("Using {} shards from ExtraRuntimeOptions", shardCount);
                    } else {
                        log.info("Using {} shards from discord recommended amount", shardCount);
                    }
                }

                // Using a shard subset. FROM_SHARD is inclusive, TO_SHARD is exclusive (else 0 to 448 would start 449 shards)
                if (ExtraRuntimeOptions.SHARD_SUBSET) {
                    if (ExtraRuntimeOptions.SHARD_SUBSET_MISSING) {
                        throw new IllegalStateException("Both mantaro.from-shard and mantaro.to-shard must be specified " +
                                "when using shard subsets. Please specify the missing one.");
                    }
                    var from = ExtraRuntimeOptions.FROM_SHARD.orElseThrow();
                    var to = ExtraRuntimeOptions.TO_SHARD.orElseThrow() - 1;
                    shardIds = IntStream.rangeClosed(from, to).boxed().collect(Collectors.toList());
                    latchCount = to - from + 1;

                    log.info("Using shard range {}-{}", from, to);
                    builder.setShards(from, to);
                } else {
                    shardIds = IntStream.range(0, shardCount).boxed().collect(Collectors.toList());
                    latchCount = shardCount;
                }
    
                // use latchCount instead of shardCount
                // latchCount is the number of shards on this process
                // shardCount is the total number of shards in all processes
                var gatewayThreads = Math.max(1, latchCount / 16);
                var rateLimitThreads = Math.max(2, latchCount * 5 / 4);
                log.info("Gateway pool: {} threads", gatewayThreads);
                log.info("Rate limit pool: {} threads", rateLimitThreads);
                builder.setGatewayPool(Executors.newScheduledThreadPool(gatewayThreads, gatewayThreadFactory), true)
                        .setRateLimitPool(Executors.newScheduledThreadPool(rateLimitThreads, requesterThreadFactory), true);
            }

            //if this isn't true we have a big problem
            if (shardIds.size() != latchCount) {
                throw new IllegalStateException("Shard ids list must have the same size as latch count");
            }

            builder.setMemberCachePolicy(new EvictingCachePolicy(shardIds, () -> leastRecentlyUsed(config.memberCacheSize)));
    
            MantaroCore.setLoadState(LoadState.LOADING_SHARDS);
            log.info("Spawning {} shards...", latchCount);
            var start = System.currentTimeMillis();
            shardStartListener.setLatch(new CountDownLatch(latchCount));
            shardManager = builder.build();

            //This is so it doesn't block command registering, lol.
            threadPool.submit(() -> {
                log.info("CountdownLatch started: Awaiting for {} shards to be counted down to start PostLoad.", latchCount);

                try {
                    shardStartListener.latch.await();
                    var elapsed = System.currentTimeMillis() - start;
                    shardManager.removeEventListener(shardStartListener);
                    startPostLoadProcedure(elapsed);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        } catch (LoginException e) {
            throw new IllegalStateException(e);
        }

        loadState = LOADED;
    }

    @SuppressWarnings("UnstableApiUsage")
    public void start() {
        if (config == null)
            throw new IllegalArgumentException("Config cannot be null!");
        if (commandsPackage == null)
            throw new IllegalArgumentException("Cannot look for commands if you don't specify where!");
        if (optsPackage == null)
            throw new IllegalArgumentException("Cannot look for options if you don't specify where!");

        if (useBanner)
            new BannerPrinter(1).printBanner();

        Set<Class<?>> commands = lookForAnnotatedOn(commandsPackage, Module.class);
        Set<Class<?>> options = lookForAnnotatedOn(optsPackage, Option.class);
        shardEventBus = new EventBus();

        startShardedInstance();

        for (Class<?> commandClass : commands) {
            try {
                shardEventBus.register(commandClass.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                log.error("Invalid module: no zero arg public constructor found for " + commandClass);
            }
        }

        for (Class<?> optionClass : options) {
            try {
                shardEventBus.register(optionClass.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                log.error("Invalid module: no zero arg public constructor found for " + optionClass);
            }
        }

        new Thread(() -> {
            // For now, only used by AsyncInfoMonitor startup and Anime Login Task.
            shardEventBus.post(new PreLoadEvent());
            // Registers all commands
            shardEventBus.post(CommandProcessor.REGISTRY);
            // Registers all options
            shardEventBus.post(new OptionRegistryEvent());
        }, "Mantaro EventBus-Post").start();
    }

    public void markAsReady() {
        loadState = POSTLOAD;
    }

    public ShardManager getShardManager() {
        return shardManager;
    }

    public Shard getShard(int id) {
        return shards.computeIfAbsent(id, Shard::new);
    }

    public Collection<Shard> getShards() {
        return Collections.unmodifiableCollection(shards.values());
    }

    private Set<Class<?>> lookForAnnotatedOn(String packageName, Class<? extends Annotation> annotation) {
        return new ClassGraph()
                .acceptPackages(packageName)
                .enableAnnotationInfo()
                .scan(2)
                .getAllClasses().stream().filter(classInfo -> classInfo.hasAnnotation(annotation.getName())).map(ClassInfo::loadClass)
                .collect(Collectors.toSet());
    }

    public EventBus getShardEventBus() {
        return this.shardEventBus;
    }

    private void startPostLoadProcedure(long elapsed) {
        MantaroBot bot = MantaroBot.getInstance();

        //Start the reconnect queue.
        bot.getCore().markAsReady();

        System.out.println("[-=-=-=-=-=- MANTARO STARTED -=-=-=-=-=-]");
        LogUtils.shard(String.format("Loaded all %d (out of %d) shards and %d commands.\nTook %s.\nCross-node shard count is %d.", shardManager.getShardsRunning(),
                bot.getManagedShards(), CommandProcessor.REGISTRY.commands().size(),
                Utils.formatDuration(elapsed), shardManager.getShardsTotal())
        );
        log.info("Loaded all shards successfully! Status: {}.", MantaroCore.getLoadState());

        bot.getCore().getShardEventBus().post(new PostLoadEvent());

        //Only update guild count from the master node.
        if (bot.isMasterNode())
            startUpdaters();

        bot.startCheckingBirthdays();
    }

    private void startUpdaters() {
        Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Mantaro-ServerCountUpdate")).scheduleAtFixedRate(() -> {
            try {
                var serverCount = 0L;
                //Fetch actual guild count.
                try(Jedis jedis = MantaroData.getDefaultJedisPool().getResource()) {
                    var stats = jedis.hgetAll("shardstats-" + config.getClientId());
                    for (var shards : stats.entrySet()) {
                        var json = new JSONObject(shards.getValue());
                        serverCount += json.getLong("guild_count");
                    }
                }

                // This will NOP if the token is null.
                for(BotListPost listSites : BotListPost.values()) {
                    listSites.createRequest(serverCount, config.clientId);
                }

                log.debug("Updated server count ({}) for all bot lists", serverCount);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, 0, 10, TimeUnit.MINUTES);
    }

    private static class ShardStartListener implements EventListener {
        private CountDownLatch latch;

        public void setLatch(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onEvent(@Nonnull GenericEvent event) {
            if (event instanceof ReadyEvent) {
                var sm = event.getJDA().getShardManager();
                if (sm == null)
                    throw new AssertionError();

                latch.countDown();
            }
        }
    }
}
