package net.kodehawa.mantarobot.commands.game;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.AnimeCmds;
import net.kodehawa.mantarobot.commands.game.core.Game;
import net.kodehawa.mantarobot.commands.game.core.GameReference;
import net.kodehawa.mantarobot.data.entities.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageGuess extends Game {

	private static final Logger LOGGER = LoggerFactory.getLogger("Game[ImageGuess]");
	private int attempts = 1;
	private String authToken = AnimeCmds.authToken;
	private String characterName = null;
	private int maxAttempts = 10;
	private String[] search = {"Mato Kuroi", "Kotori Kanbe", "Kotarou Tennouji", "Akane Senri", "Misaki Mei", "Tomoe Mami"
		, "Shintaro Kisaragi", "Momo Kisaragi", "Takane Enomoto", "Ruuko Kominato", "Homura Akemi", "Madoka Kaname"};

	public ImageGuess(){
		super();
	}

	//TODO oh please.

	@Override
	public void call(GuildMessageReceivedEvent event, Player player) {
		/*if (event.getAuthor().isFake() || !(EntityPlayer.getPlayer(event.getAuthor().getId()).getId() == player.getId() &&
				player.getGame() == type()
			&& !event.getMessage().getContent().startsWith(MantaroData.getData().get().getPrefix(event.getGuild())))) {
			return;
		}

		if (attempts > maxAttempts) {
			event.getChannel().sendMessage(EmoteReference.SAD + "You used all of your attempts, game is ending.").queue();
			endGame(event, player, false);
			return;
		}

		if (event.getMessage().getContent().equalsIgnoreCase(characterName)) {
			onSuccess(player, event);
			return;
		}

		if (event.getMessage().getContent().equalsIgnoreCase("end")) {
			endGame(event, player, false);
			return;
		}

		event.getChannel().sendMessage(EmoteReference.SAD + "That wasn't it! "
			+ EmoteReference.STOPWATCH + "You have " + (maxAttempts - attempts) + " attempts remaning").queue();

		attempts++;*/
	}

	@Override
	public boolean onStart(GuildMessageReceivedEvent event, GameReference type, Player player) {
		/*player.setCurrentGame(type, event.getChannel());
		player.setGameInstance(this);
		TextChannelWorld.of(event.getChannel()).addGame(player, this);
		int random = new Random().nextInt(search.length);
		try {
			String url = String.format("https://anilist.co/api/character/search/%1s?access_token=%2s", URLEncoder.encode(search[random], "UTF-8"), authToken);
			String json = Utils.wget(url, event);
			CharacterData[] character = GsonDataManager.GSON_PRETTY.fromJson(json, CharacterData[].class);

			String imageUrl = character[0].getImage_url_med();
			characterName = character[0].getName_first();
			if (characterName.equals("Takane")) characterName = "Ene";

			event.getChannel().sendMessage(new EmbedBuilder().setTitle("Guess the character", event.getJDA().getSelfUser().getAvatarUrl())
											.setImage(imageUrl).setFooter("You have 60 seconds to answer. (Type end to end the game)", null).build()).queue();
			super.onStart(TextChannelWorld.of(event.getChannel()), event, player);
			return true;
		} catch (Exception e) {
			onError(LOGGER, event, player, e);
			return false;
		}*/
		return false;
	}

	@Override
	public GameReference type() {
		return GameReference.IMAGEGUESS;
	}

	public String getCharacterName() {
		return characterName;
	}
}