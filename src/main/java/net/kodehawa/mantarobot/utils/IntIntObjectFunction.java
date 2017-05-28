package net.kodehawa.mantarobot.utils;

@FunctionalInterface
public interface IntIntObjectFunction<T> {
    T apply(int i, int j);
}
