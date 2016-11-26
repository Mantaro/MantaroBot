package net.kodehawa.mantarobot.cmd;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONObject;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;
import us.monoid.web.Resty;

public class Translator extends Command {

	private Resty resty = new Resty();
	
	public Translator()
	{
		setName("translate");
	}
	
	@Override
	/**
	 * I mean... it works, but google blocks you after a while.
	 */
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
        channel = evt.getChannel();
        
		String sourceLang = message[0];
		String targetLang = message[1];
		String textToEncode = beheadedMessage.replace(message[0] + " " + message[1] + " ", "");
		String textEncoded = "";
		String translatorUrl2;
		
		try 
		{
			textEncoded = URLEncoder.encode(textToEncode, "UTF-8");
		} 
		
		catch (UnsupportedEncodingException e1)
		{
			e1.printStackTrace();
		}
		
		String translatorUrl = String.format("https://translate.google.com/translate_a/single?client=at&dt=t&dt=ld&dt=qca&dt=rm&dt=bd&dj=1&hl=es-ES&ie=UTF-8&oe=UTF-8&inputm=2&otf=2&iid=1dd3b944-fa62-4b55-b330-74909a99969e&sl=%1s&tl=%2s&dt=t&q=%3s", sourceLang, targetLang, textEncoded);
        
		try 
		{
			resty.identifyAsMozilla();
			translatorUrl2 = resty.text(translatorUrl).toString();
			
			JSONObject jObject = new JSONObject(translatorUrl2);
	        JSONArray data = jObject.getJSONArray("sentences");
	         
	        for(int i = 0; i < data.length(); i++) 
            {
                JSONObject entry = data.getJSONObject(i);
                System.out.println(entry);
                channel.sendMessage("Translation for " + textToEncode +": " + entry.getString("trans")).queue();
            }
	        
			System.out.println(translatorUrl2);		
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			channel.sendMessage("Something went wrong when translating... :c").queue();
		}
	}
}
