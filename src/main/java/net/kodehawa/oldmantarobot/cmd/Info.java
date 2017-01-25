package net.kodehawa.oldmantarobot.cmd;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandType;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.oldmantarobot.core.Mantaro;
import net.kodehawa.oldmantarobot.util.GeneralUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Color;
import java.net.URLEncoder;

/**
 * Information module.
 *
 * @author Yomura
 */
public class Info extends Module {
	public Info() {
		super(Category.INFO);
		this.registerCommands();
	}

	public void registerCommands() {

		super.register("weather", new SimpleCommand() {
			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				EmbedBuilder embed = new EmbedBuilder();

				if (!content.isEmpty()) {
					try {
						long start = System.currentTimeMillis();
						//Get a parsed JSON.
						String APP_ID = Mantaro.getConfig().values().get("weatherappid").toString();
						JSONObject jObject = new JSONObject(
							GeneralUtils.instance().getObjectFromUrl("http://api.openweathermap.org/data/2.5/weather?q=" + URLEncoder.encode(
								content, "UTF-8") + "&appid=" + APP_ID, event));
						//Get the object as a array.
						JSONArray data = jObject.getJSONArray("weather");
						String status = null;
						for (int i = 0; i < data.length(); i++) {
							JSONObject entry = data.getJSONObject(i);
							status = entry.getString("main"); //Used for weather status.
						}

						JSONObject jMain = jObject.getJSONObject("main"); //Used for temperature and humidity.
						JSONObject jWind = jObject.getJSONObject("wind"); //Used for wind speed.
						JSONObject jClouds = jObject.getJSONObject("clouds"); //Used for cloudiness.
						JSONObject jsys = jObject.getJSONObject("sys"); //Used for countrycode.

						long end = System.currentTimeMillis() - start;

						String countryCode = jsys.getString("country").toLowerCase();

						Double temp = (double) jMain.get("temp"); //Temperature in Kelvin.
						double pressure = (double) jMain.get("pressure"); //Pressure in kPA.
						int hum  = (int) jMain.get("humidity"); //Humidity in percentage.
						Double ws = (double) jWind.get("speed"); //Speed in m/h.
						int clness = (int) jClouds.get("all"); //Cloudiness in percentage.

						//Simple math formulas to convert from universal to metric and imperial.
						Double finalTemperatureCelcius = temp - 273.15; //Temperature in Celcius degrees.
						Double finalTemperatureFarnheit = temp * 9 / 5 - 459.67; //Temperature in Farnheit degrees.
						Double finalWindSpeedMetric = ws * 3.6; //wind speed in km/h.
						Double finalWindSpeedImperial = ws / 0.447046; //wind speed in mph.

						embed.setColor(Color.CYAN)
							.setTitle(":flag_" + countryCode + ":" + " Forecast information for " + content) //For which city
							.setDescription(":fallen_leaf: " + status + " (" + clness + "% cloudiness)") //Clouds, sunny, etc and cloudiness.
							.addField(":thermometer: Temperature", finalTemperatureCelcius.intValue() + "°C/" + finalTemperatureFarnheit.intValue() + "°F", true)
							.addField(":droplet: Humidity", hum + "%", true)
							.addBlankField(true)
							.addField(":wind_blowing_face: Wind Speed", finalWindSpeedMetric.intValue() + "kmh / " + finalWindSpeedImperial.intValue() + "mph", true)
							.addField(":chart_with_downwards_trend: Pressure", pressure + "kPA", true)
							.addBlankField(true)
							.setFooter("Information provided by OpenWeatherMap (Process time: " + end + "ms)", null);
						//Build the embed message and send it.
						event.getChannel().sendMessage(embed.build()).queue();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					event.getChannel().sendMessage(help()).queue();
				}
			}

			@Override
			public String help() {
				return "This command retrieves information from OpenWeatherMap. Used to check **forecast information.**\n"
					+ "> Usage:\n"
					+ "~>weather [city],[countrycode]: Retrieves the forecast information for such location.\n"
					+ "> Parameters:\n"
					+ "[city]: Your city name, for example New York\n"
					+ "[countrycode]: (OPTIONAL) The code for your country, for example US (USA) or MX (Mexico).";
			}
		});
	}
}