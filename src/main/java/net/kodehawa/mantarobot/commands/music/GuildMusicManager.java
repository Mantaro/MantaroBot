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

import dev.arbjerg.lavalink.client.Link;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.requester.TrackScheduler;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import reactor.core.Disposable;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GuildMusicManager {
    private final String guildId;

    private final TrackScheduler trackScheduler;
    private boolean isAwaitingDeath;
    private Disposable listener;

    private ScheduledFuture<?> leaveTask = null;

    public GuildMusicManager(String guildId) {
        this.guildId = guildId;

        var lavaLink = MantaroBot.getInstance().getLavaLink().getLink(Long.parseLong(guildId));
        trackScheduler = new TrackScheduler(lavaLink, guildId);
    }

    private void leave() {
        var guild = trackScheduler.getGuild();
        if (guild == null) {
            getLavaLink().destroyPlayer();
            return;
        }

        isAwaitingDeath = false;

        final var requestedTextChannel = trackScheduler.getRequestedTextChannel();
        final var voiceState = guild.getSelfMember().getVoiceState();

        if (requestedTextChannel != null && requestedTextChannel.canTalk() && voiceState != null && voiceState.getChannel() != null) {
            requestedTextChannel.sendMessageFormat(
                    trackScheduler.getLanguage().get("commands.music_general.listener.leave"),
                    EmoteReference.SAD, voiceState.getChannel().getName()
            ).queue();
        }

        //This should destroy it.
        trackScheduler.stop();
    }

    public void scheduleLeave() {
        if (leaveTask != null) {
            return;
        }

        leaveTask = MantaroBot.getInstance().getExecutorService().schedule(this::leave, 2, TimeUnit.MINUTES);
    }

    public void cancelLeave() {
        if (leaveTask == null) {
            return;
        }

        leaveTask.cancel(true);
        leaveTask = null;
    }

    public Link getLavaLink() {
        return MantaroBot.getInstance().getLavaLink().getLink(Long.parseLong(guildId));
    }

    public TrackScheduler getTrackScheduler() {
        return this.trackScheduler;
    }

    public void destroy() {
        getLavaLink().destroyPlayer();
    }

    public boolean isAwaitingDeath() {
        return this.isAwaitingDeath;
    }

    public void setAwaitingDeath(boolean isAwaitingDeath) {
        this.isAwaitingDeath = isAwaitingDeath;
    }
}
