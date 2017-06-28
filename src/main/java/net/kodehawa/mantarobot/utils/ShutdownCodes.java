package net.kodehawa.mantarobot.utils;

public class ShutdownCodes {

    public static int NORMAL = 0;
    public static int REMOTE_SHUTDOWN = 1;
    public static int FATAL_FAILURE = -1;
    public static int RABBITMQ_FAILURE = 1510;
    public static int API_HANDSHAKE_FAILURE = 1520;
    public static int SHARD_FETCH_FAILURE = -110;
    public static int REBOOT_FAILURE = -10;
}
