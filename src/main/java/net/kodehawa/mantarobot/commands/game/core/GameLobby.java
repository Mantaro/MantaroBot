package net.kodehawa.mantarobot.commands.game.core;

import lombok.Getter;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.interaction.Lobby;
import net.kodehawa.mantarobot.db.entities.Player;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class GameLobby extends Lobby {

	public static final Map<TextChannel, GameLobby> LOBBYS = new HashMap<>();

	@Getter
	GuildMessageReceivedEvent event;
	@Getter
	LinkedList<Game> gamesToPlay;
	@Getter
	Guild guild;
	@Getter
	HashMap<Member, Player> players;

	public GameLobby(GuildMessageReceivedEvent event, HashMap<Member, Player> players, LinkedList<Game> games) {
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

	public void startNextGame() {
		gamesToPlay.removeFirst();
		try {
			if (gamesToPlay.getFirst().onStart(this)) {
				gamesToPlay.getFirst().call(this, players);
			} else {
				gamesToPlay.clear();
				LOBBYS.remove(getChannel());
			}
		} catch (Exception e) {
			LOBBYS.remove(getChannel());
		}
	}
}