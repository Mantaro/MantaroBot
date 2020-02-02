/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.core;

import com.github.natanbc.discordbotsapi.DiscordBotsAPI;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.sentry.Sentry;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.SessionController;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.kodehawa.mantarobot.ExtraRuntimeOptions;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.listener.VoiceChannelListener;
import net.kodehawa.mantarobot.core.listeners.MantaroListener;
import net.kodehawa.mantarobot.core.listeners.command.CommandListener;
import net.kodehawa.mantarobot.core.listeners.events.PostLoadEvent;
import net.kodehawa.mantarobot.core.listeners.events.PreLoadEvent;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.ReactionOperations;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.processor.DefaultCommandProcessor;
import net.kodehawa.mantarobot.core.processor.core.ICommandProcessor;
import net.kodehawa.mantarobot.core.shard.Shard;
import net.kodehawa.mantarobot.core.shard.jda.BucketedController;
import net.kodehawa.mantarobot.core.shard.watcher.ShardWatcher;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.services.Carbonitex;
import net.kodehawa.mantarobot.utils.Prometheus;
import net.kodehawa.mantarobot.utils.SentryHelper;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.banner.BannerPrinter;
import okhttp3.Request;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.core.LoadState.LOADED;
import static net.kodehawa.mantarobot.core.LoadState.LOADING;
import static net.kodehawa.mantarobot.core.LoadState.POSTLOAD;
import static net.kodehawa.mantarobot.core.LoadState.PRELOAD;
import static net.kodehawa.mantarobot.utils.ShutdownCodes.SHARD_FETCH_FAILURE;

public class MantaroCore {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MantaroCore.class);
    private static final VoiceChannelListener VOICE_CHANNEL_LISTENER = new VoiceChannelListener();
    
    private static LoadState loadState = PRELOAD;
    private final Map<Integer, Shard> shards = new ConcurrentHashMap<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("Mantaro-Thread-%d").build()
    );
    private final Config config;
    private final boolean isDebug;
    private final boolean useBanner;
    private final boolean useSentry;
    private String commandsPackage;
    private String optsPackage;
    private ICommandProcessor commandProcessor = new DefaultCommandProcessor();
    private EventBus shardEventBus;
    private ShardManager shardManager;
    
    public MantaroCore(Config config, boolean useBanner, boolean useSentry, boolean isDebug) {
        this.config = config;
        this.useBanner = useBanner;
        this.useSentry = useSentry;
        this.isDebug = isDebug;
        Prometheus.THREAD_POOL_COLLECTOR.add("mantaro-executor", threadPool);
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
        if(isDebug) {
            //bucketed controller still prioritizes home guild and reconnecting shards
            controller = new BucketedController(1, 213468583252983809L);
        } else {
            var bucketFactor = config.getBucketFactor();
            log.info("Using buckets of {} shards to start the bot! Assuming we're on big bot :tm: sharding.", bucketFactor);
            controller = new BucketedController(bucketFactor, 213468583252983809L);
        }
        
        var callbackThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("CallbackThread-%d")
                .setDaemon(true)
                .build();
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
            var listener = new ShardStartListener();
            var builder = new DefaultShardManagerBuilder(config.token)
                                  .setSessionController(controller)
                                  .addEventListeners(
                                          VOICE_CHANNEL_LISTENER, InteractiveOperations.listener(),
                                          ReactionOperations.listener(), MantaroBot.getInstance().getLavalink(),
                                          listener
                                  )
                                  .addEventListenerProviders(List.of(
                                          id -> new CommandListener(id, commandProcessor, threadPool, getShard(id).getMessageCache()),
                                          id -> new MantaroListener(id, threadPool, getShard(id).getMessageCache()),
                                          id -> getShard(id).getListener()
                                  ))
                                  .setEventManagerProvider(id -> getShard(id).getManager())
                                  .setBulkDeleteSplittingEnabled(false)
                                  .setVoiceDispatchInterceptor(MantaroBot.getInstance().getLavalink().getVoiceInterceptor())
                                  .setDisabledCacheFlags(EnumSet.of(CacheFlag.ACTIVITY, CacheFlag.EMOTE))
                                  .setActivity(Activity.playing("Hold on to your seatbelts!"));
            if(isDebug) {
                builder.setShardsTotal(2)
                        .setCallbackPool(Executors.newFixedThreadPool(1, callbackThreadFactory), true)
                        .setGatewayPool(Executors.newSingleThreadScheduledExecutor(gatewayThreadFactory), true)
                        .setRateLimitPool(Executors.newScheduledThreadPool(2, requesterThreadFactory), true);
            } else {
                if(ExtraRuntimeOptions.SHARD_SUBSET_MISSING) {
                    throw new IllegalStateException("Both mantaro.from-shard and mantaro.to-shard must be specified " +
                                                            "when using shard subsets. Please specify the missing one.");
                }
                var count = getInstanceShards(config.token);
                builder
                        .setCallbackPool(Executors.newFixedThreadPool(Math.max(1, count / 4), callbackThreadFactory), true)
                        .setGatewayPool(Executors.newScheduledThreadPool(Math.max(1, count / 16), gatewayThreadFactory), true)
                        .setRateLimitPool(Executors.newScheduledThreadPool(Math.max(2, count / 8), requesterThreadFactory), true);
                if(ExtraRuntimeOptions.SHARD_SUBSET) {
                    builder.setShardsTotal(ExtraRuntimeOptions.SHARD_COUNT.orElseThrow())
                            .setShards(
                                    ExtraRuntimeOptions.FROM_SHARD.orElseThrow(),
                                    ExtraRuntimeOptions.TO_SHARD.orElseThrow()
                            );
                } else {
                    builder.setShardsTotal(ExtraRuntimeOptions.SHARD_COUNT.orElse(-1));
                }
            }
            MantaroCore.setLoadState(LoadState.LOADING_SHARDS);
            log.info("Spawning shards...");
            var start = System.currentTimeMillis();
            shardManager = builder.build();
            listener.latch.await();
            var elapsed = System.currentTimeMillis() - start;
            shardManager.removeEventListener(listener);
            startPostLoadProcedure(elapsed);
        } catch(LoginException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    
        loadState = LOADED;
    }
    
    @SuppressWarnings("UnstableApiUsage")
    public MantaroCore start() {
        if(config == null)
            throw new IllegalArgumentException("Config cannot be null!");
        
        if(useSentry)
            Sentry.init(config.sentryDSN);
        if(useBanner)
            new BannerPrinter(1).printBanner();
        
        if(commandsPackage == null)
            throw new IllegalArgumentException("Cannot look for commands if you don't specify where!");
        if(optsPackage == null)
            throw new IllegalArgumentException("Cannot look for options if you don't specify where!");
        
        Set<Class<?>> commands = lookForAnnotatedOn(commandsPackage, Module.class);
        Set<Class<?>> options = lookForAnnotatedOn(optsPackage, Option.class);
        shardEventBus = new EventBus();

        startShardedInstance();
        
        for(Class<?> aClass : commands) {
            try {
                shardEventBus.register(aClass.getDeclaredConstructor().newInstance());
            } catch(Exception e) {
                log.error("Invalid module: no zero arg public constructor found for " + aClass);
            }
        }
        
        for(Class<?> clazz : options) {
            try {
                shardEventBus.register(clazz.getDeclaredConstructor().newInstance());
            } catch(Exception e) {
                log.error("Invalid module: no zero arg public constructor found for " + clazz);
            }
        }
        
        new Thread(() -> {
            //For now, only used by AsyncInfoMonitor startup and Anime Login Task.
            shardEventBus.post(new PreLoadEvent());
            //Registers all commands
            shardEventBus.post(DefaultCommandProcessor.REGISTRY);
            //Registers all options
            shardEventBus.post(new OptionRegistryEvent());
        }, "Mantaro EventBus-Post").start();
        
        return this;
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
                       .whitelistPackages(packageName)
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
        LogUtils.shard(String.format("Loaded all %d (of a total of %d) shards in %d seconds.", shardManager.getShardsRunning(),
                shardManager.getShardsTotal(), elapsed / 1000));
        log.info("Loaded all shards successfully... Starting ShardWatcher! Status: {}", MantaroCore.getLoadState());
        
        new Thread(new ShardWatcher(), "ShardWatcherThread").start();
        bot.getCore().getShardEventBus().post(new PostLoadEvent());
        
        startUpdaters();
        bot.startCheckingBirthdays();
    }
    
    private void startUpdaters() {
        Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Carbonitex post task"))
                .scheduleAtFixedRate(Carbonitex::handle, 0, 30, TimeUnit.MINUTES);
        
        if(config.dbotsorgToken != null) {
            var discordBotsAPI = new DiscordBotsAPI.Builder()
                                         .setToken(config.dbotsorgToken)
                                         .build();
            Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "dbots.org update thread")).scheduleAtFixedRate(() -> {
                try {
                    long count = MantaroBot.getInstance().getShardManager().getGuildCache().size();
                    int[] shards = MantaroBot.getInstance().getShardList().stream().mapToInt(shard -> (int) shard.getJDA().getGuildCache().size()).toArray();
                    discordBotsAPI.postStats(shards).execute();
                    log.debug("Updated server count ({}) for discordbots.org", count);
                } catch(Exception ignored) {
                }
            }, 0, 1, TimeUnit.HOURS);
        } else {
            log.warn("discordbots.org token not set in config, cannot start posting stats!");
        }
    }
    
    private static int getInstanceShards(String token) {
        if(ExtraRuntimeOptions.SHARD_SUBSET) {
            return ExtraRuntimeOptions.TO_SHARD.orElseThrow() - ExtraRuntimeOptions.FROM_SHARD.orElseThrow();
        }
        if(ExtraRuntimeOptions.SHARD_COUNT.isPresent()) {
            return ExtraRuntimeOptions.SHARD_COUNT.getAsInt();
        }
        
        try {
            var shards = new Request.Builder()
                                     .url("https://discordapp.com/api/gateway/bot")
                                     .header("Authorization", "Bot " + token)
                                     .header("Content-Type", "application/json")
                                     .build();
            
            try(var response = Utils.httpClient.newCall(shards).execute()) {
                var body = response.body();
                if(body == null) {
                    throw new IllegalStateException("Error requesting shard count: " + response.code() + " " + response.message());
                }
                var shardObject = new JSONObject(body.string());
                return shardObject.getInt("shards");
            }
            
        } catch(Exception e) {
            SentryHelper.captureExceptionContext(
                    "Exception thrown when trying to get shard count, discord isn't responding?", e, MantaroBot.class, "Shard Count Fetcher"
            );
            log.error("Unable to fetch shard count", e);
            System.exit(SHARD_FETCH_FAILURE);
        }
        return 1;
    }
    
    
    
    private static class ShardStartListener implements EventListener {
        private final CountDownLatch latch = new CountDownLatch(1);
        
        @Override
        public void onEvent(@Nonnull GenericEvent event) {
            if(event instanceof ReadyEvent) {
                var sm = event.getJDA().getShardManager();
                if(sm == null) throw new AssertionError();
                if(sm.getShardsQueued() == 0) {
                    latch.countDown();
                }
            }
        }
    }
}
