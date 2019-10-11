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

package net.kodehawa.mantarobot.commands.utils.reminders;

import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.UUID;

public class Reminder {
    private static final String table = "reminder";
    private static final JedisPool pool = MantaroData.getDefaultJedisPool();
    private static final ManagedDatabase db = MantaroData.db();

    public final String id;
    public final String reminder;

    //When should we fire this.
    public final long time;

    private final long scheduledAtMillis;
    private final String userId;
    private final String guildId;

    private Reminder(String id, String userId, String guildId, String reminder, long scheduledAt, long time) {
        this.id = id;
        this.userId = userId;
        this.guildId = guildId;
        this.reminder = reminder;
        this.time = time;
        this.scheduledAtMillis = scheduledAt;

        DBUser user = db.getUser(userId);
        UserData data = user.getData();
        data.setReminderN(user.getData().getReminderN() + 1);
        user.saveAsync();
    }

    public void schedule() {
        JSONObject r = new JSONObject()
                .put("user", userId)
                .put("guild", guildId)
                .put("scheduledAt", scheduledAtMillis)
                .put("reminder", reminder)
                .put("at", time);

        try (Jedis redis = pool.getResource()) {
            redis.hset(table, id + ":" + userId, r.toString());
        }

        DBUser user = db.getUser(userId);
        UserData data = user.getData();
        data.getReminders().add(id + ":" + userId);
        user.save();
    }

    public void cancel() {
        try (Jedis redis = pool.getResource()) {
            redis.hdel(table, id + ":" + userId);
        }

        DBUser user = db.getUser(userId);
        UserData data = user.getData();
        data.getReminders().remove(id);
        user.save();
    }

    //This is more useful now
    public static void cancel(String userId, String id) {
        try (Jedis redis = pool.getResource()) {
            redis.hdel(table, id);
        }

        DBUser user = db.getUser(userId);
        UserData data = user.getData();
        data.getReminders().remove(id);
        user.save();
    }

    public static class Builder {
        private long current;
        private String reminder;
        private long time;
        private String userId;
        private String guildId;
        private long offset;

        public Builder id(String id) {
            userId = id;
            return this;
        }

        public Builder reminder(String reminder) {
            this.reminder = reminder;
            return this;
        }

        public Builder time(long to) {
            time = to;
            return this;
        }

        public Builder current(long start) {
            current = start;
            return this;
        }

        public Builder guild(String id) {
            guildId = id;
            return this;
        }


        public Reminder build() {
            if(userId == null)
                throw new IllegalArgumentException("User ID cannot be null");
            if(reminder == null)
                throw new IllegalArgumentException("Reminder cannot be null");
            if(guildId == null)
                throw new IllegalArgumentException("Guild ID cannot be null");
            if(time <= 0)
                throw new IllegalArgumentException("Time to remind must be positive and >0");
            if(current <= 0)
                throw new IllegalArgumentException("Current time must be positive and >0");

            return new Reminder(UUID.randomUUID().toString(), userId, guildId, reminder, current, time);
        }
    }
}
