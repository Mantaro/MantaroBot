package net.kodehawa.mantarobot.cmd;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;

public class Yandere extends Command {
	
	public Yandere()
	{
		setName("yandere");
	}
	/**
	 * Holy shit. 
	 */
	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
		guild = evt.getGuild();
        author = evt.getAuthor();
        channel = evt.getChannel();
        receivedMessage = evt.getMessage();
        
        int limit = 0;
		int page = 0;
		String tagsToEncode = "no";
		String rating = "e";

		try
		{
			page = Integer.parseInt(message[1]);
			limit = Integer.parseInt(message[2]);
			tagsToEncode = message[3];
			rating = message[4];
			
			if(rating.equals("safe")){ rating = "s"; }
			if(rating.equals("questionable")){ rating = "q"; }
			if(rating.equals("explicit")){ rating = "e"; }

		}
		catch(Exception e){}
		String tagsEncoded = "";
		String yandereUrlParsed;
	
		
		try 
		{
			tagsEncoded = URLEncoder.encode(tagsToEncode, "UTF-8");
		} 
		catch (UnsupportedEncodingException e1)
		{
			e1.printStackTrace();
		}
				
		if(beheadedMessage.startsWith("get"))
		{		
			CopyOnWriteArrayList<String> urls = new CopyOnWriteArrayList<String>();
			
			if(limit > 60 ) limit = 60;
			try 
			{
				URL yandereUrl = new URL(String.format("https://yande.re/post.json?limit=%1s&page=%2s", String.valueOf(limit), String.valueOf(page)).replace(" ", ""));
	            HttpURLConnection yandereConnection = (HttpURLConnection) yandereUrl.openConnection();
	            InputStream inputstream = yandereConnection.getInputStream();

	            yandereUrlParsed = CharStreams.toString(new InputStreamReader(inputstream, Charsets.UTF_8));
	            				
		        JSONArray fetchedData = new JSONArray(yandereUrlParsed);
		         
		        for(int i = 0; i < fetchedData.length(); i++) 
	            {
	                JSONObject entry = fetchedData.getJSONObject(i);
	               if(entry.getString("rating").equals(rating))
	            	{
			            urls.add(entry.getString("file_url"));
	            	}
	            }
		        
		        int get = 1;
		        try{
		        	get = Integer.parseInt(message[3]);
		        }
		        catch(Exception e) { }
		        
		        List<TextChannel> array = channel.getJDA().getTextChannels();
		        boolean trigger = false;
		        
		        for(TextChannel channel : array)
		        {
		        	if(channel.getName().contains("lewd") | channel.getName().contains("nsfw") | channel.getName().contains("nether") && channel.getId() == channel.getId())
		        	{
		        		trigger = true;
		        		break;
		        	}
		        	else if(rating.equals("s"))
		        	{
		        		trigger = true;
		        	}
		        }
		        
		        if(trigger)
		        {
					channel.sendMessage(":thumbsup: " + "I found an image! You can get a total of " + urls.size() + " images :3\r" + urls.get(get - 1) ).queue();
		        }
		        else
		        {
		        	channel.sendMessage(":heavy_multiplication_x: " + "You only can use this command in nsfw channels!").queue();
		        }
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
				channel.sendMessage(":heavy_multiplication_x: " + "Something went wrong when fetching the image :c").queue();
			}
		}
		
		else if(beheadedMessage.startsWith("tags"))
		{
			CopyOnWriteArrayList<String> urls = new CopyOnWriteArrayList<String>();
			
			if(limit > 60 ) limit = 60;
			try 
			{
				URL yandereUrl = new URL(String.format("https://yande.re/post.json?limit=%1s&page=%2s&tags=%3s",
						String.valueOf(limit), String.valueOf(page), tagsEncoded).replace(" ", ""));
	            HttpURLConnection yandereConnection = (HttpURLConnection) yandereUrl.openConnection();
	            InputStream inputstream = yandereConnection.getInputStream();

	            yandereUrlParsed = CharStreams.toString(new InputStreamReader(inputstream, Charsets.UTF_8));
	            				
		        JSONArray fetchedData = new JSONArray(yandereUrlParsed);
		        		        
		        for(int i = 0; i < fetchedData.length(); i++) 
	            {
	                JSONObject entry = fetchedData.getJSONObject(i);
	                if(entry.getString("rating").equals(rating))
	                {
			            urls.add(entry.getString("file_url"));
	                }
	            }
		        int get = 1;
		        try{
		        	get = Integer.parseInt(message[4]);
		        }
		        catch(Exception e) { }
		        	
		        List<TextChannel> array = channel.getJDA().getTextChannels();
		        boolean trigger = false;
		        
		        for(TextChannel channel : array)
		        {
		        	if(channel.getName().contains("lewd") | channel.getName().contains("nsfw") | channel.getName().contains("nether") && channel.getId() == channel.getId())
		        	{
		        		trigger = true;
		        		break;
		        	}
		        }
		        
		        if(trigger)
		        {
					channel.sendMessage(":thumbsup: " + "I found an image!" + " with the tag **" + message[3] + "**. You can get a total of **" 
							+ urls.size() + "** images <3\r" + urls.get(get - 1) ).queue();
		        }
		        else
		        {
		        	channel.sendMessage(":heavy_multiplication_x: " +  "You only can use this command in nsfw channels!").queue();
		        }
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
				channel.sendMessage(":heavy_multiplication_x: " + "Something went wrong when fetching the image :c").queue();
			} 
		}
		
		else if(beheadedMessage.startsWith("help"))
		{
			channel.sendMessage(
					"```"
					+ "~>yandere <gets you a completely random image.<\r"
					+ "~>yandere get page limit (imgnumber rating) <gets you an image.> (image number and rating is optional) \r"
					+ "~>yandere tags page limit tag (imagenumber rating) <gets you an image with the respective tag. (image number and rating is optional)\rThis command can be only used in NSFW channels! (Unless rating has been specified as safe)```"
					).queue();
		}
		
		else if(beheadedMessage.isEmpty())
		{
			CopyOnWriteArrayList<String> urls = new CopyOnWriteArrayList<String>();
			try 
			{
				Random r = new Random();
				int randomPage = r.nextInt(4);
				URL yandereUrl = new URL(String.format("https://yande.re/post.json?limit=%1s&page=%2s",
						"60", String.valueOf(randomPage)).replace(" ", ""));
	            HttpURLConnection yandereConnection = (HttpURLConnection) yandereUrl.openConnection();
	            InputStream inputstream = yandereConnection.getInputStream();

	            yandereUrlParsed = CharStreams.toString(new InputStreamReader(inputstream, Charsets.UTF_8));
	            				
		        JSONArray fetchedData = new JSONArray(yandereUrlParsed);
		        		        
		        for(int i = 0; i < fetchedData.length(); i++) 
	            {
	                JSONObject entry = fetchedData.getJSONObject(i);
	                if(entry.getString("rating").equals("e") | entry.getString("rating").equals("q"))
	            	{
			            urls.add(entry.getString("file_url"));
	            	}
	            }
		        
		        List<TextChannel> array = channel.getJDA().getTextChannels();
		        boolean trigger = false;
		        
		        for(TextChannel channel : array)
		        {
		        	if(channel.getName().contains("lewd") | channel.getName().contains("nsfw") | channel.getName().contains("nether") && channel.getId() == channel.getId())
		        	{
		        		trigger = true;
		        		break;
		        	}
		        }
		        
		        if(trigger)
		        {
		        	int randomImage = r.nextInt(urls.size());
			        channel.sendMessage(":thumbsup: " +  "I found an image!\r" + urls.get(randomImage) ).queue();
		        }
			}
			catch(Exception e)
			{
				e.printStackTrace();
				channel.sendMessage(":heavy_multiplication_x: " + "Something went wrong when fetching the image :c").queue();
			}
		}		
		else
		{
			channel.sendMessage(":heavy_multiplication_x: " + "```Wrong usage. Use ~>yandere help to get help.```").queue();
		}
		
	}
}
