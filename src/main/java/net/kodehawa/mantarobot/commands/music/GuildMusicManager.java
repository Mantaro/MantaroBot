/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
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
 */

package net.kodehawa.mantarobot.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.Guild;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.handlers.AudioPlayerSendHandler;
import net.kodehawa.mantarobot.commands.music.requester.TrackScheduler;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GuildMusicManager {
    @Getter
    public final AudioPlayer audioPlayer;
    @Getter
    public final TrackScheduler trackScheduler;
    @Getter
    @Setter
    public boolean isAwaitingDeath;
    private ScheduledFuture<?> leaveTask = null;

    public GuildMusicManager(AudioPlayerManager manager, String guildId) {
        audioPlayer = manager.createPlayer();
        trackScheduler = new TrackScheduler(audioPlayer, guildId);
        audioPlayer.addListener(trackScheduler);
    }

    public void leave() {
        Guild guild = trackScheduler.getGuild();

        if(guild == null) return;

        isAwaitingDeath = false;
        trackScheduler.getQueue().clear();
        if(trackScheduler.getRequestedChannelParsed() != null) {
            trackScheduler.getRequestedChannelParsed().sendMessage(EmoteReference.SAD + "I decided to leave **" + guild.getSelfMember().getVoiceState().getChannel().getName() + "** " +
                    "because I was left all alone :<").queue();
        }
        trackScheduler.nextTrack(true, true);
    }

    public void scheduleLeave() {
        if(leaveTask != null) return;
        leaveTask = MantaroBot.getInstance().getExecutorService().schedule(this::leave, 2, TimeUnit.MINUTES);
    }

    public void cancelLeave() {
        if(leaveTask == null) return;
        leaveTask.cancel(true);
        leaveTask = null;
    }

    public AudioPlayerSendHandler getAudioPlayerSendHandler() {
        return new AudioPlayerSendHandler(audioPlayer);
    }
}
