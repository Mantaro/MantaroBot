package net.kodehawa.mantarobot.commands;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandType;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.Async;
import net.kodehawa.mantarobot.utils.GeneralUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class AnimeCmds extends Module {
	public static Logger LOGGER = LoggerFactory.getLogger("AnimeCmds");
	private final String CLIENT_SECRET = MantaroData.getConfig().get().alsecret;
	private String authToken;

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

	/**
	 * Refreshes the already given token in x ms. Usually every 58 minutes.
	 *
	 * @param seconds will run every x seconds
	 * @return the new AniList access token.
	 */
	private void login(int seconds) {
		Async.startAsyncTask("AniList Login Task", this::authenticate, seconds);
	}

	public AnimeCmds(){
		super(Category.MISC);
		anime();
		character();
		login(3500);
	}

	private void anime(){
		super.register("anime", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				TextChannel channel = event.getChannel();
				EmbedBuilder embed = new EmbedBuilder();
				try {
					//Set variables to use later. They will be parsed to JSON later on.
					String ANIME_TITLE = null, RELEASE_DATE = null, END_DATE = null, AVERAGE_SCORE = null, ANIME_DESCRIPTION = null, IMAGE_URL = null;
					String connection = String.format("https://anilist.co/api/anime/search/%1s?access_token=%2s",
							URLEncoder.encode(content, "UTF-8"), authToken);
					String json = GeneralUtils.instance().getObjectFromUrl(connection, event);
					JSONArray data;

					try {
						data = new JSONArray(json);
					} catch (JSONException e) {
						if (MantaroData.getConfig().get().debug) {
							e.printStackTrace();
						}
						channel.sendMessage(":heavy_multiplication_x: No results or unreadable reply from API server.").queue();
						return;
					}
					int i1 = 0;
					for (int i = 0; i < data.length(); i++) {
						//Only get first result.
						if (i1 == 0) {
							JSONObject entry = data.getJSONObject(i);
							//Set variables based in what the JSON retrieved is telling me of the anime.
							ANIME_TITLE = entry.get("title_english").toString();
							RELEASE_DATE = entry.get("start_date_fuzzy").toString(); //Returns as a date following this convention 20160116... cannot convert?
							END_DATE = entry.get("end_date_fuzzy").toString();
							ANIME_DESCRIPTION = entry.get("description").toString().replaceAll("<br>", "\n");
							AVERAGE_SCORE = entry.get("average_score").toString();
							IMAGE_URL = entry.get("image_url_lge").toString();
							i1++;
						}
					}

					//The result was unparseable by java.text.SimpleDateFormat so there I go. <THIS IS SO BAD KILL ME>
					String FINAL_RELEASE_YEAR = RELEASE_DATE.substring(0, 4);
					String FINAL_RELEASE_MONTH = RELEASE_DATE.substring(4, 6);
					String FINAL_RELEASE_DAY = RELEASE_DATE.substring(6, 8);
					String FINAL_END_YEAR = null, FINAL_END_DAY = null, FINAL_END_MONTH = null;
					if (!END_DATE.equals("null")) {
						FINAL_END_YEAR = END_DATE.substring(0, 4);
						FINAL_END_MONTH = END_DATE.substring(4, 6);
						FINAL_END_DAY = END_DATE.substring(6, 8);
					}

					String FINAL_RELEASE_DATE = FINAL_RELEASE_DAY + "/" + FINAL_RELEASE_MONTH + "/" + FINAL_RELEASE_YEAR;
					String FINAL_END_DATE;

					if (!END_DATE.equals("null")) {
						FINAL_END_DATE = FINAL_END_DAY + "/" + FINAL_END_MONTH + "/" + FINAL_END_YEAR;
					} else {
						FINAL_END_DATE = "Airing.";
					}

					//Start building the embedded message.
					embed.setColor(Color.LIGHT_GRAY)
							.setTitle("AnimeCmds information for " + GeneralUtils.instance().capitalizeEachFirstLetter(ANIME_TITLE.toLowerCase()))
							.setFooter("Information provided by AniList", null)
							.setThumbnail(IMAGE_URL)
							.addField("Description: ", ANIME_DESCRIPTION, false)
							.addField("Release date: ", FINAL_RELEASE_DATE, true)
							.addField("End date: ", FINAL_END_DATE, true)
							.addField("Average score: ", AVERAGE_SCORE + "/100", false);
				} catch (UnsupportedEncodingException e){
					e.printStackTrace();
				}
					channel.sendMessage(embed.build()).queue();
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
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

	private void character(){
		super.register("character", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				TextChannel channel = event.getChannel();
				EmbedBuilder embed = new EmbedBuilder();
				try {
					String CHAR_NAME = null, ALIASES = null, CHAR_DESCRIPTION = null, IMAGE_URL = null;
					String url = String.format("https://anilist.co/api/character/search/%1s?access_token=%2s",
							URLEncoder.encode(content, "UTF-8"), authToken);
					String json = GeneralUtils.instance().getObjectFromUrl(url, event);
					JSONArray data;
					try {
						data = new JSONArray(json);
					} catch (JSONException e) {
						if (MantaroData.getConfig().get().debug) {
							e.printStackTrace();
						}
						channel.sendMessage(":heavy_multiplication_x: No results or unreadable reply from API server.").queue();
						return;
					}
					int i1 = 0;
					for (int i = 0; i < data.length(); i++) {
						//Only get first result.
						if (i1 == 0) {
							JSONObject entry = data.getJSONObject(i);
							CHAR_NAME = entry.get("name_first").toString() + " " + entry.get("name_last");
							ALIASES = entry.get("name_alt").toString();
							IMAGE_URL = entry.get("image_url_lge").toString();
							if (entry.get("info").toString().length() < 1000) {
								CHAR_DESCRIPTION = entry.get("info").toString();
							} else {
								CHAR_DESCRIPTION = entry.get("info").toString().substring(0, 1000 - 1) + "(...)";
							}
							i1++;
						}
					}

					embed.setColor(Color.LIGHT_GRAY)
							.setThumbnail(IMAGE_URL)
							.setTitle("Information for " + CHAR_NAME);
					if (!ALIASES.equals("null")) {
						embed.setDescription("Also known as " + ALIASES);
					}
					embed.addField("Information", CHAR_DESCRIPTION, true)
							.setFooter("Information provided by AniList", null);

					channel.sendMessage(embed.build()).queue();
				} catch (Exception e) {
					LOGGER.warn("Problem processing data.", e);
					e.printStackTrace();
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
}
