package net.kodehawa.mantarobot.util;

import net.kodehawa.mantarobot.core.Mantaro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class HashMapUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger("HashMapUtils");

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

	public HashMapUtils(String fileLocation, String fileName, HashMap<String, String> map, String fileSignature, boolean isReactivated) {
		this.fileLoc = fileLocation;
		this.stringHashmap = map;
		this.name = fileName;

		if (Mantaro.isWindows()) {
			this.file = new File("C:/" + fileLocation + "/" + fileName + ".dat");
		} else if (Mantaro.isUnix()) {
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

		if (Mantaro.isWindows()) {
			this.file = new File("C:/" + fileLocation + "/" + fileName + ".dat");
		} else if (Mantaro.isUnix()) {
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

		if (Mantaro.isWindows()) {
			this.file = new File("C:/" + fileLocation + "/" + fileName + ".dat");
		} else if (Mantaro.isUnix()) {
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
		if (Mantaro.isDebugEnabled) {
			LOGGER.info("Creating new file: " + name + "...");
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
		if (Mantaro.isDebugEnabled) {
			LOGGER.info("Loading Map file: " + name, this.getClass());
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
		if (Mantaro.isDebugEnabled) {
			LOGGER.info("Loading Map file: " + name, this.getClass());
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
		if (Mantaro.isDebugEnabled) {
			LOGGER.info("Loading Map file: " + name, this.getClass());
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
		if (Mantaro.isDebugEnabled) {
			LOGGER.info("Writing Map file: " + name, this.getClass());
		}

		properties.putAll(hash);

		try {
			properties.store(new FileOutputStream(file), null);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void saveMix(File file, Map<Integer, String> hash) {
		if (Mantaro.isDebugEnabled) {
			LOGGER.info("Writing Map file: " + name, this.getClass());
		}

		properties.putAll(hash);

		try {
			properties.store(new FileOutputStream(file), null);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void saveString(File file, Map<String, String> hash) {
		if (Mantaro.isDebugEnabled) {
			LOGGER.info("Writing Map file: " + name);
		}

		properties.putAll(hash);

		try {
			properties.store(new FileOutputStream(file), null);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
