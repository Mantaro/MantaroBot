package net.kodehawa.mantarobot.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kodehawa.mantarobot.data.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

public class GsonDataManager<T> implements Supplier<T> {
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
	private static final Logger LOGGER = LoggerFactory.getLogger("GsonDataManager");
	private final File configFile;
	private final T data;

	public GsonDataManager(Class<T> clazz, String file, Supplier<T> constructor) {
		this.configFile = new File(file);
		try {
			if (!configFile.exists()) {
				LOGGER.info("Could not find config file at " + configFile.getAbsolutePath() + ", creating a new one...");
				if (configFile.createNewFile()) {
					LOGGER.info("Generated new config file at " + configFile.getAbsolutePath() + ".");
					IOUtils.write(configFile, GSON.toJson(constructor.get()));
					LOGGER.info("Please, fill the file with valid properties.");
				} else {
					LOGGER.warn("Could not create config file at " + file);
				}
				System.exit(0);
			}

			this.data = GSON.fromJson(IOUtils.read(configFile), clazz);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public T get() {
		return data;
	}

	public void update() {
		try {
			IOUtils.write(configFile, GSON.toJson(data));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}