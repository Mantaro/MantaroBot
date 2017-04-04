package net.kodehawa.mantarobot.core;

import br.com.brjdevs.java.utils.extensions.Async;
import com.google.common.primitives.Ints;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.hooks.InterfacedEventManager;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.utils.UnsafeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class MantaroEventManager extends InterfacedEventManager {
    static final Logger LOGGER = LoggerFactory.getLogger("ShardWatcher");

    private final int totalShards;

    public MantaroEventManager(int totalShards) {
        this.totalShards = totalShards;
    }

    @Override
    public void handle(Event event) {
        Async.thread("Async EventHandling", () -> super.handle(event));
    }

    public void clearListeners() {
        for(Object o : super.getRegisteredListeners())
            super.unregister(o);
    }
    
    public void checkShards(int timeout) {
        LOGGER.debug("Checking shards...");
        ShardMonitorEvent event = new ShardMonitorEvent(totalShards);
        handle(event);
        try {
            Thread.sleep(timeout);
        } catch(InterruptedException e) {
            UnsafeUtils.throwException(e);
        }
        int[] dead = event.getDeadShards();
        if(dead.length != 0) {
            LOGGER.error("Dead shards found: " + Ints.asList(dead));
            Arrays.stream(dead).forEach(id->{
                try{
                    MantaroBot.getInstance().getShard(id).restartJDA();
                } catch (Exception e){
                    LOGGER.warn("Exception while restarting shard #" + id, e);
                }
            });
            Arrays.stream(dead).forEach(id-> MantaroBot.getInstance().getShard(id).readdListeners());
        }
    }

    public void checkShards() {
        checkShards(1000);
    }
}
