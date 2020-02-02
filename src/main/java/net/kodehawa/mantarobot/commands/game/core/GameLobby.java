/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.game.core;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
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

public class GameLobby extends Lobby {
    
    public static final Map<Long, GameLobby> LOBBYS = new ConcurrentHashMap<>();
    private static final ExecutorService executorService = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                    .setNameFormat("GameLobby-CachedExecutor")
                    .build()
    );
    
    static {
        Prometheus.THREAD_POOL_COLLECTOR.add("game-lobbies", executorService);
    }

    public boolean gameLoaded = false;
    GuildMessageReceivedEvent event;
    LinkedList<Game<?>> gamesToPlay;
    Guild guild;
    List<String> players;
    I18nContext languageContext;
    
    public GameLobby(GuildMessageReceivedEvent event, I18nContext languageContext, List<String> players, LinkedList<Game<?>> games) {
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
        if(gamesToPlay.getFirst().onStart(this)) {
            setGameLoaded(false);
            LOBBYS.put(event.getChannel().getIdLong(), this);
            DBGuild dbGuild = MantaroData.db().getGuild(guild);
            dbGuild.getData().setGameTimeoutExpectedAt(String.valueOf(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(70)));
            dbGuild.save();
            
            gamesToPlay.getFirst().call(this, players);
        } else {
            //if first game fails we need this.
            LOBBYS.put(event.getChannel().getIdLong(), this);
            startNextGame(false);
        }
    }
    
    //This runs async because I need the operation to end *before* this, also if this takes too long games get stuck.
    public void startNextGame(boolean success) {
        executorService.execute(() -> {
            setGameLoaded(false);
            try {
                if(!success)
                    gamesToPlay.clear();
                else
                    gamesToPlay.removeFirst();
                
                if(gamesToPlay.isEmpty() || !success) {
                    LOBBYS.remove(getChannel().getIdLong());
                    return;
                }
                
                //fuck userbots
                Thread.sleep(250); //250ms.
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
    
    public GuildMessageReceivedEvent getEvent() {
        return this.event;
    }
    
    public LinkedList<Game<?>> getGamesToPlay() {
        return this.gamesToPlay;
    }
    
    public Guild getGuild() {
        return this.guild;
    }
    
    public List<String> getPlayers() {
        return this.players;
    }
    
    public I18nContext getLanguageContext() {
        return this.languageContext;
    }
    
    public boolean isGameLoaded() {
        return this.gameLoaded;
    }
    
    public void setGameLoaded(boolean gameLoaded) {
        this.gameLoaded = gameLoaded;
    }
}
