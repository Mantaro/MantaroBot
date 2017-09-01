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

package net.kodehawa.mantarobot.commands.game;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.commands.game.core.ImageGame;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.InteractiveOperation;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static br.com.brjdevs.java.utils.collections.CollectionUtils.random;

@Slf4j(topic = "Game [Pokemon Trivia]")
public class Pokemon extends ImageGame {
    private static final DataManager<List<String>> GUESSES = new SimpleFileDataManager("assets/mantaro/texts/pokemonguess.txt");
    private final int maxAttempts = 5;
    private List<String> expectedAnswer;

    public Pokemon() {
        super(10);
    }

	@Override
	public void call(GameLobby lobby, List<String> players) {
		InteractiveOperations.createOverriding(lobby.getChannel(), 75, new InteractiveOperation() {
			@Override
			public int run(GuildMessageReceivedEvent event) {
				return callDefault(event, lobby, players, expectedAnswer, getAttempts(), maxAttempts, 15);
			}

            @Override
            public void onExpire() {
                lobby.getChannel().sendMessage(EmoteReference.ERROR + "The time ran out! Possible answers were: " + String.join(", ", expectedAnswer)).queue();
                GameLobby.LOBBYS.remove(lobby.getChannel());
            }
        });
    }

    public boolean onStart(GameLobby lobby) {
        try {
            String[] data = random(GUESSES.get()).split("`");
            String pokemonImage = data[0];
            expectedAnswer = Stream.of(data).filter(e -> !e.equals(pokemonImage)).collect(Collectors.toList());
            sendEmbedImage(lobby.getChannel(), pokemonImage, eb -> eb
                    .setTitle("Who's that pokemon?", null)
                    .setFooter("You have 5 attempts and 75 seconds. (Type end to end the game)", null)
            ).queue();
            return true;
        } catch(Exception e) {
            lobby.getChannel().sendMessage(EmoteReference.ERROR + "Error while setting up a game.").queue();
            log.warn("Exception while setting up a game", e);
            return false;
        }
    }
}
