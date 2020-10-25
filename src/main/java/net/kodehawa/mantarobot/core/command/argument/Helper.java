package net.kodehawa.mantarobot.core.command.argument;

import java.util.Objects;

class Helper {
    static <T> boolean contains(T[] array, T value) {
        for(T t : array) {
            if (Objects.equals(t, value)) return true;
        }
        return false;
    }
}
