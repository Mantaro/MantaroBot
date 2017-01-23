package net.kodehawa.mantarobot.module;

import java.util.HashMap;
import java.util.Map;

public class Module extends Register {
	public static Map<String, String[]> moduleDescriptions = new HashMap<>();
	public static Map<String, Command> modules = new HashMap<>();
	private Category cat;

	public Module() {
	}

	public String[] getDescription(String cmdname) {
		return moduleDescriptions.get(cmdname);
	}

	public void register(String name, String description, Command command) {
		//System.out.printf("Loaded %s, %s (Cat %s) \n", name, getClass().getSimpleName(), cat);

		String[] descriptionBuilder = {
			description,
			getClass().getSimpleName(),
			cat.toString()
		};

		moduleDescriptions.put(name, descriptionBuilder);
		modules.put(name, command);
	}

	public void setCategory(Category c) {
		this.cat = c;
	}
}
