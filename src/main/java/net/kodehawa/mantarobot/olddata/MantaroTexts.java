package net.kodehawa.mantarobot.olddata;

import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;

import java.util.List;

public class MantaroTexts {

	public static final DataManager<List<String>> facts = new SimpleFileDataManager("assets/mantaro/texts/facts.txt");
	public static final DataManager<List<String>> hangmanWords = new SimpleFileDataManager("assets/mantaro/texts/hangman.txt");
	public static final DataManager<List<String>> noble = new SimpleFileDataManager("assets/mantaro/texts/noble.txt");

	public static final DataManager<List<String>> pokemon = new SimpleFileDataManager("assets/mantaro/texts/pokemonguess.txt");

	public static final DataManager<List<String>> trivia = new SimpleFileDataManager("assets/mantaro/texts/trivia.txt");

}
