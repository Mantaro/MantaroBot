package net.kodehawa.discord.Mantaro.commands;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONObject;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.kodehawa.discord.Mantaro.annotation.ModuleProperties;
import net.kodehawa.discord.Mantaro.main.Command;
import us.monoid.web.Resty;

public class CTranslator implements Command {

	private Resty resty = new Resty();
	
	@Override
	@ModuleProperties(level = "user", name = "translate", type = "utils", description = "Translates a sentence.", additionalInfo = "You need to use the code for the " + 
	"language. For example for english to french you need to type ~>translate en fr Hello")
	public boolean isAvaliable(String[] argsMain, MessageReceivedEvent evt) {
		return true;
	}

	@Override
	/**
	 * I mean... it works, but google blocks you after a while.
	 */
	public void botAction(String[] msg, String whole, String beheaded, MessageReceivedEvent evt) {
		
		String sourceLang = msg[0];
		String targetLang = msg[1];
		String textToEncode = beheaded.replace(msg[0] + " " + msg[1] + " ", "");
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
                evt.getChannel().sendMessageAsync("Translation for " + textToEncode +": " + entry.getString("trans"), null);
            }
	        
			System.out.println(translatorUrl2);		
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			evt.getChannel().sendMessageAsync("Something went wrong when translating... :c", null);
		}
	}
}
