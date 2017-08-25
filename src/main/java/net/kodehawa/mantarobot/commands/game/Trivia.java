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
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.game.core.Game;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.InteractiveOperation;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j(topic = "Game [Trivia]")
public class Trivia extends Game<String> {
	private String expectedAnswer;
	private final int maxAttempts = 2;
	private boolean hardDiff = false;
	private boolean isBool;

	@Override
	public boolean onStart(GameLobby lobby) {
		try {
			String json = Utils.wget("https://opentdb.com/api.php?amount=1&encode=base64", null);

			if(json == null) {
				lobby.getChannel().sendMessage(EmoteReference.ERROR + "Error while starting trivia. Seemingly Open Trivia DB is having trouble.").queue();
				return false;
			}

			EmbedBuilder eb = new EmbedBuilder();
			JSONObject ob = new JSONObject(json);


			JSONObject question = ob.getJSONArray("results").getJSONObject(0);

			List<Object> incorrectAnswers = question.getJSONArray("incorrect_answers").toList();
			List<String> l = new ArrayList<>();
			for(Object o : incorrectAnswers) {
					l.add("**" + fromB64(String.valueOf(o)) + "**\n");
			}

			String qu = fromB64(question.getString("question"));
			String category = fromB64(question.getString("category"));
			String diff = fromB64(question.getString("difficulty"));
			if(diff.equalsIgnoreCase("hard")) hardDiff = true;
			if(fromB64(question.getString("type")).equalsIgnoreCase("boolean")) isBool = true;

			//Why was this returning an extra space at the end? otdb pls?
			expectedAnswer = fromB64(question.getString("correct_answer")).trim();

			l.add("**" + expectedAnswer + "**\n");
			Collections.shuffle(l);
			StringBuilder sb = new StringBuilder();
			for(String s : l) sb.append(s);

			eb.setAuthor("Trivia Game", null, lobby.getEvent().getAuthor().getAvatarUrl())
					.setThumbnail("https://cdn.pixabay.com/photo/2012/04/14/16/26/question-34499_960_720.png")
					.setDescription("**" + qu + "**")
					.addField("Possibilities", sb.toString(), false)
					.addField("Difficulty", "`" + Utils.capitalize(diff) + "`", true)
					.addField("Category", "`" + category + "`", true)
					.setFooter("This times out in 2 minutes.", lobby.getEvent().getAuthor().getAvatarUrl());

			lobby.getChannel().sendMessage(eb.build()).queue();

			return true;
		} catch (Exception e) {
			lobby.getChannel().sendMessage(EmoteReference.ERROR + "Error while starting trivia.").queue();
			log.warn("Error while starting a trivia game", e);
			return false;
		}
	}

	@Override
	public void call(GameLobby lobby, HashMap<Member, Player> players) {
		InteractiveOperations.createOverriding(lobby.getChannel(), 120, new InteractiveOperation() {
				@Override
				public int run(GuildMessageReceivedEvent event) {
					return callDefault(event, lobby, players, expectedAnswer, getAttempts(), isBool ? 1 : maxAttempts, hardDiff ? 10 : 0);
				}

				@Override
				public void onExpire() {
					lobby.getChannel().sendMessage(EmoteReference.ERROR + "The time ran out! The answer was: " + expectedAnswer).queue();
					GameLobby.LOBBYS.remove(lobby.getChannel());
				}
			});
	}

	private String fromB64(String b64) {
		return new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
	}
}
