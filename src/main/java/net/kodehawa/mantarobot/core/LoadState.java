package net.kodehawa.mantarobot.core;

public enum LoadState {
    PRELOAD("PreLoad", "Starting..."), LOADING("Load", "Loading..."), LOADING_SHARDS("Shard Loading", "Loading shards..."),
    LOADED("Loaded", "Loaded"), POSTLOAD("Ready", "Ready");

    private final String s;
    private final String verbose;

    LoadState(String s, String verbose) {

        this.s = s;
        this.verbose = verbose;
    }

    @Override
    public String toString() {
        return s;
    }

    public String verbose() {
        return verbose;
    }
}
