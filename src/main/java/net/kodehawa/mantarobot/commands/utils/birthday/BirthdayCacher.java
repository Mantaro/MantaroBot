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

package net.kodehawa.mantarobot.commands.utils.birthday;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.rethinkdb.model.OptArgs;
import com.rethinkdb.utils.Types;
import net.kodehawa.mantarobot.commands.BirthdayCmd;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.exporters.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class BirthdayCacher {
    private static final Logger log = LoggerFactory.getLogger(BirthdayCacher.class);
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setNameFormat("Mantaro Birthday Assigner Executor").build());
    private final Map<String, BirthdayData> cachedBirthdays = new ConcurrentHashMap<>();
    public volatile boolean isDone;

    public BirthdayCacher() {
        Metrics.THREAD_POOL_COLLECTOR.add("birthday-cacher", executorService);
        log.info("Caching birthdays...");
        cache();
    }

    public void cache() {
        executorService.submit(() -> {
            try {
                List<Map<Object, Object>> m = r.table("users")
                        .run(MantaroData.conn(), OptArgs.of("read_mode", "outdated"), Types.mapOf(Object.class, Object.class))
                        .toList();
                cachedBirthdays.clear();

                for (Map<Object, Object> r : m) {
                    var id = String.valueOf(r.get("id"));
                    // Why?
                    if (cachedBirthdays.containsKey(id))
                        continue;

                    //Blame rethinkdb for the casting hell thx
                    @SuppressWarnings("unchecked")
                    var birthday = ((Map<String, String>) r.get("data")).get("birthday");
                    if (birthday != null && !birthday.isEmpty()) {
                        log.debug("-> PROCESS: {}", r);
                        var bd = birthday.split("-");
                        cachedBirthdays.put(id, new BirthdayData(birthday, bd[0], bd[1]));
                    }
                }

                log.debug("-> [CACHE] Birthdays: {}", cachedBirthdays);
                log.info("Clearing previous guild birthday cache...");
                BirthdayCmd.getGuildBirthdayCache().invalidateAll();
                isDone = true;
                log.info("Cached all birthdays. Current size is {}", cachedBirthdays.size());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public Map<String, BirthdayData> getCachedBirthdays() {
        return cachedBirthdays;
    }

    public static class BirthdayData {
        public String birthday;
        public String day;
        public String month;

        public BirthdayData(String birthday, String day, String month) {
            this.birthday = birthday;
            this.day = day;
            this.month = month;
        }

        public String getBirthday() {
            return this.birthday;
        }

        public void setBirthday(String birthday) {
            this.birthday = birthday;
        }

        public String getDay() {
            return this.day;
        }

        public void setDay(String day) {
            this.day = day;
        }

        public String getMonth() {
            return this.month;
        }

        public void setMonth(String month) {
            this.month = month;
        }

        @Override
        public String toString() {
            return birthday;
        }
    }
}
