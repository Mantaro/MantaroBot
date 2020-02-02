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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

public class FileCache {
    private final LoadingCache<File, byte[]> cache;
    
    public FileCache(int maxSize, int concurrencyLevel) {
        cache = CacheBuilder.newBuilder()
                        .maximumSize(maxSize)
                        .concurrencyLevel(concurrencyLevel)
                        .build(new CacheLoader<>() {
                            @Override
                            public byte[] load(@NotNull File key) throws Exception {
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
