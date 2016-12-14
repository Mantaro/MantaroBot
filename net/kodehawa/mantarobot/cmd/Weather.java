package net.kodehawa.mantarobot.cmd;

import java.awt.Color;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONObject;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.util.Utils;

public class Weather extends Command {

    public Weather()
	{
		setName("weather");
		setDescription("Retrieves weather from a city in the form of an embed message.");
		setExtendedHelp(
				"This command retrieves information from OpenWeatherMap. Used to check **forecast information.**\n"
				+ "> Usage:\n"
				+ "~>weather city,countrycode: Retrieves the forecast information for such location.\n"
				+ "> Parameters:\n"
				+ "*city*: Your city name, for example New York\n"
				+ "*countrycode*: The code for your country, for example US (USA) or MX (Mexico).");
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
                 String APP_ID = "e2abde2e6ca69e90a73ddb43199031de";
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
		         		         
		         JSONObject jMain = jObject.getJSONObject("main"); //Used for temperature and humidity.
		         JSONObject jWind = jObject.getJSONObject("wind"); //Used for wind speed.
		         JSONObject jClouds = jObject.getJSONObject("clouds"); //Used for cloudiness.
		         JSONObject jsys = jObject.getJSONObject("sys"); //Used for countrycode.
		         
				 long end = System.currentTimeMillis() - start;

				 String countryCode = jsys.getString("country").toLowerCase();

		         Double temp = (double)jMain.get("temp"); //Temperature in Kelvin.
		         int pressure = (int) jMain.get("pressure"); //Pressure in kPA.
		         int hum = (int) jMain.get("humidity"); //Humidity in percentage.
		         Double ws = (double) jWind.get("speed"); //Speed in m/h.
		         int clness = (int) jClouds.get("all"); //Cloudiness in percentage.
		         
		         //Simple math formulas to convert from universal to metric and imperial.
		         Double finalTemperatureCelcius = temp - 273.15; //Temperature in Celcius degrees.
		         Double finalTemperatureFarnheit = temp * 9/5 - 459.67; //Temperature in Farnheit degrees.
		         Double finalWindSpeedMetric = ws * 3.6; //wind speed in km/h.
		         Double finalWindSpeedImperial = ws / 0.447046; //wind speed in mph.

		         embed.setColor(Color.CYAN)
		         	.setTitle(":flag_" + countryCode + ":" + " Forecast information for " + content) //For which city
		         	.setDescription(status + " (" + clness + "% cloudiness)") //Clouds, sunny, etc and cloudiness.
		         	.addField("Temperature", finalTemperatureCelcius.intValue() + "°C/" + finalTemperatureFarnheit.intValue() + "°F", true)
		         	.addField("Humidity", hum + "%" , true)
		         	.addBlankField(true)
		         	.addField("Wind Speed", finalWindSpeedMetric.intValue() + "kmh / " + finalWindSpeedImperial.intValue() + "mph" , true)
		         	.addField("Pressure", pressure + "kPA" , true)
		         	.addBlankField(true)
		         	.setFooter("Information provided by OpenWeatherMap (Process time: " + end + "ms)", null);
		         //Build the embed message and send it.
		         channel.sendMessage(embed.build()).queue();
			 }
		     catch(Exception e){
		    	 e.printStackTrace();
		     }
		} else {
			channel.sendMessage(getExtendedHelp()).queue();
		}
	}
	
	protected String getTimeFromMillis(long millis){
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(millis	);
		return new SimpleDateFormat("HH:mm:ss").format(cal.getTime());
	}
}
