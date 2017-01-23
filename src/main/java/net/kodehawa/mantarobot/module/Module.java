package net.kodehawa.mantarobot.module;

import java.util.HashMap;
import java.util.Map;

public class Module {
	public static class Manager {
		public static Map<String, String[]> moduleDescriptions = new HashMap<>();
		public static Map<String, Command> modules = new HashMap<>();
	}

	public final Category cat;

	public Module(Category cat) {
		this.cat = cat;
	}

	public void register(String name, String description, Command command) {
		String[] descriptionBuilder = {
			description,
			getClass().getSimpleName(),
			cat.toString()
		};

		Manager.moduleDescriptions.put(name, descriptionBuilder);
		Manager.modules.put(name, command);
	}

	public void registerCommands() {
	}
}
