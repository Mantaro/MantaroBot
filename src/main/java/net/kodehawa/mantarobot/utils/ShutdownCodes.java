package net.kodehawa.mantarobot.utils;

public class ShutdownCodes {

    public static final int NORMAL = 0;
    public static final int REMOTE_SHUTDOWN = 1;
    public static final int FATAL_FAILURE = -1;
    public static final int RABBITMQ_FAILURE = 1510;
    public static final int API_HANDSHAKE_FAILURE = 1520;
    public static final int SHARD_FETCH_FAILURE = -110;
    public static final int REBOOT_FAILURE = -10;
}
