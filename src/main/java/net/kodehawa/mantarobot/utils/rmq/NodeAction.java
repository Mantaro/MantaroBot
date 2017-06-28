package net.kodehawa.mantarobot.utils.rmq;

public enum NodeAction {
    //RIP.
    SHUTDOWN,
    //Something did a boom or it has been running for too long.
    RESTART,
    //Should give a restart_shard signal to the node to restart X shard. Shard should be specified in the JSON payload.
    RESTART_SHARD,
    //meme, kappa. Should restart all node shards in a rolling fashion.
    ROLLING_RESTART
}