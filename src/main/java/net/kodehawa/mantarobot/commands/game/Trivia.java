package net.kodehawa.mantarobot.commands.game;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.game.core.Game;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

public class Trivia extends Game {

	public static final DataManager<List<String>> TRIVIA = new SimpleFileDataManager("assets/mantaro/texts/trivia.txt");
	private static final Logger LOGGER = LoggerFactory.getLogger("Game[Trivia]");
	private int attempts = 1;
	private String expectedAnswer;
	private int maxAnswers = 10;
	private int maxAttempts = 10;
	private Random rand = new Random();
	//did you just assume I answered one
	private int triviaAnswers = 1;

	public Trivia() {
		super();
	}

	//TODO oh please.

	//@Override
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
			if (triviaAnswers >= maxAnswers) {
				onSuccess(player, event, 0.6);
				return;
			}

			event.getChannel().sendMessage(EmoteReference.CORRECT + "That was it! Now you have to reply " + (maxAnswers - triviaAnswers) + " questions more to get the big prize! Type end to give up.").queue();
			triviaAnswers++;
			String[] data = trivia.get(rand.nextInt(trivia.size())).split(":");
			expectedAnswer = data[1];
			event.getChannel().sendMessage(EmoteReference.THINKING + data[0]).queue();
			attempts = 0;
			return;
		}

		event.getChannel().sendMessage(EmoteReference.SAD + "That wasn't it! "
			+ EmoteReference.STOPWATCH + "You have " + (maxAttempts - attempts) + " attempts remaining").queue();
		attempts++;*/
	}

	//@Override
	public boolean onStart(GuildMessageReceivedEvent event, /*GameReference type,*/ Player player) {
		/*try {
			player.setCurrentGame(type, event.getChannel());
			player.setGameInstance(this);
			TextChannelWorld.of(event.getChannel()).addGame(player, this);
			String[] data = trivia.get(rand.nextInt(trivia.size())).split(":");
			expectedAnswer = data[1];

			event.getChannel().sendMessage(EmoteReference.THINKING + data[0] + " (Type end to end the game)").queue();
			super.onStart(TextChannelWorld.of(event.getChannel()), event, player);
			return true;
		} catch (Exception e) {
			onError(LOGGER, event, player, e);
			return false;
		}*/
		return false;
	}

	/*@Override
	public GameReference type() {
		return GameReference.TRIVIA;
	}*/

	public String answer() {
		return expectedAnswer;
	}
}
