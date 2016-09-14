package net.kodehawa.discord.Mantaro.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logging {

	private DateFormat hour = new SimpleDateFormat("HH:mm:ss");
	private Date date = new Date();
	private String hour1 = hour.format(date);
	
	private static Logging instance = new Logging();

	public void print(String s, LogTypes type)
	{
		System.out.println("[" + hour1 + "] " + "[" + type.toString() + "] [Mantaro] " + s);
	}
	
	public static Logging instance()
	{
		return instance;
	}
}
