package net.kodehawa.mantarobot.utils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TimeAmount {
    private final long amount;
    private final TimeUnit unit;
    public TimeAmount(long amount, TimeUnit unit) {
        this.amount = amount;
        this.unit = Objects.requireNonNull(unit);
    }

    public static List<TimeAmount> normalize(List<TimeAmount> list) {
        TimeUnit unit = list.stream().map(TimeAmount::getUnit)
                .min(Comparator.naturalOrder()).orElse(TimeUnit.values()[0]);

        return list.stream().map(amount -> amount.convertTo(unit)).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "TimeAmount{" + amount + " " + unit.toString().toLowerCase() + '}';
    }

    public TimeAmount compress() {
        TimeUnit lossless = this.unit;
        TimeUnit[] timeUnits = TimeUnit.values();
        for(int i = lossless.ordinal() + 1; i < timeUnits.length; i++) {
            TimeUnit timeUnit = timeUnits[i];

            //If do back and forth conversion is lossless
            if(unit.convert(timeUnit.convert(amount, unit), timeUnit) == amount) lossless = timeUnit;
            else break;
        }

        return convertTo(lossless);
    }

    public TimeAmount convertTo(TimeUnit newUnit) {
        if(unit.equals(newUnit)) return this;
        return new TimeAmount(newUnit.convert(amount, unit), newUnit);
    }

    public long getAmount() {
        return amount;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    public void sleep() throws InterruptedException {
        unit.sleep(amount);
    }

    public void timedJoin(Thread thread) throws InterruptedException {
        unit.timedJoin(thread, amount);
    }

    public void timedWait(Object obj) throws InterruptedException {
        unit.timedWait(obj, amount);
    }

    public long toDays() {
        return unit.toDays(amount);
    }

    public long toHours() {
        return unit.toHours(amount);
    }

    public long toMicros() {
        return unit.toMicros(amount);
    }

    public long toMillis() {
        return unit.toMillis(amount);
    }

    public long toMinutes() {
        return unit.toMinutes(amount);
    }

    public long toNanos() {
        return unit.toNanos(amount);
    }

    public long toSeconds() {
        return unit.toSeconds(amount);
    }
}
