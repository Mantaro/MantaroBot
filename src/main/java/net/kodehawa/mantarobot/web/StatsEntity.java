package net.kodehawa.mantarobot.web;

import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
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
    private Integer cpuCores;
    private Long queueSize;
    private List<Integer> memoryUsage; //[used, total]
}
