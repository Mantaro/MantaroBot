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

package net.kodehawa.mantarobot.commands.music.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.utils.StringUtils;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

public class AudioUtils {
    public static String getLength(long length) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(length),
                TimeUnit.MILLISECONDS.toSeconds(length) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(length))
        );
    }

    public static String getQueueList(ConcurrentLinkedDeque<AudioTrack> queue) {
        StringBuilder sb = new StringBuilder();
        int n = 1;
        for (AudioTrack audioTrack : queue) {
            long aDuration = audioTrack.getDuration();
            String duration = String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(aDuration),
                    TimeUnit.MILLISECONDS.toSeconds(aDuration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(aDuration))
            );

            User dj = audioTrack.getUserData() != null ? MantaroBot.getInstance().getShardManager()
                    .getUserById(String.valueOf(audioTrack.getUserData())) : null;

            sb.append("**")
                    .append(n)
                    .append(". [")
                    .append(StringUtils.limit(audioTrack.getInfo().title, 30))
                    .append("](")
                    .append(audioTrack.getInfo().uri)
                    .append(")** (")
                    .append(duration)
                    .append(")")
                    .append(dj != null ? " **[" + dj.getName() + "]**" : "")
                    .append("\n");
            n++;
        }
        return sb.toString();
    }
}
