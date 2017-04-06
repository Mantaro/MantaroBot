package net.kodehawa.mantarobot.commands.game;

import br.com.brjdevs.java.utils.extensions.CollectionUtils;
import net.dv8tion.jda.core.entities.Member;
import net.kodehawa.mantarobot.commands.game.core.Game;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Trivia extends Game {

	private static final DataManager<List<String>> TRIVIA = new SimpleFileDataManager("assets/mantaro/texts/trivia.txt");
	private static final Logger LOGGER = LoggerFactory.getLogger("Game[Trivia]");
	private int attempts = 1;
	private String expectedAnswer;
	private int maxAnswers = 10;
	private int maxAttempts = 10;
	private int triviaAnswers = 1;

	public Trivia() {
		super();
	}


	@Override
	public boolean onStart(GameLobby lobby) {
		try{
			String[] s = CollectionUtils.random(TRIVIA.get()).split(":");
			expectedAnswer = s[1];
			lobby.getChannel().sendMessage(EmoteReference.MEGA + s[0]).queue();
			return true;
		} catch (Exception e){
			lobby.getChannel().sendMessage(EmoteReference.ERROR + "Error while starting trivia.").queue();
			LOGGER.warn("Error while starting a trivia game", e);
			return false;
		}
	}

	@Override
	public void call(GameLobby lobby, HashMap<Member, Player> players) {
		InteractiveOperations.create(lobby.getChannel(), "Game", (int) TimeUnit.MINUTES.toMillis(2), OptionalInt.empty(), (e) -> {
			if(!e.getChannel().getId().equals(lobby.getChannel().getId())){
				return false;
			}

			if (e.getMessage().getContent().startsWith(MantaroData.db().getGuild(lobby.getChannel().getGuild()).getData().getGuildCustomPrefix())
					|| e.getMessage().getContent().startsWith(MantaroData.config().get().getPrefix())) {
				return false;
			}

			if(players.keySet().contains(e.getMember())) {
				if (e.getMessage().getContent().equalsIgnoreCase("end")) {
					lobby.getChannel().sendMessage(EmoteReference.CORRECT + "Ended game.").queue();
					if (lobby.startNextGame()) {
						lobby.getChannel().sendMessage("Starting next game...").queue();
					}
					return true;
				}

				if (attempts >= maxAttempts) {
					lobby.getChannel().sendMessage(EmoteReference.ERROR + "Already used all attempts, ending game. Answer was: " + expectedAnswer).queue();
					if (lobby.startNextGame()) {
						lobby.getChannel().sendMessage("Starting next game...").queue();
					}
					return true;
				}
				if (e.getMessage().getContent().equalsIgnoreCase(expectedAnswer)) {
					Player player = players.get(e.getMember());
					player.addMoney(150);
					player.save();
					lobby.getChannel().sendMessage(EmoteReference.MEGA + "**" + e.getMember().getEffectiveName() + "**" + " Just won $150 credits by answering correctly!").queue();
					if (lobby.startNextGame()) {
						lobby.getChannel().sendMessage("Starting next game...").queue();
					}
					return true;
				}

				lobby.getChannel().sendMessage(EmoteReference.ERROR + "That's not it, you have " + (maxAttempts - attempts) + " attempts remaning.").queue();
				attempts++;
				return false;
			}

			return false;
		});
	}
}
