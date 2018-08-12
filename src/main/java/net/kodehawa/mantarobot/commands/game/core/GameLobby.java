/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.interaction.Lobby;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.utils.Prometheus;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class GameLobby extends Lobby {

    public static final Map<Long, GameLobby> LOBBYS = new ConcurrentHashMap<>();
    private static final ExecutorService executorService = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                .setNameFormat("GameLobby-CachedExecutor")
                .build()
    );

    @Getter
    GuildMessageReceivedEvent event;
    @Getter
    LinkedList<Game> gamesToPlay;
    @Getter
    Guild guild;
    @Getter
    List<String> players;
    @Getter
    I18nContext languageContext;

    static {
        Prometheus.THREAD_POOL_COLLECTOR.add("game-lobbies", executorService);
    }

    public GameLobby(GuildMessageReceivedEvent event, I18nContext languageContext, List<String> players, LinkedList<Game> games) {
        super(event.getGuild().getId(), event.getChannel().getId());
        this.guild = event.getGuild();
        this.event = event;
        this.players = players;
        this.languageContext = languageContext;
        this.gamesToPlay = games;
    }

    @Override
    public String toString() {
        return String.format("GameLobby{%s, %s, players:%d, channel:%s}", event.getGuild(),
                gamesToPlay.stream().map(Game::name).collect(Collectors.toList()), players.size(), getChannel());
    }

    public void startFirstGame() {
        LOBBYS.put(event.getChannel().getIdLong(), this);
        if(gamesToPlay.getFirst().onStart(this)) {
            DBGuild dbGuild = MantaroData.db().getGuild(guild);
            dbGuild.getData().setGameTimeoutExpectedAt(String.valueOf(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(70)));
            dbGuild.save();

            gamesToPlay.getFirst().call(this, players);
        } else {
            startNextGame(false);
        }
    }

    //This runs async because I need the operation to end *before* this, also if this takes too long games get stuck.
    public void startNextGame(boolean success) {
        executorService.execute(() -> {
            try {
                if(!success)
                    gamesToPlay.clear();
                else
                    gamesToPlay.removeFirst();
                
                if(gamesToPlay.isEmpty() || !success) {
                    LOBBYS.remove(getChannel().getIdLong());
                    return;
                }

                if(gamesToPlay.getFirst().onStart(this)) {
                    gamesToPlay.getFirst().call(this, players);
                } else {
                    gamesToPlay.clear();
                    LOBBYS.remove(getChannel().getIdLong());
                }
            } catch(Exception e) {
                gamesToPlay.clear();
                LOBBYS.remove(getChannel().getIdLong());
            }
        });
    }
}
