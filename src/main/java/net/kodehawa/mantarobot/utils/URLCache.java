package net.kodehawa.mantarobot.utils;

import com.mashape.unirest.http.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class URLCache {
	private static final Logger LOGGER = LoggerFactory.getLogger("URLCache");
	private static final Map<String, File> cached = new ConcurrentHashMap<>();
	private static File cacheDir = new File(URLCache.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "urlcache_files");

	static {
		if (cacheDir.isFile())
			cacheDir.delete();
		cacheDir.mkdirs();
	}

	public static void changeCacheDir(File newDir) {
		if (newDir == null) throw new NullPointerException("newDir");
		if (!newDir.isDirectory()) throw new IllegalArgumentException("Not a directory: " + newDir);
		cacheDir = newDir;
	}

	public static File getFile(String url) {
		File cachedFile = cached.get(url);
		if (cachedFile != null) return cachedFile;
		File file = null;
		try {
			file = File.createTempFile(url.replace('/', '_').replace(':', '_'), "cache", cacheDir);
			try (InputStream is = Unirest.get(url).asBinary().getRawBody();
				 FileOutputStream fos = new FileOutputStream(file)) {
				byte[] buffer = new byte[1024];
				int read;
				while ((read = is.read(buffer)) != -1)
					fos.write(buffer, 0, read);
				cached.put(url, file);
				return file;
			}
		} catch (Exception e) {
			if (file != null) file.delete();
			LOGGER.error("Error caching", e);
			throw new InternalError();
		}
	}

	private URLCache() {
	}
}
