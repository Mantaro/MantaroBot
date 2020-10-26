/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.utils.banner;

import net.dv8tion.jda.api.JDAInfo;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;

import java.util.List;

public class BannerPrinter {
    private String toPrint;

    public BannerPrinter(int spaces) {
        try {
            var defaultBanner = new SimpleFileDataManager("assets/mantaro/texts/banner.txt");
            List<String> lines = defaultBanner.get();
            var builder = new StringBuilder();

            if (spaces >= 1) {
                builder.append("\n".repeat(spaces));
            }

            for (var line : lines) {
                var toPrint = line
                        .replace("%version%", MantaroInfo.VERSION)
                        .replace("%jdaversion%", JDAInfo.VERSION);

                builder.append(toPrint).append("\n");
            }

            if (spaces > 1) {
                builder.append("\n".repeat(spaces - 1));
            }

            toPrint = builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            toPrint = "Exception arose while getting the default banner";
        }
    }

    public void printBanner() {
        System.out.println(toPrint);
    }
}
