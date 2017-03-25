package net.kodehawa.mantarobot.commands.utils.data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class BugData {
	public static class Bug {
		public boolean accepted = false;
		public String bug;
		public long messageId;
		public long reporterId;
		public long time;
	}

	public ConcurrentHashMap<Long, Bug> bugs = new ConcurrentHashMap<>();
	public AtomicLong nextId = new AtomicLong(1);
}