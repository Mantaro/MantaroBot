package net.kodehawa.mantarobot.core;

public enum LoadState {
	PRELOAD("PreLoad"), LOADING("Load"), LOADING_SHARDS("Shards Loading"), LOADED("Loaded"), POSTLOAD("Ready");

	private final String s;

	LoadState(String s) {
		this.s = s;
	}

	@Override
	public String toString() {
		return s;
	}
}
