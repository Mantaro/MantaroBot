package net.kodehawa.mantarobot.web;

import lombok.Data;

import java.util.List;

@Data
public class StatsEntity {
    private String jdaVersion;
    private String lpVersion;
    private String botVersion;
    private Integer guilds;
    private Integer users;
    private Integer shardsTotal;
    private Integer executedCommands;
    private Integer logTotal;
    private Integer musicConnections;
    private Integer parsedCPUUsage;
    private Integer queueSize;
    private List<Integer> memoryUsage; //[used, total]
}
