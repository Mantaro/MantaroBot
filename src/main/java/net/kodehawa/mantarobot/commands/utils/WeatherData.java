package net.kodehawa.mantarobot.commands.utils;

import java.util.ArrayList;
import java.util.List;

public class WeatherData {
	public class Clouds {
		public final int all = 0;
	}

	public class Main {
		public int humidity; //Humidity in percentage.
		public final double pressure = 0;
		public double speed; //Speed in m/h.
		public final double temp = 0;

		public int getHumidity() {
			return humidity;
		}

		public double getPressure() {
			return pressure;
		}

		public double getSpeed() {
			return speed;
		}

		public double getTemp() {
			return temp;
		}
	}

	public class Sys {
		public final String country = null;
	}

	public class Weather {
		public final String main = null;
	}

	public class Wind {
		public final double speed = 0;
	}

	public final Clouds clouds = new Clouds();
	public final Main main = new Main();
	public final Sys sys = new Sys();
	public final List<Weather> weather = new ArrayList<>();
	public final Wind wind = new Wind();

	public Clouds getClouds() {
		return clouds;
	}

	public Main getMain() {
		return main;
	}

	public Sys getSys() {
		return sys;
	}

	public List<Weather> getWeather() {
		return weather;
	}

	public Wind getWind() {
		return wind;
	}
}
