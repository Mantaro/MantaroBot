/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.utils.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.jetbrains.annotations.NotNull;

import java.io.*;
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
                        if (!key.isFile()) throw new IllegalArgumentException(key + ": not a file");
                        var baos = new ByteArrayOutputStream();
                        try(var fis = new FileInputStream(key)) {
                            fis.transferTo(baos);
                        }
                        return baos.toByteArray();
                    }
                });
    }

    public FileCache(int maxSize) {
        this(maxSize, 10);
    }

    private byte[] get(File file) {
        try {
            return cache.get(file);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    public InputStream input(File file) {
        return new ByteArrayInputStream(get(file));
    }
}
