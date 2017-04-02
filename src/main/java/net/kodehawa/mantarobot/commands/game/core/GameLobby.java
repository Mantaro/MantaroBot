package net.kodehawa.mantarobot.commands.game.core;

import lombok.Getter;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.interaction.Lobby;

import java.util.LinkedList;
import java.util.List;

public class GameLobby extends Lobby {

	@Getter
	List<Member> players;
	@Getter
	LinkedList<Game> gamesToPlay;
	@Getter
	GuildMessageReceivedEvent event;

	public GameLobby(GuildMessageReceivedEvent event, List<Member> players, LinkedList<Game> games){
		super(event.getChannel());
		this.event = event;
		this.players = players;
		this.gamesToPlay = games;
	}

	public void startFirstGame(){
		if(gamesToPlay.getFirst().onStart(this, players)){
			gamesToPlay.getFirst().call(this, players);
		}
	}
}
