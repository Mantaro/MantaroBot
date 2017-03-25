package net.kodehawa.mantarobot.modules;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Module {
	public static class Manager {
		public static Map<String, Pair<Command, Category>> commands = new ConcurrentHashMap<>();
	}

	public final Category category;

	public Module(Category category) {
		this.category = category;
	}

	public void onPostLoad() {
	}

	public void register(String name, Command command) {
		Manager.commands.put(name, Pair.of(command, category));
	}
}
