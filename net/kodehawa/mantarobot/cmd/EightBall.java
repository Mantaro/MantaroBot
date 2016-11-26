package net.kodehawa.mantarobot.cmd;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.json.JSONObject;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;
import us.monoid.web.Resty;

public class EightBall extends Command {

	public EightBall()
	{
		setName("8ball");
		setDescription("Retrieves answer from 8Ball. Requires a sentence.");
	}
	
	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
		Resty resty = new Resty();

		String question = beheadedMessage;
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
	        
            channel.sendMessage(":speech_balloon: " + data.getString("answer") + ".").queue();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			channel.sendMessage(":heavy_multiplication_x:" + "Something went wrong when getting 8Ball reply... :c").queue();
		}
	}

}
