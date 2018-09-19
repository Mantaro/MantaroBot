/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.ExtraRuntimeOptions;
import net.kodehawa.mantarobot.commands.music.requester.AudioLoader;
import net.kodehawa.mantarobot.commands.music.requester.TrackScheduler;
import net.kodehawa.mantarobot.commands.music.utils.AudioCmdUtils;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.utils.Prometheus;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class MantaroAudioManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MantaroAudioManager.class);

    @Getter
    private final Map<String, GuildMusicManager> musicManagers;
    @Getter
    private final AudioPlayerManager playerManager;

    public MantaroAudioManager() {
        this.musicManagers = new ConcurrentHashMap<>();
        DefaultAudioPlayerManager apm = new DefaultAudioPlayerManager();
        Prometheus.THREAD_POOL_COLLECTOR.add("lavaplayer-track-playback", apm.getExecutor());
        tryTrackingExecutor(apm, "lavaplayer-track-info", "trackInfoExecutorService");
        tryTrackingExecutor(apm, "lavaplayer-scheduled-executor", "scheduledExecutorService");
        this.playerManager = apm;
        YoutubeAudioSourceManager youtubeAudioSourceManager = new YoutubeAudioSourceManager();
        youtubeAudioSourceManager.configureRequests(config -> RequestConfig.copy(config).setCookieSpec(CookieSpecs.IGNORE_COOKIES).build());
        playerManager.registerSourceManager(youtubeAudioSourceManager);
        playerManager.registerSourceManager(new SoundCloudAudioSourceManager());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        playerManager.registerSourceManager(new BeamAudioSourceManager());
        if(!ExtraRuntimeOptions.DISABLE_NON_ALLOCATING_BUFFER) {
            log.info("STARTUP: Disabled non-allocating buffer.");
            playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        }
        //playerManager.getConfiguration().setFilterHotSwapEnabled(true);
    }

    public GuildMusicManager getMusicManager(Guild guild) {
        GuildMusicManager musicManager = musicManagers.computeIfAbsent(guild.getId(), id -> new GuildMusicManager(playerManager, guild.getId()));
        if(guild.getAudioManager().getSendingHandler() == null)
            guild.getAudioManager().setSendingHandler(musicManager.getAudioPlayerSendHandler());
        return musicManager;
    }

    public long getTotalQueueSize() {
        return musicManagers.values().stream().map(m -> m.getTrackScheduler().getQueue().size()).mapToInt(Integer::intValue).sum();
    }

    public void loadAndPlay(GuildMessageReceivedEvent event, String trackUrl, boolean skipSelection, boolean addFirst, I18nContext lang) {
        if(!AudioCmdUtils.connectToVoiceChannel(event, lang))
            return;

        GuildMusicManager musicManager = getMusicManager(event.getGuild());
        TrackScheduler scheduler = musicManager.getTrackScheduler();

        scheduler.getAudioPlayer().setPaused(false);

        if(scheduler.getQueue().isEmpty())
            scheduler.setRepeatMode(null);

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoader(musicManager, event, skipSelection, addFirst));
    }

    private static void tryTrackingExecutor(DefaultAudioPlayerManager manager, String key, String fieldName) {
        try {
            Field f = DefaultAudioPlayerManager.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            Prometheus.THREAD_POOL_COLLECTOR.add(key, (ThreadPoolExecutor)f.get(manager));
        } catch(Exception e) {
            LOGGER.error("Unable to retrieve {} executor from player manager!", key, e);
        }
    }
}
