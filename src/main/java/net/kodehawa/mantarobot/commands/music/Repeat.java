package net.kodehawa.mantarobot.commands.music;

public enum Repeat {
	SONG("song"), QUEUE("queue");

	String s;

	Repeat(String s) {
		this.s = s;
	}

	@Override
	public String toString() {
		return s;
	}
}
