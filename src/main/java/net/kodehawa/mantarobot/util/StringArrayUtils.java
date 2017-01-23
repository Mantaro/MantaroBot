package net.kodehawa.mantarobot.util;

import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.thread.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArrayList;

public class StringArrayUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger("StringArrayUtils");
	public volatile static StringArrayUtils instance = new StringArrayUtils();
	private File file;
	private CopyOnWriteArrayList<String> list;
	private String name;
	@SuppressWarnings("unused")
	private String path = "mantaro";

	private StringArrayUtils() {
	}

	/**
	 * Set all the values
	 *
	 * @param name         The name of the file
	 * @param list         The list to write
	 * @param isRewritable if you need to write on it.
	 */
	public StringArrayUtils(String name, CopyOnWriteArrayList<String> list, boolean isRewritable) {
		this.name = name;
		this.list = list;
		if (Mantaro.isWindows()) {
			this.file = new File("C:/mantaro/" + name + ".txt");
		} else if (Mantaro.isUnix()) {
			this.file = new File("/home/mantaro/" + name + ".txt");
		}

		if (!file.exists()) {
			this.createFile();
		}
		if (isRewritable) {
			create(file, list);
		}

		this.read();
	}

	public StringArrayUtils(String name, CopyOnWriteArrayList<String> list, boolean isRewritable, boolean read) {
		this.name = name;
		this.list = list;

		if (Mantaro.isWindows()) {
			this.file = new File("C:/mantaro/" + name + ".txt");
		} else if (Mantaro.isUnix()) {
			this.file = new File("/home/mantaro/" + name + ".txt");
		}
		if (!file.exists()) {
			this.createFile();
		}
		if (isRewritable) {
			create(file, list);
		}
		if (read) {
			this.read();
		}
	}

	private void create(File file, CopyOnWriteArrayList<String> list) {
		Async.asyncThread("(StringArrayUtils) Writer thread", () -> {
			if (Mantaro.isDebugEnabled) {
				LOGGER.info("Writing List file " + name, this.getClass());
			}
			try {
				FileWriter filewriter = new FileWriter(file);
				BufferedWriter buffered = new BufferedWriter(filewriter);
				for (String s : list) {
					removeDupes(list);

					buffered.write(s + "\r\n");
				}
				buffered.close();
			} catch (Exception e) {
				LOGGER.error("Problem while writing file", e);
			}
		}).run();
	}

	private void createFile() {
		if (Mantaro.isDebugEnabled) {
			LOGGER.info("Creating new file " + name + "...");
		}
		if (!file.exists()) {
			file.getParentFile().mkdirs();
			try {
				file.createNewFile();
				create(file, list);
			} catch (Exception ignored) {
			}
		}
	}

	private void read() {
		Async.asyncThread("(StringArrayUtils) File reading thread", () -> {
			LOGGER.info("Reading List file: " + name);
			try {
				FileInputStream imputstream = new FileInputStream(file.getAbsolutePath());
				DataInputStream datastream = new DataInputStream(imputstream);
				BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(datastream));
				String s;
				while ((s = bufferedreader.readLine()) != null) {
					if (!s.startsWith("//")) {
						list.add(s.trim());
					}
				}
				bufferedreader.close();
			} catch (Exception e) {
				LOGGER.warn("Problem while reading file", e);
			}
		}).run();
	}

	private void removeDupes(CopyOnWriteArrayList<String> list) {
		HashSet<String> set = new HashSet<>(list);
		list.clear();
		list.addAll(set);
	}
}