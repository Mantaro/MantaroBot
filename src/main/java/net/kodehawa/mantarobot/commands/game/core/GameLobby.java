package net.kodehawa.mantarobot.commands.game.core;

import lombok.Getter;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.game.Character;
import net.kodehawa.mantarobot.commands.game.Pokemon;
import net.kodehawa.mantarobot.commands.game.Trivia;
import net.kodehawa.mantarobot.commands.interaction.Lobby;
import net.kodehawa.dataporter.oldentities.OldPlayer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class GameLobby extends Lobby {

	public static Map<TextChannel, GameLobby> LOBBYS = new HashMap<>();
	@Getter
	private static Map<String, Game> textRepresentation = new HashMap<>();

	static {
		textRepresentation.clear();
		textRepresentation.put("trivia", new Trivia());
		textRepresentation.put("pokemon", new Pokemon());
		textRepresentation.put("character", new Character());
	}

	@Getter
	GuildMessageReceivedEvent event;
	@Getter
	LinkedList<Game> gamesToPlay;
	@Getter
	Guild guild;
	@Getter
	HashMap<Member, OldPlayer> players;

	public GameLobby(GuildMessageReceivedEvent event, HashMap<Member, OldPlayer> players, LinkedList<Game> games) {
		super(event.getChannel());
		this.guild = event.getGuild();
		this.event = event;
		this.players = players;
		this.gamesToPlay = games;
	}

	@Override
	public String toString() {
		return String.format("GameLobby{%s, %s, players:%d, channel:%s}", event.getGuild(), gamesToPlay, players.size(), getChannel());
	}

	public void startFirstGame() {
		LOBBYS.put(event.getChannel(), this);
		if (gamesToPlay.getFirst().onStart(this)) {
			gamesToPlay.getFirst().call(this, players);
		} else {
			LOBBYS.remove(getChannel());
			gamesToPlay.clear();
		}
	}

	boolean startNextGame() {
		gamesToPlay.removeFirst();
		try {
			if (gamesToPlay.getFirst().onStart(this)) {
				gamesToPlay.getFirst().call(this, players);
			} else {
				gamesToPlay.clear();
				LOBBYS.remove(getChannel());
				return false;
			}
		} catch (Exception e) {
			LOBBYS.remove(getChannel());
		}

		return false;
	}
}