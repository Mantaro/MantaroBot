package net.kodehawa.mantarobot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class MusicManager {

    final AudioPlayer player;
    final Scheduler scheduler;

    /**
     * Creates a player and a track scheduler.
     * @param manager Audio player manager to use for creating the player.
     */
    public MusicManager(AudioPlayerManager manager, MessageReceivedEvent event) {
        player = manager.createPlayer();
        scheduler = new Scheduler(event, player);
        player.addListener(scheduler);
    }

    public AudioPlayerSendHandler getSendHandler() {
        return new AudioPlayerSendHandler(player);
    }

    public Scheduler getScheduler(){
        return scheduler;
    }
}
