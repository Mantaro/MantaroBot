package net.kodehawa.mantarobot.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SimpleFileDataManager implements Supplier<List<String>> {
	private static final Logger LOGGER = LoggerFactory.getLogger("SimpleFileDataManager");
	private final List<String> data = new ArrayList<>();
	private final File file;

	public SimpleFileDataManager(String file) {
		this.file = new File(file);
		try {
			if (!this.file.exists()) {
				LOGGER.info("Could not find config file at " + this.file.getAbsolutePath() + ", creating a new one...");
				if (this.file.createNewFile()) {
					LOGGER.info("Generated new config file at " + this.file.getAbsolutePath() + ".");
					SimpleFileIO.write(this.file, this.data);
					LOGGER.info("Please, fill the file with valid properties.");
				} else {
					LOGGER.warn("Could not create config file at " + file);
				}
			}

			this.data.addAll(SimpleFileIO.read(this.file));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<String> get() {
		return data;
	}

	public void update() {
		try {
			SimpleFileIO.write(file, data);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
