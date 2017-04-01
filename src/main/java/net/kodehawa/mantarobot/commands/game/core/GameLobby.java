package net.kodehawa.mantarobot.commands.game.core;

import groovy.util.logging.Slf4j;
import lombok.Getter;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.LinkedList;
import java.util.List;

@Slf4j
public class GameLobby extends Lobby {

	@Getter
	List<Member> players;
	@Getter
	LinkedList<Game> gamesToPlay;

	public GameLobby(TextChannel channel, List<Member> players, LinkedList<Game> games){
		super(channel);
		this.players = players;
		this.gamesToPlay = games;
	}

	public Game startFirstGame(){
		if(gamesToPlay.getFirst().onStart(this, players)){
			return gamesToPlay.getFirst();
		}
		return null;
	}
}
