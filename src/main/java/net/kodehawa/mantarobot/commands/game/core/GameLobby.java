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

import java.util.*;

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

	private static Map<TextChannel, GameLobby> LOBBYS = new HashMap<>();

	static {
		textRepresentation.clear();
		textRepresentation.put("trivia", new Trivia());
		textRepresentation.put("pokemon", new Pokemon());
		textRepresentation.put("character", new ImageGuess());
	}

	public GameLobby(GuildMessageReceivedEvent event, HashMap<Member, Player> players, LinkedList<Game> games){
		super(event.getChannel());
		this.guild = event.getGuild();
		this.event = event;
		this.players = players;
		this.gamesToPlay = games;
	}

	public void startFirstGame(){
		if(gamesToPlay.getFirst().onStart(this)){
			gamesToPlay.getFirst().call(this, players);
		} else {
			LOBBYS.remove(getChannel());
			gamesToPlay.clear();
		}
	}

	boolean startNextGame(){
		try{
			Game game = gamesToPlay.get(1);
			gamesToPlay.removeFirst();
			if(game.onStart(this)){
				game.call(this, players);
			} else {
				gamesToPlay.clear();
				LOBBYS.remove(getChannel());
				return false;
			}
		} catch (IndexOutOfBoundsException e){
			return false;
		}

		return false;
	}

	@Override
	public String toString(){
		return String.format("GameLobby{%s, %s, players:%d, channel:%s}", event.getGuild(), gamesToPlay, players.size(), getChannel());
	}
}