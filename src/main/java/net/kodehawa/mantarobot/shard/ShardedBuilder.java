package net.kodehawa.mantarobot.shard;

import junit.framework.Assert;

public class ShardedBuilder {

    int amount = 1;
    boolean auto;
    boolean debug;
    String token;
    public ShardedBuilder() {
    }

    public ShardedBuilder amount(int shardAmount) {
        amount = shardAmount;
        return this;
    }

    public ShardedBuilder auto(boolean auto) {
        this.auto = auto;
        return this;
    }

    public ShardedBuilder debug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public ShardedBuilder token(String token) {
        this.token = token;
        return this;
    }

    public ShardedMantaro build() {
        Assert.assertNotNull(token, "What is the meaning of life :S");
        return new ShardedMantaro(amount, debug, auto, token);
    }
}
