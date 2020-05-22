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

import net.kodehawa.mantarobot.utils.SentryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class SimpleFileDataManager implements DataManager<List<String>> {
    public static final Pattern NEWLINE_PATTERN = Pattern.compile("\\r\\n?|\\r?\\n");
    private static final Logger log = LoggerFactory.getLogger(SimpleFileDataManager.class);
    private final List<String> data = new ArrayList<>();
    private final Path path;

    public SimpleFileDataManager(String file) {
        this.path = Paths.get(file);
        if (!this.path.toFile().exists()) {
            log.info("Could not find config file at " + this.path.toFile().getAbsolutePath() + ", creating a new one...");
            try {
                if (this.path.toFile().createNewFile()) {
                    log.info("Generated new config file at " + this.path.toFile().getAbsolutePath() + ".");
                    FileIOUtils.write(this.path, "");
                    log.info("Please, fill the file with valid properties.");
                } else {
                    SentryHelper.captureMessage("Could not create config file at " + file, this.getClass());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        try {
            Collections.addAll(data, NEWLINE_PATTERN.split(FileIOUtils.read(this.path)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        data.removeIf(s -> s.startsWith("//"));
    }

    @Override
    public List<String> get() {
        return data;
    }

    @Override
    public void save() {
        try {
            FileIOUtils.write(path, String.join("\n", this.data));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
