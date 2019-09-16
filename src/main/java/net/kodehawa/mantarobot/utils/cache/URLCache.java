/*
 * Copyright (C) 2016-2019 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.utils.cache;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.mantarobot.utils.SentryHelper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class URLCache {
    public static final File DEFAULT_CACHE_DIR = new File("urlcache_files");
    private static final Map<String, File> saved = new ConcurrentHashMap<>();
    private static final OkHttpClient okHttp = new OkHttpClient();
    private final FileCache cache;
    private File cacheDir;

    public URLCache(File cacheDir, int cacheSize) {
        this.cacheDir = cacheDir;
        cache = new FileCache(cacheSize);
        if(cacheDir.isFile())
            cacheDir.delete();
        cacheDir.mkdirs();
    }

    public URLCache(int cacheSize) {
        this(DEFAULT_CACHE_DIR, cacheSize);
    }

    public void changeCacheDir(File newDir) {
        if(newDir == null) throw new NullPointerException("newDir");
        if(!newDir.isDirectory()) throw new IllegalArgumentException("Not a directory: " + newDir);
        cacheDir = newDir;
    }

    public File getFile(String url) {
        File cachedFile = saved.get(Preconditions.checkNotNull(url, "url"));
        if(cachedFile != null) return cachedFile;
        File file = null;
        try {
            file = new File(cacheDir, url.replace('/', '_').replace(':', '_'));
            Request r = new Request.Builder()
                    .url(url)
                    .build();

            Response response = okHttp.newCall(r).execute();
            try(InputStream is = response.body().byteStream();
                FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int read;
                while((read = is.read(buffer)) != -1)
                    fos.write(buffer, 0, read);
                saved.put(url, file);
                response.close();
                return file;
            }
        } catch(Exception e) {
            e.printStackTrace();
            if(file != null) file.delete();
            SentryHelper.captureExceptionContext("Error caching", e, this.getClass(), "Cacher");
            throw new InternalError();
        }
    }

    public InputStream getInput(String url) {
        return cache.input(getFile(url));
    }
}
