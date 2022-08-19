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
import lavalink.client.io.jda.JdaLink;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.music.requester.TrackScheduler;
import net.kodehawa.mantarobot.commands.music.utils.AudioCmdUtils;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.*;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;

import static net.kodehawa.mantarobot.commands.music.utils.AudioCmdUtils.embedForQueue;

@Module
public class MusicCmds {
    private static final Logger log = LoggerFactory.getLogger(MusicCmds.class);

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(Play.class);
        cr.registerSlash(Pause.class);
        cr.registerSlash(Skip.class);
        cr.registerSlash(Queue.class);
        cr.registerSlash(NowPlaying.class);
        cr.registerSlash(Repeat.class);
        cr.registerSlash(Shuffle.class);
        cr.registerSlash(Stop.class);
        cr.registerSlash(Volume.class);
    }

    @Description("Plays a song.")
    @Category(CommandCategory.MUSIC)
    @Options({
            @Options.Option(type = OptionType.STRING, name = "song", description = "The song to play. Can be an URL or a search term.", required = true),
            @Options.Option(type = OptionType.BOOLEAN, name = "soundcloud", description = "Whether to search in soundcloud. Only use to search."),
            @Options.Option(type = OptionType.BOOLEAN, name = "top", description = "Puts song at the start of the queue. Requires DJ permissions, or Manage Server."),
            @Options.Option(type = OptionType.BOOLEAN, name = "first", description = "Whether to skip song selection and play the first song, when using search terms."),
    })
    @Help(
            description = """
                    Play songs! This connects to the voice channel you're connected to and starts playing music.
                    If the bot is already connected to a channel, this will just queue the song. You can either search or put an URL.
                    You can set the soundcloud parameter to true to search in soundcloud's library.
                    """,
            usage = "`/play song:<query> soundcloud:[true/false] top:[true/false] first:[true/false]` (Example: `/play song:bad guy` or `/play song:https://www.youtube.com/watch?v=DyDfgMOUjCI`)",
            parameters = {
                    @Help.Parameter(name = "song", description = "The song to play. Can be an URL or a search term."),
                    @Help.Parameter(name = "soundcloud", description = "Whether to search in soundcloud. Only use to search.", optional = true),
                    @Help.Parameter(name = "top", description = "Puts song at the start of the queue. Requires DJ permissions, or Manage Server.", optional = true),
                    @Help.Parameter(name = "first", description = "Whether to skip song selection and play the first song, when using search terms.", optional = true),
            }
    )
    public static class Play extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            boolean force = ctx.getOptionAsBoolean("top");
            if (!force) {
                play(ctx, ctx.getOptionAsString("song"), ctx.getOptionAsBoolean("soundcloud"), false, ctx.getOptionAsBoolean("first"));
            } else {
                if (isDJ(ctx, ctx.getMember())) {
                    play(ctx, ctx.getOptionAsString("song"), ctx.getOptionAsBoolean("soundcloud"), true, ctx.getOptionAsBoolean("first"));
                } else {
                    ctx.sendLocalized("commands.music_general.dj_only", EmoteReference.ERROR);
                }
            }
        }
    }

    @Description("Pauses the current playing song.")
    @Category(CommandCategory.MUSIC)
    public static class Pause extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            final var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
            final var trackScheduler = musicManager.getTrackScheduler();
            if (isNotInCondition(ctx, musicManager.getLavaLink())) {
                return;
            }

            var paused = !trackScheduler.getMusicPlayer().isPaused();
            var languageContext = ctx.getLanguageContext();

            trackScheduler.setPausedManually(paused);
            var toSend = EmoteReference.MEGA + (paused ? languageContext.get("commands.pause.paused") : languageContext.get("commands.pause.unpaused"));
            trackScheduler.getMusicPlayer().setPaused(paused);
            ctx.reply(toSend);

            TextChannelGround.of(ctx.getChannel()).dropItemWithChance(0, 10);
        }
    }

    @Description("Skips a song.")
    @Options({@Options.Option(type = OptionType.BOOLEAN, name = "force", description = "Whether to skip vote. DJ/Admin only.")})
    @Category(CommandCategory.MUSIC)
    public static class Skip extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            if (ctx.getOptionAsBoolean("force")) {
                var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
                var scheduler = musicManager.getTrackScheduler();

                if (isNotInCondition(ctx, musicManager.getLavaLink())) {
                    return;
                }

                ctx.sendLocalized("commands.forceskip.success", EmoteReference.CORRECT);
                scheduler.nextTrack(true, true);
                return;
            }

            try {
                var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
                var scheduler = musicManager.getTrackScheduler();

                if (isNotInCondition(ctx, musicManager.getLavaLink())) {
                    return;
                }

                var author = ctx.getAuthor();
                if (isSongOwner(scheduler, author) || isDJ(ctx, ctx.getMember())) {
                    ctx.reply("commands.skip.dj_skip", EmoteReference.CORRECT);
                    scheduler.nextTrack(true, true);
                    return;
                }

                var guildData = ctx.getDBGuild().getData();

                if (!guildData.isMusicVote()) {
                    ctx.reply("commands.skip.success", EmoteReference.CORRECT);
                    scheduler.nextTrack(true, true);
                } else {
                    List<String> voteSkips = scheduler.getVoteSkips();
                    var requiredVotes = scheduler.getRequiredVotes();

                    if (voteSkips.contains(author.getId())) {
                        voteSkips.remove(author.getId());
                        ctx.reply("commands.skip.vote.remove", EmoteReference.CORRECT, requiredVotes - voteSkips.size());
                    } else {
                        voteSkips.add(author.getId());
                        if (voteSkips.size() >= requiredVotes) {
                            ctx.reply("commands.skip.success", EmoteReference.CORRECT);
                            scheduler.nextTrack(true, true);
                            return;
                        }

                        ctx.reply("commands.skip.vote.submit", EmoteReference.OK, requiredVotes - voteSkips.size());
                    }
                }

                TextChannelGround.of(ctx.getChannel()).dropItemWithChance(0, 10);
            } catch (NullPointerException e) {
                ctx.reply("commands.skip.no_track", EmoteReference.ERROR);
            }
        }
    }

    @Description("See what track is playing now.")
    @Category(CommandCategory.MUSIC)
    public static class NowPlaying extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            final var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
            final var trackScheduler = musicManager.getTrackScheduler();
            final var audioPlayer = trackScheduler.getMusicPlayer();
            final var playingTrack = audioPlayer.getPlayingTrack();
            if (playingTrack == null) {
                ctx.reply("commands.np.no_track", EmoteReference.ERROR);
                return;
            }

            var npEmbed = new EmbedBuilder();
            final var now = audioPlayer.getTrackPosition();
            final var total = audioPlayer.getPlayingTrack().getDuration();
            final var languageContext = ctx.getLanguageContext();
            final var trackInfo = playingTrack.getInfo();

            npEmbed.setAuthor(languageContext.get("commands.np.header"), null, ctx.getGuild().getIconUrl())
                    .setThumbnail("https://i.imgur.com/FWKIR7N.png")
                    .setDescription("""
                                        \u23ef %s
                                        
                                        **[%s](%s)** `(%s/%s)`
                                        """.formatted(Utils.getProgressBar(now, total),
                                    trackInfo.title,
                                    trackInfo.uri,
                                    AudioCmdUtils.getDurationMinutes(now), total == Long.MAX_VALUE ?
                                            "stream" : AudioCmdUtils.getDurationMinutes(total)
                            )
                    ).setFooter("Enjoy the music! <3. " +
                            "Use ~>lyrics current to see the lyrics of the current song!", ctx.getAuthor().getAvatarUrl());

            ctx.reply(npEmbed.build());
            TextChannelGround.of(ctx.getChannel()).dropItemWithChance(0, 10);
        }
    }

    @Description("Shows the current music queue.")
    @Category(CommandCategory.MUSIC)
    public static class Queue extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {}

        @Description("Shows the current music queue.")
        public static class Show extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                if (!ctx.getSelfMember().hasPermission(ctx.getChannel(), Permission.MESSAGE_EMBED_LINKS)) {
                    ctx.reply("general.missing_embed_permissions");
                    return;
                }

                var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
                embedForQueue(ctx, musicManager, ctx.getLanguageContext());
                TextChannelGround.of(ctx.getChannel()).dropItemWithChance(0, 10);
            }
        }

        @Description("Clears the current queue. Needs DJ or Manage Server.")
        public static class Clear extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var mantaroAudioManager = ctx.getAudioManager();
                var musicManager = mantaroAudioManager.getMusicManager(ctx.getGuild());

                if (isNotInCondition(ctx, musicManager.getLavaLink())) {
                    return;
                }

                if (isDJ(ctx, ctx.getMember())) {
                    musicManager.getLavaLink().getPlayer().stopTrack();
                    musicManager.getTrackScheduler().stop();
                    var tempLength = musicManager.getTrackScheduler().getQueue().size();
                    ctx.reply("commands.music_general.queue.clear_success", EmoteReference.CORRECT, tempLength);

                    return;
                }

                ctx.reply("commands.music_general.queue.clear_error", EmoteReference.ERROR);
            }
        }
    }

    @Description("Repeats a song, or the entire queue.")
    @Category(CommandCategory.MUSIC)
    @Options({
            @Options.Option(type = OptionType.BOOLEAN, name = "queue", description = "Repeat the entire queue instead of only the current song.")
    })
    @Help(
            description = """
            Repeats a song or the queue, or disables it. This command is a toggle.
            It will **disable** repeat if it's ran when it's turned on, and of course enable repeat if repeat it's off.
            To repeat the queue, pass true to the queue argument.
            """,
            usage = "/repeat queue:[true/false]",
            parameters = @Help.Parameter(name = "queue", description = "Repeat the entire queue instead of only the current song.", optional = true)
    )
    public static class Repeat extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
            if (isNotInCondition(ctx, musicManager.getLavaLink())) {
                return;
            }

            final var repeatQueue = ctx.getOptionAsBoolean("queue");
            final var trackScheduler = musicManager.getTrackScheduler();

            if (repeatQueue) {
                if (trackScheduler.getRepeatMode() == TrackScheduler.Repeat.QUEUE) {
                    trackScheduler.setRepeatMode(null);
                    ctx.sendLocalized("commands.repeat.queue_cancel", EmoteReference.CORRECT);
                } else {
                    trackScheduler.setRepeatMode(TrackScheduler.Repeat.QUEUE);
                    ctx.sendLocalized("commands.repeat.queue_repeat", EmoteReference.CORRECT);
                }
            } else {
                if (trackScheduler.getRepeatMode() == TrackScheduler.Repeat.SONG) {
                    trackScheduler.setRepeatMode(null);
                    ctx.reply("commands.repeat.song_cancel", EmoteReference.CORRECT);
                } else {
                    trackScheduler.setRepeatMode(TrackScheduler.Repeat.SONG);
                    ctx.reply("commands.repeat.song_repeat", EmoteReference.CORRECT);
                }
            }

            TextChannelGround.of(ctx.getChannel()).dropItemWithChance(0, 10);
        }
    }

    @Description("Shuffles the current queue.")
    @Category(CommandCategory.MUSIC)
    public static class Shuffle extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
            if (isNotInCondition(ctx, musicManager.getLavaLink())) {
                return;
            }

            musicManager.getTrackScheduler().shuffle();
            ctx.reply("commands.shuffle.success", EmoteReference.OK);
            TextChannelGround.of(ctx.getChannel()).dropItemWithChance(0, 10);
        }
    }

    @Description("Clears the queue and leaves the current voice channel.")
    @Category(CommandCategory.MUSIC)
    public static class Stop extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            try {
                var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
                var scheduler = musicManager.getTrackScheduler();

                if (isNotInCondition(ctx, musicManager.getLavaLink())) {
                    return;
                }

                if (isDJ(ctx, ctx.getMember())) {
                    ctx.reply("commands.stop.dj_stop", EmoteReference.CORRECT);
                    stopCurrent(ctx);
                    return;
                }

                var guildData = ctx.getDBGuild().getData();
                var author = ctx.getAuthor();

                if (!guildData.isMusicVote()) {
                    ctx.reply("commands.stop.success", EmoteReference.CORRECT);
                    stopCurrent(ctx);
                } else {
                    List<String> stopVotes = scheduler.getVoteStop();
                    var requiredVotes = scheduler.getRequiredVotes();

                    if (stopVotes.contains(author.getId())) {
                        stopVotes.remove(author.getId());
                        ctx.reply("commands.stop.vote.remove", EmoteReference.CORRECT, requiredVotes - stopVotes.size());
                    } else {
                        stopVotes.add(author.getId());
                        if (stopVotes.size() >= requiredVotes) {
                            ctx.reply("commands.stop.success", EmoteReference.CORRECT);
                            stopCurrent(ctx);
                            return;
                        }

                        ctx.reply("commands.stop.vote.submit", EmoteReference.OK, requiredVotes - stopVotes.size());
                    }
                }
            } catch (NullPointerException e) {
                ctx.reply("commands.stop.no_player", EmoteReference.ERROR);
            }
        }
    }

    @Description("Sets or checks the current volume.")
    @Category(CommandCategory.MUSIC)
    @Options({
            @Options.Option(type = OptionType.INTEGER, name = "volume", description = "The volume to use. Values are 4-100. Leave empty to check current volume.", minValue = 4, maxValue = 100),
    })
    @Help(
            description = """
                    Sets the playback volume. Use `/volume` to check the volume.
                    **This is a *donator-only* feature!**
                    """
    )
    public static class Volume extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            if (ctx.getDBUser().isPremium() || ctx.getDBGuild().isPremium() || ctx.getConfig().getOwners().contains(ctx.getAuthor().getId())) {
                var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
                var lavalink = musicManager.getLavaLink();
                var volume = ctx.getOptionAsInteger("volume");

                if (ctx.getOptionAsBoolean("check") || volume == 0) {
                    var player = lavalink.getPlayer();
                    if (player.getPlayingTrack() == null) {
                        ctx.reply("commands.volume.no_player", EmoteReference.ERROR);
                        return;
                    }

                    final var filters = player.getFilters();
                    volume = (int) (filters.getVolume() * 100);
                    ctx.reply("commands.volume.check", EmoteReference.ZAP, volume, Utils.bar(volume, 50));
                    return;
                }

                if (isNotInCondition(ctx, musicManager.getLavaLink())) {
                    return;
                }

                try {
                    volume = Math.max(4, Math.min(100, volume));
                } catch (Exception e) {
                    ctx.reply("general.invalid_number", EmoteReference.ERROR);
                    return;
                }

                float finalVolume = volume / 100.0f;
                lavalink.getPlayer().getFilters()
                        .setVolume(finalVolume)
                        .commit();

                ctx.reply("commands.volume.success",
                        EmoteReference.OK, volume, Utils.bar(volume, 50)
                );
            } else {
                ctx.reply("commands.volume.premium_only", EmoteReference.ERROR);
            }
        }
    }

    private static void play(SlashContext ctx, String content, boolean soundcloud, boolean force, boolean firstSelection) {
        if (content.trim().isEmpty()) {
            ctx.reply("commands.music_general.no_song", EmoteReference.ERROR);
            return;
        }

        try {
            new URL(content);
        } catch (Exception e) {
            if (content.startsWith("soundcloud")) {
                var name = content.substring("soundcloud".length()).trim();
                if (name.isEmpty()) {
                    ctx.reply("commands.music_general.soundcloud_no_args", EmoteReference.ERROR);
                    return;
                }
                content = "scsearch: " + content;
            } else content = "ytsearch: " + content;
        }

        MantaroBot.getInstance().getAudioManager().loadAndPlay(ctx, content, firstSelection, force, ctx.getLanguageContext());
        TextChannelGround.of(ctx.getChannel()).dropItemWithChance(0, 5);
    }

    public static boolean isDJ(SlashContext ctx, Member member) {
        var djRole = member.getGuild().getRolesByName("DJ", true).stream().findFirst().orElse(null);
        var guildData = ctx.getDBGuild().getData();
        Role customDjRole = null;

        if (guildData.getDjRoleId() != null) {
            customDjRole = member.getGuild().getRoleById(guildData.getDjRoleId());
        }

        return member.isOwner()
                || member.hasPermission(Permission.MANAGE_SERVER)
                || member.hasPermission(Permission.ADMINISTRATOR)
                || (djRole != null && member.getRoles().contains(djRole))
                || (customDjRole != null && member.getRoles().contains(customDjRole));
    }

    private static void sendNotConnectedToMyChannel(SlashContext ctx) {
        ctx.edit("commands.music_general.not_connected", EmoteReference.ERROR);
    }

    /**
     * This only fires on manual stop!
     *
     * @param ctx The command context
     */
    private static void stopCurrent(SlashContext ctx) {
        try {
            var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
            var trackScheduler = musicManager.getTrackScheduler();
            var musicPlayer = trackScheduler.getMusicPlayer();

            if (musicPlayer.getPlayingTrack() != null && !musicPlayer.isPaused()) {
                musicPlayer.stopTrack();
            }

            var TEMP_QUEUE_LENGTH = trackScheduler.getQueue().size();
            trackScheduler.getQueue().clear();

            if (TEMP_QUEUE_LENGTH > 0) {
                ctx.edit("commands.stop.cleanup", EmoteReference.OK, TEMP_QUEUE_LENGTH);
            }

            // This ends up calling TrackScheduler#onTrackStart -> currentTrack == null -> TrackScheduler#onStop!
            // Beware to not close the connection twice...
            trackScheduler.nextTrack(true, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isSongOwner(TrackScheduler scheduler, User author) {
        return scheduler.getCurrentTrack().getUserData() != null && String.valueOf(scheduler.getCurrentTrack().getUserData()).equals(author.getId());
    }

    public static boolean isNotInCondition(SlashContext ctx, JdaLink player) {
        var selfVoiceState = ctx.getSelfMember().getVoiceState();
        var voiceState = ctx.getMember().getVoiceState();

        try {
            // Maybe?
            if (isDJ(ctx, ctx.getMember())) {
                return false;
            }

            // We can't do anything if voiceChannel is null, so send not connected.
            if (voiceState == null || voiceState.getChannel() == null) {
                sendNotConnectedToMyChannel(ctx);
                return true; //No player to stop/change?
            }

            // There's voice state but it isn't on a voice channel (how?), or the person is connected to another VC.
            if (!voiceState.inAudioChannel() || !voiceState.getChannel().getId().equals(player.getChannel())) {
                sendNotConnectedToMyChannel(ctx);
                return true;
            }

            // No self voice state?
            if (selfVoiceState == null) {
                ctx.reply("commands.music_general.no_player", EmoteReference.ERROR);
                return true; //No player to stop/change?
            }

            return false;
        } catch (NullPointerException e) { // Maybe a little harder to reach this?
            // Ironically before we checked for this without checking if selfVoiceState was null
            // therefore we threw a NPE when catching a NPE...
            if (selfVoiceState != null && selfVoiceState.inAudioChannel())
                log.error("Possible bug? No player even though bot is connected to a channel!", e);

            ctx.edit("commands.music_general.no_player", EmoteReference.ERROR);
            return true; // No player to stop/change?
        }
    }
}
