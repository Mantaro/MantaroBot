package net.kodehawa.mantarobot.commands.info;

import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

public class StatsHelper {
    public static <T> DoubleSummaryStatistics calculateDouble(Collection<T> collection, ToDoubleFunction<T> toDouble) {
        return collection.stream().mapToDouble(toDouble).summaryStatistics();
    }

    public static <T> IntSummaryStatistics calculateInt(Collection<T> collection, ToIntFunction<T> toInt) {
        return collection.stream().mapToInt(toInt).summaryStatistics();
    }
}
