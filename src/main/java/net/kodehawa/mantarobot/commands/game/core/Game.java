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

package net.kodehawa.mantarobot.commands.game.core;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Game<T> {
    @Setter
    @Getter
    private int attempts = 1;

	public abstract void call(GameLobby lobby, List<String> players);

	public abstract boolean onStart(GameLobby lobby);

	protected int callDefault(GuildMessageReceivedEvent e,
							  GameLobby lobby, List<String> players, List<T> expectedAnswer, int attempts, int maxAttempts, int extra) {
		if (!e.getChannel().getId().equals(lobby.getChannel().getId())) {
			return Operation.IGNORED;
		}

        for(String s : MantaroData.config().get().getPrefix()) {
            if(e.getMessage().getContent().startsWith(s)) {
                return Operation.IGNORED;
            }
        }

        if(MantaroData.db().getGuild(lobby.getChannel().getGuild()).getData().getGuildCustomPrefix() != null &&
                e.getMessage().getContent().startsWith(MantaroData.db().getGuild(lobby.getChannel().getGuild()).getData().getGuildCustomPrefix())) {
            return Operation.IGNORED;
        }

		if (players.contains(e.getAuthor().getId())) {
			if (e.getMessage().getContent().equalsIgnoreCase("end")) {
				lobby.getChannel().sendMessage(EmoteReference.CORRECT + "Ended game. Possible answers were: " + expectedAnswer.stream()
						.map(String::valueOf).collect(Collectors.joining(", "))).queue();
				lobby.startNextGame();
				return Operation.COMPLETED;
			}

            if(expectedAnswer.stream().map(String::valueOf).anyMatch(e.getMessage().getRawContent()::equalsIgnoreCase)) {
                Player player = MantaroData.db().getPlayer(e.getMember());
                int gains = 45 + extra;
                player.addMoney(gains);

                if(player.getData().getGamesWon() == 100)
                    player.getData().addBadge(Badge.GAMER);

                player.getData().setGamesWon(player.getData().getGamesWon() + 1);
                player.save();

                TextChannelGround.of(e).dropItemWithChance(Items.FLOPPY_DISK, 3);
                lobby.getChannel().sendMessage(EmoteReference.MEGA + "**" + e.getMember().getEffectiveName() + "**" + " just won $" + gains + " credits by answering correctly!").queue();
                lobby.startNextGame();
                return Operation.COMPLETED;
            }

            if(attempts >= maxAttempts) {
                lobby.getChannel().sendMessage(EmoteReference.ERROR + "Already used all attempts, ending game. Possible answers were: " + expectedAnswer.stream()
                        .map(String::valueOf).collect(Collectors.joining(" ,"))).queue();
                lobby.startNextGame(); //This should take care of removing the lobby, actually.
                return Operation.COMPLETED;
            }

            lobby.getChannel().sendMessage(EmoteReference.ERROR + "That's not it, you have " + (maxAttempts - attempts) + " attempts remaning.").queue();
            setAttempts(getAttempts() + 1);
            return Operation.IGNORED;
        }

        return Operation.IGNORED;
    }

	//ONLY FOR USE WHEN IT'S Game<String>!
	//Mostly only for trivia tho.
	@SuppressWarnings("unchecked")
	protected int callDefault(GuildMessageReceivedEvent e,
							  GameLobby lobby, List<String> players, String expectedAnswer, int attempts, int maxAttempts, int extra) {
		return callDefault(e, lobby, players, (List<T>) Collections.singletonList(expectedAnswer), attempts, maxAttempts, extra);
	}
}
