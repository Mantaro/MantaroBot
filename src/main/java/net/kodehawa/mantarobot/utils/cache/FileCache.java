package net.kodehawa.mantarobot.utils.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.SneakyThrows;

import java.io.*;
import java.util.concurrent.ExecutionException;

public class FileCache {
    private final LoadingCache<File, byte[]> cache;

    public FileCache(int maxSize, int concurrencyLevel) {
        cache = CacheBuilder.newBuilder()
                .maximumSize(maxSize)
                .concurrencyLevel(concurrencyLevel)
                .build(new CacheLoader<File, byte[]>() {
                    @SneakyThrows
                    @Override
                    public byte[] load(File key) throws Exception {
                        if(!key.isFile()) throw new IllegalArgumentException(key + ": not a file");
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        FileInputStream fis = new FileInputStream(key);
                        byte[] buffer = new byte[1024];
                        int read;
                        while((read = fis.read(buffer)) != -1) baos.write(buffer, 0, read);
                        fis.close();
                        return baos.toByteArray();
                    }
                });
    }

    public FileCache(int maxSize) {
        this(maxSize, 10);
    }

    public byte[] get(File file) {
        return get(file, true);
    }

    private byte[] get(File file, boolean copy) {
        try {
            return copy ? cache.get(file).clone() : cache.get(file);
        } catch(ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    public InputStream input(File file) {
        return new ByteArrayInputStream(get(file, false));
    }
}
