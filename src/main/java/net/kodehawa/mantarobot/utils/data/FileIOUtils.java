package net.kodehawa.mantarobot.utils.data;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileIOUtils {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    public static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), UTF8);
    }

    public static void write(Path path, String contents) throws IOException {
        Files.write(path, contents.getBytes(UTF8));
    }
}
