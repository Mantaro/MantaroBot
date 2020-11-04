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

package net.kodehawa.mantarobot.commands.music.listener;

import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMuteEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RateLimiter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class VoiceChannelListener implements EventListener {
    private final RateLimiter vcRatelimiter = new RateLimiter(TimeUnit.SECONDS, 10);

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof GuildVoiceMoveEvent) {
            onGuildVoiceMove((GuildVoiceMoveEvent) event);
        } else if (event instanceof GuildVoiceJoinEvent) {
            onGuildVoiceJoin((GuildVoiceJoinEvent) event);
        } else if (event instanceof GuildVoiceLeaveEvent) {
            onGuildVoiceLeave((GuildVoiceLeaveEvent) event);
        } else if (event instanceof GuildVoiceMuteEvent) {
            onGuildVoiceMute((GuildVoiceMuteEvent) event);
        }
    }

    private void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        if (event.getChannelJoined().getMembers().contains(event.getGuild().getSelfMember())) {
            onJoin(event.getChannelJoined());
        }

        if (event.getChannelLeft().getMembers().contains(event.getGuild().getSelfMember())) {
            onLeave(event.getChannelLeft());
        }
    }

    private void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        if (event.getChannelJoined().getMembers().contains(event.getGuild().getSelfMember())) {
            onJoin(event.getChannelJoined());
        }
    }

    private void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if (event.getChannelLeft().getMembers().contains(event.getGuild().getSelfMember())) {
            onLeave(event.getChannelLeft());
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

        if (event.isMuted()) {
            var scheduler = musicManager.getTrackScheduler();
            if (scheduler.getCurrentTrack() != null && scheduler.getRequestedTextChannel() != null) {
                var textChannel = scheduler.getRequestedTextChannel();
                //Didn't ratelimit this one because mute can only be done by admins and such? Don't think it'll get abused.
                if (textChannel.canTalk()) {
                    textChannel.sendMessageFormat(
                            scheduler.getLanguage().get("commands.music_general.listener.paused"),
                            EmoteReference.SAD
                    ).queue();
                }

                musicManager.getLavaLink().getPlayer().setPaused(true);
            }
        } else {
            if (!isAlone(voiceState.getChannel()) && musicManager.getTrackScheduler().getCurrentTrack() != null) {
                musicManager.getLavaLink().getPlayer().setPaused(false);
            }
        }

    }

    private void onJoin(VoiceChannel vc) {
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
            if (scheduler.getCurrentTrack() != null) {
                if (musicManager.isAwaitingDeath()) {
                    var textChannel = scheduler.getRequestedTextChannel();
                    if (textChannel.canTalk() && vcRatelimiter.process(vc.getGuild().getId())) {
                        textChannel.sendMessageFormat(
                                scheduler.getLanguage().get("commands.music_general.listener.resumed"),
                                EmoteReference.POPPER
                        ).queue();
                    }
                }
            }

            musicManager.cancelLeave();
            musicManager.setAwaitingDeath(false);
            musicManager.getLavaLink().getPlayer().setPaused(false);
        }
    }

    private void onLeave(VoiceChannel vc) {
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
                    ).queue(m -> m.delete()
                            .queueAfter(30, TimeUnit.SECONDS)
                    );
                }
            }

            musicManager.setAwaitingDeath(true);
            musicManager.scheduleLeave();
            musicManager.getLavaLink().getPlayer().setPaused(true);
        }
    }

    private static boolean validate(GuildVoiceState state) {
        return state == null || !state.inVoiceChannel();
    }

    private static boolean isAlone(VoiceChannel vc) {
        return vc.getMembers().stream().allMatch(m -> m.getUser().isBot());
    }
}
