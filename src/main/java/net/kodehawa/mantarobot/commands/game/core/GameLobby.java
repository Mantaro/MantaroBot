/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.game.core;

import lombok.Getter;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.interaction.Lobby;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GameLobby extends Lobby {

    public static final Map<TextChannel, GameLobby> LOBBYS = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

	@Getter
	GuildMessageReceivedEvent event;
	@Getter
	LinkedList<Game> gamesToPlay;
	@Getter
	Guild guild;
	@Getter
	List<String> players;

	public GameLobby(GuildMessageReceivedEvent event, List<String> players, LinkedList<Game> games) {
		super(event.getGuild().getId(), event.getChannel().getId());
		this.guild = event.getGuild();
		this.event = event;
		this.players = players;
		this.gamesToPlay = games;
	}

    @Override
    public String toString() {
        return String.format("GameLobby{%s, %s, players:%d, channel:%s}", event.getGuild(),
                gamesToPlay.stream().map(Game::name).collect(Collectors.toList()), players.size(), getChannel());
    }

    public void startFirstGame() {
        LOBBYS.put(event.getChannel(), this);
        if(gamesToPlay.getFirst().onStart(this)) {
            gamesToPlay.getFirst().call(this, players);
            DBGuild dbGuild = MantaroData.db().getGuild(guild);
            dbGuild.getData().setGameTimeoutExpectedAt(String.valueOf(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(75)));
            dbGuild.saveAsync();
        } else {
            LOBBYS.remove(getChannel());
            gamesToPlay.clear();
        }
    }

    //This runs async because I need the operation to end *before* this, also if this takes too long games get stuck.
    public void startNextGame() {
        executorService.execute(() -> {
            try {
                gamesToPlay.removeFirst();

                if(gamesToPlay.isEmpty()) {
                    LOBBYS.remove(getChannel());
                    return;
                }

                if (gamesToPlay.getFirst().onStart(this)) {
                    gamesToPlay.getFirst().call(this, players);
                } else {
                    gamesToPlay.clear();
                    LOBBYS.remove(getChannel());
                }
            } catch (Exception e) {
                gamesToPlay.clear();
                LOBBYS.remove(getChannel());
            }
        });
    }
}
