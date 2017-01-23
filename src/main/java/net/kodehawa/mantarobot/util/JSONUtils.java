package net.kodehawa.mantarobot.util;

import com.google.common.io.CharStreams;
import net.kodehawa.mantarobot.core.Mantaro;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;

public class JSONUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger("JSONUtils");
	private static JSONUtils instance = new JSONUtils();

	public static JSONUtils instance() {
		return instance;
	}

	private File f;

	private JSONUtils() {
	}

	public JSONUtils(HashMap<String, String> hash, String n, String subfolder, JSONObject o, boolean rewrite) {
		String name = n;
		if (Mantaro.isWindows()) {
			this.f = new File("C:/mantaro/" + subfolder + "/" + name + ".json");
		} else if (Mantaro.isUnix()) {
			this.f = new File("/home/mantaro/" + subfolder + "/" + name + ".json");
		}
		if (!f.exists()) {
			this.createFile(f);
			this.write(f, o);
		}

		if (rewrite) {
			this.write(f, o);
		}
		this.read(hash, o);
		o = getJSONObject(f);
	}

	public void createFile(File file) {
		if (!file.exists()) {
			file.getParentFile().mkdirs();
			try {
				file.createNewFile();
			} catch (Exception ignored) {
			}
		}
	}

	public JSONObject getJSONObject(File file) {
		try {
			FileInputStream is = new FileInputStream(file.getAbsolutePath());
			DataInputStream ds = new DataInputStream(is);
			BufferedReader br = new BufferedReader(new InputStreamReader(ds));
			String s = CharStreams.toString(br);
			JSONObject data = null;

			try {
				data = new JSONObject(s);
			} catch (JSONException e) {
				e.printStackTrace();
				System.out.println("No results or unreadable reply from file.");
			}

			br.close();
			return data;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public void read(HashMap<String, String> hash, JSONObject data) {
		LOGGER.info("Reading JSON data: " + data.toString());
		try {
			Iterator<?> datakeys = data.keys();
			while (datakeys.hasNext()) {
				String key = (String) datakeys.next();
				String value = data.getString(key);
				hash.put(key, value);
			}
		} catch (Exception e) {
			System.out.println("Error reading for HashMap.");
			e.printStackTrace();
		}
	}

	public void write(File file, JSONObject obj) {
		LOGGER.info("Writing JSON File " + file.getName());
		try {
			FileWriter fw = new FileWriter(file);
			fw.write(obj.toString(4));
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
