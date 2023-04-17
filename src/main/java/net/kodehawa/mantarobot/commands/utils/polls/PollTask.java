/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.utils.polls;

import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.data.JsonDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.List;

public class PollTask {
    private static final Logger log = LoggerFactory.getLogger(PollTask.class);

    public static void handle() {
        try {
            try (Jedis j = MantaroData.getDefaultJedisPool().getResource()) {
                List<String> polls = j.zrange("zpoll", 0, 14);
                log.debug("Poll check - remainder is: {}", polls.size());

                for (var poll : polls) {
                    var runningPoll = JsonDataManager.fromJson(poll, Poll.class);
                    if (System.currentTimeMillis() >= runningPoll.time()) {
                        runningPoll.end();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
