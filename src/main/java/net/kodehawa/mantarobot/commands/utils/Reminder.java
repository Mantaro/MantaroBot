package net.kodehawa.mantarobot.commands.utils;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.junit.Assert;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Reminder {

    private static final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    public static final Map<String, List<Reminder>> CURRENT_REMINDERS = new HashMap<>();
    private Future<?> scheduledReminder;
    private final String userId;
    public final String reminder;
    public final long time;
    private final long current;

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

    public void schedule() {
        CURRENT_REMINDERS.computeIfPresent(userId, (id, list) ->{
            list.add(this);
            return list;
        });

        scheduledReminder = service.schedule(() -> {
            User user = MantaroBot.getInstance().getUserById(userId);
            if(user == null) return;
            System.out.println("AAAAAAAA done");
            removeCurrent();

            //Ignore "cannot open a private channel with this user"
            AtomicReference<Consumer<Message>> c = new AtomicReference<>();
            Consumer<Throwable> ignore = (t)->{};

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
        CURRENT_REMINDERS.computeIfPresent(userId, (id, list) ->{
            list.remove(this);
            return list;
        });
    }

    public static void onDeserialization() {
        CURRENT_REMINDERS.forEach((id, reminder) -> {
            for(Reminder r : reminder) {
                r.schedule();
            }
        });
    }

    public static class Builder {
        private String userId;
        private String reminder;
        private long time;
        private long current;

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