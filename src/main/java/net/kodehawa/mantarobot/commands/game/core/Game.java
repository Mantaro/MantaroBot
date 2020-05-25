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

package net.kodehawa.mantarobot.commands.game.core;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.commands.currency.seasons.helpers.SeasonalPlayerData;
import net.kodehawa.mantarobot.commands.currency.seasons.helpers.UnifiedPlayer;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.List;
import java.util.stream.Collectors;

public abstract class Game<T> {
    protected Config config = MantaroData.config().get();
    private int attempts = 1;

    public abstract void call(GameLobby lobby, List<String> players);

    public abstract boolean onStart(GameLobby lobby);

    public abstract String name();

    protected int callDefault(GuildMessageReceivedEvent e, GameLobby lobby, List<String> players, List<T> expectedAnswer, int attempts, int maxAttempts, int extra) {
        TextChannel channel = lobby.getChannel();
        if (!e.getChannel().getId().equals(channel.getId())) {
            return Operation.IGNORED;
        }

        if (!lobby.isGameLoaded()) {
            return Operation.IGNORED;
        }

        Message message = e.getMessage();
        String contentRaw = message.getContentRaw();
        I18nContext languageContext = lobby.getLanguageContext();

        for (String s : MantaroData.config().get().getPrefix()) {
            if (contentRaw.startsWith(s)) {
                return Operation.IGNORED;
            }
        }

        if (players.contains(e.getAuthor().getId())) {
            if (contentRaw.equalsIgnoreCase("end")) {
                channel.sendMessageFormat(languageContext.get("commands.game.lobby.ended_game"),
                        EmoteReference.CORRECT, expectedAnswer.stream().map(String::valueOf).collect(Collectors.joining(", "))
                ).queue();

                lobby.startNextGame(true);
                return Operation.COMPLETED;
            }

            if (contentRaw.equalsIgnoreCase("endlobby")) {
                channel.sendMessageFormat(languageContext.get("commands.game.lobby.ended_lobby"), EmoteReference.CORRECT).queue();
                lobby.getGamesToPlay().clear();
                lobby.startNextGame(true);
                return Operation.COMPLETED;
            }

            if (expectedAnswer.stream().map(String::valueOf).anyMatch(contentRaw::equalsIgnoreCase)) {
                UnifiedPlayer unifiedPlayer = UnifiedPlayer.of(e.getAuthor(), config.getCurrentSeason());
                Player player = unifiedPlayer.getPlayer();
                PlayerData data = player.getData();
                SeasonPlayer seasonalPlayer = unifiedPlayer.getSeasonalPlayer();
                SeasonalPlayerData seasonalPlayerData = seasonalPlayer.getData();

                int gains = 45 + extra;
                unifiedPlayer.addMoney(gains);

                if (data.getGamesWon() == 100)
                    data.addBadgeIfAbsent(Badge.GAMER);

                if (data.getGamesWon() == 1000)
                    data.addBadgeIfAbsent(Badge.ADDICTED_GAMER);

                seasonalPlayerData.setGamesWon(seasonalPlayerData.getGamesWon() + 1);
                data.setGamesWon(data.getGamesWon() + 1);
                unifiedPlayer.save();

                TextChannelGround.of(e).dropItemWithChance(Items.FLOPPY_DISK, 3);
                new MessageBuilder().setContent(String.format(languageContext.get("commands.game.lobby.won_game"),
                        EmoteReference.MEGA, e.getMember().getEffectiveName(), gains))
                        .stripMentions(e.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE)
                        .sendTo(channel)
                        .queue();

                lobby.startNextGame(true);
                return Operation.COMPLETED;
            }

            if (attempts >= maxAttempts) {
                channel.sendMessageFormat(languageContext.get("commands.game.lobby.all_attempts_used"),
                        EmoteReference.ERROR, expectedAnswer.stream().map(String::valueOf).collect(Collectors.joining(", "))
                ).queue();

                lobby.startNextGame(true); //This should take care of removing the lobby, actually.
                return Operation.COMPLETED;
            }

            channel.sendMessageFormat(languageContext.get("commands.game.lobby.incorrect_answer"),
                    EmoteReference.ERROR, (maxAttempts - attempts)
            ).queue();

            setAttempts(getAttempts() + 1);
            return Operation.IGNORED;
        }

        return Operation.IGNORED;
    }

    public int getAttempts() {
        return this.attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }
}
