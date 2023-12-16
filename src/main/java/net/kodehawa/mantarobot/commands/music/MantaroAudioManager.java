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

package net.kodehawa.mantarobot.commands.music;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.arbjerg.lavalink.client.LavalinkClient;
import dev.arbjerg.lavalink.client.TrackEndEvent;
import dev.arbjerg.lavalink.client.TrackExceptionEvent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.requester.AudioLoader;
import net.kodehawa.mantarobot.commands.music.utils.AudioCmdUtils;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.utils.Lazy;

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

    private final Map<String, GuildMusicManager> musicManagers;
    private final LavalinkClient client;

    public MantaroAudioManager(LavalinkClient client) {
        this.musicManagers = new ConcurrentHashMap<>();
        this.client = client;

        registerTrackEndEvent();
        registerTrackExceptionEvent();
    }

    public GuildMusicManager getMusicManager(Guild guild) {
        return musicManagers.computeIfAbsent(guild.getId(), id -> new GuildMusicManager(guild.getId()));
    }

    public void resetMusicManagerFor(String id) {
        var previousManager = musicManagers.get(id);
        if (previousManager == null) // Nothing to do?
            return;

        previousManager.destroy();
        musicManagers.remove(id);
    }

    public long getTotalQueueSize() {
        return musicManagers.values().stream().map(m -> m.getTrackScheduler().getQueue().size()).mapToInt(Integer::intValue).sum();
    }

    private void registerTrackEndEvent() {
        MantaroBot.getInstance().getLavaLink().on(TrackEndEvent.class).subscribe((event) -> {
            final var guildId = event.getGuildId();
            final var mng = getMusicManagers().get(String.valueOf(guildId));

            if (mng != null) {
                mng.getTrackScheduler().onTrackEnd(event.getEndReason());
            }
        });
    }

    private void registerTrackExceptionEvent() {
        MantaroBot.getInstance().getLavaLink().on(TrackExceptionEvent.class).subscribe((event) -> {
            final var guildId = event.getGuildId();
            final var mng = getMusicManagers().get(String.valueOf(guildId));

            if (mng != null) {
                mng.getTrackScheduler().onTrackException();
            }
        });
    }

    public void loadAndPlay(SlashContext ctx, String trackUrl, boolean skipSelection, boolean addFirst, I18nContext lang) {
        AudioCmdUtils.connectToVoiceChannel(ctx, lang).thenAcceptAsync(bool -> {
            if (bool) {
                var musicManager = getMusicManager(ctx.getGuild());
                var scheduler = musicManager.getTrackScheduler();

                scheduler.getLink().createOrUpdatePlayer()
                        .setPaused(false)
                        .asMono()
                        .subscribe();

                if (scheduler.getQueue().isEmpty()) {
                    scheduler.setRepeatMode(null);
                }

                var state = scheduler.getGuild().getSelfMember().getVoiceState();
                if (state != null && state.getChannel() != null && state.getChannel() instanceof StageChannel stageChannel) {
                    try {
                        stageChannel.requestToSpeak().queue();
                    } catch (IllegalStateException ignored) { }
                }

                var loader = new AudioLoader(musicManager, ctx, skipSelection, addFirst);
                var link = client.getLink(ctx.getGuild().getIdLong());
                link.loadItem(trackUrl).subscribe(loader);
            }
        }, LOAD_EXECUTOR.get());
    }

    public Map<String, GuildMusicManager> getMusicManagers() {
        return this.musicManagers;
    }
}
