package net.kodehawa.discord.Mantaro.utils;

import java.util.concurrent.CopyOnWriteArrayList;

public class Values {
	
	public static CopyOnWriteArrayList<String> disabledServers = new CopyOnWriteArrayList<String>();
	public static Values instance = new Values();
	
	
	public Values(){}


	public void check()
	{
		new StringArrayFile("disabledservers", "mantaro", disabledServers, true, true);
	}
	
	public void read()
	{
		new StringArrayFile("disabledservers", "mantaro", disabledServers, false, true);
	}
}
