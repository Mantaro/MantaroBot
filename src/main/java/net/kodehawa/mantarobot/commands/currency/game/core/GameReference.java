package net.kodehawa.mantarobot.commands.currency.game.core;

public enum GameReference {

	HANGMAN("Hangman", "fun", 1), GUESS("Guess game", "memory", 2), IMAGEGUESS("Guess the character", "memory", 3), TRIVIA("Trivia", "memory", 4);

	int id;
	String name;
	String type;

	GameReference(String game, String type, int id) {
		this.name = game;
		this.type = type;
		this.id = id;
	}

	@Override
	public String toString() {
		return String.format("Game({name: %s, type: %s, id: %s})", getName(), getType(), getId());
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}
}
