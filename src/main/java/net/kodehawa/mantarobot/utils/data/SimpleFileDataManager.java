package net.kodehawa.mantarobot.utils.data;

import net.kodehawa.mantarobot.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SimpleFileDataManager implements DataManager<List<String>> {
	public static final Pattern NEWLINE_PATTERN = Pattern.compile("\\r\\n?|\\r?\\n");
	private static final Logger LOGGER = LoggerFactory.getLogger("SimpleFileDataManager");
	private final List<String> data = new ArrayList<>();
	private final Path path;

	public SimpleFileDataManager(String file) {
		this.path = Paths.get(file);
		try {
			if (!this.path.toFile().exists()) {
				LOGGER.info("Could not find config file at " + this.path.toFile().getAbsolutePath() + ", creating a new one...");
				if (this.path.toFile().createNewFile()) {
					LOGGER.info("Generated new config file at " + this.path.toFile().getAbsolutePath() + ".");
					IOUtils.write(this.path, this.data.stream().collect(Collectors.joining()));
					LOGGER.info("Please, fill the file with valid properties.");
				} else {
					LOGGER.warn("Could not create config file at " + file);
				}
			}

			Collections.addAll(data, NEWLINE_PATTERN.split(IOUtils.read(this.path)));
			data.removeIf(s -> s.startsWith("//"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<String> get() {
		return data;
	}

	public void save() {
		try {
			IOUtils.write(path, this.data.stream().collect(Collectors.joining("\n")));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
