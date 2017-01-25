package net.kodehawa.mantarobot.data;

import net.kodehawa.mantarobot.utils.GsonDataManager;

public class DataManager {
	private static GsonDataManager<Config> config;
	private static GsonDataManager<Data> data;

	public static GsonDataManager<Config> getConfig() {
		if (config == null) config = new GsonDataManager<>(Config.class, "./config.json", Config::new);
		return config;
	}

	public static GsonDataManager<Data> getData() {
		if (data == null) data = new GsonDataManager<>(Data.class, "./data.json", Data::new);
		return data;
	}

	public static void main(String[] args) {
		getConfig();
		getData();
	}
}
