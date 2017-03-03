package net.kodehawa.mantarobot.commands.currency.game;

public enum GameReference {

	HANGMAN("Hangman", "fun", 1), GUESS("Guess game", "memory", 2), IMAGEGUESS("Guess the character", "memory", 3);

	String name;
	String type;
	int id;

	GameReference(String game, String type, int id){
		this.name = game;
		this.type = type;
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public int getId() {
		return id;
	}

	@Override
	public String toString() {
		return String.format("Game({name: %s, type: %s, id: %s})", getName(), getType(), getId());
	}
}
