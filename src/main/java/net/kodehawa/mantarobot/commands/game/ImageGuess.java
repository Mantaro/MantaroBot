package net.kodehawa.mantarobot.commands.game;

import br.com.brjdevs.java.utils.extensions.CollectionUtils;
import lombok.Getter;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.kodehawa.mantarobot.commands.AnimeCmds;
import net.kodehawa.mantarobot.commands.game.core.Game;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.commands.utils.data.CharacterData;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;

public class ImageGuess extends Game {

	private static final DataManager<List<String>> NAMES = new SimpleFileDataManager("assets/mantaro/texts/animenames.txt");
	private static final Logger LOGGER = LoggerFactory.getLogger("Game[ImageGuess]");
	private String authToken = AnimeCmds.authToken;
	private String characterName = null;
	@Getter
	private int maxAttempts = 10;

	public ImageGuess() {
		super();
	}

	@Override
	public boolean onStart(GameLobby lobby) {
		try{
			characterName = CollectionUtils.random(NAMES.get());
			String url = String.format("https://anilist.co/api/character/search/%1s?access_token=%2s", URLEncoder.encode(characterName, "UTF-8"),
					authToken);
			String json = Utils.wget(url, null);
			CharacterData[] character = GsonDataManager.GSON_PRETTY.fromJson(json, CharacterData[].class);
			System.out.println(characterName);
			String imageUrl = character[0].getImage_url_med();
			lobby.getChannel().sendMessage(new EmbedBuilder().setTitle("Guess the character", null)
					.setImage(imageUrl).setFooter("You have 10 attempts and 60 seconds. (Type end to end the game)", null).build()).queue();
			return true;
		} catch (Exception e){
			lobby.getChannel().sendMessage(EmoteReference.ERROR + "Error while setting up a game.").queue();
			LOGGER.warn("Exception while setting up a game", e);
			return false;
		}
	}

	@Override
	public void call(GameLobby lobby, HashMap<Member, Player> players) {
		InteractiveOperations.create(lobby.getChannel(), "Game", (int) TimeUnit.MINUTES.toMillis(2), OptionalInt.empty(), (e) ->
			callDefault(e, lobby, players, characterName, getAttempts(), maxAttempts)
		);
	}
}