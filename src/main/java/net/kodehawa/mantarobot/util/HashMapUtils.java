package net.kodehawa.mantarobot.util;

import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.log.Log;
import net.kodehawa.mantarobot.log.Type;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class HashMapUtils {

	public volatile static HashMapUtils instance = new HashMapUtils();
	private final Properties properties = new Properties();
	String fileLocation = "";
	private File file = null;
	@SuppressWarnings("unused")
	private String fileLoc;
	//I actually use them lmfao.
	@SuppressWarnings("unused")
	private String fileName;
	private Map<Integer, Integer> intHashmap;
	private Map<Integer, String> mixHashmap;
	private String name = "";
	private Map<String, String> stringHashmap;

	private HashMapUtils() {
	}

	public HashMapUtils(String fileLocation, String fileName, HashMap<String, String> map, String fileSignature, boolean isReactivated) {
		this.fileLoc = fileLocation;
		this.stringHashmap = map;
		this.name = fileName;

		if (Mantaro.instance().isWindows()) {
			this.file = new File("C:/" + fileLocation + "/" + fileName + ".dat");
		} else if (Mantaro.instance().isUnix()) {
			this.file = new File("/home/mantaro/" + fileName + ".dat");
		}

		if (!file.exists()) {
			this.createFile();
		}
		if (isReactivated) {
			saveString(file, map);
			this.loadString();
		}

		this.loadString();
	}

	public HashMapUtils(String fileLocation, String fileName, HashMap<Integer, String> map, boolean isReactivated) {
		this.fileLoc = fileLocation;
		this.mixHashmap = map;
		this.name = fileName;

		if (Mantaro.instance().isWindows()) {
			this.file = new File("C:/" + fileLocation + "/" + fileName + ".dat");
		} else if (Mantaro.instance().isUnix()) {
			this.file = new File("/home/mantaro/" + fileName + ".dat");
		}

		if (!file.exists()) {
			this.createFile();
		}
		if (isReactivated) {
			saveMix(file, map);
			this.loadMix();
		}

	}

	public HashMapUtils(String fileLocation, String fileName, HashMap<Integer, Integer> map, int fileSignature, boolean isReactivated) {
		this.fileLoc = fileLocation;
		this.intHashmap = map;
		this.name = fileName;

		if (Mantaro.instance().isWindows()) {
			this.file = new File("C:/" + fileLocation + "/" + fileName + ".dat");
		} else if (Mantaro.instance().isUnix()) {
			this.file = new File("/home/mantaro/" + fileName + ".dat");
		}

		if (!file.exists()) {
			this.createFile();
		}
		if (isReactivated) {
			saveInt(file, map);
			this.loadInt();
		}

	}

	private void createFile() {
		if (Mantaro.instance().isDebugEnabled) {
			Log.instance().print("Creating new file: " + name + "...", Type.INFO);
		}
		if (!file.exists()) {
			file.getParentFile().mkdirs();
			try {
				file.createNewFile();
				saveString(file, stringHashmap);
			} catch (Exception ignored) {
			}
		}
	}

	private void loadInt() {
		if (Mantaro.instance().isDebugEnabled) {
			Log.instance().print("Loading Map file: " + name, this.getClass(), Type.INFO);
		}

		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(file));
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String key : properties.stringPropertyNames()) {
			intHashmap.put(Integer.valueOf(key), Integer.valueOf(properties.get(key).toString()));
		}
	}

	private void loadMix() {
		if (Mantaro.instance().isDebugEnabled) {
			Log.instance().print("Loading Map file: " + name, this.getClass(), Type.INFO);
		}

		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(file));
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String key : properties.stringPropertyNames()) {
			mixHashmap.put(Integer.valueOf(key), properties.get(key).toString());
		}
	}

	private void loadString() {
		if (Mantaro.instance().isDebugEnabled) {
			Log.instance().print("Loading Map file: " + name, this.getClass(), Type.INFO);
		}

		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(file));
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String key : properties.stringPropertyNames()) {
			stringHashmap.put(key, properties.get(key).toString());
		}
	}

	private void saveInt(File file, Map<Integer, Integer> hash) {
		if (Mantaro.instance().isDebugEnabled) {
			Log.instance().print("Writing Map file: " + name, this.getClass(), Type.INFO);
		}

		properties.putAll(hash);

		try {
			properties.store(new FileOutputStream(file), null);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void saveMix(File file, Map<Integer, String> hash) {
		if (Mantaro.instance().isDebugEnabled) {
			Log.instance().print("Writing Map file: " + name, this.getClass(), Type.INFO);
		}

		properties.putAll(hash);

		try {
			properties.store(new FileOutputStream(file), null);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void saveString(File file, Map<String, String> hash) {
		if (Mantaro.instance().isDebugEnabled) {
			Log.instance().print("Writing Map file: " + name, Type.INFO);
		}

		properties.putAll(hash);

		try {
			properties.store(new FileOutputStream(file), null);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
