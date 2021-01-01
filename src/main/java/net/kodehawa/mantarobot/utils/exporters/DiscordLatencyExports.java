/*
 * Copyright (C) 2016-2021 David Rubio Escares / Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

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
