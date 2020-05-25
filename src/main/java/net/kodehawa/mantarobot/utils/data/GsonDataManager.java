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

package net.kodehawa.mantarobot.utils.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

public class GsonDataManager<T> implements DataManager<T> {
    public static final Gson GSON_PRETTY = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create(), GSON_UNPRETTY = new GsonBuilder().serializeNulls().create();
    private static final Logger log = LoggerFactory.getLogger(GsonDataManager.class);
    private final Path configPath;
    private final T data;

    public GsonDataManager(Class<T> clazz, String file, Supplier<T> constructor) {
        this.configPath = Paths.get(file);
        if (!configPath.toFile().exists()) {
            log.info("Could not find config file at " + configPath.toFile().getAbsolutePath() + ", creating a new one...");
            try {
                if (configPath.toFile().createNewFile()) {
                    log.info("Generated new config file at " + configPath.toFile().getAbsolutePath() + ".");
                    FileIOUtils.write(configPath, GSON_PRETTY.toJson(constructor.get()));
                    log.info("Please, fill the file with valid properties.");
                } else {
                    log.warn("Could not create config file at " + file);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            System.exit(0);
        }

        try {
            this.data = GSON_PRETTY.fromJson(FileIOUtils.read(configPath), clazz);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Gson gson(boolean pretty) {
        return pretty ? GSON_PRETTY : GSON_UNPRETTY;
    }

    @Override
    public T get() {
        return data;
    }

    @Override
    public void save() {
        try {
            FileIOUtils.write(configPath, GSON_PRETTY.toJson(data));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
