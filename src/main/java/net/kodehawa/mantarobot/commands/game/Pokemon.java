/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.game;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.commands.game.core.ImageGame;
import net.kodehawa.mantarobot.commands.game.core.PokemonGameData;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.InteractiveOperation;
import net.kodehawa.mantarobot.utils.APIUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.JsonDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Pokemon extends ImageGame {
    private static final Logger log = LoggerFactory.getLogger("Game [Pokemon Trivia]");
    private static final int maxAttempts = 5;
    private List<String> expectedAnswer;

    public Pokemon() {
        super(10);
    }

    @Override
    public void call(GameLobby lobby, List<String> players) {
        InteractiveOperations.create(lobby.getChannel(), Long.parseLong(lobby.getPlayers().get(0)), 75, new InteractiveOperation() {
            @Override
            public int run(GuildMessageReceivedEvent event) {
                return callDefault(event, lobby, players, expectedAnswer, getAttempts(), maxAttempts, 15);
            }

            @Override
            public void onExpire() {
                if (lobby.getChannel() == null) {
                    GameLobby.LOBBYS.remove(Long.parseLong(lobby.getChannelId()));
                    return;
                }

                lobby.getChannel().sendMessageFormat(
                        lobby.getLanguageContext().get("commands.game.lobby_timed_out"),
                        EmoteReference.ERROR, String.join(", ", expectedAnswer)
                ).queue();

                GameLobby.LOBBYS.remove(lobby.getChannel().getIdLong());
            }

            @Override
            public void onCancel() {
                GameLobby.LOBBYS.remove(lobby.getChannel().getIdLong());
            }
        });
    }

    public boolean onStart(GameLobby lobby) {
        final var languageContext = lobby.getLanguageContext();

        try {
            var data = JsonDataManager.fromJson(APIUtils.getFrom("/mantaroapi/bot/pokemon"), PokemonGameData.class);
            expectedAnswer = data.getNames();
            sendEmbedImage(lobby.getChannel(), data.getImage(), eb ->
                    eb.setTitle(languageContext.get("commands.game.pokemon.header"), null)
                            .setFooter(languageContext.get("commands.game.pokemon.footer"), null)
            ).queue(success -> lobby.setGameLoaded(true));
            return true;
        } catch (Exception e) {
            lobby.getChannel().sendMessageFormat(languageContext.get("commands.game.error"), EmoteReference.ERROR).queue();
            log.warn("Exception while setting up a game", e);
            return false;
        }
    }

    @Override
    public String name() {
        return "pokemon";
    }
}
