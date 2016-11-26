package net.kodehawa.mantarobot.cmd;

import java.awt.Color;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;

public class Weather extends Command {

	private String APP_ID = "e2abde2e6ca69e90a73ddb43199031de";
	
	public Weather()
	{
		setName("weather");
		setDescription("Retrieves weather from a city in the form of an embed message.");
	}
	
	@Override
	public void onCommand(String[] split, String content, MessageReceivedEvent event) {
        channel = event.getChannel();
		
		if(!content.isEmpty()){
			 try {
				 URL weather = new URL("http://api.openweathermap.org/data/2.5/weather?q=" + URLEncoder.encode(content, "UTF-8") + "&appid="+ APP_ID);
		         HttpURLConnection weatherConnection = (HttpURLConnection) weather.openConnection();
		         InputStream inputstream = weatherConnection.getInputStream();
		         //Get a parsed JSON.
		         String json = CharStreams.toString(new InputStreamReader(inputstream, Charsets.UTF_8));
		            
		         JSONObject jObject = new JSONObject(json);
		         //Get the object as a list.
		         JSONArray weatherData = jObject.getJSONArray("weather");
		         String status = null;
		         for(int i = 0; i < weatherData.length(); i++) { 
		        	 JSONObject entry = weatherData.getJSONObject(i);
		             status = entry.getString("main"); //Used for weather status.
		         }
		         
		         //Round the decimal up. Also if decimal place is 0 truncate it.
		         DecimalFormat dFormat = new DecimalFormat();
		         dFormat.setRoundingMode(RoundingMode.UP);
		         
		         //Get the needed JSON Objects.
		         JSONObject jObjectMain = jObject.getJSONObject("main"); //Used for temperature and humidity.
		         JSONObject jObjectWind = jObject.getJSONObject("wind"); //Used for wind speed.
		         JSONObject jObjectClouds = jObject.getJSONObject("clouds"); //Used for cloudiness.
		         
		         String temperature = dFormat.format(Double.parseDouble(jObjectMain.get("temp").toString())); //Temperature in Kelvin.
		         String humidity = dFormat.format(Double.parseDouble(jObjectMain.get("humidity").toString())); //Humidity in percentage.
		         String windSpeed = dFormat.format(Double.parseDouble(jObjectWind.get("speed").toString())); //Speed in m/h.
		         String cloudiness = dFormat.format(Double.parseDouble(jObjectClouds.get("all").toString())); //Cloudiness in percentage.
		         
		         //Simple math formulas to convert from universal to metric and imperial.
		         String finalTemperatureCelcius = dFormat.format(Double.parseDouble(temperature) - 273.15); //Temperature in Celcius degrees.
		         String finalTemperatureFarnheit = dFormat.format(Double.parseDouble(temperature) * 9/5 - 459.67); //Temperature in Farnheit degrees.
		         String finalWindSpeedMetric = dFormat.format(Double.parseDouble(windSpeed) * 3.6); //wind speed in km/h.
		         String finalWindSpeedImperial = dFormat.format(Double.parseDouble(windSpeed) / 0.447046); //wind speed in mph.

		         EmbedBuilder embed = new EmbedBuilder();
		         embed.setColor(Color.CYAN);
		         embed.setTitle("Forecast information for " + content); //For which city
		         embed.setDescription(status + " (" + cloudiness + "% cloudiness)"); //Clouds, sunny, etc and cloudiness.
		         embed.addField("Temperature", finalTemperatureCelcius + "°C/" + finalTemperatureFarnheit + "°F", false);
		         embed.addField("Humidity", humidity + "%" , false);
		         embed.addField("Wind Speed", finalWindSpeedMetric + "km/h / " + finalWindSpeedImperial + "mph" , false);
		         embed.setFooter("Information provided by OpenWeatherMap", null);
		            
		         //Build the embed message and send it.
		         channel.sendMessage(embed.build()).queue();
		         
		         //Close the connection.
		         inputstream.close();
			 }
		     catch(Exception e){
		    	 e.printStackTrace();
		     }
		}
	}
}
