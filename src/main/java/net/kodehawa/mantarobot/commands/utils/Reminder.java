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

package net.kodehawa.mantarobot.commands.utils;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Reminder {

    public static final Map<String, List<Reminder>> CURRENT_REMINDERS = new HashMap<>();
    private static final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    public final String reminder;
    public final long time;
    private final long current;
    private final String userId;
    private Future<?> scheduledReminder;

    private Reminder(String userId, String reminder, long current, long time) {
        this.userId = userId;
        this.reminder = reminder;
        this.time = time;
        this.current = current;
        CURRENT_REMINDERS.computeIfAbsent(userId, (id) -> new ArrayList<>());
        DBUser user = MantaroData.db().getUser(userId);
        user.getData().setReminderN(user.getData().getReminderN() + 1);
        user.saveAsync();
    }

    public static void onDeserialization() {
        CURRENT_REMINDERS.forEach((id, reminder) -> {
            for(Reminder r : reminder) {
                r.schedule();
            }
        });
    }

    public void schedule() {
        CURRENT_REMINDERS.computeIfPresent(userId, (id, list) -> {
            list.add(this);
            return list;
        });

        scheduledReminder = service.schedule(() -> {
            User user = MantaroBot.getInstance().getUserById(userId);
            if(user == null) return;
            removeCurrent();

            //Ignore "cannot open a private channel with this user"
            AtomicReference<Consumer<Message>> c = new AtomicReference<>();
            Consumer<Throwable> ignore = (t) -> {
            };

            user.openPrivateChannel().queue(channel -> channel.sendMessage(
                    EmoteReference.POPPER + "**Reminder!**\n" + "You asked me to remind you of: " + reminder + "\nAt: " + new Date(current)
            ).queue(c.get(), ignore));
        }, time - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    public Reminder cancel() {
        removeCurrent();
        scheduledReminder.cancel(true);
        return this;
    }

    private void removeCurrent() {
        CURRENT_REMINDERS.computeIfPresent(userId, (id, list) -> {
            list.remove(this);
            return list;
        });
    }

    public static class Builder {
        private long current;
        private String reminder;
        private long time;
        private String userId;

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


        public Reminder build() {
            if(userId == null) throw new IllegalArgumentException("User ID cannot be null");
            if(reminder == null) throw new IllegalArgumentException("Reminder cannot be null");
            if(time <= 0) throw new IllegalArgumentException("Time to remind must be positive and >0");
            if(current <= 0) throw new IllegalArgumentException("Current time must be positive and >0");
            return new Reminder(userId, reminder, current, time);
        }
    }
}
