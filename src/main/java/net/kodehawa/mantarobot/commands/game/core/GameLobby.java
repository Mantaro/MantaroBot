package net.kodehawa.mantarobot.commands.game.core;

import lombok.Getter;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.game.ImageGuess;
import net.kodehawa.mantarobot.commands.game.Pokemon;
import net.kodehawa.mantarobot.commands.game.Trivia;
import net.kodehawa.mantarobot.commands.interaction.Lobby;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameLobby extends Lobby {

	@Getter
	HashMap<Member, Player> players;
	@Getter
	LinkedList<Game> gamesToPlay;
	@Getter
	GuildMessageReceivedEvent event;
	@Getter
	Guild guild;
	@Getter
	private static Map<String, Game> textRepresentation = new HashMap<>();

	private ScheduledExecutorService executorService;
	public static Map<TextChannel, GameLobby> LOBBYS = new HashMap<>();

	static {
		textRepresentation.clear();
		textRepresentation.put("trivia", new Trivia());
		textRepresentation.put("pokemon", new Pokemon());
		textRepresentation.put("character", new ImageGuess());
	}

	public GameLobby(GuildMessageReceivedEvent event, HashMap<Member, Player> players, LinkedList<Game> games){
		super(event.getChannel());
		executorService = Executors.newSingleThreadScheduledExecutor();
		this.guild = event.getGuild();
		this.event = event;
		this.players = players;
		this.gamesToPlay = games;
		LOBBYS.put(event.getChannel(), this);
	}

	public void startFirstGame(){
		if(gamesToPlay.getFirst().onStart(this)){
			gamesToPlay.getFirst().call(this, players);
			executorService.schedule(this::timeout, 2, TimeUnit.MINUTES);
		} else {
			LOBBYS.remove(getChannel());
			gamesToPlay.clear();
		}
	}

	public boolean startNextGame(){
		try{
			Game game = gamesToPlay.get(1);
			gamesToPlay.removeFirst();
			if(game.onStart(this)){
				executorService.shutdownNow();
				game.call(this, players);
				executorService.schedule(this::timeout, 2, TimeUnit.MINUTES);
			} else {
				gamesToPlay.clear();
				executorService.shutdownNow();
				LOBBYS.remove(getChannel());
				return false;
			}
		} catch (Exception e){
			if(e instanceof RejectedExecutionException){
				//GAMBIARRA INTENSIFIES
				return false;
			}
			LOBBYS.remove(getChannel());
			executorService.shutdownNow();
			return false;
		}

		executorService.shutdownNow();
		return false;
	}

	private void timeout(){
		gamesToPlay.clear();
		getChannel().sendMessage(EmoteReference.ERROR + "Timed out: Two minutes passed since last reply.").queue();
		LOBBYS.remove(getChannel());
	}

	@Override
	public String toString(){
		return String.format("GameLobby{%s, %s, players:%d, channel:%s}", event, gamesToPlay, players.size(), getChannel());
	}
}