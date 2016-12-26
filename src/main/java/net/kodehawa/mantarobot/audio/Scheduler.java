package net.kodehawa.mantarobot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Scheduler extends AudioEventAdapter {
    private final AudioPlayer player;
    private final BlockingQueue<AudioTrack> queue;

    public Scheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedBlockingQueue<>();
    }

    public void queue(AudioTrack track) {
        if (!player.startTrack(track, true)) {
            queue.offer(track);
        }
    }

    public void nextTrack() {
        player.startTrack(queue.poll(), false);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            nextTrack();
        }
    }

    public int getQueueSize(){
        return queue.size();
    }

    public String getQueueList(){
        StringBuilder sb = new StringBuilder();
        int n = 1;
        for(AudioTrack audioTrack : queue){
            long aDuration = audioTrack.getDuration();
            String duration = (new SimpleDateFormat("MM:ss")).format(new Date(aDuration));
            sb.append(
                    audioTrack.getInfo().title)
                    .append(" (" + duration + ")")
                    .append("[Position in list: " + n + "]")
                    .append("\n"
                    );
            n++;
        }
        return sb.toString();
    }
}
