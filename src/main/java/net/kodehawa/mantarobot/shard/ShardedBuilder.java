package net.kodehawa.mantarobot.shard;

import net.kodehawa.mantarobot.core.processor.core.ICommandProcessor;

public class ShardedBuilder {

    public ShardedBuilder() {}

    private int amount = 1;
    private boolean auto;
    private boolean debug;
    private String token;
    private ICommandProcessor commandProcessor;

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

    public ShardedBuilder commandProcessor(ICommandProcessor processor){
        this.commandProcessor = processor;
        return this;
    }

    public ShardedMantaro build() {
        if(token == null) throw new IllegalArgumentException("Token cannot be null");
        if(commandProcessor == null) throw new IllegalArgumentException("H-How do you expect me to process commands!");
        return new ShardedMantaro(amount, debug, auto, token, commandProcessor);
    }
}
