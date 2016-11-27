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

import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;

public class Yandere extends Command {
	
	public Yandere()
	{
		setName("yandere");
		setDescription("Fetches images from yande.re. For more detailed information use this command with the help argument.");
		setCommandType("user");
	}

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

		try{
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
		
		try {
			tagsEncoded = URLEncoder.encode(tagsToEncode, "UTF-8");
		} catch (UnsupportedEncodingException e1){
			e1.printStackTrace();
		}
				
        String noArgs = beheadedMessage.split(" ")[0];
		switch(noArgs){
		case "get":
			CopyOnWriteArrayList<String> urls = new CopyOnWriteArrayList<String>();
			
			if(limit > 60 ) limit = 60;
			try {
				URL yandereUrl = new URL(String.format("https://yande.re/post.json?limit=%1s&page=%2s", String.valueOf(limit), String.valueOf(page)).replace(" ", ""));
	            HttpURLConnection yandereConnection = (HttpURLConnection) yandereUrl.openConnection();
	            InputStream inputstream = yandereConnection.getInputStream();
	            yandereUrlParsed = CharStreams.toString(new InputStreamReader(inputstream, Charsets.UTF_8));
		        JSONArray fetchedData = new JSONArray(yandereUrlParsed);
		         
		        for(int i = 0; i < fetchedData.length(); i++)  {
		        	JSONObject entry = fetchedData.getJSONObject(i);
		        	if(entry.getString("rating").equals(rating)){
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
		        
		        for(MessageChannel ch : array) {
		        	if(ch.getName().contains("lewd") | ch.getName().contains("nsfw") | ch.getName().contains("nether") && ch.getId() == channel.getId())
		        	{
		        		trigger = true;
		        		break;
		        	}
		        	else if(rating.equals("s")){
		        		trigger = true;
		        	}
		        }
		        
		        if(trigger) {
		        	String s = String.format(":thumbsup: " + "I found an image! You can get a total of %1s images :3\r %2s" , urls.size(), urls.get(get - 1));
					channel.sendMessage(s).queue();
		        }
		        else{
		        	channel.sendMessage(":heavy_multiplication_x: " + "You only can use this command in nsfw channels!").queue();
		        }
			} 
			catch (IOException e) {
				e.printStackTrace();
				channel.sendMessage(":heavy_multiplication_x: " + "Something went wrong when fetching the image :c").queue();
			}
			break;
		case "tags":
			CopyOnWriteArrayList<String> url = new CopyOnWriteArrayList<String>();
			
			if(limit > 60 ) limit = 60;
			try 
			{
				URL yandereUrl = new URL(String.format("https://yande.re/post.json?limit=%1s&page=%2s&tags=%3s",
				String.valueOf(limit), String.valueOf(page), tagsEncoded).replace(" ", ""));
	            HttpURLConnection yandereConnection = (HttpURLConnection) yandereUrl.openConnection();
	            InputStream inputstream = yandereConnection.getInputStream();
	            yandereUrlParsed = CharStreams.toString(new InputStreamReader(inputstream, Charsets.UTF_8));
		        JSONArray fetchedData = new JSONArray(yandereUrlParsed);
		        		        
		        for(int i = 0; i < fetchedData.length(); i++)  {
	                JSONObject entry = fetchedData.getJSONObject(i);
	                if(entry.getString("rating").equals(rating)){
			            url.add(entry.getString("file_url"));
	                }
	            }
		        int get = 1;
		        try{
		        	get = Integer.parseInt(message[4]);
		        }
		        catch(Exception e) { }
		        	
		        List<TextChannel> array = channel.getJDA().getTextChannels();
		        boolean trigger = false;
		        
		        for(TextChannel ch : array) {
		        	//Totally not hardcoded in
		        	if(ch.getName().contains("lewd") | ch.getName().contains("nsfw") | ch.getName().contains("nether") && ch.getId().equals(channel.getId())){
		        		trigger = true;
		        		break;
		        	}
		        }
		        
		        if(trigger) {
		        	String s = String.format(":thumbsup: " + "I found an image! with the tag **1s**. You can get a total of **%2s** images <3\r %3s", message[3], url.size(), url.get(get - 1));
					channel.sendMessage(s).queue();
		        }
		        else{
		        	channel.sendMessage(":heavy_multiplication_x: " +  "You only can use this command in nsfw channels!").queue();
		        }
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
				channel.sendMessage(":heavy_multiplication_x: " + "Something went wrong when fetching the image :c").queue();
			} 
			break;
		case "help":
			channel.sendMessage(
					"```"
					+ "~>yandere <gets you a completely random image.<\r"
					+ "~>yandere get page limit (imgnumber rating) <gets you an image.> (image number and rating is optional) \r"
					+ "~>yandere tags page limit tag (imagenumber rating) <gets you an image with the respective tag. (image number and rating is optional)\rThis command can be only used in NSFW channels! (Unless rating has been specified as safe)```"
					).queue();
			break;
		case "":
			CopyOnWriteArrayList<String> urls2 = new CopyOnWriteArrayList<String>();
			try {
				Random r = new Random();
				int randomPage = r.nextInt(4);
				URL yandereUrl = new URL(String.format("https://yande.re/post.json?limit=%1s&page=%2s",
						"60", String.valueOf(randomPage)).replace(" ", ""));
	            HttpURLConnection yandereConnection = (HttpURLConnection) yandereUrl.openConnection();
	            InputStream inputstream = yandereConnection.getInputStream();

	            yandereUrlParsed = CharStreams.toString(new InputStreamReader(inputstream, Charsets.UTF_8));
	            				
		        JSONArray fetchedData = new JSONArray(yandereUrlParsed);
		        		        
		        for(int i = 0; i < fetchedData.length(); i++) {
	                JSONObject entry = fetchedData.getJSONObject(i);
	                if(entry.getString("rating").equals("e") | entry.getString("rating").equals("q")){
			            urls2.add(entry.getString("file_url"));
	            	}
	            }
		        
		        List<TextChannel> array = channel.getJDA().getTextChannels();
		        boolean trigger = false;
		        
		        for(TextChannel ch : array){
		        	if(ch.getName().contains("lewd") | ch.getName().contains("nsfw") | ch.getName().contains("nether") && ch.getId() == channel.getId()){
		        		trigger = true;
		        		break;
		        	}
		        }
		        
		        if(trigger) {
		        	int randomImage = r.nextInt(urls2.size());
			        channel.sendMessage(":thumbsup: " +  "I found an image!\r" + urls2.get(randomImage) ).queue();
		        } else{
		        	channel.sendMessage(":heavy_multiplication_x: " +  "You only can use this command in nsfw channels!").queue();
		        }
			}
			catch(Exception e){
				e.printStackTrace();
				channel.sendMessage(":heavy_multiplication_x: " + "Something went wrong when fetching the image :c").queue();
			}
			break;
		default: 	
			channel.sendMessage(":heavy_multiplication_x: " + "```Wrong usage. Use ~>yandere help to get help.```").queue();
			break;
		}
	}
}