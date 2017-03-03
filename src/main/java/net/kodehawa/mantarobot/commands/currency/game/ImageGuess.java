package net.kodehawa.mantarobot.commands.currency.game;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.AnimeCmds;
import net.kodehawa.mantarobot.commands.currency.entity.Entity;
import net.kodehawa.mantarobot.commands.currency.entity.player.EntityPlayer;
import net.kodehawa.mantarobot.commands.currency.inventory.TextChannelGround;
import net.kodehawa.mantarobot.commands.utils.data.CharacterData;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.util.Random;

import static net.kodehawa.mantarobot.utils.Utils.toByteArray;

public class ImageGuess extends Game {

	private int maxAttempts = 10;
	private int attempts = 0;
	private String characterName = null;
	private String[] search = {"Mato Kuroi", "Kotori Kanbe", "Kotarou Tennouji", "Akane Senri", "Misaki Mei", "Tomoe Mami"
						, "Shintaro Kisaragi", "Momo Kisaragi", "Takane Enomoto", "Ruuko Kominato", "Homura Akemi", "Madoka Kaname"};
	private String authToken = AnimeCmds.authToken;
	private static final Logger LOGGER = LoggerFactory.getLogger("Game[ImageGuess]");

	@Override
	public boolean onStart(GuildMessageReceivedEvent event, GameReference type, EntityPlayer player){
		player.setCurrentGame(type, event.getChannel());
		TextChannelGround.of(event.getChannel()).addEntity(player, type);
		int random = new Random().nextInt(search.length);
		try{
			String url = String.format("https://anilist.co/api/character/search/%1s?access_token=%2s", URLEncoder.encode(search[random], "UTF-8"), authToken);
			String json = Utils.wget(url, event);
			CharacterData[] character = GsonDataManager.GSON_PRETTY.fromJson(json, CharacterData[].class);

			String imageUrl = character[0].getImage_url_med();
			characterName = character[0].getName_first();
			if(characterName.equals("Takane")) characterName = "Ene";

			event.getChannel().sendMessage(EmoteReference.STOPWATCH + "Guess the character! You have 60 seconds and 10 attempts. Type end to end the game.").queue();

			byte[] image = toByteArray(imageUrl);

			if(image == null){
				event.getChannel().sendMessage(EmoteReference.SAD + "There was an error while converting the image to bytes, game needs to end.").queue();
				endGame(event, player, false);
				return false;
			}

			event.getChannel().sendFile(image, "image.png", null).queue();

			return true;
		} catch (Exception e){
			event.getChannel().sendMessage(EmoteReference.ERROR + "We cannot start this game due to an unknown error. My owners have been notified.").queue();
			LOGGER.error("Error while starting a instance of a game", e);
			endGame(event, player, false);
			return false;
		}
	}

	@Override
	public void call(GuildMessageReceivedEvent event, EntityPlayer player) {
		if (!(EntityPlayer.getPlayer(event.getAuthor().getId()).getId() == player.getId() && player.getGame() == GameReference.IMAGEGUESS
				&& !event.getMessage().getContent().startsWith(MantaroData.getData().get().getPrefix(event.getGuild())))) {
			return;
		}

		if (attempts > maxAttempts) {
			event.getChannel().sendMessage(EmoteReference.SAD + "You used all of your attempts, game is ending.").queue();
			endGame(event, player, false);
			return;
		}

			if(event.getMessage().getContent().equalsIgnoreCase(characterName)){
				long moneyAward = (long) ((player.getMoney() * 0.1) + new Random().nextInt(350));
				event.getChannel().sendMessage(EmoteReference.OK + "That's the correct answer, you won " + moneyAward + " credits for this.").queue();
				player.addMoney(moneyAward);
				player.setCurrentGame(null, event.getChannel());
				Entity.save();
				endGame(event, player, false);
				return;
			}

		if (event.getMessage().getContent().equalsIgnoreCase("end")) {
			endGame(event, player, false);
			return;
		}

		event.getChannel().sendMessage(EmoteReference.SAD + "That wasn't it! "
				+ EmoteReference.STOPWATCH + "You have " + (maxAttempts - attempts) + " remaning").queue();

		attempts++;
	}

	public String getCharacterName() {
		return characterName;
	}
}