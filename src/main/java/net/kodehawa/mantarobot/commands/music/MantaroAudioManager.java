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

package net.kodehawa.mantarobot.commands.music;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup;
import com.sedmelluq.lava.extensions.youtuberotator.planner.AbstractRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.planner.RotatingNanoIpRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.IpBlock;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.ExtraRuntimeOptions;
import net.kodehawa.mantarobot.commands.music.requester.AudioLoader;
import net.kodehawa.mantarobot.commands.music.requester.TrackScheduler;
import net.kodehawa.mantarobot.commands.music.utils.AudioCmdUtils;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MantaroAudioManager {
    private static final Lazy<Executor> LOAD_EXECUTOR = new Lazy<>(() -> Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                    .setNameFormat("AudioLoadThread-%d")
                    .setDaemon(true)
                    .build()
    ));

    private static final Logger log = LoggerFactory.getLogger(MantaroAudioManager.class);

    private final Map<String, GuildMusicManager> musicManagers;
    private final AudioPlayerManager playerManager;

    @SuppressWarnings("rawtypes")
    public MantaroAudioManager() {
        this.musicManagers = new ConcurrentHashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();

        //Youtube is special because rotation stuff.
        YoutubeAudioSourceManager youtubeAudioSourceManager = new YoutubeAudioSourceManager(true);

        //IPv6 rotation config start
        Config config = MantaroData.config().get();
        if (!config.getIpv6Block().isEmpty()) {
            AbstractRoutePlanner planner;
            String block = config.getIpv6Block();
            List<IpBlock> blocks = Collections.singletonList(new Ipv6Block(block));

            //Damn you, YouTube.
            if (config.getExcludeAddress().isEmpty())
                planner = new RotatingNanoIpRoutePlanner(blocks);
            else {
                try {
                    InetAddress blacklistedGW = InetAddress.getByName(config.getExcludeAddress());
                    planner = new RotatingNanoIpRoutePlanner(blocks, inetAddress -> !inetAddress.equals(blacklistedGW));
                } catch (Exception e) {
                    //Fallback: did I screw up putting the IP in? lmao
                    planner = new RotatingNanoIpRoutePlanner(blocks);
                    e.printStackTrace();
                }
            }

            new YoutubeIpRotatorSetup(planner)
                    .forSource(youtubeAudioSourceManager)
                    .setup();
        }
        //IPv6 rotation config end

        //Register source manager and configure the Player
        playerManager.registerSourceManager(youtubeAudioSourceManager);
        playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        playerManager.registerSourceManager(new BeamAudioSourceManager());
        if (!ExtraRuntimeOptions.DISABLE_NON_ALLOCATING_BUFFER) {
            log.info("Enabled non-allocating audio buffer.");
            playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        }
    }

    public GuildMusicManager getMusicManager(Guild guild) {
        return musicManagers.computeIfAbsent(guild.getId(), id -> new GuildMusicManager(guild.getId()));
    }

    public long getTotalQueueSize() {
        return musicManagers.values().stream().map(m -> m.getTrackScheduler().getQueue().size()).mapToInt(Integer::intValue).sum();
    }

    public void loadAndPlay(GuildMessageReceivedEvent event, String trackUrl, boolean skipSelection, boolean addFirst, I18nContext lang) {
        AudioCmdUtils.connectToVoiceChannel(event, lang).thenAcceptAsync(b -> {
            if (b) {
                GuildMusicManager musicManager = getMusicManager(event.getGuild());
                TrackScheduler scheduler = musicManager.getTrackScheduler();
                scheduler.getMusicPlayer().setPaused(false);

                if (scheduler.getQueue().isEmpty())
                    scheduler.setRepeatMode(null);

                AudioLoader loader = new AudioLoader(musicManager, event, skipSelection, addFirst);
                playerManager.loadItemOrdered(musicManager, trackUrl, loader);
            }
        }, LOAD_EXECUTOR.get());
    }

    public Map<String, GuildMusicManager> getMusicManagers() {
        return this.musicManagers;
    }

    public AudioPlayerManager getPlayerManager() {
        return this.playerManager;
    }
}
