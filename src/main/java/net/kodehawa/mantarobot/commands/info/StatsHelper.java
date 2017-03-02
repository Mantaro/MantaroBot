package net.kodehawa.mantarobot.commands.info;

import java.util.Collection;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class StatsHelper {
	public static class CalculatedDoubleValues {
		public final double min, max, avg;

		public CalculatedDoubleValues(double min, double max, double avg) {
			this.min = min;
			this.max = max;
			this.avg = avg;
		}
	}

	public static class CalculatedIntValues {
		public final double avg;
		public final int min, max;

		public CalculatedIntValues(int min, int max, double avg) {
			this.min = min;
			this.max = max;
			this.avg = avg;
		}
	}

	public static CalculatedIntValues calculate(IntStream stream) {
		List<IntStream> streams = clone(stream, 3);
		return new CalculatedIntValues(streams.remove(0).min().orElse(0), streams.remove(0).max().orElse(0), streams.remove(0).average().orElse(0));
	}

	public static CalculatedDoubleValues calculate(DoubleStream stream) {
		List<DoubleStream> streams = clone(stream, 3);
		return new CalculatedDoubleValues(streams.remove(0).min().orElse(0), streams.remove(0).max().orElse(0), streams.remove(0).average().orElse(0));
	}

	public static <T> CalculatedDoubleValues calculateDouble(Collection<T> collection, ToDoubleFunction<T> toDouble) {
		return calculate(collection.stream().mapToDouble(toDouble));
	}

	public static <T> CalculatedIntValues calculateInt(Collection<T> collection, ToIntFunction<T> toInt) {
		return calculate(collection.stream().mapToInt(toInt));
	}

	public static <T> List<IntStream> clone(IntStream original, int times) {
		return clone(original.mapToObj(Integer::valueOf), times).stream().map(s -> s.mapToInt(Integer::intValue)).collect(Collectors.toList());
	}

	public static <T> List<DoubleStream> clone(DoubleStream original, int times) {
		return clone(original.mapToObj(Double::valueOf), times).stream().map(s -> s.mapToDouble(Double::doubleValue)).collect(Collectors.toList());
	}

	public static <T> List<Stream<T>> clone(Stream<T> original, int times) {
		List<T> collect = original.collect(Collectors.toList());
		return IntStream.range(0, times).mapToObj(value -> collect.stream()).collect(Collectors.toList());
	}

}
