package net.kodehawa.mantarobot.utils;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONArray;
import org.json.JSONObject;
import us.monoid.web.Resty;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;

public class GeneralUtils {

	/**
	 * Monitors CPU usage if needed.
	 *
	 * @author Yomura
	 */
	public static class PerformanceMonitor {

	}

	public static final GeneralUtils.PerformanceMonitor pm = new GeneralUtils.PerformanceMonitor();
	private volatile static GeneralUtils instance = new GeneralUtils();

	/**
	 * @return The new instance of this class.
	 */
	public static GeneralUtils instance() {
		return instance;
	}

	public static Iterable<String> iterate(Matcher matcher) {
		return new Iterable<String>() {
			@Override
			public Iterator<String> iterator() {
				return new Iterator<String>() {
					@Override
					public boolean hasNext() {
						return matcher.find();
					}

					@Override
					public String next() {
						return matcher.group();
					}
				};
			}

			@Override
			public void forEach(Consumer<? super String> action) {
				while (matcher.find()) {
					action.accept(matcher.group());
				}
			}
		};
	}

	public static synchronized String paste(String toSend) {
		HttpURLConnection connection = null;
		try {
			URL url = new URL("https://hastebin.com/documents");
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("User-Agent", "Mantaro");
			connection.setRequestProperty("Content-Type", "text/plain");
			connection.setDoInput(true);
			connection.setDoOutput(true);

			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			wr.writeBytes(toSend);
			wr.flush();
			wr.close();

			InputStream inputstream = connection.getInputStream();
			String json = CharStreams.toString(new InputStreamReader(inputstream, Charsets.UTF_8));
			System.out.println(json);
			JSONObject jObject = new JSONObject(json);
			String pasteToken = jObject.getString("key");
			return "https://hastebin.com/" + pasteToken;

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (connection == null) return null;
			connection.disconnect();
		}
	}

	/**
	 * Capitalizes the first letter of a string.
	 *
	 * @param s the string to capitalize
	 * @return A string with the first letter capitalized.
	 */
	public static String capitalize(String s) {
		if (s.length() == 0) return s;
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}

	/**
	 * Capitalizes each first letter after a space.
	 *
	 * @param original the string to capitalize.
	 * @return a string That Looks Like This. Useful for titles.
	 */
	public String capitalizeEachFirstLetter(String original) {
		if (original == null || original.length() == 0) {
			return original;
		}

		String[] words = original.split("\\s");
		StringBuilder builder = new StringBuilder();
		for (String s : words) {
			builder.append(capitalize(s)).append(" ");
		}
		return builder.toString();
	}

	public String getDurationMinutes(long length) {
		return String.format("%d:%02d minutes",
			TimeUnit.MILLISECONDS.toMinutes(length),
			TimeUnit.MILLISECONDS.toSeconds(length) -
				TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(length))
		);
	}

	/**
	 * Gets a JSON Array from a specified URL
	 *
	 * @param url The URL to fetch the JSON from.
	 * @param evt JDA message event.
	 * @return The retrieved JSON object.
	 */
	public JSONArray getJSONArrayFromUrl(String url, GuildMessageReceivedEvent evt) {
		String urlParsed = getObjectFromUrl(url, evt);
		return new JSONArray(urlParsed);
	}

	/**
	 * Gets a JSON Array from a specified URL
	 *
	 * @param url The URL to fetch the JSON from.
	 * @param evt JDA message event.
	 * @return The retrieved JSON object.
	 */
	public JSONObject getJSONObjectFromUrl(String url, GuildMessageReceivedEvent evt) {
		String urlParsed = getObjectFromUrl(url, evt);
		return new JSONObject(urlParsed);
	}

	/**
	 * Fetches an Object from any given URL. Uses vanilla Java methods.
	 * Can retrieve text, JSON Objects, XML and probably more.
	 *
	 * @param url   The URL to get the object from.
	 * @param event
	 * @return The object as a parsed UTF-8 string.
	 */
	public String getObjectFromUrl(String url, GuildMessageReceivedEvent event) {
		String webobject = null;

		try {
			URL ur1 = new URL(url);
			HttpURLConnection ccnn = (HttpURLConnection) ur1.openConnection();
			ccnn.setRequestProperty("User-Agent", "Mantaro");
			InputStream ism = ccnn.getInputStream();
			webobject = CharStreams.toString(new InputStreamReader(ism, Charsets.UTF_8));
		} catch (Exception e) {
			e.printStackTrace();
			event.getChannel().sendMessage("\u274C Error retrieving data from URL.");
		}

		return webobject;
	}

	/**
	 * Same than above, but using resty. Way easier tbh.
	 *
	 * @param url   The URL to get the object from.
	 * @param event JDA message event.
	 * @return The object as a parsed string.
	 */
	public String restyGetObjectFromUrl(String url, GuildMessageReceivedEvent event) {
		String url2 = null;
		try {
			Resty resty = new Resty();
			resty.identifyAsMozilla();
			url2 = resty.text(url).toString();
		} catch (IOException e) {
			e.printStackTrace();
			event.getChannel().sendMessage("\u274C Error retrieving data from URL [Resty]");
		}

		return url2;
	}

	public String toPrettyJson(String jsonString) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonParser jsonParser = new JsonParser();
		JsonElement jsonElement = jsonParser.parse(jsonString);
		return gson.toJson(jsonElement);
	}
}