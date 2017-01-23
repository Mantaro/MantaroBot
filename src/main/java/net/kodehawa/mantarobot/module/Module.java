package net.kodehawa.mantarobot.module;

import java.util.HashMap;

public class Module extends Register {
	public static HashMap<String, String[]> moduleDescriptions = new HashMap<>();
	public static HashMap<String, Callback> modules = new HashMap<>();
	private Category cat;

	public Module() {
	}

	public String[] getDescription(String cmdname) {
		return moduleDescriptions.get(cmdname);
	}

	public void register(String name, String description, Callback callback) {
		//System.out.printf("Loaded %s, %s (Cat %s) \n", name, getClass().getSimpleName(), cat);

		String[] descriptionBuilder = {
			description,
			getClass().getSimpleName(),
			cat.toString()
		};

		moduleDescriptions.put(name, descriptionBuilder);
		modules.put(name, callback);
	}

	public void setCategory(Category c) {
		this.cat = c;
	}
}
