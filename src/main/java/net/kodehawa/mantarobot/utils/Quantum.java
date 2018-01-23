package net.kodehawa.mantarobot.utils;

/**
 * @author amy
 * @since 1/23/18.
 */
public interface Quantum<Q> {
    /**
     * Quantum things are always quantum.
     * @return
     */
    default boolean isQuantum() {
        return true;
    }
}
