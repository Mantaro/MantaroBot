package net.kodehawa.mantarobot.commands.currency.game;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.entity.player.EntityPlayer;
import net.kodehawa.mantarobot.commands.currency.game.core.Game;
import net.kodehawa.mantarobot.commands.currency.game.core.GameReference;
import net.kodehawa.mantarobot.commands.currency.world.TextChannelWorld;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

public class Trivia extends Game {

	private String expectedAnswer;
	private int maxAttempts = 10;

	//did you just assume I answered one
	private int triviaAnswers = 1;
	private int maxAnswers = 10;
	private int attempts = 0;
	private static final Logger LOGGER = LoggerFactory.getLogger("Game[Trivia]");
	private List<String> trivia = MantaroData.getTrivia().get();
	private Random rand = new Random();

	@Override
	public boolean onStart(GuildMessageReceivedEvent event, GameReference type, EntityPlayer player) {
		try{
			player.setCurrentGame(type, event.getChannel());
			TextChannelWorld.of(event.getChannel()).addEntity(player, type);
			String[] data = trivia.get(rand.nextInt(trivia.size())).split(":");
			expectedAnswer = data[1];

			event.getChannel().sendMessage(EmoteReference.THINKING + data[0] + " (Type end to end the game)").queue();

			return true;
		} catch (Exception e){
			onError(LOGGER, event, player, e);
			return false;
		}
	}

	@Override
	public void call(GuildMessageReceivedEvent event, EntityPlayer player) {
		if (!(EntityPlayer.getPlayer(event.getAuthor().getId()).getId() == player.getId() && player.getGame() == GameReference.TRIVIA
				&& !event.getMessage().getContent().startsWith(MantaroData.getData().get().getPrefix(event.getGuild())))) {
			return;
		}

		if(event.getMessage().getContent().equalsIgnoreCase("end")){
			endGame(event, player, false);
			return;
		}

		if (attempts > maxAttempts) {
			event.getChannel().sendMessage(EmoteReference.SAD + "You used all of your attempts, game is ending.").queue();
			endGame(event, player, false);
			return;
		}

		if(event.getMessage().getContent().equalsIgnoreCase(expectedAnswer)){
			if(triviaAnswers >= maxAnswers){
				onSuccess(player, event, 0.6);
				return;
			}

			event.getChannel().sendMessage(EmoteReference.CORRECT + "That was it! Now you have to reply " + (maxAnswers - triviaAnswers) + " questions more to get the big prize! Type end to give up.").queue();
			String[] data = trivia.get(rand.nextInt(trivia.size())).split(":");
			expectedAnswer = data[1];
			event.getChannel().sendMessage(EmoteReference.THINKING + data[0]).queue();
			attempts = 0;
			return;
		}

		event.getChannel().sendMessage(EmoteReference.SAD + "That wasn't it! "
				+ EmoteReference.STOPWATCH + "You have " + (maxAttempts - attempts) + " attempts remaning").queue();
		attempts++;
	}
}
