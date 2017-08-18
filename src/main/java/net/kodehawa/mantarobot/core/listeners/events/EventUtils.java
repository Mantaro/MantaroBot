package net.kodehawa.mantarobot.core.listeners.events;

import net.dv8tion.jda.core.events.Event;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.shard.ShardedMantaro;

public class EventUtils {

    private static final ShardedMantaro shardedMantaro = MantaroBot.getInstance().getShardedMantaro();

    public static void propagateEvent(MantaroEvent event){
        for(MantaroEventManager manager : shardedMantaro.getManagers()){
            manager.handle((Event) event);
        }

        event.onPropagation();
    }
}
