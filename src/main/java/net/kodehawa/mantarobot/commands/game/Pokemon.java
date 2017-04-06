package net.kodehawa.mantarobot.commands.game;

import br.com.brjdevs.java.utils.extensions.CollectionUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.kodehawa.mantarobot.commands.game.core.Game;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;

public class Pokemon extends Game {

	private static final DataManager<List<String>> GUESSES = new SimpleFileDataManager("assets/mantaro/texts/pokemonguess.txt");
	private static final Logger LOGGER = LoggerFactory.getLogger("Game[PokemonTrivia]");
	private String expectedAnswer;
	private int maxAttempts = 10;

	public Pokemon() {
		super();
	}

	public boolean onStart(GameLobby lobby) {
		try{
			String[] data = CollectionUtils.random(GUESSES.get()).split("`");
			String pokemonImage = data[0];
			expectedAnswer = data[1];
			System.out.println(expectedAnswer);
			lobby.getChannel().sendMessage(new EmbedBuilder().setTitle("Who's that pokemon?", null)
					.setImage(pokemonImage).setFooter("You have 10 attempts and 120 seconds. (Type end to end the game)", null).build()).queue();
			return true;
		} catch (Exception e){
			lobby.getChannel().sendMessage(EmoteReference.ERROR + "Error while setting up a game.").queue();
			LOGGER.warn("Exception while setting up a game", e);
			return false;
		}
	}

	@Override
	public void call(GameLobby lobby, HashMap<Member, Player> players) {
		InteractiveOperations.create(lobby.getChannel(), "Game", (int) TimeUnit.MINUTES.toMillis(2), OptionalInt.empty(), (e) ->
			callDefault(e, lobby, players, expectedAnswer, getAttempts(), maxAttempts)
		);
	}
}