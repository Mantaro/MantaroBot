/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.game.core;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.game.core.lobby.GameLobby;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.List;
import java.util.stream.Collectors;

public abstract class Game<T> {
    private static final ManagedDatabase managedDatabase = MantaroData.db();
    protected final Config config = MantaroData.config().get();
    private int attempts = 1;

    public abstract void call(GameLobby lobby, List<String> players);

    public abstract boolean onStart(GameLobby lobby);

    public abstract String name();

    protected int callDefaultButton(ButtonInteractionEvent event, long userId, GameLobby lobby, String expectedAnswer, String expectedAnswerRaw, int attempts, int maxAttempts, int extra) {
        if (!event.isFromGuild()) {
            return Operation.IGNORED;
        }

        if (!lobby.isGameLoaded()) {
            return Operation.IGNORED;
        }

        if (event.getUser().getIdLong() != userId) {
            return Operation.IGNORED;
        }

        var button = event.getButton();
        if (button.getId() == null) {
            return Operation.IGNORED;
        }

        var languageContext = lobby.getLanguageContext();
        if (button.getId().equals("end-game")) {
            event.getHook().editOriginal("").setComponents().queue();
            event.getHook().sendMessage(
                    languageContext.get("commands.game.lobby.ended_game").formatted(EmoteReference.CORRECT, expectedAnswerRaw)
            ).queue();

            lobby.startNextGame(true);
            return Operation.COMPLETED;
        }

        if (button.getId().equals(expectedAnswer)) {
            var player = managedDatabase.getPlayer(event.getUser());
            var gains = 70 + extra;
            player.addMoney(gains);
            if (player.getGamesWon() == 100) {
                player.addBadgeIfAbsent(Badge.GAMER);
            }

            if (player.getGamesWon() == 1000) {
                player.addBadgeIfAbsent(Badge.ADDICTED_GAMER);
            }

            player.gamesWon(player.getGamesWon() + 1);
            player.updateAllChanged();

            TextChannelGround.of(event.getChannel()).dropItemWithChance(ItemReference.FLOPPY_DISK, 3);
            // Remove components from original message.
            event.getHook().editOriginal("").setComponents().queue();
            event.getHook().sendMessage(
                    languageContext.get("commands.game.lobby.won_game").formatted(EmoteReference.MEGA, event.getUser().getName(), gains)
            ).queue();

            lobby.startNextGame(true);
            return Operation.COMPLETED;
        }

        if (attempts >= maxAttempts) {
            event.getHook().editOriginal("").setComponents().queue();
            event.getHook().sendMessage(
                    languageContext.get("commands.game.lobby.all_attempts_used").formatted(EmoteReference.ERROR, expectedAnswerRaw)
            ).queue();

            lobby.startNextGame(true); // This should take care of removing the lobby, actually.
            return Operation.COMPLETED;
        }

        event.getHook().sendMessage(languageContext.get("commands.game.lobby.incorrect_answer").formatted(
                EmoteReference.ERROR, (maxAttempts - attempts)
        )).setEphemeral(true).queue();

        setAttempts(getAttempts() + 1);
        return Operation.IGNORED;
    }

    protected int callDefault(MessageReceivedEvent e, GameLobby lobby, List<String> players, List<T> expectedAnswer,
                              int attempts, int maxAttempts, int extra) {
        if (!e.isFromGuild()) {
            return Operation.IGNORED;
        }

        var channel = lobby.getChannel();
        if (!e.getChannel().getId().equals(channel.getId())) {
            return Operation.IGNORED;
        }

        if (!lobby.isGameLoaded()) {
            return Operation.IGNORED;
        }

        var ctx = lobby.getContext();
        if (players.contains(e.getAuthor().getId())) {
            var message = e.getMessage();
            var contentRaw = message.getContentRaw();
            for (var s : MantaroData.config().get().getPrefix()) {
                if (contentRaw.startsWith(s)) {
                    return Operation.IGNORED;
                }
            }

            if (contentRaw.equalsIgnoreCase("end")) {
                ctx.reply("commands.game.lobby.ended_game",
                        EmoteReference.CORRECT, expectedAnswer.stream().map(String::valueOf).collect(Collectors.joining(", "))
                );

                lobby.startNextGame(true);
                return Operation.COMPLETED;
            }

            if (contentRaw.equalsIgnoreCase("endlobby")) {
                ctx.reply("commands.game.lobby.ended_lobby", EmoteReference.CORRECT);
                lobby.getGamesToPlay().clear();
                lobby.startNextGame(true);
                return Operation.COMPLETED;
            }

            // iOS quotes keep screwing up stuff ;w;
            contentRaw = contentRaw
                    .replaceAll("’", "'")
                    .replaceAll("‘", "'")
                    .trim();

            if (expectedAnswer.stream().map(String::valueOf).anyMatch(contentRaw::equalsIgnoreCase)) {
                var player = managedDatabase.getPlayer(e.getAuthor());
                var gains = 70 + extra;
                player.addMoney(gains);

                if (player.getGamesWon() == 100) {
                    player.addBadgeIfAbsent(Badge.GAMER);
                }

                if (player.getGamesWon() == 1000) {
                    player.addBadgeIfAbsent(Badge.ADDICTED_GAMER);
                }

                player.gamesWon(player.getGamesWon() + 1);
                player.updateAllChanged();

                TextChannelGround.of(e.getChannel()).dropItemWithChance(ItemReference.FLOPPY_DISK, 3);
                ctx.reply("commands.game.lobby.won_game", EmoteReference.MEGA, e.getMember().getEffectiveName(), gains);

                lobby.startNextGame(true);
                return Operation.COMPLETED;
            }

            if (attempts >= maxAttempts) {
                ctx.reply("commands.game.lobby.all_attempts_used",
                        EmoteReference.ERROR, expectedAnswer.stream().map(String::valueOf).collect(Collectors.joining(", "))
                );

                lobby.startNextGame(true); // This should take care of removing the lobby, actually.
                return Operation.COMPLETED;
            }

            ctx.reply("commands.game.lobby.incorrect_answer", EmoteReference.ERROR, (maxAttempts - attempts));
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
