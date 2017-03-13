package net.kodehawa.mantarobot.commands.rpg.game;

import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.rpg.entity.player.EntityPlayer;
import net.kodehawa.mantarobot.commands.rpg.game.core.Game;
import net.kodehawa.mantarobot.commands.rpg.game.core.GameReference;
import net.kodehawa.mantarobot.commands.rpg.world.TextChannelWorld;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.Random;

public class Pokemon extends Game {

	private static final Logger LOGGER = LoggerFactory.getLogger("Game[PokemonTrivia]");
	private int attempts = 0;
	private String expectedAnswer;
	private int maxAttempts = 10;

	@Override
	public void call(GuildMessageReceivedEvent event, EntityPlayer player) {
		if (event.getAuthor().isFake() || !(EntityPlayer.getPlayer(event.getAuthor().getId()).getId() == player.getId() && player.getGame() == GameReference.TRIVIA
				&& !event.getMessage().getContent().startsWith(MantaroData.getData().get().getPrefix(event.getGuild())))) {
			return;
		}

		if (event.getMessage().getContent().equalsIgnoreCase("end")) {
			endGame(event, player, this, false);
			return;
		}

		if (attempts > maxAttempts) {
			event.getChannel().sendMessage(EmoteReference.SAD + "You used all of your attempts, game is ending.").queue();
			endGame(event, player, this, false);
			return;
		}

		if (event.getMessage().getContent().equalsIgnoreCase(expectedAnswer)) {
			onSuccess(player, this, event);
			return;
		}

		event.getChannel().sendMessage(EmoteReference.SAD + "That wasn't it! "
			+ EmoteReference.STOPWATCH + "You have " + (maxAttempts - attempts) + " attempts remaning").queue();

		attempts++;
	}

	@Override
	public boolean onStart(GuildMessageReceivedEvent event, GameReference type, EntityPlayer player) {
		try {
			TextChannelWorld.of(event.getChannel()).addGame(type);
			player.setCurrentGame(type, event.getChannel());
			Random rand = new Random();
			List<String> guesses = MantaroData.getPokemonGuesses().get();
			String[] data = guesses.get(rand.nextInt(guesses.size())).split("`");
			String pokemonImage = data[0];
			expectedAnswer = data[1];

			event.getChannel().sendFile(new URL(pokemonImage).openStream(), "pokemon.jpg",
				new MessageBuilder().append(EmoteReference.TALKING).append("Who's that pokemon?. You have 10 attempts to do it. (Type end to end the game)")
					.build()).queue();

			return true;
		} catch (Exception e) {
			onError(LOGGER, event, this, player, e);
			return false;
		}
	}

	public String answer(){
		return expectedAnswer;
	}

	@Override
	public GameReference type() {
		return GameReference.TRIVIA;
	}
}
