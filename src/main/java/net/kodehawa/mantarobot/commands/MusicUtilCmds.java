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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.music.utils.AudioCmdUtils;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.kodehawa.mantarobot.commands.MusicCmds.*;
import static org.apache.commons.lang3.StringUtils.replaceEach;

@Module
public class MusicUtilCmds {
    @Subscribe
    public void reset(CommandRegistry cr) {
        cr.register("restartsong", new SimpleCommand(CommandCategory.MUSIC) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var manager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
                var lavalinkPlayer = manager.getLavaLink().getPlayer();

                if (lavalinkPlayer.getPlayingTrack() == null) {
                    ctx.sendLocalized("commands.music_general.not_playing", EmoteReference.ERROR);
                    return;
                }

                if (isDJ(ctx, ctx.getMember())) {
                    lavalinkPlayer.seekTo(1);
                    ctx.sendLocalized("commands.restartsong.success", EmoteReference.CORRECT);
                } else {
                    ctx.sendLocalized("commands.music_general.dj_only", EmoteReference.ERROR);
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Restarts the playback of the current song.")
                        .build();
            }
        });
    }

    @Subscribe
    public void skipahead(CommandRegistry cr) {
        cr.register("forward", new SimpleCommand(CommandCategory.MUSIC) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (args.length == 0) {
                    ctx.sendLocalized("commands.skipahead.no_time", EmoteReference.ERROR);
                    return;
                }

                var manager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
                var lavalinkPlayer = manager.getLavaLink().getPlayer();

                if (lavalinkPlayer.getPlayingTrack() == null) {
                    ctx.sendLocalized("commands.music_general.not_playing", EmoteReference.ERROR);
                    return;
                }

                if (isSongOwner(manager.getTrackScheduler(), ctx.getAuthor()) || isDJ(ctx, ctx.getMember())) {
                    try {
                        var amt = Utils.parseTime(args[0]);
                        if (amt < 0) {
                            //same as in rewind here
                            ctx.sendLocalized("commands.rewind.negative", EmoteReference.ERROR);
                            return;
                        }

                        var track = lavalinkPlayer.getPlayingTrack();
                        var position = lavalinkPlayer.getTrackPosition();
                        if (position + amt > track.getDuration()) {
                            ctx.sendLocalized("commands.skipahead.past_duration", EmoteReference.ERROR);
                            return;
                        }

                        lavalinkPlayer.seekTo(position + amt);
                        ctx.sendLocalized("commands.skipahead.success", EmoteReference.CORRECT, AudioCmdUtils.getDurationMinutes(position + amt));
                    } catch (NumberFormatException ex) {
                        ctx.sendLocalized("general.invalid_number", EmoteReference.ERROR);
                    }
                } else {
                    ctx.sendLocalized("commands.music_general.dj_only", EmoteReference.ERROR);
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Fast forwards the current song a specified amount of time.")
                        .setUsage("~>forward <time>")
                        .addParameter("time",
                                "The amount of minutes to rewind. Time is in this format: 1m29s (1 minute and 29s), for example."
                        ).build();
            }
        });

        cr.registerAlias("forward", "skipahead");
        cr.registerAlias("forward", "seek");
    }

    @Subscribe
    public void rewind(CommandRegistry cr) {
        cr.register("rewind", new SimpleCommand(CommandCategory.MUSIC) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (args.length == 0) {
                    ctx.sendLocalized("commands.rewind.no_time", EmoteReference.ERROR);
                    return;
                }

                var manager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
                var lavalinkPlayer = manager.getLavaLink().getPlayer();
                if (lavalinkPlayer.getPlayingTrack() == null) {
                    ctx.sendLocalized("commands.music_general.not_playing", EmoteReference.ERROR);
                    return;
                }

                if (isSongOwner(manager.getTrackScheduler(), ctx.getAuthor()) || isDJ(ctx, ctx.getMember())) {
                    try {
                        var amt = Utils.parseTime(args[0]);
                        if (amt < 0) {
                            ctx.sendLocalized("commands.rewind.negative", EmoteReference.ERROR);
                            return;
                        }

                        var position = lavalinkPlayer.getTrackPosition();
                        if (position - amt < 0) {
                            ctx.sendLocalized("commands.rewind.before_beginning", EmoteReference.ERROR);
                            return;
                        }

                        lavalinkPlayer.seekTo(position - amt);
                        ctx.sendLocalized("commands.rewind.success", EmoteReference.CORRECT, AudioCmdUtils.getDurationMinutes(position - amt));
                    } catch (NumberFormatException ex) {
                        ctx.sendLocalized("general.invalid_number", EmoteReference.ERROR);
                    }
                } else {
                    ctx.sendLocalized("commands.music_general.dj_only", EmoteReference.ERROR);
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Rewind the current song a specified amount of time.")
                        .setUsage("~>rewind <time>")
                        .addParameter("time", "The amount of minutes to rewind. " +
                                "Time is in this format: 1m29s (1 minute and 29s), for example."
                        ).build();
            }
        });
    }

    @Subscribe
    public void move(CommandRegistry cr) {
        cr.register("move", new SimpleCommand(CommandCategory.MUSIC) {
            final IncreasingRateLimiter rl = new IncreasingRateLimiter.Builder()
                    .limit(1)
                    .spamTolerance(2)
                    .cooldown(15, TimeUnit.SECONDS)
                    .maxCooldown(40, TimeUnit.SECONDS)
                    .randomIncrement(true)
                    .pool(MantaroData.getDefaultJedisPool())
                    .prefix("move")
                    .build();

            @Override
            public void call(Context ctx, String content, String[] args) {
                Guild guild = ctx.getGuild();

                if (!RatelimitUtils.ratelimit(rl, ctx)) {
                    return;
                }

                var audioManager = MantaroBot.getInstance().getAudioManager();
                if (content.isEmpty()) {
                    var link = audioManager.getMusicManager(guild).getLavaLink();

                    try {
                        var voiceState = ctx.getMember().getVoiceState();
                        var botVoiceState =  ctx.getSelfMember().getVoiceState();
                        if (voiceState == null || botVoiceState == null) {
                            ctx.sendLocalized("commands.move.no_voice", EmoteReference.ERROR);
                            return;
                        }

                        var vc = voiceState.getChannel();
                        if (vc == null) {
                            ctx.sendLocalized("commands.move.no_voice", EmoteReference.ERROR);
                            return;
                        }

                        if (vc != botVoiceState.getChannel()) {
                            ctx.sendLocalized("commands.move.attempt", EmoteReference.THINKING);
                            AudioCmdUtils.openAudioConnection(ctx.getEvent(), link, vc, ctx.getLanguageContext());
                            return;
                        }

                        ctx.sendLocalized("commands.move.error_moving", EmoteReference.ERROR);
                        return;
                    } catch (Exception e) {
                        if (e instanceof PermissionException) {
                            ctx.sendLocalized("commands.move.cannot_connect", EmoteReference.ERROR);
                            return;
                        }

                        ctx.sendLocalized("commands.move.non_existent_channel", EmoteReference.ERROR);
                        return;
                    }
                }

                try {
                    var vc = ctx.getGuild().getVoiceChannelsByName(content, true).get(0);
                    var link = audioManager.getMusicManager(ctx.getGuild()).getLavaLink();

                    AudioCmdUtils.openAudioConnection(ctx.getEvent(), link, vc, ctx.getLanguageContext());
                    ctx.sendLocalized("commands.move.success", EmoteReference.OK, vc.getName());
                } catch (IndexOutOfBoundsException e) {
                    ctx.sendLocalized("commands.move.vc_not_found", EmoteReference.ERROR);
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Move me from one VC to another.")
                        .setUsage("`~>move <vc>`")
                        .addParameter("vc", "The voice channel to move to (exact name, case-insensitive). " +
                                "If you don't specify this, I'll try to move to the channel you're currently in.")
                        .build();
            }
        });
    }

    @Subscribe
    public void removetrack(CommandRegistry cr) {
        cr.register("removetrack", new SimpleCommand(CommandCategory.MUSIC) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                final var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
                final var trackScheduler = musicManager.getTrackScheduler();

                if (isNotInCondition(ctx, musicManager.getLavaLink())) {
                    return;
                }

                if (!isDJ(ctx, ctx.getMember())) {
                    ctx.sendLocalized("commands.removetrack.not_dj", EmoteReference.ERROR);
                    return;
                }

                var queue = trackScheduler.getQueueAsList();
                HashSet<Integer> selected = new HashSet<>();
                var last = Integer.toString(queue.size());

                for (var param : args) {
                    var arg = replaceEach(
                            param,
                            new String[]{"first", "next", "last", "all"},
                            new String[]{"1", "1", last, "0-" + last}
                    );

                    if (arg.contains("-") || arg.contains("~")) {
                        var range = content.split("[-~]");
                        if (range.length != 2) {
                            ctx.sendLocalized("commands.removetrack.invalid_range", EmoteReference.ERROR, param);
                            return;
                        }

                        try {
                            int iStart = Integer.parseInt(range[0]) - 1, iEnd = Integer.parseInt(range[1]) - 1;
                            if (iStart < 0 || iStart >= queue.size()) {
                                ctx.sendLocalized("commands.removetrack.no_track", EmoteReference.ERROR, iStart);
                                return;
                            }

                            if (iEnd < 0 || iEnd >= queue.size()) {
                                ctx.sendLocalized("commands.removetrack.no_track", EmoteReference.ERROR, iEnd);
                                return;
                            }

                            var toRemove = IntStream.rangeClosed(iStart, iEnd).boxed().collect(Collectors.toList());
                            selected.addAll(toRemove);
                        } catch (NumberFormatException ex) {
                            ctx.sendLocalized("commands.removetrack.invalid_number", EmoteReference.ERROR, param);
                            return;
                        }
                    } else {
                        try {
                            var i = Integer.parseInt(arg) - 1;

                            if (i < 0 || i >= queue.size()) {
                                ctx.sendLocalized("commands.removetrack.no_track", EmoteReference.ERROR, i);
                                return;
                            }

                            selected.add(i);
                        } catch (NumberFormatException ex) {
                            ctx.sendLocalized("commands.removetrack.invalid_number_range", EmoteReference.ERROR, arg);
                            return;
                        }
                    }
                }

                // Iterators have no concept of index so just make it ourselves.
                int itv = 0;
                var initialSize = selected.size();

                // Removing an element by index on a List causes the next element to be
                // shifted by one, just use an iterator instead.
                for (Iterator<AudioTrack> it = queue.iterator(); it.hasNext();) {
                    // No need to loop if empty.
                    if (selected.isEmpty()) {
                        break;
                    }

                    AudioTrack element = it.next();
                    if (selected.contains(itv)) {
                        it.remove();
                        selected.remove(itv);
                    }

                    itv++;
                }

                // Accept the changed queue instead of the old one.
                trackScheduler.acceptNewQueue(queue);

                ctx.sendLocalized("commands.removetrack.success", EmoteReference.CORRECT, initialSize);
                TextChannelGround.of(ctx.getEvent()).dropItemWithChance(0, 10);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Remove the specified track from the queue.")
                        .setUsage("`~>removetrack <track number/first/next/last>` (Any of them, only one at a time)")
                        .addParameter("track number", "The position of the track in the current queue. " +
                                "You can also specify a range (1-10, for example) to delete the first 10 tracks of the queue.")
                        .addParameter("first", "The first track of the queue.")
                        .addParameter("next", "The next track of the queue.")
                        .addParameter("last", "The last track of the queue.")
                        .build();
            }
        });
    }
}
