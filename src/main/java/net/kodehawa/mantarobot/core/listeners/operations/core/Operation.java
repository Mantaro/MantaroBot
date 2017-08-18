package net.kodehawa.mantarobot.core.listeners.operations.core;

public interface Operation {
    int RESET_TIMEOUT = 1, COMPLETED = 2, IGNORED = 3;

    default void onExpire() {

    }

    default void onCancel() {

    }
}
