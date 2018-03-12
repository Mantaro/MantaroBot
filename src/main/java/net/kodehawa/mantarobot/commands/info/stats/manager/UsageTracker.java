package net.kodehawa.mantarobot.commands.info.stats.manager;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "WeakerAccess"})
public class UsageTracker {
    private static final Map<String, UsageTracker> TRACKERS = new ConcurrentHashMap<>();

    private final AtomicInteger second = new AtomicInteger();
    private final CircularIntArray minute = new CircularIntArray(60);
    private final CircularIntArray hour = new CircularIntArray(60);
    private final CircularIntArray day = new CircularIntArray(24);
    private final AtomicInteger total = new AtomicInteger();
    private final Map<String, UsageTracker> subcommands = new ConcurrentHashMap<>();
    private final UsageTracker parent;
    private final String command;

    static {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r->{
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("UsageTrackerUpdateThread");
            return t;
        });
        executor.scheduleAtFixedRate(()->{
            TRACKERS.values().forEach(UsageTracker::rollSecond);
        }, 1, 1, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(()->{
            TRACKERS.values().forEach(UsageTracker::rollMinute);
        }, 1, 1, TimeUnit.MINUTES);
        executor.scheduleAtFixedRate(()->{
            TRACKERS.values().forEach(UsageTracker::rollHour);
        }, 1, 1, TimeUnit.HOURS);
    }

    private UsageTracker(UsageTracker parent, String command) {
        this.parent = parent;
        this.command = command;
    }

    public String getCommandName() {
        return command;
    }

    public void increment() {
        if(parent != null) parent.increment();
        second.incrementAndGet();
        total.incrementAndGet();
    }

    public UsageTracker subcommand(String name) {
        return subcommands.computeIfAbsent(name.toLowerCase(), unused->new UsageTracker(this, name.toLowerCase()));
    }

    public int secondUsages() {
        return second.get();
    }

    public int minuteUsages() {
        return minute.sum() + secondUsages();
    }

    public int hourlyUsages() {
        return hour.sum() + minuteUsages();
    }

    public int dailyUsages() {
        return day.sum() + hourlyUsages();
    }

    public int totalUsages() {
        return total.get();
    }

    private void rollSecond() {
        minute.put(second.getAndSet(0));
        subcommands.values().forEach(UsageTracker::rollSecond);
    }

    private void rollMinute() {
        hour.put(minute.sum());
        subcommands.values().forEach(UsageTracker::rollMinute);
    }

    private void rollHour() {
        day.put(hour.sum());
        subcommands.values().forEach(UsageTracker::rollHour);
    }

    public static UsageTracker tracker(String name) {
        return TRACKERS.computeIfAbsent(name.toLowerCase(), unused->new UsageTracker(null, name.toLowerCase()));
    }

    public static int total(TrackerIntFunction mapper) {
        return TRACKERS.values().stream().mapToInt(mapper::apply).sum();
    }

    public static List<UsageTracker> highestMinute(int limit) {
        return highest(limit, UsageTracker::minuteUsages);
    }

    public static List<UsageTracker> highestHourly(int limit) {
        return highest(limit, UsageTracker::hourlyUsages);
    }

    public static List<UsageTracker> highestDaily(int limit) {
        return highest(limit, UsageTracker::dailyUsages);
    }

    public static List<UsageTracker> highestTotal(int limit) {
        return highest(limit, UsageTracker::totalUsages);
    }

    public static List<UsageTracker> highest(int limit, TrackerIntFunction sortMapper) {
        return TRACKERS.values().stream()
                .sorted(Comparator.comparingInt(sortMapper::apply).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @FunctionalInterface
    public interface TrackerIntFunction {
        int apply(UsageTracker tracker);
    }

    private static class CircularIntArray {
        private final AtomicInteger index = new AtomicInteger();
        private final int size;
        private final int[] array;

        CircularIntArray(int size) {
            this.size = size;
            this.array = new int[size];
        }

        void put(int value) {
            array[index.getAndUpdate(v->(v + 1) % size)] = value;
        }

        int sum() {
            int idx = index.get();
            int sum = 0;
            for(int i = 0; i < size; i++) {
                sum += array[(i + idx) % size];
            }
            return sum;
        }
    }
}
