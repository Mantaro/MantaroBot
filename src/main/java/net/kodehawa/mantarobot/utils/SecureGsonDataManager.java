package net.kodehawa.mantarobot.utils;

import com.google.gson.Gson;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;
import java.util.function.Supplier;

//TODO Use this for potentially big JSON files
public class SecureGsonDataManager<T> implements Supplier<T> {

	public static final Gson GSON = GsonDataManager.GSON_UNPRETTY;
	private static final Logger LOGGER = LoggerFactory.getLogger("SecureGsonDataManager");

	private final Path path;
	private final T data;

	public SecureGsonDataManager(Class<T> clazz, String file, Supplier<T> constructor) {
		this.path = Paths.get(file);
		try {
			if (!path.toFile().exists()) {
				LOGGER.info("Could not find file at " + path.toFile().getAbsolutePath() + ", creating a new one...");
				if (path.toFile().createNewFile()) {
					LOGGER.info("Filling file at " + path.toFile().getAbsolutePath() + ".");
					data = constructor.get();
					try(BufferedWriter writer = Files.newBufferedWriter(path)) {
						Properties properties = new Properties();
						String encoded = new String(Base64.getEncoder().encode(GSON.toJson(data).getBytes("UTF-8")),"UTF-8");
						properties.put("data", encoded);
						properties.put("checksum", DigestUtils.md5Hex(encoded));
						properties.put("sanity", "true");
						properties.store(writer, path.toFile().getName());
					}
					LOGGER.info("Please, fill the file with valid properties.");
				} else {
					LOGGER.warn("Could not create file at " + file);
				}
				System.exit(0);
				throw new IllegalStateException("Checksum failed. Aborting.");
			}

			Properties properties = new Properties();
			try (BufferedReader reader = Files.newBufferedReader(path)) {
				properties.load(reader);
				String encoded = properties.getProperty("data"), decoded = new String(Base64.getDecoder().decode(encoded.getBytes("UTF-8")),"UTF-8");
				if (Boolean.valueOf(properties.getProperty("sanity")) && !DigestUtils.md5Hex(encoded).equals(properties.getProperty("checksum"))) {
					throw new IllegalStateException("Checksum failed. Aborting.");
				}
				data = GSON.fromJson(decoded, clazz);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public T get() {
		return data;
	}

	public void update() {
		try(BufferedWriter writer = Files.newBufferedWriter(path)) {
			Properties properties = new Properties();
			String encoded = new String(Base64.getEncoder().encode(GSON.toJson(data).getBytes("UTF-8")),"UTF-8");
			properties.put("data", encoded);
			properties.put("checksum", DigestUtils.md5Hex(encoded));
			properties.put("sanity", "true");
			properties.store(writer, path.toFile().getName());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}