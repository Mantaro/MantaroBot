package net.kodehawa.mantarobot.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class IOUtils {
	private static void byteArrayToCharArray(byte[] bytes, char[] chars) {
		for (int i = 0; i < bytes.length; i++)
			chars[i] = (char) bytes[i];
	}

	public static String read(File f) throws IOException {
		StringBuilder sb = new StringBuilder();
		FileInputStream fis = new FileInputStream(f);
		byte[] buffer = new byte[1024];
		char[] charBuffer = new char[1024];
		int read;
		while ((read = fis.read(buffer)) != -1) {
			byteArrayToCharArray(buffer, charBuffer);
			sb.append(charBuffer, 0, read);
		}
		fis.close();
		return sb.toString();
	}

	public static void write(File f, String s) throws IOException {
		FileOutputStream fos = new FileOutputStream(f);
		fos.write(s.getBytes("UTF-8"));
		fos.close();
	}
}
