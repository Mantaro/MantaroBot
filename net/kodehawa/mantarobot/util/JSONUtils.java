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

import net.kodehawa.mantarobot.log.LogType;
import net.kodehawa.mantarobot.log.Logger;

public class JSONUtils {
	private static JSONUtils instance = new JSONUtils();

	public JSONObject getJSONObject(File file){
		try{
			FileInputStream is = new FileInputStream(file.getAbsolutePath());
			DataInputStream ds = new DataInputStream(is);
			BufferedReader br = new BufferedReader(new InputStreamReader(ds));
			String s = CharStreams.toString(br);
			JSONObject data = null;
			
			try{
	        	data = new JSONObject(s);
			}
			catch(JSONException e){
				e.printStackTrace();
				System.out.println("No results or unreadable reply from file.");
			}
			
			br.close();
			return data;
		}
		catch(Exception e){
			e.printStackTrace();
		} 
		
		return null;
	}
	
	public void write(File file, JSONObject obj){
		Logger.instance().print("Writting JSON File " + file.getName(), LogType.INFO);
		try {
			FileWriter fw = new FileWriter(file);
			fw.write(obj.toString(4));
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void createFile(File file)
	{
		if(!file.exists()){
			file.getParentFile().mkdirs();
			try{
				file.createNewFile();
			}
			catch(Exception e){}
		}
	}
	
	public void read(HashMap<String, String> hash, JSONObject data){
		Logger.instance().print("Reading JSON File.", LogType.INFO);
		try{
			Iterator<?> datakeys = data.keys();
	        while(datakeys.hasNext()){
	            String key = (String)datakeys.next();
	            String value = data.getString(key); 
	            hash.put(key, value);
	        }
		} catch (Exception e){
			System.out.println("Error reading for HashMap.");
			e.printStackTrace();
		}
	}
	
	
	public static JSONUtils instance(){
		return instance;
	}
}
