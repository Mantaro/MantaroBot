package net.kodehawa.mantarobot.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SimpleFileIO {
	public static List<String> read(File file) throws IOException {
		List<String> list = new ArrayList<>();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String s;
		while ((s = reader.readLine()) != null) if (!s.startsWith("//")) list.add(s.trim());
		reader.close();
		return list;
	}

	public static void write(File file, List<String> list) throws IOException {
		FileOutputStream outputStream = new FileOutputStream(file);
		outputStream.write(list.stream().collect(Collectors.joining("\n")).getBytes("UTF-8"));
		outputStream.close();
	}
}
