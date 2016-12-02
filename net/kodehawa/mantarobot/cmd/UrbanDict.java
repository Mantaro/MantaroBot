package net.kodehawa.mantarobot.cmd;

import java.awt.Color;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.util.Utils;

public class UrbanDict extends Command {


	public UrbanDict()
	{
		setName("urban");
		setDescription("Retrieves definitions from urban dictionary. Use ~>help urban to get more info.");
		setExtendedHelp(
				"Retrieves definitions from **Urban Dictionary**.\r"
				+ "Usage: \r"
				+ "~>urban term->number: Gets a definition based on parameters.\r"
				+ "Parameter description:\r"
				+ "*term*: The term you want to look up the urban definition for.\r"
				+ "*number*: **OPTIONAL** Parameter defined with the modifier '->' after the term. You don't need to use it.\r"
				+ "For example putting 2 will fetch the second result on Urban Dictionary"
				);
		setCommandType("user");
	}

	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
		//Initialize the variables I need to use.
        channel = evt.getChannel();
        //First split is definition, second one is number. I would use space but we need the ability to fetch with spaces too.
		String beheadedSplit[] = beheadedMessage.split("->");
	    EmbedBuilder embed = new EmbedBuilder();

		if(!beheadedMessage.isEmpty()){
    		ArrayList<String> definitions = new ArrayList<String>(); //Will use later to store definitions.
    		ArrayList<String> thumbsup = new ArrayList<String>();
    		ArrayList<String> thumbsdown = new ArrayList<String>(); //Will use later to store definitions.
    		ArrayList<String> urls = new ArrayList<String>(); //Will use later to store definitions.
			long start = System.currentTimeMillis();
    		String url = null;
			try {
				url = "http://api.urbandictionary.com/v0/define?term=" + URLEncoder.encode(beheadedSplit[0], "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
	        String json = Utils.instance().restyGetObjectFromUrl(url, evt);
	        JSONObject jObject = new JSONObject(json);
	        JSONArray data = jObject.getJSONArray("list");
	        for(int i = 0; i < data.length(); i++){ //Loop though the JSON
	        	JSONObject entry = data.getJSONObject(i);
	        	//Get the definition from the JSON.
	            definitions.add(entry.getString("definition"));
	            thumbsup.add(entry.get("thumbs_up").toString()); //int -> String
	            thumbsdown.add(entry.get("thumbs_down").toString()); //int -> String
	            urls.add(entry.getString("permalink"));
	        }
	        long end = System.currentTimeMillis() - start;
			switch (beheadedSplit.length)
			{
			case 1: 
				embed.setTitle("Urban Dictionary definition for " + beheadedMessage)
					.setDescription("Main definition.")
					.setThumbnail("https://everythingfat.files.wordpress.com/2013/01/ud-logo.jpg")
					.setUrl(urls.get(0))
					.setColor(Color.GREEN)
					.addField("Definition", definitions.get(0), false)
					.addField("Thumbs up", thumbsup.get(0), true)
					.addField("Thumbs down", thumbsdown.get(0), true)
					.setFooter("Information by Urban Dictionary (Process time: " + end + "ms)", null);
				channel.sendMessage(embed.build()).queue();
				break;
			case 2: 
				int defn = Integer.parseInt(beheadedSplit[1]) - 1;
				String defns = String.valueOf(defn+1);
				embed.setTitle("Urban Dictionary definition for " + beheadedSplit[0])
					.setThumbnail("https://everythingfat.files.wordpress.com/2013/01/ud-logo.jpg")
					.setDescription("Definition " + defns)
					.setColor(Color.PINK)
					.setUrl(urls.get(defn))					
					.addField("Definition", definitions.get(defn), false)
					.addField("Thumbs up", thumbsup.get(defn), true)
					.addField("Thumbs down", thumbsdown.get(defn), true)
					.setFooter("Information by Urban Dictionary", null);
				channel.sendMessage(embed.build()).queue();
				break;
			}
		}
	}
}

