package net.kodehawa.mantarobot.core;

public enum State {
	PRELOAD("PreLoad", "Starting..."), LOADING("Load", "Loading..."), LOADED("Loaded", "Loaded."), POSTLOAD("Ready", "Ready.");

	private final String s;
	private final String verbose;

	State(String s, String verbose) {

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
