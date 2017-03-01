package net.kodehawa.mantarobot.commands;

import com.google.gson.JsonSyntaxException;
import com.mashape.unirest.http.Unirest;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.utils.data.AnimeData;
import net.kodehawa.mantarobot.commands.utils.data.CharacterData;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.Async;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import net.kodehawa.mantarobot.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.net.URLEncoder;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

public class AnimeCmds extends Module {
	private static final Logger LOGGER = LoggerFactory.getLogger("AnimeCmds");
	private final String CLIENT_SECRET = MantaroData.getConfig().get().alsecret;
	private String authToken;

	public AnimeCmds() {
		super(Category.FUN);
		anime();
		character();
	}

	@Override
	public void onPostLoad() {
		super.onPostLoad();
		Async.startAsyncTask("AniList Login Task", this::authenticate, 1900);
	}

	private void anime() {
		super.register("anime", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				try {
					String connection = String.format("https://anilist.co/api/anime/search/%1s?access_token=%2s", URLEncoder.encode(content, "UTF-8"), authToken);
					String json = Utils.wget(connection, event);
					AnimeData[] type = GsonDataManager.GSON_PRETTY.fromJson(json, AnimeData[].class);

					if (type.length == 1) {
						animeData(event, type, 0);
						return;
					}

					EmbedBuilder builder = new EmbedBuilder().setColor(Color.CYAN).setTitle("Anime selection. Type a number to continue.", null).setFooter("This timeouts in 10 seconds.", null);
					StringBuilder b = new StringBuilder();
					for (int i = 0; i < 5 && i < type.length; i++) {
						AnimeData animeData = type[i];
						if (animeData != null)
							b.append('[').append(i + 1).append("] ").append(animeData.title_english).append(" (").append(animeData.title_japanese).append(")").append("\n");
					}

					event.getChannel().sendMessage(builder.setDescription(b.toString()).build()).queue();

					IntConsumer animeSelector = (c) -> {
						animeData(event, type, c - 1);
						event.getMessage().addReaction("\ud83d\udc4c").queue();
					};

					DiscordUtils.selectInt(event, type.length + 1, animeSelector);
				} catch (Exception e) {
					if (e instanceof JsonSyntaxException) {
						event.getChannel().sendMessage(":heavy_multiplication_x: No results or the API query was unsuccessful").queue();
						return;
					}
					event.getChannel().sendMessage("**Houston, we have a problem!**\n\n > We received a ``" + e.getClass().getSimpleName() + "`` while trying to process the command. \nError: ``" + e.getMessage() + "``").queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Anime command")
					.setDescription("Retrieves anime info from **AniList** (For anime characters use ~>character).\n"
						+ "Usage: \n"
						+ "~>anime <animename>: Gets information of an anime based on parameters.\n"
						+ "Parameter description:\n"
						+ "animename: The name of the anime you are looking for. Make sure to write it similar to the original english name.\n")
					.setColor(Color.PINK)
					.build();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

		});
	}

	private void animeData(GuildMessageReceivedEvent event, AnimeData[] type, int pick) {
		String ANIME_TITLE = type[pick].getTitle_english();
		String RELEASE_DATE = StringUtils.substringBefore(type[pick].getStart_date(), "T");
		String END_DATE = StringUtils.substringBefore(type[pick].getEnd_date(), "T");
		String ANIME_DESCRIPTION = type[pick].getDescription().replaceAll("<br>", "\n");
		String AVERAGE_SCORE = type[pick].getAverage_score();
		String IMAGE_URL = type[pick].getImage_url_lge();
		String TYPE = Utils.capitalize(type[pick].getSeries_type());
		String EPISODES = type[pick].getTotal_episodes().toString();
		String DURATION = type[pick].getDuration().toString();
		String GENRES = type[pick].getGenres().stream().collect(Collectors.joining(", "));

		//Start building the embedded message.
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(Color.LIGHT_GRAY)
			.setAuthor("Anime information for " + ANIME_TITLE, "http://anilist.co/anime/"
				+ type[0].getId(), type[0].getImage_url_sml())
			.setFooter("Information provided by AniList", null)
			.setThumbnail(IMAGE_URL)
			.addField("Description: ", ANIME_DESCRIPTION.length() <= 1024 ? ANIME_DESCRIPTION : ANIME_DESCRIPTION.substring(0, 1020) + "...", false)
			.addField("Release date: ", RELEASE_DATE, true)
			.addField("End date: ", END_DATE, true)
			.addField("Average score: ", AVERAGE_SCORE + "/100", true)
			.addField("Type", TYPE, true)
			.addField("Episodes", EPISODES, true)
			.addField("Episode Duration", DURATION + " minutes.", true)
			.addField("Genres", GENRES, false);
		event.getChannel().sendMessage(embed.build()).queue();
	}

	/**
	 * returns the new AniList access token.
	 */
	public void authenticate() {
		String aniList = "https://anilist.co/api/auth/access_token";
		String CLIENT_ID = "kodehawa-o43eq";
		try {
			authToken = Unirest.post(aniList)
				.header("User-Agent", "Mantaro")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.body("grant_type=client_credentials&client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET)
				.asJson()
				.getBody()
				.getObject().getString("access_token");
			LOGGER.info("Updated auth token.");
		} catch (Exception e) {
			LOGGER.warn("Problem while updating auth token! <@155867458203287552> check it out", e);
		}
	}

	private void character() {
		super.register("character", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				try {
					String url = String.format("https://anilist.co/api/character/search/%1s?access_token=%2s", URLEncoder.encode(content, "UTF-8"), authToken);
					String json = Utils.wget(url, event);
					CharacterData[] character = GsonDataManager.GSON_PRETTY.fromJson(json, CharacterData[].class);

					if (character.length == 1) {
						characterData(event, character, 0);
						return;
					}

					EmbedBuilder builder = new EmbedBuilder().setColor(Color.CYAN).setTitle("Character selection. Type a number to continue.", null).setFooter("This timeouts in 10 seconds.", null);
					StringBuilder b = new StringBuilder();

					for (int i = 0; i < 5 && i < character.length; i++) {
						CharacterData characterData = character[i];
						if (characterData != null)
							b.append('[').append(i + 1).append("] ").append(characterData.name_first).append(" ").append(characterData.name_last).append("\n");
					}

					event.getChannel().sendMessage(builder.setDescription(b.toString()).build()).queue();

					IntConsumer characterSelector = (c) -> {
						characterData(event, character, c - 1);
						event.getMessage().addReaction("\ud83d\udc4c").queue();
					};
					DiscordUtils.selectInt(event, character.length + 1, characterSelector);
				} catch (Exception e) {
					if (e instanceof JsonSyntaxException) {
						event.getChannel().sendMessage(":heavy_multiplication_x: No results or the API query was unsuccessful").queue();
						return;
					}
					LOGGER.warn("Problem processing data.", e);
					event.getChannel().sendMessage("**Houston, we have a problem!**\n\n > We received a ``" + e.getClass().getSimpleName() + "`` while trying to process the command. \nError: ``" + e.getMessage() + "``").queue();
				}
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Character command")
					.setDescription("Retrieves character info from **AniList**.\n"
						+ "Usage: \n"
						+ "~>character <charname>: Gets information of a character based on parameters.\n"
						+ "Parameter description:\n"
						+ "character: The name of the character you are looking info of. Make sure to write the exact character name or close to it.\n")
					.setColor(Color.DARK_GRAY)
					.build();
			}
		});
	}

	private void characterData(GuildMessageReceivedEvent event, CharacterData[] character, int pick) {
		String JAP_NAME = character[pick].getName_japanese() == null ? "" : "\n(" + character[pick].getName_japanese() + ")";
		String CHAR_NAME = character[pick].getName_first() + " " + character[pick].getName_last() + JAP_NAME;
		String ALIASES = character[pick].getName_alt() == null ? "No aliases" : "Also known as: " + character[pick].getName_alt();
		String IMAGE_URL = character[pick].getImage_url_med();
		String CHAR_DESCRIPTION = character[pick].getInfo().isEmpty() ? "No info."
			: character[pick].getInfo().length() <= 1024 ? character[pick].getInfo() : character[pick].getInfo().substring(0, 1020 - 1) + "...";
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(Color.LIGHT_GRAY)
			.setThumbnail(IMAGE_URL)
			.setAuthor("Information for " + CHAR_NAME, "http://anilist.co/character/" + character[0].getId(), IMAGE_URL)
			.setDescription(ALIASES)
			.addField("Information", CHAR_DESCRIPTION, true)
			.setFooter("Information provided by AniList", null);

		event.getChannel().sendMessage(embed.build()).queue();
	}
}
