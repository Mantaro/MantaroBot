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

package net.kodehawa.mantarobot.commands.game;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.kodehawa.mantarobot.commands.game.core.Game;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.listeners.operations.ButtonOperations;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.ButtonOperation;
import net.kodehawa.mantarobot.core.listeners.operations.core.InteractiveOperation;
import net.kodehawa.mantarobot.utils.Snow64;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

public class Trivia extends Game<String> {
    private static final Logger log = LoggerFactory.getLogger("Game [Trivia]");
    private static final String OTDB_URL = "https://opentdb.com/api.php?amount=1&encode=base64";
    private static final SecureRandom random = new SecureRandom();
    private final TriviaDifficulty difficulty;
    private final List<Button> buttons = new ArrayList<>();
    private String answerRaw;
    private String answer;
    private boolean hardDiff = false;
    private Message message;

    public Trivia(TriviaDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    @Override
    public void call(GameLobby lobby, List<String> players) {
        ButtonOperations.create(message, 60, new ButtonOperation() {
            @Override
            public int click(ButtonInteractionEvent event) {
                return callDefaultButton(event, Long.parseLong(lobby.getPlayers().get(0)), lobby, answer, answerRaw, getAttempts(), 1, hardDiff ? 10 : 0);
            }

            @Override
            public void onExpire() {
                if (lobby.getChannel() == null) {
                    GameLobby.LOBBYS.remove(Long.parseLong(lobby.getChannelId()));
                    return;
                }

                lobby.getContext().edit("commands.game.lobby_timed_out", EmoteReference.ERROR, answerRaw);
                GameLobby.LOBBYS.remove(lobby.getChannel().getIdLong());
            }

            @Override
            public void onCancel() {
                GameLobby.LOBBYS.remove(lobby.getChannel().getIdLong());
            }
        }, buttons);
    }

    @Override
    public boolean onStart(GameLobby lobby) {
        final var languageContext = lobby.getLanguageContext();
        try {
            var json = Utils.httpRequest(OTDB_URL + (difficulty == null ? "" : "&difficulty=" + difficulty.name().toLowerCase()));
            if (json == null) {
                lobby.getContext().sendLocalized("commands.game.trivia.fetch_error", EmoteReference.ERROR);
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
            answerRaw = fromB64(question.getString("correct_answer")).trim();

            var q = fromB64(question.getString("question"));
            var category = fromB64(question.getString("category"));
            var diff = fromB64(question.getString("difficulty"));

            //value: raw, id: Snow64.toSnow64(raw.length() + System.currentTimeMillis() + random.nextInt(100))))
            Map<String, String> buttonIds = new HashMap<>();
            for (var a : answers) {
                buttonIds.put(a, Snow64.toSnow64(a.length() + System.currentTimeMillis() + random.nextInt(100)));
            }

            hardDiff = diff.equalsIgnoreCase("hard");
            answers.add(answerRaw);

            answer = Snow64.toSnow64(answerRaw.length() + System.currentTimeMillis() + random.nextInt(100));
            buttonIds.put(answerRaw, answer);
            Collections.shuffle(answers);

            var sb = new StringBuilder();
            for (var s : answers) {
                sb.append(EmoteReference.WHITE_CIRCLE).append(" ").append("**").append(s).append("**\n");
            }

            eb.setAuthor("Trivia Game", null, lobby.getContext().getAuthor().getAvatarUrl())
                    .setThumbnail("https://i.imgur.com/7TITtHb.png")
                    .setDescription("**" + q + "**")
                    .setColor(Color.PINK)
                    .addField(EmoteReference.PENCIL.toHeaderString() + languageContext.get("commands.game.trivia.possibilities"),
                            sb.toString(), false
                    )
                    .addField(EmoteReference.ZAP.toHeaderString() + languageContext.get("commands.game.trivia.difficulty"),
                            "`" + Utils.capitalize(diff) + "`", true
                    )
                    .addField(EmoteReference.CALENDAR2.toHeaderString() + languageContext.get("commands.game.trivia.category"),
                            "`" + category + "`", true
                    )
                    .setFooter(String.format(
                            languageContext.get("commands.game.trivia_end_footer"), 1),
                            lobby.getContext().getAuthor().getAvatarUrl()
                    );

            buttonIds.forEach((value, hash) -> {
                buttons.add(Button.primary(hash, value));
            });

            Collections.shuffle(buttons);
            buttons.add(Button.danger("end-game", languageContext.get("buttons.end")));
            message = lobby.getContext().sendResult(eb.build());

            lobby.setGameLoaded(true);
            return true;
        } catch (Exception e) {
            lobby.getContext().sendLocalized("commands.game.error", EmoteReference.ERROR);
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
