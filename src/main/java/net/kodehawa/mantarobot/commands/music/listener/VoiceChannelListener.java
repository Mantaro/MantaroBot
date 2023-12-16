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

package net.kodehawa.mantarobot.commands.music.listener;

import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMuteEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RateLimiter;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class VoiceChannelListener implements EventListener {
    private final RateLimiter vcRatelimiter = new RateLimiter(TimeUnit.SECONDS, 5);

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof GuildVoiceUpdateEvent voiceUpdateEvent) {
            onGuildVoiceUpdate(voiceUpdateEvent);
        } else if (event instanceof GuildVoiceMuteEvent voiceMuteEvent) {
            onGuildVoiceMute(voiceMuteEvent);
        }
    }

    private void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getNewValue() != null && event.getChannelJoined() != null && event.getChannelLeft() != null) { // assume move
            if (event.getChannelJoined().getMembers().contains(event.getGuild().getSelfMember())) {
                onJoin(event.getChannelJoined());

                // Check if we're alone.
                // It might seem weird to call onLeave, but this is basically what this method does: check if we're alone,
                // then schedule leave if we are alone.
                onLeave(event.getChannelJoined());
            }

            if (event.getChannelLeft().getMembers().contains(event.getGuild().getSelfMember())) {
                onLeave(event.getChannelLeft());
            }

            return;
        }

        if (event.getNewValue() == null && event.getChannelLeft() != null) { // leave
            if (event.getChannelLeft().getMembers().contains(event.getGuild().getSelfMember())) {
                onLeave(event.getChannelLeft());
            } else { // We're not on the VC anymore?
                var musicManager = MantaroBot.getInstance()
                        .getAudioManager()
                        .getMusicManager(event.getGuild());

                if (musicManager == null) {
                    return;
                }

                var scheduler = musicManager.getTrackScheduler();
                var musicPlayer = scheduler.getMusicPlayer();
                var player = musicPlayer.block(Duration.ofMillis(300));
                if (player.getTrack() != null && !player.getPaused()) {
                    scheduler.stopCurrentTrack();
                }

                scheduler.getQueue().clear();
                // Stop the music player.
                scheduler.nextTrack(true, true);

                return;
            }
        }

        if (event.getOldValue() == null && event.getChannelJoined() != null) { // join
            if (event.getChannelJoined().getMembers().contains(event.getGuild().getSelfMember())) {
                onJoin(event.getChannelJoined());
            }
        }
    }

    private void onGuildVoiceMute(GuildVoiceMuteEvent event) {
        if (event.getMember().getUser().getIdLong() != event.getJDA().getSelfUser().getIdLong()) {
            return;
        }

        var voiceState = event.getVoiceState();
        if (validate(voiceState)) {
            return;
        }

        var musicManager = MantaroBot.getInstance()
                .getAudioManager()
                .getMusicManager(event.getGuild());

        if (musicManager == null) {
            return;
        }

        var scheduler = musicManager.getTrackScheduler();
        var player = musicManager.getLavaLink().getPlayer();
        if (event.isMuted()) {
            if (scheduler.getCurrentTrack() != null && scheduler.getRequestedTextChannel() != null) {
                var textChannel = scheduler.getRequestedTextChannel();
                //Didn't ratelimit this one because mute can only be done by admins and such? Don't think it'll get abused.
                if (textChannel != null && textChannel.canTalk()) {
                    textChannel.sendMessageFormat(
                            scheduler.getLanguage().get("commands.music_general.listener.paused"),
                            EmoteReference.SAD
                    ).queue();
                }

                scheduler.getLink().createOrUpdatePlayer()
                        .setPaused(true)
                        .subscribe();
            }
        } else {
            if (voiceState.getChannel() == null) {
                return;
            }

            if (!isAlone(voiceState.getChannel()) && musicManager.getTrackScheduler().getCurrentTrack() != null) {
                if (!scheduler.isPausedManually()) {
                    scheduler.getLink().createOrUpdatePlayer()
                            .setPaused(true)
                            .subscribe();
                }
            }
        }

    }

    private void onJoin(AudioChannel vc) {
        var vs = vc.getGuild().getSelfMember().getVoiceState();
        if (validate(vs)) {
            return;
        }

        if (!isAlone(vc)) {
            var musicManager = MantaroBot.getInstance()
                    .getAudioManager()
                    .getMusicManager(vc.getGuild());

            if (musicManager == null) {
                return;
            }

            var scheduler = musicManager.getTrackScheduler();
            if (musicManager.isAwaitingDeath()) {
                if (scheduler.getCurrentTrack() != null) {
                    var channel = scheduler.getRequestedTextChannel();
                    if (channel.canTalk() && vcRatelimiter.process(vc.getGuild().getId())) {
                        if (scheduler.isPausedManually()) {
                            channel.sendMessageFormat(
                                    scheduler.getLanguage().get("commands.music_general.listener.not_resumed"),
                                    EmoteReference.POPPER
                            ).queue();
                        } else {
                            channel.sendMessageFormat(
                                    scheduler.getLanguage().get("commands.music_general.listener.resumed"),
                                    EmoteReference.POPPER
                            ).queue();
                        }
                    }
                }

                if (!scheduler.isPausedManually()) {
                    scheduler.getLink().createOrUpdatePlayer()
                            .setPaused(false)
                            .subscribe();
                }

                musicManager.cancelLeave();
                musicManager.setAwaitingDeath(false);
            }
        }
    }

    private void onLeave(AudioChannel vc) {
        var vs = vc.getGuild().getSelfMember().getVoiceState();
        if (validate(vs)) {
            return;
        }

        if (isAlone(vc)) {
            var musicManager = MantaroBot.getInstance()
                    .getAudioManager()
                    .getMusicManager(vc.getGuild());

            if (musicManager == null) {
                return;
            }

            var scheduler = musicManager.getTrackScheduler();
            if (scheduler != null && scheduler.getCurrentTrack() != null && scheduler.getRequestedTextChannel() != null) {
                var textChannel = scheduler.getRequestedTextChannel();
                if (textChannel.canTalk() && vcRatelimiter.process(vc.getGuild().getId())) {
                    textChannel.sendMessageFormat(scheduler.getLanguage().get(
                            "commands.music_general.listener.left_alone"),
                            EmoteReference.THINKING, vc.getName()
                    ).queue();
                }
            }

            musicManager.setAwaitingDeath(true);
            musicManager.scheduleLeave();
            if (scheduler != null) {
                scheduler.getLink().createOrUpdatePlayer()
                        .setPaused(true)
                        .subscribe();
            }
        }
    }

    private static boolean validate(GuildVoiceState state) {
        return state == null || !state.inAudioChannel();
    }

    private static boolean isAlone(AudioChannel vc) {
        return vc.getMembers().stream().allMatch(m -> m.getUser().isBot());
    }
}
