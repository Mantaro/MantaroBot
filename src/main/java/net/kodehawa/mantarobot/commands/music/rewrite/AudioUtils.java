package net.kodehawa.mantarobot.commands.music.rewrite;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class AudioUtils {
    public static String getQueueList(BlockingQueue<AudioTrackContext> queue) {
        StringBuilder sb = new StringBuilder();
        int n = 1;
        for (AudioTrackContext audioTrack : queue) {
            long aDuration = audioTrack.getDuration();
            String duration = String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(aDuration),
                    TimeUnit.MILLISECONDS.toSeconds(aDuration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(aDuration))
            );

            sb.append("[").append(n)
                    .append("] ")
                    .append(audioTrack.getInfo().title)
                    .append(" **(")
                    .append(duration)
                    .append(")**").append(audioTrack.getDJ() != null ? " DJ: " + audioTrack.getDJ().getName() : "")
                    .append("\n"
                    );
            n++;
        }
        return sb.toString();
    }

    public static String getLength(long length) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(length),
                TimeUnit.MILLISECONDS.toSeconds(length) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(length))
        );
    }

}
