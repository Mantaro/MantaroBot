/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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

package net.kodehawa.mantarobot.core;

import br.com.brjdevs.java.utils.async.Async;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.sentry.Sentry;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.mantarobot.core.listeners.events.PreLoadEvent;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.processor.DefaultCommandProcessor;
import net.kodehawa.mantarobot.core.processor.core.ICommandProcessor;
import net.kodehawa.mantarobot.core.shard.ShardedBuilder;
import net.kodehawa.mantarobot.core.shard.ShardedMantaro;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.banner.BannerPrinter;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static net.kodehawa.mantarobot.core.LoadState.*;

@Slf4j
public class MantaroCore {

    private final Config config;
    private final boolean isDebug;
    private final boolean useBanner;
    private final boolean useSentry;
    private String commandsPackage;
    private String optsPackage;
    private ShardedMantaro shardedMantaro;

    @Getter
    private ICommandProcessor commandProcessor = new DefaultCommandProcessor();
    @Getter
    private EventBus shardEventBus;
    @Getter
    private ExecutorService commonExecutor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("Mantaro-CommonExecutor Thread-%d").build());
    @Getter
    @Setter
    private static LoadState loadState = PRELOAD;

    public MantaroCore(Config config, boolean useBanner, boolean useSentry, boolean isDebug) {
        this.config = config;
        this.useBanner = useBanner;
        this.useSentry = useSentry;
        this.isDebug = isDebug;
    }

    public static boolean hasLoadedCompletely() {
        return getLoadState().equals(POSTLOAD);
    }

    public MantaroCore setOptionsPackage(String optionsPackage) {
        this.optsPackage = optionsPackage;
        return this;
    }

    public MantaroCore setCommandsPackage(String commandsPackage) {
        this.commandsPackage = commandsPackage;
        return this;
    }

    public MantaroCore setCustomCommandProcessor(ICommandProcessor processor) {
        this.commandProcessor = processor;
        return this;
    }

    private void startShardedInstance() throws Exception {
        loadState = LOADING;

        shardedMantaro = new ShardedBuilder()
                .debug(isDebug)
                .auto(true)
                .token(config.token)
                .commandProcessor(commandProcessor)
                .build();

        Async.thread("MantaroCore-ShardInit", () -> shardedMantaro.shard());

        loadState = LOADED;
    }

    private void startSingleShardInstance() throws Exception {
        loadState = LOADING;

        shardedMantaro = new ShardedBuilder()
                .amount(1)
                .token(config.token)
                .commandProcessor(commandProcessor)
                .build();

        Async.thread("MantaroCore-ShardInit", () -> shardedMantaro.shard());

        loadState = LOADED;
    }

    public MantaroCore startMainComponents(boolean single) throws Exception {
        if(config == null) throw new IllegalArgumentException("Config cannot be null!");

        if(useSentry)
            Sentry.init(config.sentryDSN);
        if(useBanner)
            new BannerPrinter(1).printBanner();

        if(commandsPackage == null)
            throw new IllegalArgumentException("Cannot look for commands if you don't specify where!");
        if(optsPackage == null)
            throw new IllegalArgumentException("Cannot look for options if you don't specify where!");

        Future<Set<Class<?>>> commands = lookForAnnotatedOn(commandsPackage, Module.class);
        Future<Set<Class<?>>> options = lookForAnnotatedOn(optsPackage, Option.class);

        if(single) {
            startSingleShardInstance();
        } else {
            startShardedInstance();
        }

        shardEventBus = new EventBus();
        for(Class<?> aClass : commands.get()) {
            try {
                shardEventBus.register(aClass.newInstance());
            } catch(Exception e) {
                log.error("Invalid module: no zero arg public constructor found for " + aClass);
            }
        }

        for(Class<?> clazz : options.get()) {
            try {
                shardEventBus.register(clazz.newInstance());
            } catch(Exception e) {
                log.error("Invalid module: no zero arg public constructor found for " + clazz);
            }
        }

        Async.thread("Mantaro EventBus-Post", () -> {
            //For now, only used by AsyncInfoMonitor startup and Anime Login Task.
            shardEventBus.post(new PreLoadEvent());
            //Registers all commands
            shardEventBus.post(DefaultCommandProcessor.REGISTRY);
            //Registers all options
            shardEventBus.post(new OptionRegistryEvent());
        });

        return this;
    }

    public ShardedMantaro getShardedInstance() {
        return shardedMantaro;
    }

    public void markAsReady() {
        loadState = POSTLOAD;
    }

    private Future<Set<Class<?>>> lookForAnnotatedOn(String packageName, Class<? extends Annotation> annotation) {
        return Async.future("Annotation Lookup (" + annotation.getSimpleName() + ")", () ->
                new Reflections(packageName, new MethodAnnotationsScanner(), new TypeAnnotationsScanner(), new SubTypesScanner()).getTypesAnnotatedWith(annotation)
        );
    }
}
