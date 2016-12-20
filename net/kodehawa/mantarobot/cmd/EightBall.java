package net.kodehawa.mantarobot.cmd;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.json.JSONObject;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.management.Command;
import net.kodehawa.mantarobot.util.Utils;

public class EightBall extends Command {

	public EightBall(){
		setName("8ball");
		setDescription("Retrieves an answer from 8Ball. Requires a sentence.");
		setExtendedHelp("Retrieves an answer from 8Ball. Requires a sentence.\n"
						+ "~>8ball [question]. Retrieves an answer from 8ball based on the question provided.");
		setCommandType("user");
	}
	
	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
		if(beheadedMessage.isEmpty()){
			String textEncoded = "";
			String url2;
			
			try {
				textEncoded = URLEncoder.encode(beheadedMessage, "UTF-8");
			} catch (UnsupportedEncodingException ignored){} //Shouldn't fail.
			
			String URL = String.format("https://8ball.delegator.com/magic/JSON/%1s", textEncoded);
			url2 = Utils.instance().restyGetObjectFromUrl(URL, evt);
				
			JSONObject jObject = new JSONObject(url2);
		    JSONObject data = jObject.getJSONObject("magic");
		        
	        channel.sendMessage(":speech_balloon: " + data.getString("answer") + ".").queue();
		} else{
			channel.sendMessage(getExtendedHelp()).queue();
		}
	} 
}
