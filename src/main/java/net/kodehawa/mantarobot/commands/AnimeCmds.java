package net.kodehawa.mantarobot.commands;

import br.com.brjdevs.java.utils.extensions.Async;
import com.google.gson.JsonSyntaxException;
import com.mashape.unirest.http.Unirest;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.utils.data.AnimeData;
import net.kodehawa.mantarobot.commands.utils.data.CharacterData;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.*;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import org.apache.commons.lang3.StringUtils;

import java.awt.Color;
import java.net.URLEncoder;
import java.util.stream.Collectors;

@Slf4j
@RegisterCommand.Class
public class AnimeCmds implements HasPostLoad {
	public static String authToken;

	@RegisterCommand
	public static void anime(CommandRegistry cr) {
		cr.register("anime", new SimpleCommandCompat(Category.FUN, "Gets information of an anime based on parameters.") {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				try {
					String connection = String.format("https://anilist.co/api/anime/search/%1s?access_token=%2s", URLEncoder.encode(content, "UTF-8"), authToken);
					String json = Utils.wget(connection, event);
					AnimeData[] type = GsonDataManager.GSON_PRETTY.fromJson(json, AnimeData[].class);

					if (type.length == 1) {
						animeData(event, type[0]);
						return;
					}

					DiscordUtils.selectList(event, type, anime -> String.format("%s (%s)",
						anime.getTitle_english(), anime.getTitle_japanese()),
						s -> baseEmbed(event, "Anime selection. Type a number to continue.").setDescription(s).build(),
						anime -> animeData(event, anime));
				} catch (Exception e) {
					if (e instanceof JsonSyntaxException) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "No results or the API query was unsuccessful").queue();
						return;
					}

					if (e instanceof NullPointerException) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "We got a wrong API result for this specific search. Maybe try another one?").queue();
						return;
					}
					event.getChannel().sendMessage(EmoteReference.ERROR + "**Houston, we have a problem!**\n\n > We received a ``" + e.getClass().getSimpleName() + "`` while trying to process the command. \nError: ``" + e.getMessage() + "``").queue();
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

	private static void animeData(GuildMessageReceivedEvent event, AnimeData type) {
		String ANIME_TITLE = type.getTitle_english();
		String RELEASE_DATE = StringUtils.substringBefore(type.getStart_date(), "T");
		String END_DATE = StringUtils.substringBefore(type.getEnd_date(), "T");
		String ANIME_DESCRIPTION = type.getDescription().replaceAll("<br>", "\n");
		String AVERAGE_SCORE = type.getAverage_score();
		String IMAGE_URL = type.getImage_url_lge();
		String TYPE = Utils.capitalize(type.getSeries_type());
		String EPISODES = type.getTotal_episodes().toString();
		String DURATION = type.getDuration().toString();
		String GENRES = type.getGenres().stream().collect(Collectors.joining(", "));

		//Start building the embedded message.
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(Color.LIGHT_GRAY)
			.setAuthor("Anime information for " + ANIME_TITLE, "http://anilist.co/anime/"
				+ type.getId(), type.getImage_url_sml())
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

	@RegisterCommand
	public static void character(CommandRegistry cr) {
		cr.register("character", new SimpleCommandCompat(Category.FUN, "Gets information of a character based on parameters.") {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				try {
					String url = String.format("https://anilist.co/api/character/search/%1s?access_token=%2s", URLEncoder.encode(content, "UTF-8"), authToken);
					String json = Utils.wget(url, event);
					CharacterData[] character = GsonDataManager.GSON_PRETTY.fromJson(json, CharacterData[].class);

					if (character.length == 1) {
						characterData(event, character[0]);
						return;
					}

					DiscordUtils.selectList(event, character, character1 -> String.format("%s %s",
						character1.name_last, character1.name_first),
						s -> baseEmbed(event, "Character selection. Type a number to continue.").setDescription(s).build(),
						character1 -> characterData(event, character1));
				} catch (Exception e) {
					if (e instanceof JsonSyntaxException) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "No results or the API query was unsuccessful").queue();
						return;
					}
					log.warn("Problem processing data.", e);
					event.getChannel().sendMessage(EmoteReference.ERROR + "**Houston, we have a problem!**\n\n > We received a ``" + e.getClass().getSimpleName() + "`` while trying to process the command. \nError: ``" + e.getMessage() + "``").queue();
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

	private static void characterData(GuildMessageReceivedEvent event, CharacterData character) {
		String JAP_NAME = character.getName_japanese() == null ? "" : "\n(" + character.getName_japanese() + ")";
		String CHAR_NAME = character.getName_first() + " " + character.getName_last() + JAP_NAME;
		String ALIASES = character.getName_alt() == null ? "No aliases" : "Also known as: " + character.getName_alt();
		String IMAGE_URL = character.getImage_url_med();
		String CHAR_DESCRIPTION = character.getInfo().isEmpty() ? "No info."
			: character.getInfo().length() <= 1024 ? character.getInfo() : character.getInfo().substring(0, 1020 - 1) + "...";
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(Color.LIGHT_GRAY)
			.setThumbnail(IMAGE_URL)
			.setAuthor("Information for " + CHAR_NAME, "http://anilist.co/character/" + character.getId(), IMAGE_URL)
			.setDescription(ALIASES)
			.addField("Information", CHAR_DESCRIPTION, true)
			.setFooter("Information provided by AniList", null);

		event.getChannel().sendMessage(embed.build()).queue();
	}

	private final String CLIENT_SECRET = MantaroData.config().get().alsecret;

	@Override
	public void onPostLoad() {
		Async.task("AniList Login Task", this::authenticate, 1900);
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
			log.info("Updated auth token.");
		} catch (Exception e) {
			log.warn("Problem while updating auth token! <@155867458203287552>, check nohup.out.");
			log.warn("Problem while updating auth token! <@155867458203287552> check it out", e);
		}
	}
}
