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

package net.kodehawa.mantarobot.utils;

import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.annotation.Nullable;
import java.io.IOException;

public class APIUtils {
    private static final Config config = MantaroData.config().get();
    private static final OkHttpClient httpClient = new OkHttpClient();

    @Nullable
    public static Badge getHushBadge(String name, Utils.HushType type) {
        if (!config.needApi) {
            return null; //nothing to query on.
        }

        try {
            var request = new Request.Builder()
                    .url(config.apiTwoUrl + "/mantaroapi/bot/hush")
                    .addHeader("Authorization", config.getApiAuthKey())
                    .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                    .post(RequestBody.create(
                            okhttp3.MediaType.parse("application/json"),
                            new JSONObject()
                                    .put("type", type) //lowercase -> subcat in json
                                    .put("name", name) //key, will return result from type.name
                                    .toString()
                    ))
                    .build();

            try(var response = httpClient.newCall(request).execute()) {
                var body = response.body();

                if (body == null) {
                    throw new IllegalStateException("Body is null");
                }
                return Badge.lookupFromString(new JSONObject(new JSONTokener(
                        body.byteStream()
                )).getString("hush"));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String getFrom(String route) throws IOException {
        var request = new Request.Builder()
                .url(config.apiTwoUrl + route)
                .addHeader("Authorization", config.getApiAuthKey())
                .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                .get()
                .build();

        try(var response = httpClient.newCall(request).execute()) {
            var body = response.body();
            if (body == null) {
                throw new IllegalStateException("Body is null");
            }

            return body.string();
        }
    }

    public static Pair<Boolean, String> getPledgeInformation(String user) {
        if (!config.needApi) {
            return null; //nothing to query on.
        }

        try {
            var request = new Request.Builder()
                    .url(config.apiTwoUrl + "/mantaroapi/bot/patreon/check")
                    .addHeader("Authorization", config.getApiAuthKey())
                    .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                    .post(RequestBody.create(
                            okhttp3.MediaType.parse("application/json"),
                            new JSONObject()
                                    .put("id", user)
                                    .put("context", config.isPremiumBot())
                                    .toString()
                    ))
                    .build();

            try(var response = httpClient.newCall(request).execute()) {
                var body = response.body();
                if (body == null) {
                    throw new IllegalStateException("Body is null");
                }

                var reply = new JSONObject(new JSONTokener(body.byteStream()));

                return new Pair<>(reply.getBoolean("active"), reply.getString("amount"));
            }

        } catch (Exception ex) {
            // Don't disable premium if the api is wonky, no need to be a meanie.
            ex.printStackTrace();

            if (config.isPremiumBot()) {
                // Same as above, but send pledge = false but an amount of 4. This is to signal the
                // handler that we have a wrong reply.
                return Pair.of(false, "100000");
            } else {
                return null;
            }
        }
    }
}
