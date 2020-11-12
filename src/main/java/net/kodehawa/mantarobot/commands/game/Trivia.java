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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.game.core.Game;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.InteractiveOperation;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Trivia extends Game<String> {
    private static final Logger log = LoggerFactory.getLogger("Game [Trivia]");
    private static final String OTDB_URL = "https://opentdb.com/api.php?amount=1&encode=base64";
    private static final int maxAttempts = 2;
    private final TriviaDifficulty difficulty;
    private final List<String> expectedAnswer = new ArrayList<>();
    private boolean hardDiff = false;
    private boolean isBool;

    public Trivia(TriviaDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    @Override
    public void call(GameLobby lobby, List<String> players) {
        InteractiveOperations.create(lobby.getChannel(), Long.parseLong(lobby.getPlayers().get(0)), 60, new InteractiveOperation() {
            @Override
            public int run(GuildMessageReceivedEvent event) {
                return callDefault(event, lobby, players, expectedAnswer, getAttempts(), isBool ? 1 : maxAttempts, hardDiff ? 10 : 0);
            }

            @Override
            public void onExpire() {
                if (lobby.getChannel() == null) {
                    GameLobby.LOBBYS.remove(Long.parseLong(lobby.getChannelId()));
                    return;
                }

                lobby.getChannel().sendMessageFormat(
                        lobby.getLanguageContext().get("commands.game.lobby_timed_out"),
                        EmoteReference.ERROR, expectedAnswer.get(0)
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
        final var languageContext = lobby.getLanguageContext();
        try {
            var json = Utils.httpRequest(OTDB_URL + (difficulty == null ? "" : "&difficulty=" + difficulty.name().toLowerCase()));

            if (json == null) {
                lobby.getChannel().sendMessageFormat(languageContext.get("commands.game.trivia.fetch_error"), EmoteReference.ERROR).queue();
                return false;
            }

            var eb = new EmbedBuilder();
            var ob = new JSONObject(json);

            var question = ob.getJSONArray("results").getJSONObject(0);

            var answers = question
                    .getJSONArray("incorrect_answers")
                    .toList()
                    .stream()
                    .map(v -> fromB64(String.valueOf(v)))
                    .collect(Collectors.toList());

            var qu = fromB64(question.getString("question"));
            var category = fromB64(question.getString("category"));
            var diff = fromB64(question.getString("difficulty"));

            hardDiff = diff.equalsIgnoreCase("hard");
            isBool = fromB64(question.getString("type")).equalsIgnoreCase("boolean");

            expectedAnswer.add(fromB64(question.getString("correct_answer")).trim());

            answers.add(expectedAnswer.get(0));
            Collections.shuffle(answers);

            var sb = new StringBuilder();
            var i = 1;
            for (var s : answers) {
                if (s.equals(expectedAnswer.get(0))) {
                    expectedAnswer.add(String.valueOf(i));
                }

                sb.append("*").append(i).append(".-* ").append("**").append(s).append("**\n");
                i++;
            }

            eb.setAuthor("Trivia Game", null, lobby.getEvent().getAuthor().getAvatarUrl())
                    .setThumbnail("https://i.imgur.com/7TITtHb.png")
                    .setDescription("**" + qu + "**")
                    .setColor(Color.PINK)
                    .addField(languageContext.get("commands.game.trivia.possibilities"),
                            sb.toString(), false
                    )
                    .addField(languageContext.get("commands.game.trivia.difficulty"),
                            "`" + Utils.capitalize(diff) + "`", true
                    )
                    .addField(languageContext.get("commands.game.trivia.category"),
                            "`" + category + "`", true
                    )
                    .setFooter(String.format(
                            languageContext.get("commands.game.trivia_end_footer"), isBool ? 1 : 2),
                            lobby.getEvent().getAuthor().getAvatarUrl()
                    );

            lobby.getChannel().sendMessage(eb.build()).queue(success -> lobby.setGameLoaded(true));

            return true;
        } catch (Exception e) {
            lobby.getChannel().sendMessageFormat(languageContext.get("commands.game.error"), EmoteReference.ERROR).queue();
            log.warn("Error while starting a trivia game", e);
            return false;
        }
    }

    @Override
    public String name() {
        return "trivia";
    }

    private String fromB64(String b64) {
        return new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
    }
}
