package net.kodehawa.mantarobot.shard;

public class ShardedBuilder {

    private int amount = 1;
    private boolean auto;
    private boolean debug;
    private String token;

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
        return new ShardedMantaro(amount, debug, auto, token);
    }
}
