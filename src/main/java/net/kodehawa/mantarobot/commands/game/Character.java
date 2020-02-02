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

package net.kodehawa.mantarobot.commands.game;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonSyntaxException;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.anime.CharacterData;
import net.kodehawa.mantarobot.commands.anime.KitsuRetriever;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.commands.game.core.ImageGame;
import net.kodehawa.mantarobot.commands.info.stats.manager.GameStatsManager;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.InteractiveOperation;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Character extends ImageGame {
    private static final DataManager<List<String>> NAMES = new SimpleFileDataManager("assets/mantaro/texts/animenames.txt");
    private static final Logger log = org.slf4j.LoggerFactory.getLogger("Game [Character]");
    //Avoid AniList ratelimits, we don't need more than fetching the image either way and URL shouldn't change in a short amount of time.
    private static Cache<String, String> imgCache = CacheBuilder.newBuilder()
                                                            .maximumSize(50)
                                                            .build();
    
    private final int maxAttempts = 5;
    private String characterName;
    private List<String> characterNameL;
    private Random random = new Random();
    
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
                if(lobby.getChannel() == null)
                    return;
                
                lobby.getChannel().sendMessageFormat(lobby.getLanguageContext().get("commands.game.lobby_timed_out"), EmoteReference.ERROR, String.join(" ,", characterNameL)).queue();
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
            List<String> strings = NAMES.get();
            GameStatsManager.log(name());
            characterNameL = new ArrayList<>();
            characterName = strings.get(random.nextInt(strings.size()));
            
            String imageUrl = imgCache.getIfPresent(characterName);
            
            if(imageUrl == null) {
                List<CharacterData> characters = KitsuRetriever.searchCharacters(characterName);
                if(characters.isEmpty()) {
                    lobby.getChannel().sendMessageFormat(languageContext.get("commands.game.character_load_error"), EmoteReference.WARNING, characterName).queue();
                    return false;
                }
                
                CharacterData character = characters.get(0);
                
                imageUrl = character.getAttributes().getImage().getOriginal();
                //insert into cache
                if(imageUrl != null)
                    imgCache.put(characterName, imageUrl);
            }
            
            //Allow for replying with only the first name of the character.
            if(characterName.contains(" ") && !characterName.contains("Sailor")) {
                characterNameL.add(characterName.split(" ")[0]);
            }
            
            characterNameL.add(characterName);
            sendEmbedImage(lobby.getChannel(), imageUrl, eb -> eb
                                                                       .setTitle(languageContext.get("commands.game.character_start"), null)
                                                                       .setFooter(languageContext.get("commands.game.end_footer"), null)
            ).queue(success -> lobby.setGameLoaded(true));
            return true;
        } catch(JsonSyntaxException ex) {
            lobby.getChannel().sendMessageFormat(languageContext.get("commands.game.character_load_error"), EmoteReference.WARNING, characterName).queue();
            return false;
        } catch(Exception e) {
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
        return this.maxAttempts;
    }
}
