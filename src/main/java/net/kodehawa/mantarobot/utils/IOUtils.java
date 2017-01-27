package net.kodehawa.mantarobot.utils;

import java.io.*;
import java.util.stream.Collectors;

public class IOUtils {
	public static String read(File f) throws IOException {
		try (FileReader fr = new FileReader(f); BufferedReader reader = new BufferedReader(fr)) {
			return reader.lines().collect(Collectors.joining());
		}
	}

	public static void write(File f, String s) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(f)) {
			fos.write(s.getBytes("UTF-8"));
		}
	}
}
