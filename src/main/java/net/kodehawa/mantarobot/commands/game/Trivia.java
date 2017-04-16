package net.kodehawa.mantarobot.commands.game;

import br.com.brjdevs.java.utils.extensions.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.game.core.Game;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperation;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;

import java.util.HashMap;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;

@Slf4j(topic = "Game[Trivia]")
public class Trivia extends Game {
	private static final DataManager<List<String>> TRIVIA = new SimpleFileDataManager("assets/mantaro/texts/trivia.txt");
	private String expectedAnswer;
	private int maxAttempts = 10;

	public Trivia() {
		super();
	}

	@Override
	public void call(GameLobby lobby, HashMap<Member, Player> players) {
		InteractiveOperations.create(lobby.getChannel(), "Game", (int) TimeUnit.MINUTES.toMillis(2), OptionalInt.empty(), new InteractiveOperation() {
					@Override
					public boolean run(GuildMessageReceivedEvent event) {
						return callDefault(event, lobby, players, expectedAnswer, getAttempts(), maxAttempts);
					}

					@Override
					public void onExpire(){
						lobby.getChannel().sendMessage(EmoteReference.ERROR + "The time ran out! Correct answer was " + expectedAnswer).queue();
						GameLobby.LOBBYS.remove(lobby.getChannel());
					}
				}
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
			log.warn("Error while starting a trivia game", e);
			return false;
		}
	}
}