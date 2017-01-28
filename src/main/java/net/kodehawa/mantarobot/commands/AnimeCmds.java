package net.kodehawa.mantarobot.commands;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.utils.AnimeData;
import net.kodehawa.mantarobot.commands.utils.CharacterData;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandType;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.Async;
import net.kodehawa.mantarobot.utils.GeneralUtils;
import net.kodehawa.mantarobot.utils.GsonDataManager;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AnimeCmds extends Module {
	public static Logger LOGGER = LoggerFactory.getLogger("AnimeCmds");
	private final String CLIENT_SECRET = MantaroData.getConfig().get().alsecret;
	private String authToken;

	public AnimeCmds() {
		super(Category.FUN);
		anime();
		character();
		login(3500);
	}

	private void anime() {
		super.register("anime", new SimpleCommand() {
			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				TextChannel channel = event.getChannel();
				EmbedBuilder embed = new EmbedBuilder();
				try {
					//Set variables to use later. They will be parsed to JSON later on.
					String connection = String.format("https://anilist.co/api/anime/search/%1s?access_token=%2s",
						URLEncoder.encode(content, "UTF-8"), authToken);
					String json = GeneralUtils.instance().getObjectFromUrl(connection, event);
					AnimeData[] type = GsonDataManager.GSON.fromJson(json, AnimeData[].class);

					String ANIME_TITLE = type[0].title_english;
					String RELEASE_DATE = StringUtils.substringBefore(type[0].start_date, "T");
					String END_DATE = StringUtils.substringBefore(type[0].end_date, "T");
					String ANIME_DESCRIPTION = type[0].description.replaceAll("<br>", "\n");
					String AVERAGE_SCORE = type[0].average_score;
					String IMAGE_URL = type[0].image_url_lge;
					String TYPE = GeneralUtils.capitalize(type[0].series_type);
					String EPISODES = type[0].total_episodes.toString();
					String DURATION = type[0].duration.toString();
					String GENRES = type[0].genres.stream().collect(Collectors.joining(", "));

					//Start building the embedded message.
					embed.setColor(Color.LIGHT_GRAY)
						.setAuthor("Anime information for " + ANIME_TITLE, "http://anilist.co/anime/"
								+ type[0].id, type[0].image_url_sml)
						.setFooter("Information provided by AniList", null)
						.setThumbnail(IMAGE_URL)
						.addField("Description: ", ANIME_DESCRIPTION, false)
						.addField("Release date: ", RELEASE_DATE, true)
						.addField("End date: ", END_DATE, true)
						.addField("Average score: ", AVERAGE_SCORE + "/100", true)
						.addField("Type", TYPE, true)
						.addField("Episodes", EPISODES, true)
						.addField("Episode Duration", DURATION + " minutes.", true)
						.addField("Genres", GENRES, false);
					channel.sendMessage(embed.build()).queue();
				} catch (Exception e) {
					event.getChannel().sendMessage("**Houston, we have a problem!**\n\n > We received a ``" + e.getClass().getSimpleName() + "`` while trying to process the command. \nError: ``" + e.getMessage() + "``").queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "AnimeCmds command")
					.setDescription("Retrieves anime info from **AniList** (For anime characters use ~>character).\n"
						+ "Usage: \n"
						+ "~>anime [animename]: Gets information of an anime based on parameters.\n"
						+ "Parameter description:\n"
						+ "[animename]: The name of the anime you are looking for. Make sure to write it similar to the original english name.\n")
					.setColor(Color.PINK)
					.build();
			}
		});
	}

	private void character() {
		super.register("character", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				TextChannel channel = event.getChannel();
				EmbedBuilder embed = new EmbedBuilder();
				try {
					String url = String.format("https://anilist.co/api/character/search/%1s?access_token=%2s", URLEncoder.encode(content, "UTF-8"), authToken);
					String json = GeneralUtils.instance().getObjectFromUrl(url, event);
					CharacterData[] character = GsonDataManager.GSON.fromJson(json, CharacterData[].class);

					String CHAR_NAME = character[0].name_first + " " + character[0].name_last + "\n(" + character[0].name_japanese + ")";
					String ALIASES = character[0].name_alt == null ? "No aliases" : "Also known as: " + character[0].name_alt;
					String IMAGE_URL = character[0].image_url_med;
					String CHAR_DESCRIPTION = character[0].info.isEmpty() ? "No info."
							: character[0].info.length() > 1000 ? character[0].info.substring(0, 1000-1) + "..." : character[0].info;

					embed.setColor(Color.LIGHT_GRAY)
						.setThumbnail(IMAGE_URL)
						.setAuthor("Information for " + CHAR_NAME, "http://anilist.co/character/" + character[0].id, IMAGE_URL)
						.setDescription(ALIASES)
						.addField("Information", CHAR_DESCRIPTION, true)
						.setFooter("Information provided by AniList", null);

					channel.sendMessage(embed.build()).queue();
				} catch (Exception e) {
					LOGGER.warn("Problem processing data.", e);
					event.getChannel().sendMessage("**Houston, we have a problem!**\n\n > We received a ``" + e.getClass().getSimpleName() + "`` while trying to process the command. \nError: ``" + e.getMessage() + "``").queue();
				}
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "AnimeCmds character command")
					.setDescription("Retrieves character info from **AniList**.\n"
						+ "Usage: \n"
						+ "~>character [charname]: Gets information of a character based on parameters.\n"
						+ "Parameter description:\n"
						+ "[character]: The name of the character you are looking info of. Make sure to write the exact character name or close to it.\n")
					.setColor(Color.DARK_GRAY)
					.build();
			}
		});
	}

	/**
	 * Refreshes the already given token in x ms. Usually every 58 minutes.
	 *
	 * @param seconds will run every x seconds
	 * @return the new AniList access token.
	 */
	private void login(int seconds) {
		Async.startAsyncTask("AniList Login Task", this::authenticate, seconds);
	}

	/**
	 * @return The new AniList access token.
	 */
	private void authenticate() {
		URL aniList;
		try {
			aniList = new URL("https://anilist.co/api/auth/access_token");
			HttpURLConnection alc = (HttpURLConnection) aniList.openConnection();
			alc.setRequestMethod("POST");
			alc.setRequestProperty("User-Agent", "Mantaro");
			alc.setDoOutput(true);
			alc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			OutputStreamWriter osw = new OutputStreamWriter(alc.getOutputStream());
			String CLIENT_ID = "kodehawa-o43eq";
			osw.write("grant_type=client_credentials&client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET);
			osw.flush();
			InputStream inputstream = alc.getInputStream();
			String json = CharStreams.toString(new InputStreamReader(inputstream, Charsets.UTF_8));
			JSONObject jObject = new JSONObject(json);
			authToken = jObject.getString("access_token");
			LOGGER.info("Updated auth token.");
		} catch (Exception e) {
			LOGGER.warn("Problem while updating auth token! " + e.getCause() + " " + e.getMessage());
			if (MantaroData.getConfig().get().debug) {
				e.printStackTrace();
			}
		}
	}
}
