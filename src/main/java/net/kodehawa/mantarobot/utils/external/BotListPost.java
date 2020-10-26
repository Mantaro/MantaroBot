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

package net.kodehawa.mantarobot.utils.external;

import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.data.MantaroData;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static net.kodehawa.mantarobot.utils.Utils.httpClient;

public enum BotListPost {
    DISCORD_BOT_LIST("https://discordbotlist.com/api/v1/bots/%s/stats", "guilds", MantaroData.config().get().getDblToken()),
    TOP_GG("https://top.gg/api/bots/%s/stats", "server_count", MantaroData.config().get().getDbotsorgToken()),
    DISCORD_BOATS("https://discord.boats/api/bot/%s", "server_count", MantaroData.config().get().getDiscordBoatsToken()),
    BOTS_ON_DISCORD("https://discordbotlist.com/api/v1/bots/%s/stats", "guilds", MantaroData.config().get().getDblToken());

    private static final Logger log = LoggerFactory.getLogger(BotListPost.class);
    private final String path;
    private final String guildValue;
    private final String token;

    BotListPost(String path, String guildValue, String token) {
        this.path = path;
        this.guildValue = guildValue;
        this.token = token;
    }

    public void createRequest(long currentCount, String clientId) throws IOException {
        if (token == null) {
            return;
        }

        var post = RequestBody.create(MediaType.parse("application/json"),
                new JSONObject()
                        .put(guildValue, currentCount)
                        .toString()
        );

        var request = new Request.Builder()
                .url(String.format(path, clientId))
                .header("User-Agent", MantaroInfo.USER_AGENT)
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .post(post)
                .build();


        httpClient.newCall(request).execute().close();
        log.debug("Updated stats for {} with {}", this, currentCount);
    }

    @Override
    public String toString() {
        return path;
    }
}
