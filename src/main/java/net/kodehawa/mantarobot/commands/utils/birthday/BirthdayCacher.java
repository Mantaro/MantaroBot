/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
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
 */

package net.kodehawa.mantarobot.commands.utils.birthday;

import com.rethinkdb.model.OptArgs;
import com.rethinkdb.net.Cursor;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.mantarobot.data.MantaroData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.rethinkdb.RethinkDB.r;

/**
 * Caches the birthday date of all users seen on bot startup and adds them to a local ConcurrentHashMap.
 * This will later be used on {@link BirthdayTask}
 */
@Slf4j
public class BirthdayCacher {
    public Map<String, String> cachedBirthdays = new ConcurrentHashMap<>();
    public volatile boolean isDone;

    public BirthdayCacher() {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        log.info("Caching birthdays...");
        executorService.submit(() -> {
            try {
                Cursor<Map> m = r.table("users").run(MantaroData.conn(), OptArgs.of("read_mode", "outdated"));

                List<Map> m1 = m.toList();

                for(Map r : m1) {
                    //Blame rethinkdb for the casting hell thx
                    String birthday = (String) ((HashMap) r.get("data")).get("birthday");
                    if(birthday != null && !birthday.isEmpty()) {
                        log.debug("-> PROCESS: {}", r);
                        cachedBirthdays.putIfAbsent(String.valueOf(r.get("id")), birthday);
                    }
                }

                log.debug("-> [CACHE] Birthdays: {}", cachedBirthdays);

                m.close();
                isDone = true;
                log.info("Cached all birthdays!");
            } catch(Exception e) {
                e.printStackTrace();
            }
        });
    }
}
