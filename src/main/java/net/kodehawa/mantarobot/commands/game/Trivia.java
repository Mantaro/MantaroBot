package net.kodehawa.mantarobot.commands.game;

import br.com.brjdevs.java.utils.extensions.CollectionUtils;
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

public class Trivia extends Game {

	private static final Logger LOGGER = LoggerFactory.getLogger("Game[Trivia]");
	private static final DataManager<List<String>> TRIVIA = new SimpleFileDataManager("assets/mantaro/texts/trivia.txt");
	private String expectedAnswer;
	private int maxAttempts = 10;

	public Trivia() {
		super();
	}

	@Override
	public void call(GameLobby lobby, HashMap<Member, Player> players) {
		InteractiveOperations.create(lobby.getChannel(), "Game", (int) TimeUnit.MINUTES.toMillis(2), OptionalInt.empty(), (e) ->
			callDefault(e, lobby, players, expectedAnswer, getAttempts(), maxAttempts)
		);
	}

	@Override
	public boolean onStart(GameLobby lobby) {
		try {
			String[] s = CollectionUtils.random(TRIVIA.get()).split(":");
			expectedAnswer = s[1];
			lobby.getChannel().sendMessage(EmoteReference.MEGA + s[0]).queue();
			return true;
		} catch (Exception e) {
			lobby.getChannel().sendMessage(EmoteReference.ERROR + "Error while starting trivia.").queue();
			LOGGER.warn("Error while starting a trivia game", e);
			return false;
		}
	}
}
