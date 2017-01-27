package net.kodehawa.mantarobot.data;

import net.kodehawa.mantarobot.commands.utils.QuotesData;
import net.kodehawa.mantarobot.utils.GsonDataManager;

import java.io.File;

public class MantaroData {
	private static GsonDataManager<Config> config;
	private static GsonDataManager<Data> data;
	private static GsonDataManager<QuotesData> quotes;

	public static GsonDataManager<Config> getConfig() {
		if (config == null) config = new GsonDataManager<>(Config.class, "./config.json", Config::new);
		return config;
	}

	public static GsonDataManager<Data> getData() {
		if (data == null) data = new GsonDataManager<>(Data.class, "./data.json", Data::new);
		return data;
	}

	public static GsonDataManager<QuotesData> getQuotes() {
		if (quotes == null) quotes = new GsonDataManager<>(QuotesData.class, "./quotes.json", QuotesData::new);
		return quotes;
	}

	public static void main(String[] args) {
		getConfig();
		File testFile = new File("");
		String currentPath = testFile.getAbsolutePath();
		System.out.println("current path is: " + currentPath);

		System.out.println(getConfig().get().token);
		getData();
	}
}