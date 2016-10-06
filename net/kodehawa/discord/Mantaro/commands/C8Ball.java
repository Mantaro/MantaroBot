package net.kodehawa.discord.Mantaro.commands;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.json.JSONObject;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;
import us.monoid.web.Resty;

public class C8Ball implements Command {

	@Override
	@ModuleProperties(level = "user", name = "8ball", type = "fun", description = "Fetches an answer from 8ball.")
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		Resty resty = new Resty();

		String question = beheaded;
		String textEncoded = "";
		String url2;
		
		try 
		{
			textEncoded = URLEncoder.encode(question, "UTF-8");
		} 
		
		catch (UnsupportedEncodingException e1)
		{
			e1.printStackTrace();
		}
		
		String URL = String.format("https://8ball.delegator.com/magic/JSON/%1s", textEncoded);
        
		try 
		{
			resty.identifyAsMozilla();
			url2 = resty.text(URL).toString();
			
			JSONObject jObject = new JSONObject(url2);
	        JSONObject data = jObject.getJSONObject("magic");
	        
            evt.getChannel().sendMessageAsync(data.getString("answer") + ".", null);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			evt.getChannel().sendMessageAsync("Something went wrong when getting 8Ball reply... :c", null);
		}
	}

}
