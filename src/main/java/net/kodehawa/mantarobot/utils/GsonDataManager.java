package net.kodehawa.mantarobot.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

public class GsonDataManager<T> implements Supplier<T> {
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
	private static final Logger LOGGER = LoggerFactory.getLogger("GsonDataManager");
	private final Path configPath;
	private final T data;

	public GsonDataManager(Class<T> clazz, String file, Supplier<T> constructor) {
		this.configPath = Paths.get(file);
		try {
			if (!configPath.toFile().exists()) {
				LOGGER.info("Could not find config file at " + configPath.toFile().getAbsolutePath() + ", creating a new one...");
				if (configPath.toFile().createNewFile()) {
					LOGGER.info("Generated new config file at " + configPath.toFile().getAbsolutePath() + ".");
					IOUtils.write(configPath, GSON.toJson(constructor.get()));
					LOGGER.info("Please, fill the file with valid properties.");
				} else {
					LOGGER.warn("Could not create config file at " + file);
				}
				System.exit(0);
			}

			this.data = GSON.fromJson(IOUtils.read(configPath), clazz);
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
			IOUtils.write(configPath, GSON.toJson(data));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}