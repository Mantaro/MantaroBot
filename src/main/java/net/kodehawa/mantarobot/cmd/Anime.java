package net.kodehawa.mantarobot.cmd;

import java.awt.Color;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import net.kodehawa.mantarobot.log.Type;
import net.kodehawa.mantarobot.log.Log;
import net.kodehawa.mantarobot.module.Callback;
import net.kodehawa.mantarobot.module.CommandType;
import net.kodehawa.mantarobot.module.Module;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.thread.AsyncHelper;
import net.kodehawa.mantarobot.util.Utils;

/**
 * Anime module. Returns results for anime, manga and anime character queries.
 * Using AniList API.
 * @author Yomura
 */
public class Anime extends Module {

	private final String CLIENT_SECRET = Mantaro.instance().getConfig().values().get("alsecret").toString();
	private String authToken;

	public Anime(){
		this.registerCommands();
		login(2000);
	}

	@Override
	public void registerCommands(){
		super.register("anime", "Retrieves information about an anime.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				guild = event.getGuild();
				author = event.getAuthor();
				channel = event.getChannel();
				receivedMessage = event.getMessage();
				EmbedBuilder embed = new EmbedBuilder();
				try {
					//Set variables to use later. They will be parsed to JSON later on.
					String ANIME_TITLE = null, RELEASE_DATE = null, END_DATE = null, AVERAGE_SCORE = null, ANIME_DESCRIPTION = null, IMAGE_URL = null;
					String connection = String.format("https://anilist.co/api/anime/search/%1s?access_token=%2s",
							URLEncoder.encode(content, "UTF-8"), authToken);
					String json = Utils.instance().getObjectFromUrl(connection, event);
					JSONArray data;

					try{
						data = new JSONArray(json);
					}
					catch(JSONException e){
						if(Mantaro.instance().isDebugEnabled){
							e.printStackTrace();
						}
						channel.sendMessage(":heavy_multiplication_x: No results or unreadable reply from API server.").queue();
						return;
					}
					int i1 = 0;
					for(int i = 0; i < data.length(); i++) {
						//Only get first result.
						if(i1 == 0){
							JSONObject entry = data.getJSONObject(i);
							//Set variables based in what the JSON retrieved is telling me of the anime.
							ANIME_TITLE = entry.get("title_english").toString();
							RELEASE_DATE = entry.get("start_date_fuzzy").toString(); //Returns as a date following this convention 20160116... cannot convert?
							END_DATE = entry.get("end_date_fuzzy").toString();
							ANIME_DESCRIPTION = entry.get("description").toString().replaceAll("<br>", "");
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
					if(!END_DATE.equals("null")){
						FINAL_END_YEAR = END_DATE.substring(0, 4);
						FINAL_END_MONTH = END_DATE.substring(4, 6);
						FINAL_END_DAY = END_DATE.substring(6, 8);
					}

					String FINAL_RELEASE_DATE = FINAL_RELEASE_DAY+"/"+FINAL_RELEASE_MONTH+"/"+FINAL_RELEASE_YEAR;
					String FINAL_END_DATE;

					if(!END_DATE.equals("null")){
						FINAL_END_DATE = FINAL_END_DAY+"/"+FINAL_END_MONTH+"/"+FINAL_END_YEAR;
					} else{
						FINAL_END_DATE = "Airing.";
					}

					//Start building the embedded message.
					embed.setColor(Color.LIGHT_GRAY)
							.setTitle("Anime information for " + Utils.instance().capitalizeEachFirstLetter(ANIME_TITLE.toLowerCase()))
							.setFooter("Information provided by AniList", null)
							.setThumbnail(IMAGE_URL)
							.addField("Description: ", ANIME_DESCRIPTION, false)
							.addField("Release date: ", FINAL_RELEASE_DATE, true)
							.addField("End date: ", FINAL_END_DATE, true)
							.addField("Average score: ", AVERAGE_SCORE+"/100", false);

					//Build the embedded and send it.
					channel.sendMessage(embed.build()).queue();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public String help() {
				return  "Retrieves anime info from **AniList** (For anime characters use ~>character).\n"
						+ "Usage: \n"
						+ "~>anime [animename]: Gets information of an anime based on parameters.\n"
						+ "Parameter description:\n"
						+ "[animename]: The name of the anime you are looking for. Make sure to write it similar to the original english name.\n";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("character", "Retrieves information about a character.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				guild = event.getGuild();
				author = event.getAuthor();
				channel = event.getChannel();
				receivedMessage = event.getMessage();
				EmbedBuilder embed = new EmbedBuilder();
				try {
					String CHAR_NAME = null, ALIASES =  null, CHAR_DESCRIPTION = null, IMAGE_URL = null;
					String url = String.format("https://anilist.co/api/character/search/%1s?access_token=%2s",
							URLEncoder.encode(content, "UTF-8"), authToken);
					String json = Utils.instance().getObjectFromUrl(url, event);
					JSONArray data;
					try{
						data = new JSONArray(json);
					}
					catch(JSONException e){
						if(Mantaro.instance().isDebugEnabled){
							e.printStackTrace();
						}
						channel.sendMessage(":heavy_multiplication_x: No results or unreadable reply from API server.").queue();
						return;
					}
					int i1 = 0;
					for(int i = 0; i < data.length(); i++) {
						//Only get first result.
						if(i1 == 0){
							JSONObject entry = data.getJSONObject(i);
							CHAR_NAME = entry.get("name_first").toString() + " " + entry.get("name_last");
							ALIASES = entry.get("name_alt").toString();
							IMAGE_URL = entry.get("image_url_lge").toString();
							if(entry.get("info").toString().length() < 1000){
								CHAR_DESCRIPTION = entry.get("info").toString();
							} else{
								CHAR_DESCRIPTION = entry.get("info").toString().substring(0, 1000 - 1) + "(...)";
							}
							i1++;
						}
					}

					embed.setColor(Color.LIGHT_GRAY)
							.setThumbnail(IMAGE_URL)
							.setTitle("Information for " + CHAR_NAME);
					if(!ALIASES.equals("null")){
						embed.setDescription("Also known as " + ALIASES);
					}
					embed.addField("Information", CHAR_DESCRIPTION, true)
							.setFooter("Information provided by AniList", null);

					channel.sendMessage(embed.build()).queue();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public String help() {
				return  "Retrieves character info from **AniList**.\n"
						+ "Usage: \n"
						+ "~>character [charname]: Gets information of a character based on parameters.\n"
						+ "Parameter description:\n"
						+ "[character]: The name of the character you are looking info of. Make sure to write the exact character name or close to it.\n";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});
	}
	
	/**
	 * @return The new AniList access token.
	 */
	private void authenticate(){
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
			Log.instance().print("Updated auth token.", this.getClass(), Type.INFO);
		} catch (Exception e) {
			Log.instance().print("Problem while updating auth token!" + e.getCause() + " " + e.getMessage(), this.getClass(), Type.WARNING);
			if(Mantaro.instance().isDebugEnabled){ e.printStackTrace(); }
		}
	}

	/**
	 * Refreshes the already given token in x ms. Usually 30 minutes.
	 * @param seconds will run every x seconds
	 * @return the new AniList access token.
	 */
	private void login(int seconds){
		Runnable loginTask = this::authenticate;
		AsyncHelper.instance().startAsyncTask("AniList Login Task", loginTask, seconds);
	}
}
