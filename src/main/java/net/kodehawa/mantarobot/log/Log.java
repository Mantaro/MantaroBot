package net.kodehawa.mantarobot.log;

import net.kodehawa.mantarobot.core.Mantaro;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {

	private final DateFormat hour = new SimpleDateFormat("HH:mm:ss");
	private final Date date = new Date();
	private final String hour1 = hour.format(date);
	
	private static final Log instance = new Log();

	public void print(String content, Type type)
	{
		System.out.println("[" + hour1 + "] " + "[" + Mantaro.instance().getState() + "] " + "[" + type.toString() + "] [Mantaro]: " + content);
	}
	
	public void print(String content, Class clazz, Type type)
	{
		System.out.println("[" + hour1+ "] " + "[" + type.toString() + "] " + "[Mantaro" + "/" + clazz.getSimpleName() + "]: " + content);
}

	public void print(State state, String content, Class clazz, Type type)
	{
		System.out.println("[" + hour1+ "] " + "[" + type.toString() + "] " + "[" + state + "] " + "[Mantaro" + "/" + clazz.getSimpleName() + "]: " + content);
	}

	public static Log instance()
	{
		return instance;
	}
}