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

import br.com.brjdevs.java.utils.collections.CollectionUtils;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.AnimeCmds;
import net.kodehawa.mantarobot.commands.anime.CharacterData;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.commands.game.core.ImageGame;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.InteractiveOperation;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@Slf4j(topic = "Game [Character]")
public class Character extends ImageGame {
    private static final DataManager<List<String>> NAMES = new SimpleFileDataManager("assets/mantaro/texts/animenames.txt");
    private final String authToken = AnimeCmds.authToken;
    @Getter
    private final int maxAttempts = 10;
    private String characterName;
    private List<String> characterNameL;

    public Character() {
        super(10);
    }

	@Override
	public void call(GameLobby lobby, List<String> players) {
		InteractiveOperations.createOverriding(lobby.getChannel(), 60, new InteractiveOperation() {
			@Override
			public int run(GuildMessageReceivedEvent e) {
				return callDefault(e, lobby, players, characterNameL, getAttempts(), maxAttempts, 0);
			}

            @Override
            public void onExpire() {
                lobby.getChannel().sendMessage(EmoteReference.ERROR + "The time ran out! Correct answer was " + String.join(" ,", characterNameL)).queue();
                GameLobby.LOBBYS.remove(lobby.getChannel());
            }

            @Override
            public void onCancel(){
                GameLobby.LOBBYS.remove(lobby.getChannel());
            }
        });
    }

    @Override
    public boolean onStart(GameLobby lobby) {
        try {
            characterNameL = new ArrayList<>();
            characterName = CollectionUtils.random(NAMES.get());
            String url = String.format("https://anilist.co/api/character/search/%1s?access_token=%2s", URLEncoder.encode(characterName, "UTF-8"),
                    authToken);
            String json = Utils.wget(url, null);
            CharacterData[] character = GsonDataManager.GSON_PRETTY.fromJson(json, CharacterData[].class);
            String imageUrl = character[0].getImage_url_med();
            //Allow for replying with only the first name.
            if(characterName.contains(" ") && !characterName.contains("Sailor")) {
                characterNameL.add(characterName.split(" ")[0]);
            }
            characterNameL.add(characterName);
            sendEmbedImage(lobby.getChannel(), imageUrl, eb -> eb
                    .setTitle("Guess the character", null)
                    .setFooter("You have 10 attempts and 60 seconds. (Type end to end the game)", null)
            ).queue();
            return true;
        } catch(Exception e) {
            if(e instanceof JsonSyntaxException) {
                lobby.getChannel().sendMessage(EmoteReference.WARNING + "Report this in the official server please. Failed to setup game for pre-saved character: " + characterName).queue();
                return false;
            }
            lobby.getChannel().sendMessage(EmoteReference.ERROR + "Error while setting up a game.").queue();
            log.warn("Exception while setting up a game", e);
            return false;
        }
    }
}
