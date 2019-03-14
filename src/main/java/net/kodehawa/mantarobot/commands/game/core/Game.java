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

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.commands.currency.seasons.helpers.UnifiedPlayer;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.List;
import java.util.stream.Collectors;

public abstract class Game<T> {
    @Setter
    @Getter
    private int attempts = 1;

    protected Config config = MantaroData.config().get();

    public abstract void call(GameLobby lobby, List<String> players);

    public abstract boolean onStart(GameLobby lobby);

    public abstract String name();

    protected int callDefault(GuildMessageReceivedEvent e,
                              GameLobby lobby, List<String> players, List<T> expectedAnswer, int attempts, int maxAttempts, int extra) {
        if(!e.getChannel().getId().equals(lobby.getChannel().getId())) {
            return Operation.IGNORED;
        }

        if(!lobby.isGameLoaded()) {
            return Operation.IGNORED;
        }

        for(String s : MantaroData.config().get().getPrefix()) {
            if(e.getMessage().getContentRaw().startsWith(s)) {
                return Operation.IGNORED;
            }
        }

        if(MantaroData.db().getGuild(lobby.getChannel().getGuild()).getData().getGuildCustomPrefix() != null &&
                e.getMessage().getContentRaw().startsWith(MantaroData.db().getGuild(lobby.getChannel().getGuild()).getData().getGuildCustomPrefix())) {
            return Operation.IGNORED;
        }

        if(players.contains(e.getAuthor().getId())) {
            if(e.getMessage().getContentRaw().equalsIgnoreCase("end")) {
                lobby.getChannel().sendMessageFormat(lobby.getLanguageContext().get("commands.game.lobby.ended_game"), EmoteReference.CORRECT, expectedAnswer.stream().map(String::valueOf).collect(Collectors.joining(", "))).queue();
                lobby.startNextGame(true);
                return Operation.COMPLETED;
            }

            if(e.getMessage().getContentRaw().equalsIgnoreCase("endlobby")) {
                lobby.getChannel().sendMessageFormat(lobby.getLanguageContext().get("commands.game.lobby.ended_lobby"),EmoteReference.CORRECT).queue();
                lobby.getGamesToPlay().clear();
                lobby.startNextGame(true);
                return Operation.COMPLETED;
            }

            if(expectedAnswer.stream().map(String::valueOf).anyMatch(e.getMessage().getContentRaw()::equalsIgnoreCase)) {
                UnifiedPlayer unifiedPlayer = UnifiedPlayer.of(e.getAuthor(), config.getCurrentSeason());
                Player player = unifiedPlayer.getPlayer();
                SeasonPlayer seasonalPlayer = unifiedPlayer.getSeasonalPlayer();
                int gains = 45 + extra;
                unifiedPlayer.addMoney(gains);

                if(player.getData().getGamesWon() == 100)
                    player.getData().addBadgeIfAbsent(Badge.GAMER);

                if(player.getData().getGamesWon() == 1000)
                    player.getData().addBadgeIfAbsent(Badge.ADDICTED_GAMER);

                if(maxAttempts > 2)
                    seasonalPlayer.getData().setGamesWon(seasonalPlayer.getData().getGamesWon() + 1);

                player.getData().setGamesWon(player.getData().getGamesWon() + 1);
                unifiedPlayer.save();

                TextChannelGround.of(e).dropItemWithChance(Items.FLOPPY_DISK, 3);
                new MessageBuilder().setContent(String.format(lobby.getLanguageContext().get("commands.game.lobby.won_game"), EmoteReference.MEGA, e.getMember().getEffectiveName(), gains))
                        .stripMentions(e.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE)
                        .sendTo(lobby.getChannel())
                        .queue();

                lobby.startNextGame(true);
                return Operation.COMPLETED;
            }

            if(attempts >= maxAttempts) {
                lobby.getChannel().sendMessageFormat(lobby.getLanguageContext().get("commands.game.lobby.all_attempts_used"), EmoteReference.ERROR, expectedAnswer.stream().map(String::valueOf).collect(Collectors.joining(", "))).queue();
                lobby.startNextGame(true); //This should take care of removing the lobby, actually.
                return Operation.COMPLETED;
            }

            lobby.getChannel().sendMessageFormat(lobby.getLanguageContext().get("commands.game.lobby.incorrect_answer"), EmoteReference.ERROR, (maxAttempts - attempts)).queue();
            setAttempts(getAttempts() + 1);
            return Operation.IGNORED;
        }

        return Operation.IGNORED;
    }
}
