package net.kodehawa.mantarobot.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.io.CharStreams;

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
		try {
			FileWriter fw = new FileWriter(file);
			fw.write(obj.toString(4));
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static JSONUtils instance(){
		return instance;
	}
}
