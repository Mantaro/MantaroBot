package net.kodehawa.mantarobot.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.osu.api.ciyfhx.Mod;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import us.monoid.web.Resty;

public class Utils {

	private volatile static Utils instance = new Utils();
	private HashMap<Mod, String> mods = new HashMap<Mod, String>();
	
	public Utils(){
		putMods();
	}
	
	/**
	 * Fetches an Object from any given URL. Uses vanilla Java methods.
	 * Can retrieve text, JSON Objects, XML and probably more.
	 * @param url
	 * @return The object as a parsed UTF-8 string.
	 */
	public String getObjectFromUrl(String url, MessageReceivedEvent event){
		String webobject = null;

		try {
			URL ur1 = new URL(url);
			HttpURLConnection ccnn = (HttpURLConnection) ur1.openConnection();
	    	ccnn.setRequestProperty("User-Agent", "Mantaro");
	    	InputStream ism = ccnn.getInputStream();
			webobject = CharStreams.toString(new InputStreamReader(ism, Charsets.UTF_8));
		} catch (Exception e) {
			e.printStackTrace();
			event.getChannel().sendMessage(":heavy_multiplication_x: Error retrieving data from URL.");
		}
		
		return webobject;
	}
	
	/**
	 * Same than above, but using resty. Way easier tbh.
	 * @param url
	 * @param event
	 * @return The object as a parsed string.
	 */
	public String restyGetObjectFromUrl(String url, MessageReceivedEvent event){
		String url2 = null;
		try {
			Resty resty = new Resty();
			resty.identifyAsMozilla();
			url2 = resty.text(url).toString();
		} catch (IOException e) {
			e.printStackTrace();
			event.getChannel().sendMessage(":heavy_multiplication_x: Error retrieving data from URL [Resty]");
		}
		
		return url2;
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
	
	/**
	 * From osu!api returned results, put a abbreviated value.
	 */
	private void putMods(){
		mods.put(Mod.HIDDEN, "HD");
		mods.put(Mod.HARD_ROCK, "HR");
		mods.put(Mod.DOUBLE_TIME, "DT");
		mods.put(Mod.FLASHLIGHT, "FL");
		mods.put(Mod.NO_FAIL, "NF");
		mods.put(Mod.AUTOPLAY, "AP");
		mods.put(Mod.HALF_TIME, "HT");
		mods.put(Mod.EASY, "EZ");
		mods.put(Mod.NIGHTCORE, "NC");
		mods.put(Mod.RELAX, "RX");
		mods.put(Mod.SPUN_OUT, "SO");
		mods.put(Mod.SUDDEN_DEATH, "SD");
	}

	/**
	 * @param Mod key
	 * @return a abbreviated, standardized osu! mod.
	 */
	public String getMod(Mod key){
		return mods.get(key);
	}
	
	/**
	 * @return The new instance of this class.
	 */
	public static Utils instance(){
		return instance;
	}
}