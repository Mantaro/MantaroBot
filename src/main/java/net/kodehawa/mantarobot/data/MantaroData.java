package net.kodehawa.mantarobot.data;

import net.kodehawa.mantarobot.commands.utils.data.QuotesData;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;

import java.io.File;
import java.io.IOException;

public class MantaroData {
	private static SimpleFileDataManager bleach;
	private static GsonDataManager<Config> config;
	private static GsonDataManager<Data> data;
	private static SimpleFileDataManager facts;
	private static SimpleFileDataManager greeting;
	private static SimpleFileDataManager hugs;
	private static SimpleFileDataManager kisses;
	private static SimpleFileDataManager noble;
	private static SimpleFileDataManager patting;
	private static GsonDataManager<QuotesData> quotes;
	private static SimpleFileDataManager splashes;
	private static SimpleFileDataManager tsunderelines;

	public static SimpleFileDataManager getBleach() {
		if (bleach == null) bleach = new SimpleFileDataManager("bleach.txt");
		return bleach;
	}

	public static GsonDataManager<Config> getConfig() {
		if (config == null) config = new GsonDataManager<>(Config.class, "config.json", Config::new, true);
		return config;
	}

	public static GsonDataManager<Data> getData() {
		if (data == null) data = new GsonDataManager<>(Data.class, "data.json", Data::new, false);
		return data;
	}

	public static SimpleFileDataManager getFacts() {
		if (facts == null) facts = new SimpleFileDataManager("facts.txt");
		return facts;
	}

	public static SimpleFileDataManager getGreeting() {
		if (greeting == null) greeting = new SimpleFileDataManager("greeting.txt");
		return greeting;
	}

	public static SimpleFileDataManager getHugs() {
		if (hugs == null) hugs = new SimpleFileDataManager("hugs.txt");
		return hugs;
	}

	public static SimpleFileDataManager getKisses() {
		if (kisses == null) kisses = new SimpleFileDataManager("kisses.txt");
		return kisses;
	}

	public static SimpleFileDataManager getNoble() {
		if (noble == null) noble = new SimpleFileDataManager("noble.txt");
		return noble;
	}

	public static SimpleFileDataManager getPatting() {
		if (patting == null) patting = new SimpleFileDataManager("patting.txt");
		return patting;
	}

	public static GsonDataManager<QuotesData> getQuotes() {
		if (quotes == null) quotes = new GsonDataManager<>(QuotesData.class, "quotes.json", QuotesData::new, false);
		return quotes;
	}

	public static SimpleFileDataManager getSplashes() {
		if (splashes == null) splashes = new SimpleFileDataManager("splashes.txt");
		return splashes;
	}

	public static SimpleFileDataManager getTsundereLines() {
		if (tsunderelines == null) tsunderelines = new SimpleFileDataManager("tsunderelines.txt");
		return tsunderelines;
	}

	public static void main(String[] args) throws IOException {
		File testFile = new File("");
		String currentPath = testFile.getAbsolutePath();
		System.out.println("current path is: " + currentPath);
	}
}