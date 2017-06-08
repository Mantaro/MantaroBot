package net.kodehawa.mantarobot.web;

import lombok.AllArgsConstructor;
import net.kodehawa.mantarobot.commands.info.GuildStatsManager;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@AllArgsConstructor
public class GuildsEntity {
    public Map<GuildStatsManager.LoggedEvent, AtomicInteger> total;
    public Map<GuildStatsManager.LoggedEvent, AtomicInteger> today;
    public Map<GuildStatsManager.LoggedEvent, AtomicInteger> hourly;
    public Map<GuildStatsManager.LoggedEvent, AtomicInteger> now;
}