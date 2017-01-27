package net.kodehawa.mantarobot.utils;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

public class SimpleFileIO {
	public static List<String> read(File file) throws IOException {
		try (FileReader fr = new FileReader(file); BufferedReader reader = new BufferedReader(fr)) {
			return reader.lines().filter(s -> !s.startsWith("//")).collect(Collectors.toList());
		}
	}

	public static void write(File file, List<String> list) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(list.stream().collect(Collectors.joining("\n")).getBytes("UTF-8"));
		}
	}
}
