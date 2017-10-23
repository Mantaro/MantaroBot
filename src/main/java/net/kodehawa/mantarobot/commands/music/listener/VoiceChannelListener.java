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

package net.kodehawa.mantarobot.commands.music.listener;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.dv8tion.jda.core.entities.GuildVoiceState;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMuteEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.GuildMusicManager;
import net.kodehawa.mantarobot.core.MantaroCore;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class VoiceChannelListener implements EventListener {

    private final ExecutorService executor;

    public VoiceChannelListener() {
        ThreadFactory normalTPNamedFactory =
                new ThreadFactoryBuilder()
                        .setNameFormat("Mantaro-VoiceExecutor Thread-%d")
                        .build();
        executor = Executors.newCachedThreadPool(normalTPNamedFactory);
    }

    private static boolean validate(GuildVoiceState state) {
        return state == null || !state.inVoiceChannel();
    }

    private static boolean isAlone(VoiceChannel vc) {
        return vc.getMembers().stream().filter(m -> !m.getUser().isBot()).count() == 0;
    }

    @Override
    public void onEvent(Event event) {

        if(!MantaroCore.hasLoadedCompletely()) return;

        if(event instanceof GuildVoiceMoveEvent) {
            executor.execute(() -> onGuildVoiceMove((GuildVoiceMoveEvent) event));
        } else if(event instanceof GuildVoiceJoinEvent) {
            executor.execute(() -> onGuildVoiceJoin((GuildVoiceJoinEvent) event));
        } else if(event instanceof GuildVoiceLeaveEvent) {
            executor.execute(() -> onGuildVoiceLeave((GuildVoiceLeaveEvent) event));
        } else if(event instanceof GuildVoiceMuteEvent) {
            executor.execute(() -> onGuildVoiceMute((GuildVoiceMuteEvent) event));
        }
    }

    private void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        if(event.getChannelJoined().getMembers().contains(event.getGuild().getSelfMember()))
            onJoin(event.getChannelJoined());
        if(event.getChannelLeft().getMembers().contains(event.getGuild().getSelfMember()))
            onLeave(event.getChannelLeft());
    }

    private void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        if(event.getChannelJoined().getMembers().contains(event.getGuild().getSelfMember()))
            onJoin(event.getChannelJoined());
    }

    private void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if(event.getChannelLeft().getMembers().contains(event.getGuild().getSelfMember()))
            onLeave(event.getChannelLeft());
    }

    private void onGuildVoiceMute(GuildVoiceMuteEvent event) {
        if(event.getMember().getUser().getIdLong() != event.getJDA().getSelfUser().getIdLong()) return;
        GuildVoiceState vs = event.getVoiceState();
        if(validate(vs)) return;
        GuildMusicManager gmm = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
        if(gmm != null) {
            if(event.isMuted()) {
                if(gmm.getTrackScheduler().getCurrentTrack() != null && gmm.getTrackScheduler().getRequestedChannelParsed() != null) {
                    gmm.getTrackScheduler().getRequestedChannelParsed().sendMessage(EmoteReference.SAD + "Pausing player because I got muted :(").queue();
                    gmm.getAudioPlayer().setPaused(true);
                }
            } else {
                if(!isAlone(vs.getChannel())) {
                    if(gmm.getTrackScheduler().getCurrentTrack() != null) {
                        gmm.getAudioPlayer().setPaused(false);
                    }
                }
            }
        }
    }

    private void onJoin(VoiceChannel vc) {
        GuildVoiceState vs = vc.getGuild().getSelfMember().getVoiceState();
        if(validate(vs)) return;
        if(!isAlone(vc)) {
            GuildMusicManager gmm = MantaroBot.getInstance().getAudioManager().getMusicManager(vc.getGuild());
            if(gmm != null) {
                if(gmm.getTrackScheduler().getCurrentTrack() != null) {
                    if(gmm.isAwaitingDeath()) {
                        gmm.getTrackScheduler().getRequestedChannelParsed().sendMessage(EmoteReference.POPPER +
                                "Resuming playback because someone joined!").queue();
                    }
                }

                gmm.cancelLeave();
                gmm.setAwaitingDeath(false);
                gmm.getAudioPlayer().setPaused(false);
            }
        }
    }

    private void onLeave(VoiceChannel vc) {
        GuildVoiceState vs = vc.getGuild().getSelfMember().getVoiceState();
        if(validate(vs)) return;
        if(isAlone(vc)) {
            GuildMusicManager gmm = MantaroBot.getInstance().getAudioManager().getMusicManager(vc.getGuild());
            if(gmm != null) {
                if(gmm.getTrackScheduler() != null && gmm.getTrackScheduler().getCurrentTrack() != null && gmm.getTrackScheduler().getRequestedChannelParsed() != null) {
                    gmm.getTrackScheduler().getRequestedChannelParsed().sendMessage(EmoteReference.THINKING + "I'll leave **" + vc.getName() + "** " +
                            "in 2 minutes because I was left all " +
                            "alone :<").queue();
                }

                gmm.setAwaitingDeath(true);
                gmm.scheduleLeave();
                gmm.getAudioPlayer().setPaused(true);
            }
        }
    }
}
