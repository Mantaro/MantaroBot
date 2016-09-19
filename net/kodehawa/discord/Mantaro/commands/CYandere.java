package net.kodehawa.discord.Mantaro.commands;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;


public class CYandere implements Command {
	
	
	@Override
	@ModuleProperties(level = "user", name = "yandere", type = "special", description = "Gets an image from yande.re. ~>yandere help for more details on how to use it. This command can be only used in NSFW channels!",
			additionalInfo = "Possible args: get/tags/help", takesArgs = true)
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {

		int limit = Integer.parseInt(msg[2]);
		int page = Integer.parseInt(msg[1]);
		String tagsToEncode = msg[3];
		String tagsEncoded = "";
		String yandereUrl2;
		
		try 
		{
			tagsEncoded = URLEncoder.encode(tagsToEncode, "UTF-8");
		} 
		catch (UnsupportedEncodingException e1)
		{
			e1.printStackTrace();
		}
				
		if(beheaded.startsWith("get"))
		{		
			CopyOnWriteArrayList<String> urls = new CopyOnWriteArrayList<String>();
			
			if(limit > 60 ) limit = 60;
			try 
			{
				URL yandereUrl = new URL(String.format("https://yande.re/post.json?limit=%1s&page=%2s", String.valueOf(limit), String.valueOf(page)).replace(" ", ""));
	            HttpURLConnection yandere1 = (HttpURLConnection) yandereUrl.openConnection();
	            System.out.println(String.format("https://yande.re/post.json?limit=%1s&page=%2s", String.valueOf(limit), String.valueOf(page)).replace(" ", ""));
	            InputStream inputstream = yandere1.getInputStream();

	            yandereUrl2 = CharStreams.toString(new InputStreamReader(inputstream, Charsets.UTF_8));
	            				
		        JSONArray data = new JSONArray(yandereUrl2);
		         
		        for(int i = 0; i < data.length(); i++) 
	            {
	                JSONObject entry = data.getJSONObject(i);
	               
		            urls.add(entry.getString("file_url"));
	            }
		        
		        int woah = 1;
		        try{
		        	woah = Integer.parseInt(msg[3]);
		        }
		        catch(Exception e) { }
		        
		        List<TextChannel> array = evt.getChannel().getJDA().getTextChannels();
		        boolean trigger = false;
		        
		        for(TextChannel t : array)
		        {
		        	if(t.getName().contains("lewd") | t.getName().contains("nsfw") | t.getName().contains("nether") && t.getId() == evt.getChannel().getId())
		        	{
		        		trigger = true;
		        		break;
		        	}
		        }
		        
		        if(trigger)
		        {
					evt.getChannel().sendMessageAsync("I found an image! You can get a total of " + urls.size() + " images :3\r" + urls.get(woah - 1) , null);
		        }
		        else
		        {
		        	evt.getChannel().sendMessageAsync("You only can use this command in nsfw channels!", null);
		        }
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
				evt.getChannel().sendMessageAsync("Something went wrong when fetching the image :c", null);
			}
		}
		
		else if(beheaded.startsWith("tags"))
		{
			CopyOnWriteArrayList<String> urls = new CopyOnWriteArrayList<String>();
			
			if(limit > 60 ) limit = 60;
			try 
			{
				URL yandereUrl = new URL(String.format("https://yande.re/post.json?limit=%1s&page=%2s&tags=%3s",
						String.valueOf(limit), String.valueOf(page), tagsEncoded).replace(" ", ""));
	            HttpURLConnection yandere1 = (HttpURLConnection) yandereUrl.openConnection();
	            InputStream inputstream = yandere1.getInputStream();

	            yandereUrl2 = CharStreams.toString(new InputStreamReader(inputstream, Charsets.UTF_8));
	            				
		        JSONArray data = new JSONArray(yandereUrl2);
		        		        
		        for(int i = 0; i < data.length(); i++) 
	            {
	                JSONObject entry = data.getJSONObject(i);
		            urls.add(entry.getString("file_url"));
	            }
		        int woah = 1;
		        try{
		        	woah = Integer.parseInt(msg[4]);
		        }
		        catch(Exception e) { }
		        	
		        List<TextChannel> array = evt.getChannel().getJDA().getTextChannels();
		        boolean trigger = false;
		        
		        for(TextChannel t : array)
		        {
		        	if(t.getName().contains("lewd") | t.getName().contains("nsfw") | t.getName().contains("nether") && t.getId() == evt.getChannel().getId())
		        	{
		        		trigger = true;
		        		break;
		        	}
		        }
		        
		        if(trigger)
		        {
					evt.getChannel().sendMessageAsync("I found an image!" + " with the tag **" + msg[3] + "**. You can get a total of **" 
							+ urls.size() + "** images :3\r" + urls.get(woah - 1) , null);
		        }
		        else
		        {
		        	evt.getChannel().sendMessageAsync("You only can use this command in nsfw channels!", null);
		        }
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
				evt.getChannel().sendMessageAsync("Something went wrong when fetching the image :c", null);
			} 
		}
		
		else if(beheaded.startsWith("help"))
		{
			evt.getChannel().sendMessageAsync(
					"```"
					+ "~>yandere get page limit imagenumber gets you an image.\r"
					+ "~>yandere tags page limit tag imagenumber gets you an image with the respective tag.\r This command can be only used in NSFW channels!```"
					, null);
		}
		
		else
		{
			evt.getChannel().sendMessageAsync("```Wrong usage. Use ~>konachan help to get help.```", null);;
			
		}
	}

}
