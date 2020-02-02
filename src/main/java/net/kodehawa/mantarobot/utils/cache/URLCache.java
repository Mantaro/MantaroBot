/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.utils.cache;

import com.google.common.base.Preconditions;
import net.kodehawa.mantarobot.utils.SentryHelper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class URLCache {
    public static final File DEFAULT_CACHE_DIR = new File("urlcache_files");
    private static final Map<String, File> saved = new ConcurrentHashMap<>();
    private static final OkHttpClient okHttp = new OkHttpClient();
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(URLCache.class);
    private final FileCache cache;
    private File cacheDir;
    
    public URLCache(File cacheDir, int cacheSize) {
        this.cacheDir = cacheDir;
        var path = cacheDir.toPath();
        if(Files.exists(path) && !Files.isDirectory(path)) {
            try {
                Files.delete(path);
                Files.createDirectories(path);
            } catch(IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        
        cache = new FileCache(cacheSize);
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
            
            try(Response response = okHttp.newCall(r).execute();
                FileOutputStream fos = new FileOutputStream(file)) {
                var body = response.body();
                if(body == null) {
                    throw new IllegalStateException("Null response body! Code: " + response.code() + " " + response.message());
                }
                body.byteStream().transferTo(fos);
                saved.put(url, file);
                return file;
            }
        } catch(Exception e) {
            if(file != null) {
                try {
                    Files.delete(file.toPath());
                } catch(IOException e2) {
                    e.addSuppressed(e2);
                }
            }
            e.printStackTrace();
            SentryHelper.captureExceptionContext("Error caching", e, this.getClass(), "Cacher");
            throw new InternalError();
        }
    }
    
    public InputStream getInput(String url) {
        return cache.input(getFile(url));
    }
}
