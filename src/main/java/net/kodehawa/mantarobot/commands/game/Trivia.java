package net.kodehawa.mantarobot.commands.game;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.game.core.Game;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperation;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Slf4j(topic = "Game [Trivia]")
public class Trivia extends Game {
	private List<String> expectedAnswer;
	private int maxAttempts = 2;
	private boolean hardDiff = false;

	@Override
	public boolean onStart(GameLobby lobby) {
		try {
			String json = Utils.wget("https://opentdb.com/api.php?amount=1", null);

			if(json == null){
				lobby.getChannel().sendMessage(EmoteReference.ERROR + "Error while starting trivia. Seemingly Open Trivia DB is having trouble.").queue();
				return false;
			}

			EmbedBuilder eb = new EmbedBuilder();
			expectedAnswer = new ArrayList<>();
			JSONObject ob = new JSONObject(json);


			JSONObject question = ob.getJSONArray("results").getJSONObject(0);

			List<Object> incorrectAnswers = question.getJSONArray("incorrect_answers").toList();
			List<String> l = new ArrayList<>();
			for(Object o : incorrectAnswers) l.add("**" + String.valueOf(o) + "**\n");

			String qu = Jsoup.parse(question.getString("question")).text();
			String category = question.getString("category");
			String diff = question.getString("difficulty");
			if(diff.equalsIgnoreCase("hard")) hardDiff = true;

			expectedAnswer.add(Jsoup.parse(question.getString("correct_answer")).text());

			l.add("**" + expectedAnswer.stream().collect(Collectors.joining("\n")) + "**\n");
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
		InteractiveOperations.create(lobby.getChannel(), 120, new InteractiveOperation() {
				@Override
				public int run(GuildMessageReceivedEvent event) {
					return callDefault(event, lobby, players, expectedAnswer, getAttempts(), maxAttempts, hardDiff ? 10 : 0);
				}

				@Override
				public void onExpire() {
					lobby.getChannel().sendMessage(EmoteReference.ERROR + "The time ran out! Possible answers were: " + expectedAnswer.stream().collect(Collectors.joining(" ,"))).queue();
					GameLobby.LOBBYS.remove(lobby.getChannel());
				}
			});
	}
}