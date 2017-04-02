package net.kodehawa.mantarobot.commands.game;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.game.core.Game;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Pokemon extends Game {

	public static final DataManager<List<String>> pokemon = new SimpleFileDataManager("assets/mantaro/texts/pokemonguess.txt");
	private static final Logger LOGGER = LoggerFactory.getLogger("Game[PokemonTrivia]");
	private int attempts = 1;
	private String expectedAnswer;
	private int maxAttempts = 10;

	public Pokemon() {
		super();
	}

	//TODO oh please.

	@Override
	public boolean onStart(GameLobby lobby, List<Member> players) {
		/*try {
			player.setCurrentGame(type, event.getChannel());
			player.setGameInstance(this);
			TextChannelWorld.of(event.getChannel()).addGame(player, this);
			Random rand = new Random();
			List<String> guesses = pokemon.get();
			String[] data = guesses.get(rand.nextInt(guesses.size())).split("`");
			String pokemonImage = data[0];
			expectedAnswer = data[1];

			event.getChannel().sendMessage(new EmbedBuilder().setTitle("Who's that pokemon?", event.getJDA().getSelfUser().getAvatarUrl())
					.setImage(pokemonImage).setFooter("You have 10 attempts and 60 seconds. (Type end to end the game)", null).build()).queue();
			super.onStart(TextChannelWorld.of(event.getChannel()), event, player);
			return true;
		} catch (Exception e) {
			onError(LOGGER, event, player, e);
			return false;
		}*/
		return false;
	}

	@Override
	public void call(GameLobby lobby, List<Member> players) {

	}

	/*@Override
	public GameReference type() {
		return GameReference.TRIVIA;
	}*/

	public String answer() {
		return expectedAnswer;
	}
}
