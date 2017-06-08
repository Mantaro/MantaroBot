package net.kodehawa.mantarobot.web;

import lombok.AllArgsConstructor;
import net.dv8tion.jda.core.JDA;

import java.util.List;

@AllArgsConstructor
public class ShardInfo {
    private List<Integer> ids;
    private List<JDA.Status> statuses;
    private List<Integer> users;
    private List<Integer> guilds;
    private List<Long> musicConnections;
    private List<Long> lastUnifiedJDALastEventTimes;
}
