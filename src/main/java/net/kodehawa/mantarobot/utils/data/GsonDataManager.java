package net.kodehawa.mantarobot.utils.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kodehawa.mantarobot.utils.FileIOUtils;
import net.kodehawa.mantarobot.utils.UnsafeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

public class GsonDataManager<T> implements DataManager<T> {
	public static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().serializeNulls().create(), GSON_UNPRETTY = new GsonBuilder().serializeNulls().create();
	private static final Logger LOGGER = LoggerFactory.getLogger("GsonDataManager");

	public static Gson gson(boolean pretty) {
		return pretty ? GSON_PRETTY : GSON_UNPRETTY;
	}

	private final Path configPath;
	private final T data;
	private final boolean pretty;

	public GsonDataManager(Class<T> clazz, String file, Supplier<T> constructor, boolean pretty) {
		this.configPath = Paths.get(file);
		this.pretty = pretty;
		try {
			if (!configPath.toFile().exists()) {
				LOGGER.info("Could not find config file at " + configPath.toFile().getAbsolutePath() + ", creating a new one...");
				if (configPath.toFile().createNewFile()) {
					LOGGER.info("Generated new config file at " + configPath.toFile().getAbsolutePath() + ".");
					FileIOUtils.write(configPath, gson(pretty).toJson(constructor.get()));
					LOGGER.info("Please, fill the file with valid properties.");
				} else {
					LOGGER.warn("Could not create config file at " + file);
				}
				System.exit(0);
			}

			this.data = gson(pretty).fromJson(FileIOUtils.read(configPath), clazz);
		} catch (IOException e) {
            UnsafeUtils.throwException(e);
            throw new AssertionError(); //otherwise it doesn't compile
		}
	}

	@Override
	public T get() {
		return data;
	}

	@Override
	public void save() {
		try {
			FileIOUtils.write(configPath, GSON_PRETTY.toJson(data));
		} catch (IOException e) {
            UnsafeUtils.throwException(e);
		}
	}
}