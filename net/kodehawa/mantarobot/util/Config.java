package net.kodehawa.mantarobot.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.io.CharStreams;

/**
 * This loads all configurations stored in the config.json file.
 * This class loads *before* everything else.
 * @author Yomura
 */
public class Config {
	
	private volatile static Config cl = new Config();
	private String OS = System.getProperty("os.name").toLowerCase();
	private HashMap<String, Object> properties = new HashMap<String, Object>();
	private final JSONObject objdata = new JSONObject();
	private File config;
	private String BOT_TOKEN = "";
	private String AL_SCR = "";
	private String OSU_KEY = "";
	private boolean DEBUG_ENABLED;
	private boolean CONSOLE;
	
	public Config(){
		System.out.println("Loading config...");
		objdata.put("token", BOT_TOKEN);
		objdata.put("alsecret", AL_SCR);
		objdata.put("osuapikey", OSU_KEY);
		objdata.put("debug", DEBUG_ENABLED);
		objdata.put("console", CONSOLE);
		
		if(isWindows()){
			this.config = new File("C:/mantaro/config/config.json");
		}
		else if(isUnix()){
			this.config = new File("/home/mantaro/config/config.json");
		}
		createFile();
		setValues();
	}
	
	private JSONObject getConfigObject(){
		try
		{
			FileInputStream is = new FileInputStream(config.getAbsolutePath());
			DataInputStream ds = new DataInputStream(is);
			BufferedReader br = new BufferedReader(new InputStreamReader(ds));
			String s = CharStreams.toString(br);
			JSONObject data = null;
			
			try{
	        	data = new JSONObject(s);
			}
			catch(JSONException e){
				e.printStackTrace();
				System.out.println("No results or unreadable reply from file. Cannot start");
				System.exit(-1);
			}
			
			br.close();
			return data;
		}
		catch(Exception e){}
		
		return null;
	}
	
	private void setValues(){
		try{
			JSONObject data = this.getConfigObject();
			Iterator<?> datakeys = data.keys();

	        while(datakeys.hasNext()){
	            String key = (String)datakeys.next();
	            Object value = data.get(key); 
	            properties.put(key, value);
	        }
		} catch (Exception e){
			e.printStackTrace();
			System.out.println("Please populate config.json file in /mantaro/config.");
			System.out.println("Exiting...");
			System.exit(-1);
		}
		
	}
	
	private void createFile()
	{
		if(!config.exists()){
			config.getParentFile().mkdirs();
			try{
				config.createNewFile();
				writeValues(config, objdata);
			}
			catch(Exception e){}
		}
	}
	
	private void writeValues(File file, JSONObject obj){
		try {
			FileWriter fw = new FileWriter(file);
			fw.write(objdata.toString(4));
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Config load(){
		return cl;
	}

	public HashMap<String, Object> values(){
		return properties;
	}
	
	private boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

    private boolean isUnix() {
        return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 );
    }
}
