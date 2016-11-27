package net.kodehawa.mantarobot.cmd;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;

public class UrbanDict extends Command {

	public UrbanDict()
	{
		setName("urban");
		setDescription("Retrieves definitions from urban dictionary. Usage example: ~>urban Otaku");
		setCommandType("user");
	}

	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
		//Initialize the variables I need to use.
        channel = evt.getChannel();
        //First split is definition, second one is number. I would use space but we need the hability to fetch with spaces too.
		String beheadedSplit[] = beheadedMessage.split(":");
		
		if(!beheadedMessage.isEmpty()){
    		ArrayList<String> definitions = new ArrayList<String>(); //Will use later to store definitions.
			 try {
				 //The UrbanDictionary URL needs to be fetched, with the respective arguments.
				 URL dictionary = new URL("http://api.urbandictionary.com/v0/define?term=" + URLEncoder.encode(beheadedSplit[0], "UTF-8"));
				 //Open a connection to the URL.
		         HttpURLConnection urban = (HttpURLConnection) dictionary.openConnection();
		         InputStream inputstream = urban.getInputStream();
		         //Fetch the JSON that I get. It has multiple definitions.
		         String json = CharStreams.toString(new InputStreamReader(inputstream, Charsets.UTF_8));
		         JSONObject jObject = new JSONObject(json);
		         //Get the object as a list.
		         JSONArray data = jObject.getJSONArray("list");
		         for(int i = 0; i < data.length(); i++) //Loop though the JSON
		         {
		        	 JSONObject entry = data.getJSONObject(i);
		        	 //Get the definition from the JSON.
		             definitions.add(entry.getString("definition"));
		         }
		         inputstream.close();
			 } catch(Exception e) {
		    	 e.printStackTrace();
		     }
			 
			 switch (beheadedSplit.length)
			 {
			 //If you provide a definition number, get that number, if you don't, get default.
			 case 1: channel.sendMessage(":speech_balloon: " + "Top definition for **" + beheadedMessage + "** is\r" + definitions.get(0)).queue(); break;
			 case 2: channel.sendMessage(":speech_balloon: " + "Definition N° " + beheadedSplit[1] + " for **" + beheadedSplit[0] + "** is\r" + definitions.get(Integer.parseInt(beheadedSplit[1]))).queue(); break;
			 }
		}
       
	}
}

