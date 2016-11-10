package net.kodehawa.discord.Mantaro.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.kodehawa.discord.Mantaro.bot.MantaroBot;

public class HashMapUtils {

	public volatile static HashMapUtils instance = new HashMapUtils();
	public Map<String, String> stringHashmap;
	public Map<Integer, String> mixHashmap;
	public Map<Integer, Integer> intHashmap;
	Properties properties = new Properties();
	String fileLocation = "";
	File file = null;
	String name = "";
	//I actually use them lmfao.
	@SuppressWarnings("unused")
	private String fileName;
	@SuppressWarnings("unused")
	private String fileLoc;

	private HashMapUtils(){};
	
	public HashMapUtils(String fileLocation, String fileName, HashMap<String, String> map, String fileSignature, boolean isReactivated)
	{
		this.fileLoc = fileLocation;
		this.stringHashmap = map;
		this.name = fileName;
		
		if(MantaroBot.getInstance().isWindows())
		{
			this.file = new File("C:/"+fileLocation+"/"+fileName+".da");
		}
		else if(MantaroBot.getInstance().isUnix())
		{
			this.file = new File("/home/mantaro/" +fileName+".da");
		}

		if(!file.exists())
		{
		   this.createFile();
		}
		if(isReactivated)
		{
			saveString(file, map);
			this.loadString();
		}
		
		this.loadString();
	}
	

	public HashMapUtils(String fileLocation, String fileName, HashMap<Integer, String> map, boolean isReactivated)
	{
		this.fileLoc = fileLocation;
		this.mixHashmap = map;
		this.name = fileName;
		
		if(MantaroBot.getInstance().isWindows())
		{
			this.file = new File("C:/"+fileLocation+"/"+fileName+".da");
		}
		else if(MantaroBot.getInstance().isUnix())
		{
			this.file = new File("/home/mantaro/" +fileName+".da");
		}
		
		if(!file.exists())
		{
		   this.createFile();
		}
		if(isReactivated)
		{
			saveMix(file, map);
			this.loadMix();
		}
		
	}
	
	public HashMapUtils(String fileLocation, String fileName, HashMap<Integer, Integer> map, int fileSignature, boolean isReactivated)
	{
		this.fileLoc = fileLocation;
		this.intHashmap = map;
		this.name = fileName;
		
		if(MantaroBot.getInstance().isWindows())
		{
			this.file = new File("C:/"+fileLocation+"/"+fileName+".da");
		}
		else if(MantaroBot.getInstance().isUnix())
		{
			this.file = new File("/home/mantaro/" +fileName+".da");
		}
		
		if(!file.exists())
		{
		   this.createFile();
		}
		if(isReactivated)
		{
			saveInt(file, map);
			this.loadInt();
		}
		
	}
	
	private void createFile()
	{
		if(MantaroBot.getInstance().debugMode){ Logger.instance().print("Creating new file" + name + "...", LogType.INFO); }
		if(!file.exists())
		{
			file.getParentFile().mkdirs();
			try
			{
				file.createNewFile();
				saveString(file, stringHashmap);
			}
			catch(Exception e)
			{}
		}
	}
	
	public void saveString(File file, Map<String, String> hash)
	{
		if(MantaroBot.getInstance().debugMode){ Logger.instance().print("Writing Map file "+name, LogType.INFO); }

		properties.putAll(hash);

		try {
			properties.store(new FileOutputStream(file), null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	
	public void saveInt(File file, Map<Integer, Integer> hash)
	{
		if(MantaroBot.getInstance().debugMode){ Logger.instance().print("Writing Map file "+name, LogType.INFO); }

		properties.putAll(hash);

		try {
			properties.store(new FileOutputStream(file), null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public void saveMix(File file, Map<Integer, String> hash)
	{
		if(MantaroBot.getInstance().debugMode){ Logger.instance().print("Writing Map file "+name, LogType.INFO); }

		properties.putAll(hash);

		try {
			properties.store(new FileOutputStream(file), null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public void loadString()
	{
		if(MantaroBot.getInstance().debugMode){ Logger.instance().print("Loading Map file "+name, LogType.INFO); }

		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String key : properties.stringPropertyNames()) 
		{
			   stringHashmap.put(key, properties.get(key).toString());
		}
	}
	
	public void loadInt()
	{
		if(MantaroBot.getInstance().debugMode){ Logger.instance().print("Loading Map file "+name, LogType.INFO); }

		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String key : properties.stringPropertyNames()) 
		{
			intHashmap.put(Integer.valueOf(key), Integer.valueOf(properties.get(key).toString()));
		}
	}

	public void loadMix()
	{
		if(MantaroBot.getInstance().debugMode){ Logger.instance().print("Loading Map file "+name, LogType.INFO); }

		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String key : properties.stringPropertyNames()) 
		{
			mixHashmap.put(Integer.valueOf(key), properties.get(key).toString());
		}
	}
}
