package net.kodehawa.mantarobot.commands.rpg.game;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.rpg.game.core.Game;
import net.kodehawa.mantarobot.commands.rpg.game.core.GameReference;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Pokemon extends Game {

	private static final Logger LOGGER = LoggerFactory.getLogger("Game[PokemonTrivia]");
	public static final DataManager<List<String>> pokemon = new SimpleFileDataManager("assets/mantaro/texts/pokemonguess.txt");
	private int attempts = 1;
	private String expectedAnswer;
	private int maxAttempts = 10;

	public Pokemon(){
		super();
	}

	//TODO oh please.

	@Override
	public void call(GuildMessageReceivedEvent event, Player player) {
		/*if (event.getAuthor().isFake() || !(EntityPlayer.getPlayer(event.getAuthor().getId()).getId() == player.getId() &&
				player.getGame() == type()
				&& !event.getMessage().getContent().startsWith(MantaroData.getData().get().getPrefix(event.getGuild())))) {
			return;
		}

		if (event.getMessage().getContent().equalsIgnoreCase("end")) {
			endGame(event, player, false);
			return;
		}

		if (attempts > maxAttempts) {
			event.getChannel().sendMessage(EmoteReference.SAD + "You used all of your attempts, game is ending.").queue();
			endGame(event, player, false);
			return;
		}

		if (event.getMessage().getContent().equalsIgnoreCase(expectedAnswer)) {
			onSuccess(player, event);
			return;
		}

		event.getChannel().sendMessage(EmoteReference.SAD + "That wasn't it! "
			+ EmoteReference.STOPWATCH + "You have " + (maxAttempts - attempts) + " attempts remaning").queue();

		attempts++;*/
	}

	@Override
	public boolean onStart(GuildMessageReceivedEvent event, GameReference type, Player player) {
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

	public String answer(){
		return expectedAnswer;
	}

	@Override
	public GameReference type() {
		return GameReference.TRIVIA;
	}
}
