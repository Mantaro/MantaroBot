package net.kodehawa.mantarobot.utils.banner;

import net.dv8tion.jda.core.JDAInfo;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;

import java.util.List;

public class BannerPrinter {
    private final DataManager<List<String>> defaultBanner = new SimpleFileDataManager("assets/mantaro/texts/banner.txt");
    private String toPrint;

    public BannerPrinter(int spaces) {
        try {
            List<String> lines = defaultBanner.get();
            StringBuilder builder = new StringBuilder();

            if(spaces >= 1) {
                for(int i = 0; i < spaces; i++) {
                    builder.append("\n");
                }
            }

            for(String line : lines) {
                String toPrint = line.replace("%version%", MantaroInfo.VERSION)
                        .replace("%jdaversion%", JDAInfo.VERSION);
                builder.append(toPrint).append("\n");
            }

            if(spaces > 1) {
                for(int i = 1; i < spaces; i++) {
                    builder.append("\n");
                }
            }

            toPrint = builder.toString();
        } catch(Exception e) {
            e.printStackTrace();
            toPrint = "Exception arised while getting the default banner";
        }

    }

    public BannerPrinter(int spaces, DataManager<List<String>> dm) {
        List<String> lines = dm.get();
        StringBuilder builder = new StringBuilder();

        if(spaces >= 1) {
            for(int i = 0; i < spaces; i++) {
                builder.append("\n");
            }
        }

        for(String line : lines) {
            String toPrint = line.replace("%version%", MantaroInfo.VERSION)
                    .replace("%jdaversion%", JDAInfo.VERSION);
            builder.append(toPrint).append("\n");
        }

        if(spaces >= 1) {
            for(int i = 0; i < spaces; i++) {
                builder.append("\n");
            }
        }

        toPrint = builder.toString();
    }

    public BannerPrinter() {
        try {
            List<String> lines = defaultBanner.get();
            StringBuilder builder = new StringBuilder();
            for(String line : lines) {
                String toPrint = line.replace("%version%", MantaroInfo.VERSION)
                        .replace("%jdaversion%", JDAInfo.VERSION);
                builder.append(toPrint).append("\n");
            }
            toPrint = builder.toString();
        } catch(Exception e) {
            e.printStackTrace();
            toPrint = "Exception arised while getting the default banner";
        }
    }

    public BannerPrinter(DataManager<List<String>> dm) {
        List<String> lines = dm.get();
        StringBuilder builder = new StringBuilder();
        for(String line : lines) {
            String toPrint = line.replace("%version%", MantaroInfo.VERSION)
                    .replace("%jdaversion%", JDAInfo.VERSION);
            builder.append(toPrint).append("\n");
        }
        toPrint = builder.toString();
    }

    public void printBanner() {
        System.out.println(toPrint);
    }
}
