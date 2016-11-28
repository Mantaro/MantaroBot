package net.kodehawa.mantarobot.cmd;

import java.awt.Color;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.core.Mantaro;

/**
 * Anime module. Returns results for anime, manga and anime character queries.
 * Using AniList API.
 * @author Yomura
 */
public class Anime extends Command {

	private String CLIENT_ID = "kodehawa-o43eq";
	public static String CLIENT_SECRET = "";
	String authToken;

	public Anime(){
		setName("anime");
		authenticate();
	}
	
	/**
	 * Assuming we already have the access token.
	 */
	@Override
	public void onCommand(String[] split, String content, MessageReceivedEvent event) {
		guild = event.getGuild();
        author = event.getAuthor();
        channel = event.getChannel();
        receivedMessage = event.getMessage();
        
		if(content.startsWith("info")){
			
			try {
				//Set variables to use later. They will be parsed to JSON later on.
				String ANIME_TITLE = null, RELEASE_DATE = null, END_DATE = null, AVERAGE_SCORE = null, ANIME_DESCRIPTION = null, IMAGE_URL = null;
				
				//Open a connection to the AniList API
				URL anime = new URL("https://anilist.co/api/anime/search/" + content.replace("info ", "") + "?access_token=" + authToken);
				HttpURLConnection animec = (HttpURLConnection) anime.openConnection();
		        animec.setRequestProperty("User-Agent", "Mantaro");
		        InputStream ism = animec.getInputStream();
				String json;
				json = CharStreams.toString(new InputStreamReader(ism, Charsets.UTF_8));
				JSONArray animeData = null;
				try{
		        	animeData = new JSONArray(json);
				}
				catch(JSONException e){
					if(Mantaro.instance().isDebugEnabled){
						e.printStackTrace();
					}
					channel.sendMessage("No results or unreadable reply from API server.").queue();
					return;
				}
		        int i1 = 0;
				for(int i = 0; i < animeData.length(); i++) { 
					//Only get first result.
					if(i1 == 0){
						JSONObject entry = animeData.getJSONObject(i);
						
						//Set variables based in what the JSON retrieved is telling me of the anime.
						ANIME_TITLE = entry.get("title_english").toString();
						RELEASE_DATE = entry.get("start_date_fuzzy").toString(); //Returns as a date following this convention 20160116... cannot convert?
						END_DATE = entry.get("end_date_fuzzy").toString();
						ANIME_DESCRIPTION = entry.get("description").toString().replaceAll("<br>", "");
						AVERAGE_SCORE = entry.get("average_score").toString();
						IMAGE_URL = entry.get("image_url_lge").toString();
						
						i1 = i1 + 1;
					}
					
				}
				
				//The result was unparseable by java.text.SimpleDateFormat so there I go.
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
				String FINAL_END_DATE = "";
				
				if(!END_DATE.equals("null")){
					FINAL_END_DATE = FINAL_END_DAY+"/"+FINAL_END_MONTH+"/"+FINAL_END_YEAR;
				} else{
					FINAL_END_DATE = "On air.";
				}

				//Start building the embedded message.
				EmbedBuilder builder = new EmbedBuilder();
				builder.setColor(Color.LIGHT_GRAY)
					.setTitle("Anime information for " + capitalizeEachFirstLetter(ANIME_TITLE.toLowerCase()))
					.setFooter("Information provided by AniList", null)
					.setThumbnail(IMAGE_URL)
					.addField("Description: ", ANIME_DESCRIPTION, false)
					.addField("Release date: ", FINAL_RELEASE_DATE, true)
					.addField("End date: ", FINAL_END_DATE, true)
					.addField("Average score: ", AVERAGE_SCORE+"/100", false);

				//Build the embedded and send it.
				channel.sendMessage(builder.build()).queue();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if(content.startsWith("character")){
			URL anime;
			try {
				String CHAR_NAME = null, ALIASES =  null, CHAR_DESCRIPTION = null, IMAGE_URL = null;
				anime = new URL("https://anilist.co/api/character/search/" + URLEncoder.encode(content.replace("character ", ""), "UTF-8") + "?access_token=" + authToken);
				HttpURLConnection acn = (HttpURLConnection) anime.openConnection();
	        	acn.setRequestProperty("User-Agent", "Mantaro");
	        	InputStream ism = acn.getInputStream();
				String json;
				json = CharStreams.toString(new InputStreamReader(ism, Charsets.UTF_8));
				JSONArray data = null;
				try{
		        	data = new JSONArray(json);
				}
				catch(JSONException e){
					if(Mantaro.instance().isDebugEnabled){
						e.printStackTrace();
					}
					channel.sendMessage("No results or unreadable reply from API server.").queue();
					return;
				}
	        	System.out.println(json);
		        int i1 = 0;
	        	for(int i = 0; i < data.length(); i++) { 
					//Only get first result.
					if(i1 == 0){
						JSONObject entry = data.getJSONObject(i);
						CHAR_NAME = entry.get("name_first").toString() + " " + entry.get("name_last");
			        	ALIASES = entry.get("name_alt").toString();
			        	IMAGE_URL = entry.get("image_url_lge").toString();
			        	if(entry.get("info").toString().toString().length() < 1000){
				        	CHAR_DESCRIPTION = entry.get("info").toString().toString();
			        	} else{
				        	CHAR_DESCRIPTION = entry.get("info").toString().toString().substring(0, 1000 - 1) + "(...)";
			        	}
						i1 = i1 + 1;
					}
	        	}
	        	
				EmbedBuilder builder = new EmbedBuilder();
				builder.setColor(Color.LIGHT_GRAY)
					.setThumbnail(IMAGE_URL)
					.setTitle("Information for " + CHAR_NAME);
				if(!ALIASES.equals("null")){
					builder.setDescription("Also known as " + ALIASES);
				}
				builder.addField("Information", CHAR_DESCRIPTION, true)
					.setFooter("Information provided by AniList", null);
				
				channel.sendMessage(builder.build()).queue();
			}
	        catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * This is extremely recursive, lmfao.
	 * @return The new AniList access token.
	 */
	private String authenticate(){
		URL aniList;
		try {
			aniList = new URL("https://anilist.co/api/auth/access_token");
	        HttpURLConnection alc = (HttpURLConnection) aniList.openConnection();
	        alc.setRequestMethod("POST");
	        alc.setRequestProperty("User-Agent", "Mantaro");
	        alc.setDoOutput(true);
	        alc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	        OutputStreamWriter osw = new OutputStreamWriter(alc.getOutputStream());
	        osw.write("grant_type=client_credentials&client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET);
	        osw.flush(); 
	        InputStream inputstream = alc.getInputStream();
	        String json = CharStreams.toString(new InputStreamReader(inputstream, Charsets.UTF_8));
	        JSONObject jObject = new JSONObject(json);
	        authToken = jObject.getString("access_token");
		} catch (Exception e) {
			e.printStackTrace();
		}

		refreshToken(1800000);

		return authToken;
	}

	/**
	 * Refreshes the already given token in x ms. Usually 30 minutes.
	 * @param ms
	 * @return the new AniList access token.
	 */

	private void refreshToken(int ms){
		TimerTask timerTask = new TimerTask() 
	    {
	        public void run() { 
	        	authenticate();
	        	System.out.println("Updated auth token.");
	        } 
	     }; 
		 Timer timer = new Timer(); 
		 timer.schedule(timerTask, ms, ms);
	}
	
	/**
	 * Capitalizes each first letter after a space.
	 * @param original
	 * @return a string That Looks Like This. Useful for titles.
	 */
	public String capitalizeEachFirstLetter(String original) {
	    if (original == null || original.length() == 0) {
	        return original;
	    }
	    
	    String line = original;
	    String[] words = line.split("\\s");
	    StringBuilder builder = new StringBuilder();
	    for(String s : words) {
	        builder.append(capitalize(s) + " ");
	    }
	    return builder.toString();
	}
	
	/**
	 * Capitalizes the first letter of a string.
	 * @param s
	 * @return A string with the first letter capitalized.
	 */
	public String capitalize(String s) {
        if (s.length() == 0) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
