package net.kodehawa.discord.Mantaro.commands;

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

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;

public class CUrbanDictionary implements Command {

	@Override
	@ModuleProperties(level = "user", name = "urban", type = "common", description = "Gets a definition from Urban Dictionary.")
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		
		String beheadedSplit[] = beheaded.split(":");
		
		if(!beheaded.isEmpty())
		{
    		ArrayList<String> definitions = new ArrayList<String>();

			 try {
				 
				 URL dictionary = new URL("http://api.urbandictionary.com/v0/define?term=" + URLEncoder.encode(beheadedSplit[0], "UTF-8"));

		         HttpURLConnection urban = (HttpURLConnection) dictionary.openConnection();
		         InputStream inputstream = urban.getInputStream();
		         
		         String json = CharStreams.toString(new InputStreamReader(inputstream, Charsets.UTF_8));
		            
		         JSONObject jObject = new JSONObject(json);
		         JSONArray data = jObject.getJSONArray("list");
		            
		         for(int i = 0; i < data.length(); i++)
		         {
		        	 JSONObject entry = data.getJSONObject(i);
		             definitions.add(entry.getString("definition"));
		         }
		            
		         inputstream.close();
			 }
		     catch(Exception e)
		     {
		    	 e.printStackTrace();
		     }

			 switch (beheadedSplit.length)
			 {
			 case 1: evt.getChannel().sendMessageAsync("Top definition for **" + beheaded + "** is\r" + definitions.get(0), null); break;
			 case 2: evt.getChannel().sendMessageAsync("Definition N° " + beheadedSplit[1] + " for **" + beheadedSplit[0] + "** is\r" + definitions.get(Integer.parseInt(beheadedSplit[1])), null); break;
			 }
		}
       
	}
}

