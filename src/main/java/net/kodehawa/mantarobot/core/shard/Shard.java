package net.kodehawa.mantarobot.core.shard;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.prometheus.client.Gauge;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.requests.ratelimit.IBucket;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.listeners.entities.CachedMessage;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class Shard {
    private static final Gauge ratelimitBucket = new Gauge.Builder()
                                                         .name("ratelimitBucket")
                                                         .help("shard queue size")
                                                         .labelNames("shardId")
                                                         .register();
    public static final Function<JDA, Integer> QUEUE_SIZE = jda -> {
        int sum = 0;
        for(final IBucket bucket : ((JDAImpl) jda).getRequester().getRateLimiter().getRouteBuckets()) {
            sum += bucket.getRequests().size();
        }
        
        return sum;
    };
    
    private final Cache<Long, Optional<CachedMessage>> messageCache =
            CacheBuilder.newBuilder().concurrencyLevel(5).maximumSize(2500).build();
    
    private final MantaroEventManager manager = new MantaroEventManager();
    private final int id;
    private final EventListener listener;
    private ScheduledFuture<?> queueSizes;
    private JDA jda;
    
    public Shard(int id) {
        this.id = id;
        this.listener = new ListenerAdapter() {
            @Override
            public synchronized void onReady(@Nonnull ReadyEvent event) {
                jda = event.getJDA();
                if(queueSizes != null) {
                    queueSizes.cancel(true);
                }
                queueSizes = MantaroBot.getInstance().getExecutorService().scheduleAtFixedRate(
                        () -> ratelimitBucket.labels(String.valueOf(id))
                                      .set(QUEUE_SIZE.apply(event.getJDA())), 1, 1, TimeUnit.MINUTES
                );
            }
        };
    }
    
    public int getId() {
        return id;
    }
    
    public Cache<Long, Optional<CachedMessage>> getMessageCache() {
        return messageCache;
    }
    
    public MantaroEventManager getManager() {
        return manager;
    }
    
    public EventListener getListener() {
        return listener;
    }
    
    public JDA getJDA() {
        return Objects.requireNonNull(jda, "Shard has not been started yet");
    }
}
