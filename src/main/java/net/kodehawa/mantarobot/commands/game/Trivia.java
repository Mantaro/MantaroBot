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
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;

@Slf4j(topic = "Game [Trivia]")
public class Trivia extends Game {
	private static final DataManager<List<String>> TRIVIA = new SimpleFileDataManager("assets/mantaro/texts/trivia.txt");
	private List<String> expectedAnswer;
	private int maxAttempts = 10;

	public Trivia() {
		super();
	}

	@Override
	public void call(GameLobby lobby, HashMap<Member, Player> players) {
		InteractiveOperations.create(lobby.getChannel(), "Game", (int) TimeUnit.MINUTES.toMillis(2), OptionalInt.empty(), new InteractiveOperation() {
				@Override
				public boolean run(GuildMessageReceivedEvent event) {
					return callDefault(event, lobby, players, expectedAnswer, getAttempts(), maxAttempts);
				}

				@Override
				public void onExpire() {
					lobby.getChannel().sendMessage(EmoteReference.ERROR + "The time ran out! Correct answer was " + expectedAnswer).queue();
					GameLobby.LOBBYS.remove(lobby.getChannel());
				}
			}
		);
	}

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
			String qu = Jsoup.parse(question.getString("question")).text();
			String category = question.getString("category");
			String diff = question.getString("difficulty");

			expectedAnswer.add(Jsoup.parse(question.getString("correct_answer")).text());

			eb.setAuthor("Trivia Game", null, lobby.getEvent().getAuthor().getAvatarUrl())
					.setThumbnail("https://cdn.pixabay.com/photo/2012/04/14/16/26/question-34499_960_720.png")
						.setDescription("**" + qu + "**")
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
}