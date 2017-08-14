package net.kodehawa.mantarobot.commands.game;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.commands.game.core.ImageGame;
import net.kodehawa.mantarobot.core.listeners.operations.core.InteractiveOperation;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static br.com.brjdevs.java.utils.collections.CollectionUtils.random;

@Slf4j(topic = "Game [Pokemon Trivia]")
public class Pokemon extends ImageGame {
	private static final DataManager<List<String>> GUESSES = new SimpleFileDataManager("assets/mantaro/texts/pokemonguess.txt");
	private List<String> expectedAnswer;
	private final int maxAttempts = 5;

	public Pokemon() {
		super(10);
	}

	@Override
	public void call(GameLobby lobby, HashMap<Member, Player> players) {
		InteractiveOperations.createOverriding(lobby.getChannel(), 120, new InteractiveOperation() {
			@Override
			public int run(GuildMessageReceivedEvent event) {
				return callDefault(event, lobby, players, expectedAnswer, getAttempts(), maxAttempts, 15);
			}

			@Override
			public void onExpire() {
				lobby.getChannel().sendMessage(EmoteReference.ERROR + "The time ran out! Possible answers were: " + expectedAnswer.stream().collect(Collectors.joining(" ,"))).queue();
				GameLobby.LOBBYS.remove(lobby.getChannel());
			}
		});
	}

	public boolean onStart(GameLobby lobby) {
		try {
			String[] data = random(GUESSES.get()).split("`");
			String pokemonImage = data[0];
			expectedAnswer = Stream.of(data).filter(e -> !e.equals(pokemonImage)).collect(Collectors.toList());
			sendEmbedImage(lobby.getChannel(), pokemonImage, eb -> eb
				.setTitle("Who's that pokemon?", null)
				.setFooter("You have 5 attempts and 120 seconds. (Type end to end the game)", null)
			).queue();
			return true;
		} catch (Exception e) {
			lobby.getChannel().sendMessage(EmoteReference.ERROR + "Error while setting up a game.").queue();
			log.warn("Exception while setting up a game", e);
			return false;
		}
	}
}