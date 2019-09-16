/*
 * Copyright (C) 2016-2019 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.services;

import lombok.extern.slf4j.Slf4j;
import net.kodehawa.mantarobot.MantaroBot;
import okhttp3.*;

import static net.kodehawa.mantarobot.data.MantaroData.config;

@Slf4j
public class Carbonitex {
    private final String carbonToken = config().get().carbonToken;
    private final OkHttpClient httpClient = new OkHttpClient();

    public void handle() {
        if(carbonToken != null) {
            long newC = MantaroBot.getInstance().getGuildCache().size();
            try {
                RequestBody body = new FormBody.Builder()
                        .add("key", carbonToken)
                        .add("servercount", String.valueOf(newC))
                        .build();

                Request request = new Request.Builder()
                        .url("https://www.carbonitex.net/discord/data/botdata.php")
                        .post(body)
                        .build();

                Response response = httpClient.newCall(request).execute();
                response.close();
            } catch(Exception ignored) {
            }
        }
    }
}
