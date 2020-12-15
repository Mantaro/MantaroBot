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

import com.fasterxml.jackson.core.JsonProcessingException;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.kodehawa.mantarobot.commands.game.core.AnimeGameData;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.commands.game.core.ImageGame;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.InteractiveOperation;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.utils.APIUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.JsonDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Character extends ImageGame {
    private static final Logger log = LoggerFactory.getLogger("Game [Character]");
    private static final int maxAttempts = 5;
    private String characterName;
    private List<String> characterNameL;

    public Character() {
        super(10);
    }

    @Override
    public void call(GameLobby lobby, List<String> players) {
        InteractiveOperations.create(lobby.getChannel(), Long.parseLong(lobby.getPlayers().get(0)), 60, new InteractiveOperation() {
            @Override
            public int run(GuildMessageReceivedEvent e) {
                return callDefault(e, lobby, players, characterNameL, getAttempts(), maxAttempts, 0);
            }

            @Override
            public void onExpire() {
                if (lobby.getChannel() == null)
                    return;

                lobby.getChannel().sendMessageFormat(
                        lobby.getLanguageContext().get("commands.game.lobby_timed_out"),
                        EmoteReference.ERROR, String.join(" ,", characterNameL)
                ).queue();

                GameLobby.LOBBYS.remove(lobby.getChannel().getIdLong());
            }

            @Override
            public void onCancel() {
                GameLobby.LOBBYS.remove(lobby.getChannel().getIdLong());
            }
        });
    }

    @Override
    public boolean onStart(GameLobby lobby) {
        final I18nContext languageContext = lobby.getLanguageContext();
        try {
            var data = JsonDataManager.fromJson(APIUtils.getFrom("/mantaroapi/bot/character"), AnimeGameData.class);
            characterNameL = new ArrayList<>();
            characterName = data.getName();
            var imageUrl = data.getImage();

            //Allow for replying with only the first name of the character.
            if (characterName.contains(" ")) {
                characterNameL.add(characterName.split(" ")[0]);
            }

            characterNameL.add(characterName);
            sendEmbedImage(lobby.getChannel(), imageUrl, eb -> eb
                    .setTitle(languageContext.get("commands.game.character_start"), null)
                    .setFooter(languageContext.get("commands.game.end_footer"), null)
            ).queue(success -> lobby.setGameLoaded(true));
            return true;
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
            lobby.getChannel().sendMessageFormat(
                    languageContext.get("commands.game.character_load_error"), EmoteReference.WARNING, characterName
            ).queue();

            return false;
        } catch (InsufficientPermissionException ex) {
            lobby.getChannel().sendMessageFormat(languageContext.get("commands.game.error_missing_permissions"), EmoteReference.ERROR).queue();
            return false;
        } catch (Exception e) {
            lobby.getChannel().sendMessageFormat(languageContext.get("commands.game.error"), EmoteReference.ERROR).queue();
            log.warn("Exception while setting up a game", e);
            return false;
        }
    }

    @Override
    public String name() {
        return "character";
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }
}
