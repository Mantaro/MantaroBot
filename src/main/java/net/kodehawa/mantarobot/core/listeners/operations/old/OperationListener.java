package net.kodehawa.mantarobot.core.listeners.operations.old;

public interface OperationListener {
    int RESET_TIMEOUT = 1, COMPLETED = 2, IGNORED = 3;

    default void onExpire() {

    }

    default void onCancel() {

    }
}
