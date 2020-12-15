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

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import lavalink.client.io.jda.JdaLink;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.info.stats.StatsManager;
import net.kodehawa.mantarobot.commands.music.requester.TrackScheduler;
import net.kodehawa.mantarobot.commands.music.utils.AudioCmdUtils;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static net.kodehawa.mantarobot.commands.music.utils.AudioCmdUtils.embedForQueue;

@Module
public class MusicCmds {
    private static final Logger log = LoggerFactory.getLogger(MusicCmds.class);

    @Subscribe
    public void forceskip(CommandRegistry cr) {
        cr.register("forceskip", new SimpleCommand(CommandCategory.MUSIC, CommandPermission.ADMIN) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
                var scheduler = musicManager.getTrackScheduler();

                if (isNotInCondition(ctx, musicManager.getLavaLink())) {
                    return;
                }

                ctx.sendLocalized("commands.forceskip.success", EmoteReference.CORRECT);
                scheduler.nextTrack(true, true);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Well, administrators should be able to forceskip, shouldn't they?. " +
                                "`~>skip` has the same effect if you have the DJ role")
                        .build();
            }
        });

        cr.registerAlias("forceskip", "fs");
    }

    @Subscribe
    public void playnow(CommandRegistry cr) {
        cr.register("playnow", new SimpleCommand(CommandCategory.MUSIC) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (isDJ(ctx, ctx.getMember())) {
                    play(ctx, content, true);
                } else {
                    ctx.sendLocalized("commands.music_general.dj_only", EmoteReference.ERROR);
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Puts a song on the front of the queue. Run `~>skip` after this to play it.")
                        .setUsage("`~>playnow [soundcloud] <song>`")
                        .addParameter("song", "The song to play, can be either a soundcloud or youtube link, or a search query.")
                        .addParameterOptional("soundcloud", "Whether to use soundcloud for search.")
                        .build();
            }
        });
    }

    @Subscribe
    public void np(CommandRegistry cr) {
        cr.register("np", new SimpleCommand(CommandCategory.MUSIC) {
            @Override
            public void call(Context ctx, String content, String[] args) {
                final var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
                final var trackScheduler = musicManager.getTrackScheduler();
                final var audioPlayer = trackScheduler.getMusicPlayer();
                final var playingTrack = audioPlayer.getPlayingTrack();
                if (playingTrack == null) {
                    ctx.sendLocalized("commands.np.no_track", EmoteReference.ERROR);
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

                ctx.send(npEmbed.build());
                TextChannelGround.of(ctx.getEvent()).dropItemWithChance(0, 10);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("See what track is playing now.")
                        .build();
            }
        });
    }


    @Subscribe
    public void pause(CommandRegistry cr) {
        cr.register("pause", new SimpleCommand(CommandCategory.MUSIC) {
            @Override
            public void call(Context ctx, String content, String[] args) {
                var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());

                if (isNotInCondition(ctx, musicManager.getLavaLink())) {
                    return;
                }

                var paused = !musicManager.getTrackScheduler().getMusicPlayer().isPaused();
                var languageContext = ctx.getLanguageContext();

                var toSend = EmoteReference.MEGA + (paused ? languageContext.get("commands.pause.paused") : languageContext.get("commands.pause.unpaused"));
                musicManager.getTrackScheduler().getMusicPlayer().setPaused(paused);
                ctx.send(toSend);

                TextChannelGround.of(ctx.getEvent()).dropItemWithChance(0, 10);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Pause or unpause the current track. (If paused, will resume, if playing, will pause)")
                        .build();
            }
        });


        //Glowing brain meme
        cr.registerAlias("pause", "resume");
        cr.registerAlias("pause", "unpause");
    }

    @Subscribe
    public void play(CommandRegistry cr) {
        cr.register("play", new SimpleCommand(CommandCategory.MUSIC) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                play(ctx, content, false);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Play songs! This connects to the voice channel the user that triggers it it's connected to, *only* if there is " +
                                "no song playing currently and Mantaro isn't bound to any channel. " +
                                "Basically this works as a join command on the first song. If you're lost, use `~>music` for examples.\n" +
                                "You can use `~>play soundcloud <search>` to search in soundcloud's library.")
                        .setUsage("~>play <song>")
                        .addParameter("song",
                                "The song to play. Can be a youtube or soundcloud URL, or a search result " +
                                        "(Example: `~>play despacito` or `~>play https://www.youtube.com/watch?v=jjDO91gNiCU`)"
                        ).build();
            }
        });
    }

    @Subscribe
    public void forceplay(CommandRegistry cr) {
        cr.register("forceplay", new SimpleCommand(CommandCategory.MUSIC) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (content.trim().isEmpty()) {
                    ctx.sendLocalized("commands.music_general.no_song", EmoteReference.ERROR);
                    return;
                }

                try {
                    new URL(content);
                } catch (Exception e) {
                    if (content.startsWith("soundcloud")) content = ("scsearch: " + content).replace("soundcloud ", "");
                    else content = "ytsearch: " + content;
                }

                ctx.getAudioManager().loadAndPlay(ctx.getEvent(), content, true, false, ctx.getLanguageContext());
                TextChannelGround.of(ctx.getEvent()).dropItemWithChance(0, 5);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("""
                                **This command doesn't put the song at the start of the queue, for that use `~>playnow`!**
                                Play the first song I find in your search. 
                                This connects to the voice channel the user that triggers it it's connected to, 
                                *only* if there is no song playing currently and Mantaro isn't bound to any channel. 
                                Basically this works as a join command on the first song. 
                                If you're lost, use `~>music` for examples.
                                
                                You can use `~>forceplay soundcloud <search>` to search in soundcloud's library.""")
                        .setUsage("~>forceplay <song>")
                        .addParameter("song", "The song to play. Can be a youtube or soundcloud URL, or a search result " +
                                "(Example: `~>play despacito` or `~>play https://www.youtube.com/watch?v=jjDO91gNiCU`)"
                        ).build();
            }
        });
    }

    @Subscribe
    public void queue(CommandRegistry cr) {
        TreeCommand queueCommand = cr.register("queue", new TreeCommand(CommandCategory.MUSIC) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, I18nContext languageContext, String content) {
                        var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
                        embedForQueue(ctx.getEvent(), musicManager, ctx.getLanguageContext());
                        TextChannelGround.of(ctx.getEvent()).dropItemWithChance(0, 10);
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Shows you the current queue.")
                        .setUsage("`~>queue [page]`")
                        .addParameter("page", "The page of the queue you want to see. This is optional.")
                        .build();
            }
        });

        queueCommand.addSubCommand("clear", new SubCommand() {
            @Override
            public String description() {
                return "Clears the queue.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var mantaroAudioManager = ctx.getAudioManager();
                var musicManager = mantaroAudioManager.getMusicManager(ctx.getGuild());

                if (isNotInCondition(ctx, musicManager.getLavaLink())) {
                    return;
                }

                if (isDJ(ctx, ctx.getMember())) {
                    musicManager.getLavaLink().getPlayer().stopTrack();
                    musicManager.getTrackScheduler().stop();
                    var tempLength = musicManager.getTrackScheduler().getQueue().size();
                    ctx.sendLocalized("commands.music_general.queue.clear_success", EmoteReference.CORRECT, tempLength);

                    return;
                }

                ctx.sendLocalized("commands.music_general.queue.clear_error", EmoteReference.ERROR);
            }
        });

        cr.registerAlias("queue", "q");
    }

    @Subscribe
    public void repeat(CommandRegistry cr) {
        cr.register("repeat", new SimpleCommand(CommandCategory.MUSIC) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());

                if (isNotInCondition(ctx, musicManager.getLavaLink())) {
                    return;
                }

                final var trackScheduler = musicManager.getTrackScheduler();

                if (args.length == 0) {
                    if (trackScheduler.getRepeatMode() == TrackScheduler.Repeat.SONG) {
                        trackScheduler.setRepeatMode(null);
                        ctx.sendLocalized("commands.repeat.song_cancel", EmoteReference.CORRECT);
                    } else {
                        trackScheduler.setRepeatMode(TrackScheduler.Repeat.SONG);
                        ctx.sendLocalized("commands.repeat.song_repeat", EmoteReference.CORRECT);
                    }

                    TextChannelGround.of(ctx.getEvent()).dropItemWithChance(0, 10);
                } else {
                    if (args[0].equalsIgnoreCase("queue")) {
                        if (trackScheduler.getRepeatMode() == TrackScheduler.Repeat.QUEUE) {
                            trackScheduler.setRepeatMode(null);
                            ctx.sendLocalized("commands.repeat.queue_cancel", EmoteReference.CORRECT);
                        } else {
                            trackScheduler.setRepeatMode(TrackScheduler.Repeat.QUEUE);
                            ctx.sendLocalized("commands.repeat.queue_repeat", EmoteReference.CORRECT);
                        }

                        TextChannelGround.of(ctx.getEvent()).dropItemWithChance(0, 10);
                    }
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Repeats a song, or disables repeat. This command is a toggle. " +
                                "It will **disable** repeat if it's ran when it's turned on, and of course enable repeat if repeat it's off."
                        ).setUsage("`~>repeat [queue]`")
                        .addParameterOptional("queue", "Add this if you want to repeat the queue (`~>repeat queue`)")
                        .build();
            }
        });
    }

    @Subscribe
    public void nextSong(CommandRegistry cr) {
        cr.register("ns", new SimpleCommand(CommandCategory.MUSIC) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
                var scheduler = musicManager.getTrackScheduler();
                var next = scheduler.getQueue().peek();

                if (next == null) {
                    ctx.sendLocalized("commands.nextsong.no_song_next", EmoteReference.TALKING);
                } else {
                    ctx.sendLocalized("commands.nextsong.format",
                            EmoteReference.MEGA, next.getInfo().title, AudioCmdUtils.getDurationMinutes(next.getDuration()), scheduler.getQueue().size()
                    );
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Shows the next song in queue.")
                        .build();
            }
        });

        cr.registerAlias("ns", "nextsong");
    }

    @Subscribe
    public void shuffle(CommandRegistry cr) {
        cr.register("shuffle", new SimpleCommand(CommandCategory.MUSIC) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());

                if (isNotInCondition(ctx, musicManager.getLavaLink())) {
                    return;
                }

                musicManager.getTrackScheduler().shuffle();
                ctx.sendLocalized("commands.shuffle.success", EmoteReference.OK);
                TextChannelGround.of(ctx.getEvent()).dropItemWithChance(0, 10);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Shuffle the current queue.")
                        .build();
            }
        });
    }

    @Subscribe
    public void skip(CommandRegistry cr) {
        cr.register("skip", new SimpleCommand(CommandCategory.MUSIC) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                try {
                    var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
                    var scheduler = musicManager.getTrackScheduler();

                    if (isNotInCondition(ctx, musicManager.getLavaLink())) {
                        return;
                    }

                    var author = ctx.getAuthor();
                    if (isSongOwner(scheduler, author) || isDJ(ctx, ctx.getMember())) {
                        ctx.sendLocalized("commands.skip.dj_skip", EmoteReference.CORRECT);
                        scheduler.nextTrack(true, true);
                        return;
                    }

                    var guildData = ctx.getDBGuild().getData();

                    if (!guildData.isMusicVote()) {
                        ctx.sendLocalized("commands.skip.success", EmoteReference.CORRECT);
                        scheduler.nextTrack(true, true);
                    } else {
                        List<String> voteSkips = scheduler.getVoteSkips();
                        var requiredVotes = scheduler.getRequiredVotes();

                        if (voteSkips.contains(author.getId())) {
                            voteSkips.remove(author.getId());
                            ctx.sendLocalized("commands.skip.vote.remove", EmoteReference.CORRECT, requiredVotes - voteSkips.size());
                        } else {
                            voteSkips.add(author.getId());
                            if (voteSkips.size() >= requiredVotes) {
                                ctx.sendLocalized("commands.skip.success", EmoteReference.CORRECT);
                                scheduler.nextTrack(true, true);
                                return;
                            }

                            ctx.sendLocalized("commands.skip.vote.submit", EmoteReference.OK, requiredVotes - voteSkips.size());
                        }
                    }

                    TextChannelGround.of(ctx.getEvent()).dropItemWithChance(0, 10);
                } catch (NullPointerException e) {
                    ctx.sendLocalized("commands.skip.no_track", EmoteReference.ERROR);
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Stops the current track and continues to the next, if one exists.")
                        .build();
            }
        });
    }

    @Subscribe
    public void stop(CommandRegistry cr) {
        cr.register("stop", new SimpleCommand(CommandCategory.MUSIC) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                try {
                    var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
                    var scheduler = musicManager.getTrackScheduler();

                    if (isNotInCondition(ctx, musicManager.getLavaLink())) {
                        return;
                    }

                    if (isDJ(ctx, ctx.getMember())) {
                        ctx.sendLocalized("commands.stop.dj_stop", EmoteReference.CORRECT);
                        stopCurrent(ctx);
                        return;
                    }

                    var guildData = ctx.getDBGuild().getData();
                    var author = ctx.getAuthor();

                    if (!guildData.isMusicVote()) {
                        ctx.sendLocalized("commands.stop.success", EmoteReference.CORRECT);
                        stopCurrent(ctx);
                    } else {
                        List<String> stopVotes = scheduler.getVoteStop();
                        var requiredVotes = scheduler.getRequiredVotes();

                        if (stopVotes.contains(author.getId())) {
                            stopVotes.remove(author.getId());
                            ctx.sendLocalized("commands.stop.vote.remove", EmoteReference.CORRECT, requiredVotes - stopVotes.size());
                        } else {
                            stopVotes.add(author.getId());
                            if (stopVotes.size() >= requiredVotes) {
                                ctx.sendLocalized("commands.stop.success", EmoteReference.CORRECT);
                                stopCurrent(ctx);
                                return;
                            }

                            ctx.sendLocalized("commands.stop.vote.submit", EmoteReference.OK, requiredVotes - stopVotes.size());
                        }
                    }
                } catch (NullPointerException e) {
                    ctx.sendLocalized("commands.stop.no_player", EmoteReference.ERROR);
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Clears the queue and leaves the voice channel.")
                        .build();
            }
        });
    }

    @Subscribe
    public void volume(CommandRegistry cr) {
        cr.register("volume", new SimpleCommand(CommandCategory.MUSIC) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (ctx.getDBUser().isPremium() || ctx.getDBGuild().isPremium() || ctx.getConfig().getOwners().contains(ctx.getAuthor().getId())) {
                    var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
                    var lavalink = musicManager.getLavaLink();

                    if (content.isEmpty() || content.equals("check")) { // just in case
                        var player = lavalink.getPlayer();
                        if (player.getPlayingTrack() == null) {
                            ctx.sendLocalized("commands.volume.no_player", EmoteReference.ERROR);
                            return;
                        }

                        final var filters = player.getFilters();
                        var volume = (int) (filters.getVolume() * 100);
                        ctx.sendLocalized("commands.volume.check", EmoteReference.ZAP, volume, StatsManager.bar(volume, 50));
                        return;
                    }

                    if (isNotInCondition(ctx, musicManager.getLavaLink())) {
                        return;
                    }

                    int volume;
                    try {
                        volume = Math.max(4, Math.min(100, Integer.parseInt(content)));
                    } catch (Exception e) {
                        ctx.sendLocalized("general.invalid_number", EmoteReference.ERROR);
                        return;
                    }

                    float finalVolume = volume / 100.0f;
                    lavalink.getPlayer().getFilters()
                            .setVolume(finalVolume)
                            .commit();

                    ctx.sendLocalized("commands.volume.success",
                            EmoteReference.OK, volume, StatsManager.bar(volume, 50)
                    );
                } else {
                    ctx.sendLocalized("commands.volume.premium_only", EmoteReference.ERROR);
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription(
                                """
                                Sets the playback volume. Use `~>volume` to check the volume.
                                **This is a *donator-only* feature!**
                                """
                        ).setUsage("`~>volume <volume>`")
                        .addParameter("volume", "The volume, a number from 4 to 100 that you want to set it to.")
                        .build();
            }
        });
    }

    @Subscribe
    public void music(CommandRegistry cr) {
        cr.register("music", new SimpleCommand(CommandCategory.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var languageContext = ctx.getLanguageContext();

                ctx.send(
                        String.join("\n",
                                languageContext.get("commands.music_usage.1"),
                                languageContext.get("commands.music_usage.2"),
                                languageContext.get("commands.music_usage.3"),
                                languageContext.get("commands.music_usage.4"),
                                languageContext.get("commands.music_usage.5"),
                                languageContext.get("commands.music_usage.6"),
                                languageContext.get("commands.music_usage.7"),
                                languageContext.get("commands.music_usage.8"),
                                languageContext.get("commands.music_usage.9"),
                                languageContext.get("commands.music_usage.10"),
                                languageContext.get("commands.music_usage.11"),
                                languageContext.get("commands.music_usage.12")
                        )
                );
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Tells you how to use music. " +
                                "Yes, this is only a guide. If you need to see the actual music commands, do `~>help audio`"
                        ).build();
            }
        });
    }

    @Subscribe
    public void lyrics(CommandRegistry cr) {
        cr.register("lyrics", new SimpleCommand(CommandCategory.MUSIC) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var search = content.trim();
                if (search.equals("current") || search.isEmpty()) {
                    var musicManager = ctx.getAudioManager().getMusicManager(ctx.getGuild());
                    var scheduler = musicManager.getTrackScheduler();
                    var currentTrack = scheduler.getCurrentTrack();

                    if (currentTrack == null) {
                        ctx.sendLocalized("commands.lyrics.no_current_track", EmoteReference.ERROR);
                        return;
                    }

                    search = currentTrack.getInfo().title;
                }

                var result = Utils.httpRequest("https://lyrics.tsu.sh/v1/?q=" + URLEncoder.encode(search, StandardCharsets.UTF_8));
                if (result == null) {
                    ctx.sendLocalized("commands.lyrics.error_searching", EmoteReference.ERROR);
                    return;
                }

                var results = new JSONObject(result);
                if (!results.isNull("empty")) {
                    ctx.sendLocalized("commands.lyrics.error_searching", EmoteReference.ERROR);
                    return;
                }

                // Replace more than 2 line breaks with 2 line breaks.
                var lyrics = StringEscapeUtils.unescapeHtml4(results.getString("content").replaceAll("\n{2,}", "\n\n"));
                var songObject = results.getJSONObject("song");
                var fullTitle = songObject.getString("full_title");
                var icon = songObject.getString("icon");
                var divided = DiscordUtils.divideString(500, lyrics.trim());
                var languageContext = ctx.getLanguageContext();

                DiscordUtils.list(ctx.getEvent(), 30, false, 900, (p, total) -> {
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle(languageContext.get("commands.lyrics.header").formatted(EmoteReference.HEART, fullTitle))
                            .setThumbnail(icon)
                            .setFooter(languageContext.get("commands.lyrics.footer").formatted(p, total));

                    return embed;
                }, divided);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Looks up the lyrics of a song.")
                        .setUsage("`~>lyrics [current/search term]`")
                        .addParameterOptional("current", "Searches the lyrics for the song currently playing.")
                        .addParameterOptional("search term", "The song to look up lyrics for.")
                        .build();
            }
        });
    }

    private void play(Context ctx, String content, boolean force) {
        if (content.trim().isEmpty()) {
            ctx.sendLocalized("commands.music_general.no_song", EmoteReference.ERROR);
            return;
        }

        try {
            new URL(content);
        } catch (Exception e) {
            if (content.startsWith("soundcloud")) {
                var name = content.substring("soundcloud".length()).trim();
                if (name.isEmpty()) {
                    ctx.sendLocalized("commands.music_general.soundcloud_no_args", EmoteReference.ERROR);
                    return;
                }
                content = "scsearch: " + content;
            } else content = "ytsearch: " + content;
        }

        MantaroBot.getInstance().getAudioManager().loadAndPlay(
                ctx.getEvent(), content, false, force, ctx.getLanguageContext()
        );

        TextChannelGround.of(ctx.getEvent()).dropItemWithChance(0, 5);
    }

    public static boolean isDJ(Context ctx, Member member) {
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

    private static void sendNotConnectedToMyChannel(TextChannel channel, I18nContext lang) {
        channel.sendMessageFormat(lang.get("commands.music_general.not_connected") + "\n", EmoteReference.ERROR).queue();
    }

    /**
     * This only fires on manual stop!
     *
     * @param ctx The command context
     */
    private void stopCurrent(Context ctx) {
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
                ctx.sendLocalized("commands.stop.cleanup", EmoteReference.OK, TEMP_QUEUE_LENGTH);
            }

            //This ends up calling TrackScheduler#onTrackStart -> currentTrack == null -> TrackScheduler#onStop!
            //Beware to not close the connection twice...
            trackScheduler.nextTrack(true, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isSongOwner(TrackScheduler scheduler, User author) {
        return scheduler.getCurrentTrack().getUserData() != null && String.valueOf(scheduler.getCurrentTrack().getUserData()).equals(author.getId());
    }

    public static boolean isNotInCondition(Context ctx, JdaLink player) {
        var selfVoiceState = ctx.getSelfMember().getVoiceState();
        var voiceState = ctx.getMember().getVoiceState();

        try {
            // Maybe?
            if (isDJ(ctx, ctx.getMember())) {
                return false;
            }

            // We can't do anything if voiceChannel is null, so send not connected.
            if (voiceState == null || voiceState.getChannel() == null) {
                sendNotConnectedToMyChannel(ctx.getChannel(), ctx.getLanguageContext());
                return true; //No player to stop/change?
            }

            // There's voice state but it isn't on a voice channel (how?), or the person is connected to another VC.
            if (!voiceState.inVoiceChannel() || !voiceState.getChannel().getId().equals(player.getChannel())) {
                sendNotConnectedToMyChannel(ctx.getChannel(), ctx.getLanguageContext());
                return true;
            }

            // No self voice state?
            if (selfVoiceState == null) {
                ctx.sendLocalized("commands.music_general.no_player", EmoteReference.ERROR);
                return true; //No player to stop/change?
            }

            return false;
        } catch (NullPointerException e) { // Maybe a little harder to reach this?
            // Ironically before we checked for this without checking if selfVoiceState was null
            // therefore we threw a NPE when catching a NPE...
            if (selfVoiceState != null && selfVoiceState.inVoiceChannel())
                log.error("Possible bug? No player even though bot is connected to a channel!", e);

            ctx.sendLocalized("commands.music_general.no_player", EmoteReference.ERROR);
            return true; // No player to stop/change?
        }
    }
}
