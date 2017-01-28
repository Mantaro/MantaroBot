package net.kodehawa.mantarobot.commands.utils;

import java.util.ArrayList;
import java.util.List;

public class WeatherData {
	public class Clouds {
		public int all = 0;
	}

	public class Main {
		public int humidity; //Humidity in percentage.
		public double pressure = 0;
		public double speed; //Speed in m/h.
		public double temp = 0;
	}

	public class Sys {
		public String country = null;
	}

	public class Weather {
		public String main = null;
	}

	public class Wind {
		public double speed = 0;
	}

	public Clouds clouds = new Clouds();
	public Main main = new Main();
	public Sys sys = new Sys();
	public List<Weather> weather = new ArrayList<>();
	public Wind wind = new Wind();
}
