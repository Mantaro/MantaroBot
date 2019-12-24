/*
 * Copyright (C) 2016-2019 David Alejandro Rubio Escares / Kodehawa
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
 *
 */

package net.kodehawa.mantarobot.core;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.sentry.Sentry;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.SessionController;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.listener.VoiceChannelListener;
import net.kodehawa.mantarobot.core.listeners.MantaroListener;
import net.kodehawa.mantarobot.core.listeners.command.CommandListener;
import net.kodehawa.mantarobot.core.listeners.events.PreLoadEvent;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.ReactionOperations;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.processor.DefaultCommandProcessor;
import net.kodehawa.mantarobot.core.processor.core.ICommandProcessor;
import net.kodehawa.mantarobot.core.shard.Shard;
import net.kodehawa.mantarobot.core.shard.jda.BucketedController;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.Prometheus;
import net.kodehawa.mantarobot.utils.banner.BannerPrinter;
import org.slf4j.Logger;

import javax.security.auth.login.LoginException;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.core.LoadState.LOADED;
import static net.kodehawa.mantarobot.core.LoadState.LOADING;
import static net.kodehawa.mantarobot.core.LoadState.POSTLOAD;
import static net.kodehawa.mantarobot.core.LoadState.PRELOAD;

public class MantaroCore {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MantaroCore.class);
    private static final VoiceChannelListener VOICE_CHANNEL_LISTENER = new VoiceChannelListener();
    
    private static LoadState loadState = PRELOAD;
    private final Map<Integer, Shard> shards = new ConcurrentHashMap<>();
    private final Config config;
    private final boolean isDebug;
    private final boolean useBanner;
    private final boolean useSentry;
    private String commandsPackage;
    private String optsPackage;
    private ICommandProcessor commandProcessor = new DefaultCommandProcessor();
    private EventBus shardEventBus;
    private ExecutorService threadPool = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("Mantaro-Thread-%d").build()
    );
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
        
        var shardCount = -1;
        if(isDebug) {
            shardCount = 2;
        }
        SessionController controller;
        if(isDebug) {
            //bucketed controller still prioritizes home guild and reconnecting shards
            controller = new BucketedController(1, 213468583252983809L);
        } else {
            var bucketFactor = config.getBucketFactor();
            log.info("Using buckets of {} shards to start the bot! Assuming we're on big bot :tm: sharding.", bucketFactor);
            controller = new BucketedController(bucketFactor, 213468583252983809L);
        }
    
        try {
            shardManager = new DefaultShardManagerBuilder(config.token)
                    .setSessionController(controller)
                    .setShardsTotal(shardCount)
                    .addEventListeners(
                            VOICE_CHANNEL_LISTENER, InteractiveOperations.listener(),
                            ReactionOperations.listener(), MantaroBot.getInstance().getLavalink()
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
                    .setActivity(Activity.playing("Hold on to your seatbelts!"))
                    .build();
        } catch(LoginException e) {
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
    
        startShardedInstance();
        
        shardEventBus = new EventBus();
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
}
