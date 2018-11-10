/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.managers.AudioManager;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.info.stats.manager.StatsManager;
import net.kodehawa.mantarobot.commands.music.GuildMusicManager;
import net.kodehawa.mantarobot.commands.music.requester.TrackScheduler;
import net.kodehawa.mantarobot.commands.music.utils.AudioCmdUtils;
import net.kodehawa.mantarobot.commands.music.utils.AudioUtils;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.RateLimiter;

import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static net.kodehawa.mantarobot.commands.music.utils.AudioCmdUtils.embedForQueue;
import static net.kodehawa.mantarobot.utils.Utils.handleDefaultRatelimit;
import static org.apache.commons.lang3.StringUtils.replaceEach;

@Module
@Slf4j
@SuppressWarnings("unused")
public class MusicCmds {
    @Subscribe
    public void forceskip(CommandRegistry cr) {
        cr.register("forceskip", new SimpleCommand(Category.MUSIC, CommandPermission.ADMIN) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(!isInConditionTo(event, languageContext))
                    return;

                TrackScheduler scheduler = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler();
                event.getChannel().sendMessageFormat(languageContext.get("commands.forceskip.success"), EmoteReference.CORRECT).queue();
                scheduler.nextTrack(true, true);
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Force skip")
                        .setDescription("Well, administrators should be able to forceskip, shouldn't they?\n" +
                                "`~>skip` has the same effect if you're a DJ.")
                        .build();
            }
        });

        cr.registerAlias("forceskip", "fs");
    }

    @Subscribe
    public void move(CommandRegistry cr) {
        cr.register("move", new SimpleCommand(Category.MUSIC) {
            final RateLimiter rl = new RateLimiter(TimeUnit.SECONDS, 20);

            @Override
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                Guild guild = event.getGuild();

                if(!handleDefaultRatelimit(rl, event.getAuthor(), event, languageContext))
                    return;

                if(content.isEmpty()) {
                    AudioManager am = guild.getAudioManager();

                    try {
                        VoiceChannel vc = guild.getMember(event.getAuthor()).getVoiceState().getChannel();

                        if(vc != guild.getMember(event.getJDA().getSelfUser()).getVoiceState().getChannel()) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.move.attempt"), EmoteReference.THINKING).queue();
                            AudioCmdUtils.openAudioConnection(event, am, vc, languageContext);
                            return;
                        }

                        event.getChannel().sendMessageFormat(languageContext.get("commands.move.error_moving"), EmoteReference.ERROR).queue();
                        return;
                    } catch(Exception e) {
                        if(e instanceof PermissionException) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.move.cannot_connect"), EmoteReference.ERROR).queue();
                            return;
                        }

                        event.getChannel().sendMessageFormat(languageContext.get("commands.move.non_existent_channel"), EmoteReference.ERROR).queue();
                        return;
                    }
                }

                try {
                    VoiceChannel vc = event.getGuild().getVoiceChannelsByName(content, true).get(0);
                    AudioManager am = event.getGuild().getAudioManager();

                    AudioCmdUtils.openAudioConnection(event, am, vc, languageContext);
                    event.getChannel().sendMessageFormat(languageContext.get("commands.move.success"), EmoteReference.OK, vc.getName()).queue();
                } catch(IndexOutOfBoundsException e) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.move.vc_not_found"), EmoteReference.ERROR).queue();
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return baseEmbed(event, "Voice Channel move command")
                        .setDescription("Move me from one VC to another")
                        .addField("Usage", "~>move <vc>", false)
                        .addField(
                                "Parameters", "vc: voice channel to move the bot to (exact name, case-insensitive).", false)
                        .addField(
                                "Special cases",
                                "If you don't specify a channel name, I will try to move to the channel you're in, " +
                                        "as long as it's not the same one I'm in currently!", false
                        )
                        .build();
            }

        });
    }

    @Subscribe
    public void playnow(CommandRegistry cr) {
        cr.register("playnow", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(isDJ(event.getMember())) {
                    if(content.trim().isEmpty()) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.music_general.no_song"), EmoteReference.ERROR).queue();
                        return;
                    }

                    try {
                        new URL(content);
                    } catch(Exception e) {
                        if(content.startsWith("sonudcloud")) {
                            String name = content.substring("soundcloud".length()).trim();
                            if(name.isEmpty()) {
                                event.getChannel().sendMessageFormat(
                                        languageContext.get("commands.music_general.soundcloud_no_args"),
                                        EmoteReference.ERROR
                                ).queue();
                                return;
                            }
                            content = "scsearch: " + content;
                        }
                        else content = "ytsearch: " + content;
                    }

                    MantaroBot.getInstance().getAudioManager().loadAndPlay(event, content, false, true, languageContext);
                    TextChannelGround.of(event).dropItemWithChance(0, 5);
                } else {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.music_general.dj_only"), EmoteReference.ERROR).queue();
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Play Now command")
                        .setDescription("**Puts a song on the front of the queue and plays it immediately**")
                        .build();
            }
        });
    }

    @Subscribe
    public void np(CommandRegistry cr) {
        cr.register("np", new SimpleCommand(Category.MUSIC) {
            @Override
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
                if(musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack() == null) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.np.no_track"), EmoteReference.ERROR).queue();
                    return;
                }

                EmbedBuilder npEmbed = new EmbedBuilder();
                long now = musicManager.getTrackScheduler().getCurrentTrack().getPosition();
                long total = musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack().getDuration();
                npEmbed.setAuthor(languageContext.get("commands.np.header"), null, event.getGuild().getIconUrl())
                        .setThumbnail("http://www.clipartbest.com/cliparts/jix/6zx/jix6zx4dT.png")
                        .setDescription("\n\u23ef " + AudioCmdUtils.getProgressBar(now, total) + "\n\n" +
                                "**[" + musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack()
                                .getInfo().title + "]"
                                + "(" + musicManager.getTrackScheduler().getAudioPlayer().getPlayingTrack()
                                .getInfo().uri + ")** "
                                + String.format("`(%s/%s)`", Utils.getDurationMinutes(now), total == Long.MAX_VALUE ? "stream" : Utils.getDurationMinutes(total)))
                        .setFooter("Enjoy the music! <3", event.getAuthor().getAvatarUrl());

                event.getChannel().sendMessage(npEmbed.build()).queue();
                TextChannelGround.of(event).dropItemWithChance(0, 10);
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return baseEmbed(event, "Now Playing (np) Command")
                        .addField("Description", "See what track is playing now.", false).build();
            }
        });
    }


    @Subscribe
    public void pause(CommandRegistry cr) {
        cr.register("pause", new SimpleCommand(Category.MUSIC) {
            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Pause Command")
                        .addField("Description", "Pause or unpause the current track.", false)
                        .addField("Usage:", "~>pause (if paused, I will unpause, vice versa)", false).build();
            }

            @Override
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(!isInConditionTo(event, languageContext))
                    return;

                GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());

                boolean paused = !musicManager.getTrackScheduler().getAudioPlayer().isPaused();
                String toSend = EmoteReference.MEGA + (paused ? languageContext.get("commands.pause.paused") : languageContext.get("commands.pause.unpaused"));
                musicManager.getTrackScheduler().getAudioPlayer().setPaused(paused);
                event.getChannel().sendMessage(toSend).queue();
                TextChannelGround.of(event).dropItemWithChance(0, 10);
            }
        });

        cr.registerAlias("pause", "resume");
    }

    @Subscribe
    public void play(CommandRegistry cr) {
        cr.register("play", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(content.trim().isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.music_general.no_song"), EmoteReference.ERROR).queue();
                    return;
                }

                try {
                    new URL(content);
                } catch(Exception e) {
                    if(content.startsWith("soundcloud")) {
                        String name = content.substring("soundcloud".length()).trim();
                        if(name.isEmpty()) {
                            event.getChannel().sendMessageFormat(
                                    languageContext.get("commands.music_general.soundcloud_no_args"),
                                    EmoteReference.ERROR
                            ).queue();
                            return;
                        }
                        content = "scsearch: " + content;
                    }
                    else content = "ytsearch: " + content;
                }

                MantaroBot.getInstance().getAudioManager().loadAndPlay(event, content, false, false, languageContext);
                TextChannelGround.of(event).dropItemWithChance(0, 5);
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Play Command")
                        .addField("Description", "Play songs! This connects to the voice channel the user that triggers it it's connected to, *only* if there is" +
                                " no song playing currently and Mantaro isn't bound to any channel. Basically this works as a join command on the first song.", false)
                        .addField("Usage", "~>play <song url> (playlists and song names are also acceptable)", false)
                        .addField(
                                "Tip", "If you do ~>play <search term> I'll search youtube (default), " +
                                        "but if you do ~>play soundcloud <search term> It will search soundcloud (not for usage w/links).",
                                false
                        )
                        .build();
            }
        });
    }

    @Subscribe
    public void forceplay(CommandRegistry cr) {
        cr.register("forceplay", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(content.trim().isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.music_general.no_song"), EmoteReference.ERROR).queue();
                    return;
                }

                try {
                    new URL(content);
                } catch(Exception e) {
                    if(content.startsWith("soundcloud")) content = ("scsearch: " + content).replace("soundcloud ", "");
                    else content = "ytsearch: " + content;
                }

                MantaroBot.getInstance().getAudioManager().loadAndPlay(event, content, true, false, languageContext);
                TextChannelGround.of(event).dropItemWithChance(0, 5);
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Forceplay Command")
                        .addField("Description", "**This command doesn't put the song at the start of the queue, for that use `~>playnow`!**\n" +
                                "Play the first song I find in your search. This connects to the voice channel the user that triggers it it's connected to, *only* if there is" +
                                " no song playing currently and Mantaro isn't bound to any channel. Basically this works as a join command on the first song.", false)
                        .addField("Usage", "~>forceplay <song url> (playlists and song names are also acceptable)", false)
                        .addField("Tip", "If you do ~>forceplay <search term> I'll search youtube (default), " +
                                        "but if you do ~>forceplay soundcloud <search term> It will search soundcloud (not for usage w/links).",
                                false)
                        .build();
            }
        });
    }

    @Subscribe
    public void rewind(CommandRegistry cr) {
        cr.register("rewind", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(args.length == 0) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.rewind.no_time"), EmoteReference.ERROR).queue();
                    return;
                }

                GuildMusicManager manager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
                if(manager.getAudioPlayer().getPlayingTrack() == null) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.music_general.not_playing"), EmoteReference.ERROR).queue();
                    return;
                }

                if(isDJ(event.getMember())) {
                    try {
                        long amt = Utils.parseTime(args[0]);
                        if(amt < 0) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.rewind.negative"), EmoteReference.ERROR).queue();
                            return;
                        }

                        AudioTrack track = manager.getAudioPlayer().getPlayingTrack();
                        long position = track.getPosition();
                        if(position - amt < 0) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.rewind.before_beggining"), EmoteReference.ERROR).queue();
                            return;
                        }

                        track.setPosition(position - amt);
                        event.getChannel().sendMessageFormat(languageContext.get("commands.rewind.success"),
                                EmoteReference.CORRECT, AudioUtils.getLength(position - amt)
                        ).queue();
                    } catch(NumberFormatException ex) {
                        event.getChannel().sendMessageFormat(languageContext.get("general.invalid_number"), EmoteReference.ERROR).queue();
                    }
                } else
                    event.getChannel().sendMessageFormat(languageContext.get("commands.music_general.dj_only"), EmoteReference.ERROR).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Rewind Command")
                        .addField("Description", "Rewind the current song a specified amount of seconds", false)
                        .addField("Usage", "~>rewind <time>\nTime is in this format: 1m29s (1 minute and 29s)", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void reset(CommandRegistry cr) {
        cr.register("restartsong", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                GuildMusicManager manager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
                if(manager.getAudioPlayer().getPlayingTrack() == null) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.music.music_general.not_playing"), EmoteReference.ERROR).queue();
                    return;
                }

                if(isDJ(event.getMember())) {
                    AudioTrack track = manager.getAudioPlayer().getPlayingTrack();
                    track.setPosition(0L);
                    event.getChannel().sendMessageFormat(languageContext.get("commands.restartsong.success"), EmoteReference.CORRECT).queue();
                } else {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.music_general.dj_only"), EmoteReference.ERROR).queue();
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Song restart")
                        .setDescription("**Restarts the playback of the current song**")
                        .build();
            }
        });
    }

    @Subscribe
    public void skipahead(CommandRegistry cr) {
        cr.register("skipahead", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(args.length == 0) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.skipahead.no_time"), EmoteReference.ERROR).queue();
                    return;
                }

                GuildMusicManager manager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
                if(manager.getAudioPlayer().getPlayingTrack() == null) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.music_general.not_playing"), EmoteReference.ERROR).queue();
                    return;
                }

                if(isDJ(event.getMember())) {
                    try {
                        long amt = Utils.parseTime(args[0]);
                        if(amt < 0) {
                            //same as in rewind here
                            event.getChannel().sendMessageFormat(languageContext.get("commands.rewind.negative"), EmoteReference.ERROR).queue();
                            return;
                        }
                        AudioTrack track = manager.getAudioPlayer().getPlayingTrack();
                        long position = track.getPosition();
                        if(position + amt > track.getDuration()) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.skipahead.past_duration"), EmoteReference.ERROR).queue();
                            return;
                        }
                        track.setPosition(position + amt);
                        event.getChannel().sendMessageFormat(languageContext.get("commands.skipahead.success"), 
                                EmoteReference.CORRECT, AudioUtils.getLength(position + amt)
                        ).queue();
                    } catch(NumberFormatException ex) {
                        event.getChannel().sendMessageFormat(languageContext.get("general.invalid_number"), EmoteReference.ERROR).queue();
                    }
                } else
                    event.getChannel().sendMessageFormat(languageContext.get("commands.music_general.dj_only"), EmoteReference.ERROR).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Skip Ahead Command")
                        .addField("Description", "Fast forward the current song a specified amount of time", false)
                        .addField("Usage", "~>skipahead <time>\nTime is in this format: 1m29s (1 minute and 29s)", false)
                        .build();
            }
        });

        cr.registerAlias("skipahead", "forward");
    }


    @Subscribe
    public void queue(CommandRegistry cr) {
        cr.register("queue", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(
                        event.getGuild());
                int page = 0;
                try {
                    page = Math.max(Integer.parseInt(args[0]), 1);
                } catch(Exception ignored) {
                }

                if(content.startsWith("clear")) {
                    if(!isInConditionTo(event, languageContext)) {
                        return;
                    }

                    if(isDJ(event.getMember())) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.music_general.queue.header"), EmoteReference.CORRECT).queue();
                        int TEMP_QUEUE_LENGTH = musicManager.getTrackScheduler().getQueue().size();
                        MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler().getQueue().clear();

                        event.getChannel().sendMessageFormat(languageContext.get("commands.music_general.queue.clear_success"), EmoteReference.CORRECT, TEMP_QUEUE_LENGTH).queue();

                        MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler().stop();
                        return;
                    }

                    event.getChannel().sendMessageFormat(languageContext.get("commands.music_general.queue.clear_error"), EmoteReference.ERROR).queue();
                    return;
                }

                embedForQueue(page, event, musicManager, languageContext);
                TextChannelGround.of(event).dropItemWithChance(0, 10);
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Queue Command")
                        .setDescription("**Either returns the current queue playing on the server or clears it.**")
                        .addField("Usage:", "`~>queue` - **Shows the queue**\n" +
                                        "`~>queue clear` - **Clears the queue**",
                                false
                        )
                        .addField("Considerations", "If music is playing at 2x speed please do `~>opts musicspeedup fix`", false)
                        .build();
            }
        });

        cr.registerAlias("queue", "q");
    }

    @Subscribe
    public void removetrack(CommandRegistry cr) {
        cr.register("removetrack", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(!isInConditionTo(event, languageContext))
                    return;

                MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler()
                        .getQueueAsList(list -> {
                            TIntHashSet selected = new TIntHashSet();
                            String last = Integer.toString(list.size() - 1);

                            for(String param : args) {
                                String arg = replaceEach(
                                        param,
                                        new String[]{"first", "next", "last", "all"},
                                        new String[]{"0", "0", last, "0-" + last}
                                );

                                if(arg.contains("-") || arg.contains("~")) {
                                    String[] range = content.split("[-~]");

                                    if(range.length != 2) {
                                        event.getChannel().sendMessageFormat(languageContext.get("commands.removetrack.invalid_range"),
                                                EmoteReference.ERROR, param
                                        ).queue();
                                        return;
                                    }

                                    try {
                                        int iStart = Integer.parseInt(range[0]) - 1, iEnd = Integer.parseInt(range[1]) - 1;

                                        if(iStart < 0 || iStart >= list.size()) {
                                            event.getChannel().sendMessageFormat(
                                                    languageContext.get("commands.removetrack.no_track"), EmoteReference.ERROR, iStart
                                            ).queue();
                                            return;
                                        }

                                        if(iEnd < 0 || iEnd >= list.size()) {
                                            event.getChannel().sendMessageFormat(
                                                    languageContext.get("commands.removetrack.no_track"), EmoteReference.ERROR, iEnd
                                            ).queue();
                                            return;
                                        }

                                        selected.addAll(IntStream.rangeClosed(iStart, iEnd).toArray());
                                    } catch(NumberFormatException ex) {
                                        event.getChannel().sendMessageFormat(
                                                languageContext.get("commands.removetrack.invalid_number"), EmoteReference.ERROR, param
                                        ).queue();
                                        return;
                                    }
                                } else {
                                    try {
                                        int i = Integer.parseInt(content) - 1;

                                        if(i < 0 || i >= list.size()) {
                                            event.getChannel().sendMessageFormat(
                                                    languageContext.get("commands.removetrack.no_track"), EmoteReference.ERROR, i
                                            ).queue();
                                            return;
                                        }

                                        selected.add(i);
                                    } catch(NumberFormatException ex) {
                                        event.getChannel().sendMessageFormat(
                                                languageContext.get("commands.removetrack.invalid_number_range"), EmoteReference.ERROR, arg
                                        ).queue();
                                        return;
                                    }
                                }
                            }

                            TIntIterator i = selected.iterator();
                            while(i.hasNext()) list.remove(i.next());

                            event.getChannel().sendMessageFormat(languageContext.get("commands.removetrack.success"), EmoteReference.CORRECT, selected.size()).queue();

                            TextChannelGround.of(event).dropItemWithChance(0, 10);
                        });
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Remove Track Command")
                        .setDescription("**Remove the specified track from the queue.**")
                        .addField(
                                "Usage:", "`~>removetrack <tracknumber/first/next/last>` (as specified on the ~>queue command)",
                                false
                        )
                        .addField("Parameters:", "`tracknumber`: the number of the track to remove\n" +
                                "`first`: remove the first track\n"
                                + "`next`: remove the next track\n"
                                + "`last`: remove the last track\n"
                                + "You can also specify a range (1-10, for example) to delete the first 10 tracks of the queue.", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void repeat(CommandRegistry cr) {
        cr.register("repeat", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(!isInConditionTo(event, languageContext)) {
                    return;
                }

                GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
                final TrackScheduler trackScheduler = musicManager.getTrackScheduler();

                if(args.length == 0) {
                    if(trackScheduler.getRepeatMode() == TrackScheduler.Repeat.SONG) {
                        trackScheduler.setRepeatMode(null);
                        event.getChannel().sendMessageFormat(languageContext.get("commands.repeat.song_cancel"), EmoteReference.CORRECT).queue();
                    } else {
                        trackScheduler.setRepeatMode(TrackScheduler.Repeat.SONG);
                        event.getChannel().sendMessageFormat(languageContext.get("commands.repeat.song_repeat"), EmoteReference.CORRECT).queue();
                    }

                    TextChannelGround.of(event).dropItemWithChance(0, 10);
                } else {
                    if(args[0].equalsIgnoreCase("queue")) {
                        if(trackScheduler.getRepeatMode() == TrackScheduler.Repeat.QUEUE) {
                            trackScheduler.setRepeatMode(null);
                            event.getChannel().sendMessageFormat(languageContext.get("commands.repeat.queue_cancel"), EmoteReference.CORRECT).queue();
                        } else {
                            trackScheduler.setRepeatMode(TrackScheduler.Repeat.QUEUE);
                            event.getChannel().sendMessageFormat(languageContext.get("commands.repeat.queue_repeat"), EmoteReference.CORRECT).queue();
                        }

                        TextChannelGround.of(event).dropItemWithChance(0, 10);
                    }
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Repeat command")
                        .setDescription("**Repeats a song.**")
                        .addField("Usage", "`~>repeat` - **Toggles repeat**\n" +
                                "`~>repeat queue` - **Repeats the entire queue**.", false)
                        .addField(
                                "Warning",
                                "Might not work correctly if I leave the voice channel after you have disabled repeat. *To fix, just " +
                                        "add a song to the queue*", true
                        )
                        .build();
            }
        });
    }

    @Subscribe
    public void nextSong(CommandRegistry cr) {
        cr.register("ns", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
                TrackScheduler scheduler = musicManager.getTrackScheduler();

                AudioTrack next = scheduler.getQueue().peek();

                if(next == null) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.nextsong.no_song_next"), EmoteReference.TALKING).queue();
                } else {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.nextsong.format"),
                            EmoteReference.MEGA, next.getInfo().title, Utils.getDurationMinutes(next.getDuration()), scheduler.getQueue().size()
                    ).queue();
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Next Song Command")
                        .addField("Description", "**Shows the next song in queue!**", false)
                        .build();
            }
        });

        cr.registerAlias("ns", "nextsong");
    }

    @Subscribe
    public void shuffle(CommandRegistry cr) {
        cr.register("shuffle", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(!isInConditionTo(event, languageContext))
                    return;

                MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler().shuffle();
                event.getChannel().sendMessageFormat(languageContext.get("commands.shuffle.success"), EmoteReference.OK).queue();
                TextChannelGround.of(event).dropItemWithChance(0, 10);
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Shuffle Command")
                        .addField("Description", "**Shuffle the current queue!**", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void skip(CommandRegistry cr) {
        cr.register("skip", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                try {
                    if(!isInConditionTo(event, languageContext))
                        return;

                    TrackScheduler scheduler = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler();

                    if(scheduler.getCurrentTrack().getUserData() != null &&
                            String.valueOf(scheduler.getCurrentTrack().getUserData()).equals(event.getAuthor().getId()) || isDJ(event.getMember())) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.skip.dj_skip"), EmoteReference.CORRECT).queue();
                        scheduler.nextTrack(true, true);
                        return;
                    }

                    GuildData guildData = MantaroData.db().getGuild(event.getGuild()).getData();

                    if(!guildData.isMusicVote()) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.skip.success"), EmoteReference.CORRECT).queue();
                        scheduler.nextTrack(true, true);
                    } else {
                        List<String> voteSkips = scheduler.getVoteSkips();
                        int requiredVotes = scheduler.getRequiredVotes();
                        if(voteSkips.contains(event.getAuthor().getId())) {
                            voteSkips.remove(event.getAuthor().getId());
                            event.getChannel().sendMessageFormat(languageContext.get("commands.skip.vote.remove"), EmoteReference.CORRECT, requiredVotes - voteSkips.size()).queue();
                        } else {
                            voteSkips.add(event.getAuthor().getId());
                            if(voteSkips.size() >= requiredVotes) {
                                event.getChannel().sendMessageFormat(languageContext.get("commands.skip.success"), EmoteReference.CORRECT).queue();
                                scheduler.nextTrack(true, true);
                                return;
                            }

                            event.getChannel().sendMessageFormat(languageContext.get("commands.skip.vote.submit"), EmoteReference.OK, requiredVotes - voteSkips.size()).queue();
                        }
                    }

                    TextChannelGround.of(event).dropItemWithChance(0, 10);
                } catch(NullPointerException e) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.skip.no_track"), EmoteReference.ERROR).queue();
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Skip Command")
                        .setDescription("**Stops the current track and continues to the next, if one exists.**")
                        .build();
            }
        });
    }

    @Subscribe
    public void stop(CommandRegistry cr) {
        cr.register("stop", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                try {
                    if(!isInConditionTo(event, languageContext))
                        return;

                    TrackScheduler scheduler = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler();
                    if(isDJ(event.getMember())) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.stop.dj_stop"), EmoteReference.CORRECT).queue();
                        stop(event, languageContext);
                        return;
                    }

                    GuildData guildData = MantaroData.db().getGuild(event.getGuild()).getData();

                    if(!guildData.isMusicVote()) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.stop.success"), EmoteReference.CORRECT).queue();
                        stop(event, languageContext);
                    } else {
                        List<String> stopVotes = scheduler.getVoteStop();
                        int requiredVotes = scheduler.getRequiredVotes();
                        if(stopVotes.contains(event.getAuthor().getId())) {
                            stopVotes.remove(event.getAuthor().getId());
                            event.getChannel().sendMessageFormat(languageContext.get("commands.stop.vote.remove"), EmoteReference.CORRECT, requiredVotes - stopVotes.size()).queue();
                        } else {
                            stopVotes.add(event.getAuthor().getId());
                            if(stopVotes.size() >= requiredVotes) {
                                event.getChannel().sendMessageFormat(languageContext.get("commands.stop.success"), EmoteReference.CORRECT).queue();
                                stop(event, languageContext);
                                return;
                            }

                            event.getChannel().sendMessageFormat(languageContext.get("commands.stop.vote.submit"), EmoteReference.OK, requiredVotes - stopVotes.size()).queue();
                        }
                    }
                } catch(NullPointerException e) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.stop.no_player"), EmoteReference.ERROR).queue();
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Stop Command")
                        .setDescription("**Clears the queue and leaves the voice channel.**\n" +
                                "If this won't work but the bot is in a channel, tell an admin/bot commander to run `forcestop`")
                        .build();
            }
        });
    }

    @Subscribe
    public void volume(CommandRegistry cr) {
        cr.register("volume", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(MantaroData.db().getUser(event.getMember()).isPremium() ||
                        MantaroData.db().getGuild(event.getMember()).isPremium() ||
                        MantaroData.config().get().getOwners().contains(event.getAuthor().getId())) {

                    if(!isInConditionTo(event, languageContext))
                        return;

                    if(args.length == 0) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.volume.no_args"), EmoteReference.ERROR).queue();
                        return;
                    }

                    AudioPlayer player = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler().getAudioPlayer();

                    if(args[0].equals("check")) {
                        event.getChannel().sendMessageFormat(
                                languageContext.get("commands.volume.check"), EmoteReference.ZAP, player.getVolume(), StatsManager.bar(player.getVolume(), 50)
                        ).queue();
                        return;
                    }

                    int volume;
                    try {
                        volume = Math.max(4, Math.min(100, Integer.parseInt(args[0])));
                    } catch(Exception e) {
                        event.getChannel().sendMessageFormat(languageContext.get("general.invalid_number"), EmoteReference.ERROR).queue();
                        return;
                    }

                    player.setVolume(volume);
                    event.getChannel().sendMessageFormat(languageContext.get("commands.volume.success"),
                            EmoteReference.OK, volume, StatsManager.bar(volume, 50)
                    ).queue();
                } else {
                    event.getChannel().sendMessage(
                            EmoteReference.ERROR + "This is a premium-only feature. In order to get" +
                                    " donator benefits like this one you can pledge on patreon (https://www.patreon.com/mantaro). Thanks for understanding.\n" +
                                    "Premium features can be either bound to an user or a server, please, if you donate, join the support guild and ask for it.")
                            .queue();
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Volume command")
                        .addField("Usage", "`~>volume <number>` - **Sets the volume**", false)
                        .addField("Parameters", "`number` - **An integer between 1 and 100**", false)
                        .addField("Notice", "**This is a *donator-only* feature!**" +
                                "\nTo check the current volume, do `~>volume check. Minimum volume is 4.`", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void music(CommandRegistry cr) {
        cr.register("music", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                event.getChannel().sendMessage(
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
                ).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Music")
                        .setDescription("**Tells you how to use music.**")
                        .build();
            }
        });
    }

    private boolean isDJ(Member member) {
        Role djRole = member.getGuild().getRolesByName("DJ", true).stream().findFirst().orElse(null);
        return member.isOwner() || member.hasPermission(Permission.MANAGE_SERVER) || member.hasPermission(Permission.ADMINISTRATOR) ||
                (djRole != null && member.getRoles().contains(djRole));
    }

    private void sendNotConnectedToMyChannel(TextChannel channel, I18nContext lang) {
        channel.sendMessageFormat(lang.get("commands.music_general.not_connected") + "\n", EmoteReference.ERROR).queue();
    }

    /**
     * This only fires on manual stop!
     *
     * @param event event context of what guild to use to stop it
     */
    private void stop(GuildMessageReceivedEvent event, I18nContext lang) {
        GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
        TrackScheduler trackScheduler = musicManager.getTrackScheduler();
        if(trackScheduler.getAudioPlayer().getPlayingTrack() != null && !trackScheduler.getAudioPlayer().isPaused()) {
            trackScheduler.getAudioPlayer().getPlayingTrack().stop();
        }

        int TEMP_QUEUE_LENGTH = trackScheduler.getQueue().size();
        trackScheduler.getQueue().clear();

        if(TEMP_QUEUE_LENGTH > 0) {
            event.getChannel().sendMessageFormat(lang.get("commands.stop.cleanup"), EmoteReference.OK, TEMP_QUEUE_LENGTH).queue();
        }

        //This ends up calling TrackScheduler#onTrackStart -> currentTrack == null -> TrackScheduler#onStop!
        //Beware to not close the connection twice...
        trackScheduler.nextTrack(true, true);
    }

    private boolean isInConditionTo(GuildMessageReceivedEvent event, I18nContext lang) {
        try {
            if(!event.getMember().getVoiceState().inVoiceChannel() ||
                    event.getMember().getVoiceState().getChannel().getIdLong() != event.getGuild().getAudioManager().getConnectedChannel().getIdLong()) {

                if(isDJ(event.getMember())) {
                    return true;
                }

                sendNotConnectedToMyChannel(event.getChannel(), lang);
                return false;
            }

            return true;
        } catch(NullPointerException e) {
            event.getChannel().sendMessageFormat(lang.get("commands.music_general.no_player"), EmoteReference.ERROR).queue();
            return false; //No player to stop/change?
        }
    }
}
