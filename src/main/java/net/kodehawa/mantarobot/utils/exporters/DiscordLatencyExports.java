package net.kodehawa.mantarobot.utils.exporters;

import io.prometheus.client.Gauge;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.utils.Prometheus;

import java.util.concurrent.TimeUnit;

public class DiscordLatencyExports {
    private static final double MILLISECONDS_PER_SECOND = 1000;

    private static final Gauge GATEWAY_LATENCY = Gauge.build()
            .name("mantaro_shard_latency")
            .help("Gateway latency in seconds, per shard")
            .labelNames("shard")
            .create();
    private static final Gauge REST_LATENCY = Gauge.build()
            .name("mantaro_rest_latency")
            .help("Rest latency in seconds")
            .create();

    public static void register() {
        GATEWAY_LATENCY.register();
        REST_LATENCY.register();

        MantaroBot.getInstance().getExecutorService().scheduleAtFixedRate(() -> {
            var shards = MantaroBot.getInstance().getShardManager().getShardCache();
            shards.forEach(s -> {
                var ping = s.getGatewayPing();

                if (ping >= 0) {
                    GATEWAY_LATENCY.labels(String.valueOf(s.getShardInfo().getShardId()))
                            .set(ping / MILLISECONDS_PER_SECOND);
                }
            });
            shards.iterator().next().getRestPing().queue(ping ->
                    REST_LATENCY.set(ping / MILLISECONDS_PER_SECOND));
        }, 0, Prometheus.UPDATE_PERIOD.toMillis(), TimeUnit.MILLISECONDS);
    }
}
