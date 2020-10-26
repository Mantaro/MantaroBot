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
import net.dv8tion.jda.api.entities.Member;
import net.kodehawa.mantarobot.commands.music.GuildMusicManager;
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

    public static String getQueueList(ConcurrentLinkedDeque<AudioTrack> queue, GuildMusicManager manager) {
        var sb = new StringBuilder();
        var n = 1;
        for (var audioTrack : queue) {
            var aDuration = audioTrack.getDuration();
            var duration = String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(aDuration),
                    TimeUnit.MILLISECONDS.toSeconds(aDuration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(aDuration))
            );

            Member dj = null;
            if (audioTrack.getUserData() != null) {
                try {
                    dj = manager.getTrackScheduler().getGuild().retrieveMemberById(String.valueOf(audioTrack.getUserData()), false).complete();
                } catch (Exception ignored) { }
            }

            sb.append("**")
                    .append(n)
                    .append(". [")
                    .append(StringUtils.limit(audioTrack.getInfo().title, 30))
                    .append("](")
                    .append(audioTrack.getInfo().uri)
                    .append(")** (")
                    .append(duration)
                    .append(")")
                    .append(dj != null ? " **[" + dj.getUser().getName() + "]**" : "")
                    .append("\n");
            n++;
        }
        return sb.toString();
    }
}
