/*
 * Copyright (C) 2016-2022 David Rubio Escares / Kodehawa
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

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.music.utils.AudioCmdUtils;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.meta.Help;
import net.kodehawa.mantarobot.core.command.meta.Options;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
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
    public static final IncreasingRateLimiter moveRatelimit = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(2)
            .cooldown(15, TimeUnit.SECONDS)
            .maxCooldown(40, TimeUnit.SECONDS)
            .randomIncrement(true)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("move")
            .build();

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(RestartSong.class);
        cr.registerSlash(Forward.class);
        cr.registerSlash(Rewind.class);
        cr.registerSlash(Move.class);
        cr.registerSlash(RemoveTrack.class);
    }

    @Description("Restarts the playback of the current song.")
    @Category(CommandCategory.MUSIC)
    public static class RestartSong extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var manager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
            var lavalinkPlayer = manager.getLavaLink().getPlayer();

            if (lavalinkPlayer.getPlayingTrack() == null) {
                ctx.reply("commands.music_general.not_playing", EmoteReference.ERROR);
                return;
            }

            if (isDJ(ctx, ctx.getMember())) {
                lavalinkPlayer.seekTo(1);
                ctx.reply("commands.restartsong.success", EmoteReference.CORRECT);
            } else {
                ctx.reply("commands.music_general.dj_only", EmoteReference.ERROR);
            }
        }
    }

    @Description("Fast forwards the current song a specified amount of time.")
    @Category(CommandCategory.MUSIC)
    @Options({
            @Options.Option(type = OptionType.STRING, name = "time", description = "The amount of time to forward. Time is in this format: 1m29s (1 minute and 29s), for example.", required = true)
    })
    public static class Forward extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var manager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
            var lavalinkPlayer = manager.getLavaLink().getPlayer();

            if (lavalinkPlayer.getPlayingTrack() == null) {
                ctx.reply("commands.music_general.not_playing", EmoteReference.ERROR);
                return;
            }

            if (isSongOwner(manager.getTrackScheduler(), ctx.getAuthor()) || isDJ(ctx, ctx.getMember())) {
                try {
                    var amt = Utils.parseTime(ctx.getOptionAsString("time"));
                    if (amt < 0) {
                        //same as in rewind here
                        ctx.reply("commands.rewind.negative", EmoteReference.ERROR);
                        return;
                    }

                    if (amt < 1000) {
                        ctx.reply("commands.rewind.too_little", EmoteReference.ERROR);
                        return;
                    }

                    var track = lavalinkPlayer.getPlayingTrack();
                    var position = lavalinkPlayer.getTrackPosition();
                    if (position + amt > track.getDuration()) {
                        ctx.reply("commands.skipahead.past_duration", EmoteReference.ERROR);
                        return;
                    }

                    lavalinkPlayer.seekTo(position + amt);
                    ctx.reply("commands.skipahead.success", EmoteReference.CORRECT, AudioCmdUtils.getDurationMinutes(position + amt));
                } catch (NumberFormatException ex) {
                    ctx.reply("general.invalid_number", EmoteReference.ERROR);
                }
            } else {
                ctx.reply("commands.music_general.dj_only", EmoteReference.ERROR);
            }
        }
    }

    @Description("Rewinds the current song a specified amount of time.")
    @Category(CommandCategory.MUSIC)
    @Options({
            @Options.Option(type = OptionType.STRING, name = "time", description = "The amount of time to rewind. Time is in this format: 1m29s (1 minute and 29s), for example.", required = true)
    })
    public static class Rewind extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var manager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
            var lavalinkPlayer = manager.getLavaLink().getPlayer();
            if (lavalinkPlayer.getPlayingTrack() == null) {
                ctx.reply("commands.music_general.not_playing", EmoteReference.ERROR);
                return;
            }

            if (isSongOwner(manager.getTrackScheduler(), ctx.getAuthor()) || isDJ(ctx, ctx.getMember())) {
                try {
                    var amt = Utils.parseTime(ctx.getOptionAsString("time"));
                    if (amt < 0) {
                        ctx.reply("commands.rewind.negative", EmoteReference.ERROR);
                        return;
                    }

                    if (amt < 1000) {
                        ctx.reply("commands.rewind.too_little", EmoteReference.ERROR);
                        return;
                    }

                    var position = lavalinkPlayer.getTrackPosition();
                    if (position - amt < 0) {
                        ctx.reply("commands.rewind.before_beginning", EmoteReference.ERROR);
                        return;
                    }

                    lavalinkPlayer.seekTo(position - amt);
                    ctx.reply("commands.rewind.success", EmoteReference.CORRECT, AudioCmdUtils.getDurationMinutes(position - amt));
                } catch (NumberFormatException ex) {
                    ctx.reply("general.invalid_number", EmoteReference.ERROR);
                }
            } else {
                ctx.reply("commands.music_general.dj_only", EmoteReference.ERROR);
            }
        }
    }

    @Description("Move me from one VC to another.")
    @Category(CommandCategory.MUSIC)
    @Options({
            @Options.Option(type = OptionType.CHANNEL, name = "channel", description = "The voice channel to move to. If empty, it'll use the voice channel you're in.")
    })
    public static class Move extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var guild = ctx.getGuild();
            if (!RatelimitUtils.ratelimit(moveRatelimit, ctx)) {
                return;
            }

            var audioManager = MantaroBot.getInstance().getAudioManager();
            var option = ctx.getOption("channel");
            VoiceChannel channel = null;
            if (option != null) {
                try {
                    if (option.getAsChannel().getType() == ChannelType.VOICE) {
                        channel = option.getAsChannel().asVoiceChannel();
                    }
                } catch (IllegalStateException ex) {
                    ctx.reply("commands.move.vc_not_found", EmoteReference.ERROR);
                    return;
                }
            }

            if (channel == null) {
                var link = audioManager.getMusicManager(guild).getLavaLink();

                try {
                    var voiceState = ctx.getMember().getVoiceState();
                    var botVoiceState =  ctx.getSelfMember().getVoiceState();
                    if (voiceState == null || botVoiceState == null) {
                        ctx.reply("commands.move.no_voice", EmoteReference.ERROR);
                        return;
                    }

                    var vc = voiceState.getChannel();
                    if (vc == null) {
                        ctx.reply("commands.move.no_voice", EmoteReference.ERROR);
                        return;
                    }

                    if (vc != botVoiceState.getChannel()) {
                        ctx.reply("commands.move.attempt", EmoteReference.THINKING);
                        AudioCmdUtils.openAudioConnection(ctx, link, vc, ctx.getLanguageContext());
                        return;
                    }

                    ctx.reply("commands.move.error_moving", EmoteReference.ERROR);
                    return;
                } catch (Exception e) {
                    if (e instanceof PermissionException) {
                        ctx.reply("commands.move.cannot_connect", EmoteReference.ERROR);
                        return;
                    }

                    ctx.reply("commands.move.non_existent_channel", EmoteReference.ERROR);
                    return;
                }
            }

            try {
                var link = audioManager.getMusicManager(ctx.getGuild()).getLavaLink();
                ctx.reply("commands.move.success", EmoteReference.OK, channel.getName());
                AudioCmdUtils.openAudioConnection(ctx, link, channel, ctx.getLanguageContext());
            } catch (IndexOutOfBoundsException e) {
                ctx.reply("commands.move.vc_not_found", EmoteReference.ERROR);
            }
        }
    }

    @Description("Removes a track or a range of tracks from the queue.")
    @Category(CommandCategory.MUSIC)
    @Options({
            @Options.Option(type = OptionType.STRING, name = "range", description = "The song to remove, or a range of them (1-10). Can also use first or last.", required = true)
    })
    @Help(
            description = "Removes a track or a range of tracks from the queue.",
            parameters = {
                    @Help.Parameter(name = "range", description = """
                            The position of the track in the current queue. You can also use a range, like 1-10.
                            For example, to remove tracks 1 through 7 of the queue, you can specify 1-7.
                            You can use first to remove the first track, next to remove the next track and last to remove the last track.
                            """)
            },
            usage = "`/removetrack <track number/range/first/next/last>` (Any of them, only one at a time)"
    )
    public static class RemoveTrack extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            final var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
            final var trackScheduler = musicManager.getTrackScheduler();
            if (isNotInCondition(ctx, musicManager.getLavaLink())) {
                return;
            }

            if (!isDJ(ctx, ctx.getMember())) {
                ctx.reply("commands.removetrack.not_dj", EmoteReference.ERROR);
                return;
            }

            var queue = trackScheduler.getQueueAsList();
            HashSet<Integer> selected = new HashSet<>();
            var last = Integer.toString(queue.size());
            var param = ctx.getOptionAsString("range");
            var arg = replaceEach(
                    param,
                    new String[]{"first", "next", "last", "all"},
                    new String[]{"1", "1", last, "0-" + last}
            );

            if (arg.contains("-") || arg.contains("~")) {
                var range = param.split("[-~]");
                if (range.length != 2) {
                    ctx.reply("commands.removetrack.invalid_range", EmoteReference.ERROR, param);
                    return;
                }

                try {
                    int iStart = Integer.parseInt(range[0]) - 1, iEnd = Integer.parseInt(range[1]) - 1;
                    if (iStart < 0 || iStart >= queue.size()) {
                        ctx.reply("commands.removetrack.no_track", EmoteReference.ERROR, iStart);
                        return;
                    }

                    if (iEnd < 0 || iEnd >= queue.size()) {
                        ctx.reply("commands.removetrack.no_track", EmoteReference.ERROR, iEnd);
                        return;
                    }

                    var toRemove = IntStream.rangeClosed(iStart, iEnd).boxed().collect(Collectors.toList());
                    selected.addAll(toRemove);
                } catch (NumberFormatException ex) {
                    ctx.reply("commands.removetrack.invalid_number", EmoteReference.ERROR, param);
                    return;
                }
            } else {
                try {
                    var i = Integer.parseInt(arg) - 1;

                    if (i < 0 || i >= queue.size()) {
                        ctx.reply("commands.removetrack.no_track", EmoteReference.ERROR, i);
                        return;
                    }

                    selected.add(i);
                } catch (NumberFormatException ex) {
                    ctx.reply("commands.removetrack.invalid_number_range", EmoteReference.ERROR, arg);
                    return;
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

            ctx.reply("commands.removetrack.success", EmoteReference.CORRECT, initialSize);
            TextChannelGround.of(ctx.getChannel()).dropItemWithChance(0, 10);
        }
    }
}
