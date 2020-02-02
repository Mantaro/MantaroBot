/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.music.handlers;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.kodehawa.mantarobot.ExtraRuntimeOptions;

import java.nio.ByteBuffer;

public class AudioPlayerSendHandler implements AudioSendHandler {
    private final AudioPlayer audioPlayer;
    private final MutableAudioFrame frame;
    private AudioFrame lastFrame;
    private int lost;
    private int total;
    
    public AudioPlayerSendHandler(AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
        if(ExtraRuntimeOptions.DISABLE_NON_ALLOCATING_BUFFER) {
            this.frame = null;
        } else {
            this.frame = new MutableAudioFrame();
            frame.setFormat(StandardAudioDataFormats.DISCORD_OPUS);
            frame.setBuffer(ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize()));
        }
    }
    
    @Override
    public boolean canProvide() {
        boolean provided = ExtraRuntimeOptions.DISABLE_NON_ALLOCATING_BUFFER ? (lastFrame = audioPlayer.provide()) != null : audioPlayer.provide(frame);
        if(!audioPlayer.isPaused()) {
            total++;
            if(!provided) lost++;
        }
        return provided;
    }
    
    @Override
    public ByteBuffer provide20MsAudio() {
        return ByteBuffer.wrap((ExtraRuntimeOptions.DISABLE_NON_ALLOCATING_BUFFER ? lastFrame : frame).getData());
    }
    
    @Override
    public boolean isOpus() {
        return true;
    }
    
    public int getLost() {
        return this.lost;
    }
    
    public int getTotal() {
        return this.total;
    }
}
