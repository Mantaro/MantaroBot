package net.kodehawa.mantarobot.log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

	private final DateFormat hour = new SimpleDateFormat("HH:mm:ss");
	private final Date date = new Date();
	private final String hour1 = hour.format(date);
	
	private static final Logger instance = new Logger();

	public void print(String s, LogType type)
	{
		System.out.println("[" + hour1 + "] " + "[" + type.toString() + "] [Mantaro]: " + s);
	}
	
	public void print(String s, Class clazz, LogType type)
	{
		System.out.println("[" + hour1+ "]" + "[" + type.toString() + "]" + "[Mantaro" + "/" + clazz.getSimpleName() + "]: " + s);
	}
	
	public static Logger instance()
	{
		return instance;
	}
}
