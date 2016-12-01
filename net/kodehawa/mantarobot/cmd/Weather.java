package net.kodehawa.mantarobot.cmd;

import java.awt.Color;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.text.DecimalFormat;

import org.json.JSONArray;
import org.json.JSONObject;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.util.Utils;

public class Weather extends Command {

	private String APP_ID = "e2abde2e6ca69e90a73ddb43199031de";
	
	public Weather()
	{
		setName("weather");
		setDescription("Retrieves weather from a city in the form of an embed message.");
		setCommandType("user");
	}
	
	@Override
	public void onCommand(String[] split, String content, MessageReceivedEvent event) {
        channel = event.getChannel();
        EmbedBuilder embed = new EmbedBuilder();

		if(!content.isEmpty()){
			 try {
				 long start = System.currentTimeMillis();
		         //Get a parsed JSON.
				 String url = Utils.instance().getObjectFromUrl("http://api.openweathermap.org/data/2.5/weather?q=" + URLEncoder.encode(content, "UTF-8") + "&appid="+ APP_ID, event);
		         String json = url;
		         JSONObject jObject = new JSONObject(json);
		         //Get the object as a array.
		         JSONArray data = jObject.getJSONArray("weather");
		         String status = null;
		         for(int i = 0; i < data.length(); i++) { 
		        	 JSONObject entry = data.getJSONObject(i);
		             status = entry.getString("main"); //Used for weather status.
		         }
		         
		         //Round the decimal up. Also if decimal place is 0 truncate it.
		         DecimalFormat dFormat = new DecimalFormat();
		         dFormat.setRoundingMode(RoundingMode.UP);
		         
		         //Get the needed JSON Objects.
		         JSONObject jMain = jObject.getJSONObject("main"); //Used for temperature and humidity.
		         JSONObject jWind = jObject.getJSONObject("wind"); //Used for wind speed.
		         JSONObject jClouds = jObject.getJSONObject("clouds"); //Used for cloudiness.
				 long end = System.currentTimeMillis() - start;

		         String temp = dFormat.format(Double.parseDouble(jMain.get("temp").toString())); //Temperature in Kelvin.
		         String hum = dFormat.format(Double.parseDouble(jMain.get("humidity").toString())); //Humidity in percentage.
		         String ws = dFormat.format(Double.parseDouble(jWind.get("speed").toString())); //Speed in m/h.
		         String clness = dFormat.format(Double.parseDouble(jClouds.get("all").toString())); //Cloudiness in percentage.
		         
		         //Simple math formulas to convert from universal to metric and imperial.
		         String finalTemperatureCelcius = dFormat.format(Double.parseDouble(temp) - 273.15); //Temperature in Celcius degrees.
		         String finalTemperatureFarnheit = dFormat.format(Double.parseDouble(temp) * 9/5 - 459.67); //Temperature in Farnheit degrees.
		         String finalWindSpeedMetric = dFormat.format(Double.parseDouble(ws) * 3.6); //wind speed in km/h.
		         String finalWindSpeedImperial = dFormat.format(Double.parseDouble(ws) / 0.447046); //wind speed in mph.

		         embed.setColor(Color.CYAN)
		         	.setTitle("Forecast information for " + content) //For which city
		         	.setDescription(status + " (" + clness + "% cloudiness)") //Clouds, sunny, etc and cloudiness.
		         	.addField("Temperature", finalTemperatureCelcius + "°C/" + finalTemperatureFarnheit + "°F", true)
		         	.addField("Humidity", hum + "%" , true)
		         	.addField("Wind Speed", finalWindSpeedMetric + "km/h / " + finalWindSpeedImperial + "mph" , false)
		         	.setFooter("Information provided by OpenWeatherMap (Process time: " + end + "ms)", null);
		         //Build the embed message and send it.
		         channel.sendMessage(embed.build()).queue();
			 }
		     catch(Exception e){
		    	 e.printStackTrace();
		     }
		}
	}
}
