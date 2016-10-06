package net.kodehawa.discord.Mantaro.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArrayList;

import net.kodehawa.discord.Mantaro.bot.MantaroBot;

public class StringArrayFile {
	public volatile static StringArrayFile instance = new StringArrayFile() ;
	private String name;
	private File file;
	@SuppressWarnings("unused")
	private String path;
	public CopyOnWriteArrayList<String> list;
	
	private StringArrayFile(){}

	public void removeDupes(CopyOnWriteArrayList<String> list) 
    {
        HashSet<String> set = new HashSet<String>(list);
        list.clear();
        list.addAll(set);
    }
	
	/**
	 * Set all the values
	 * @param name
	 * @param path
	 * @param list
	 */
	public StringArrayFile(String name, String path, CopyOnWriteArrayList<String> list, boolean isRewritable)
	{
		this.name = name;
		this.list = list;
		this.path = path;
		if(MantaroBot.getInstance().isWindows())
		{
			this.file = new File("C:/"+path+"/"+name+".txt");
		}
		else if(MantaroBot.getInstance().isUnix())
		{
			this.file = new File("/home/mantaro/"+name+".txt");
		}
		
		if(!file.exists())
		{
		   this.createFile();
		}
		if(isRewritable)
		{
			create(file, list);
		}
		
		this.read();
	}
	
	public StringArrayFile(String name, String path, CopyOnWriteArrayList<String> list, boolean isRewritable, boolean read)
	{
		this.name = name;
		this.list = list;
		this.path = path;
		
		if(MantaroBot.getInstance().isWindows())
		{
			this.file = new File("C:/"+path+"/"+name+".txt");
		}
		else if(MantaroBot.getInstance().isUnix())
		{
			this.file = new File("/home/mantaro/" +name+".txt");
		}
		
		if(!file.exists())
		{
		   this.createFile();
		}
		if(isRewritable)
		{
			create(file, list);
		}
		
		if(read)
		{
			this.read();
		}
	}
	
	
	private void createFile()
	{
		if(MantaroBot.getInstance().debugMode){ Logging.instance().print("Creating new file" + name + "...", LogTypes.INFO); }
		if(!file.exists())
		{
			file.getParentFile().mkdirs();
			try
			{
				file.createNewFile();
				create(file, list);
			}
			catch(Exception e)
			{}
		}
	}
	
	public void modify(File file, CopyOnWriteArrayList<String> list){
		create(file, list);
	}
	
	private void create(File file, CopyOnWriteArrayList<String> list){
		if(MantaroBot.getInstance().debugMode){ Logging.instance().print("Writing List file "+name, LogTypes.INFO); }
		try
		{
			FileWriter filewriter = new FileWriter(file);
			BufferedWriter buffered = new BufferedWriter(filewriter);
			for(String s : (CopyOnWriteArrayList<String>)list){
				removeDupes(list);
				
				buffered.write(s+"\r\n");
			}
			buffered.close();
		}
		
		catch(Exception e)
		{
			Logging.instance().print("Problem while writting file", LogTypes.WARN);
			e.printStackTrace();
		}
	}
	
	private void read(){
		Logging.instance().print("Reading List file: "+name, LogTypes.INFO);
		try
		{
			FileInputStream imputstream = new FileInputStream(file.getAbsolutePath());
			DataInputStream datastream = new DataInputStream(imputstream);
			BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(datastream));
			String s;
			while((s = bufferedreader.readLine()) != null){
				list.add(s.trim());
			}
			bufferedreader.close();
		}
		catch(Exception e)
		{
			Logging.instance().print("Problem while reading file", LogTypes.WARN);
			e.printStackTrace();
		}
	}
}
