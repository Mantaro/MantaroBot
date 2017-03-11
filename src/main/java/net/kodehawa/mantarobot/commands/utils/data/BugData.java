package net.kodehawa.mantarobot.commands.utils.data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class BugData {
    public static class Bug {
        public String bug;
        public String reporterId;
        public long time;
    }

    public ConcurrentHashMap<Long, Bug> bugs = new ConcurrentHashMap<>();
    public AtomicLong nextId = new AtomicLong(1);
}