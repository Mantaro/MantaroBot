package net.kodehawa.mantarobot.core;

import br.com.brjdevs.java.utils.async.Async;
import com.google.common.eventbus.EventBus;
import io.sentry.Sentry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.listeners.events.PostLoadEvent;
import net.kodehawa.mantarobot.core.processor.DefaultCommandProcessor;
import net.kodehawa.mantarobot.core.processor.core.ICommandProcessor;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.shard.ShardedBuilder;
import net.kodehawa.mantarobot.shard.ShardedMantaro;
import net.kodehawa.mantarobot.shard.watcher.ShardWatcher;
import net.kodehawa.mantarobot.utils.banner.BannerPrinter;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.concurrent.Future;

import static net.kodehawa.mantarobot.MantaroBot.loadState;
import static net.kodehawa.mantarobot.core.LoadState.LOADED;
import static net.kodehawa.mantarobot.core.LoadState.LOADING;

@Slf4j
public class MantaroCore {

    private final Config config;
    private final boolean useBanner;
    private final boolean useSentry;
    private final boolean isDebug;
    private String commandsPackage;
    private String optsPackage;
    private ICommandProcessor commandProcessor = new DefaultCommandProcessor();
    private ShardedMantaro shardedMantaro;

    public MantaroCore(Config config, boolean useBanner, boolean useSentry, boolean isDebug){
        this.config = config;
        this.useBanner = useBanner;
        this.useSentry = useSentry;
        this.isDebug = isDebug;
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
                .commandProcessor(new DefaultCommandProcessor())
                .build();

        shardedMantaro.shard();
        Async.thread("ShardWatcherThread", new ShardWatcher());

        loadState = LOADED;
    }

    private void startSingleShardInstance() throws Exception {
        loadState = LOADING;

        shardedMantaro = new ShardedBuilder()
                .amount(1)
                .token(config.token)
                .commandProcessor(new DefaultCommandProcessor())
                .build();

        shardedMantaro.shard();

        loadState = LOADED;
    }

    public MantaroCore startMainComponents(boolean single) throws Exception {
        if(config == null) throw new IllegalArgumentException("Config cannot be null!");

        if(useSentry)
            Sentry.init(config.sentryDSN);
        if(useBanner)
            new BannerPrinter(1).printBanner();

        if(commandsPackage == null) throw new IllegalArgumentException("Cannot look for commands if you don't specify where!");
        if(optsPackage == null) throw new IllegalArgumentException("Cannot look for options if you don't specify where!");

        Future<Set<Class<?>>> commands = lookForAnnotatedOn(commandsPackage, Module.class);
        Future<Set<Class<?>>> options = lookForAnnotatedOn(optsPackage, Option.class);

        if(single){
            startSingleShardInstance();
        } else {
            startShardedInstance();
        }

        EventBus bus = new EventBus();
        commands.get().forEach(clazz->{
            try {
                bus.register(clazz.newInstance());
            } catch(Exception e) {
                log.error("Invalid module: no zero arg public constructor found for " + clazz);
            }
        });

        EventBus bus1 = new EventBus();
        options.get().forEach(clazz->{
            try {
                bus1.register(clazz.newInstance());
            } catch(Exception e) {
                log.error("Invalid module: no zero arg public constructor found for " + clazz);
            }
        });

        bus.post(DefaultCommandProcessor.REGISTRY);
        bus.post(new PostLoadEvent());
        bus1.post(new OptionRegistryEvent());

        return this;
    }

    public ShardedMantaro getShardedInstance(){
        return shardedMantaro;
    }

    private Future<Set<Class<?>>> lookForAnnotatedOn(String packageName, Class<? extends Annotation> annotation) {
        return Async.future("Annotation Lookup (" + annotation + ")", () ->
                new Reflections(packageName,new MethodAnnotationsScanner(),new TypeAnnotationsScanner(), new SubTypesScanner()).getTypesAnnotatedWith(annotation)
        );
    }
}
