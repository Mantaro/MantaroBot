/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;
import lavalink.client.io.jda.JdaLink;
import lavalink.client.player.IPlayer;
import lavalink.client.player.LavalinkPlayer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.info.stats.manager.StatsManager;
import net.kodehawa.mantarobot.commands.music.GuildMusicManager;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.commands.music.requester.TrackScheduler;
import net.kodehawa.mantarobot.commands.music.utils.AudioCmdUtils;
import net.kodehawa.mantarobot.commands.music.utils.AudioUtils;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.RateLimiter;
import org.slf4j.Logger;

import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static net.kodehawa.mantarobot.commands.music.utils.AudioCmdUtils.embedForQueue;
import static net.kodehawa.mantarobot.utils.Utils.handleDefaultIncreasingRatelimit;
import static org.apache.commons.lang3.StringUtils.replaceEach;

@Module
@SuppressWarnings("unused")
public class MusicCmds {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MusicCmds.class);
    
    @Subscribe
    public void forceskip(CommandRegistry cr) {
        cr.register("forceskip", new SimpleCommand(Category.MUSIC, CommandPermission.ADMIN) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
                TrackScheduler scheduler = musicManager.getTrackScheduler();
                
                if(!isInConditionTo(event, musicManager.getLavaLink(), languageContext))
                    return;
                
                event.getChannel().sendMessageFormat(languageContext.get("commands.forceskip.success"), EmoteReference.CORRECT).queue();
                scheduler.nextTrack(true, true);
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Well, administrators should be able to forceskip, shouldn't they?. `~>skip` has the same effect if you have the DJ role")
                               .build();
            }
        });
        
        cr.registerAlias("forceskip", "fs");
    }
    
    @Subscribe
    public void move(CommandRegistry cr) {
        cr.register("move", new SimpleCommand(Category.MUSIC) {
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
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                Guild guild = event.getGuild();
                TextChannel channel = event.getChannel();
                
                if(!handleDefaultIncreasingRatelimit(rl, event.getAuthor(), event, languageContext))
                    return;
                
                if(content.isEmpty()) {
                    JdaLink link = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getLavaLink();
                    
                    try {
                        VoiceChannel vc = guild.getMember(event.getAuthor()).getVoiceState().getChannel();
                        
                        if(vc != guild.getMember(event.getJDA().getSelfUser()).getVoiceState().getChannel()) {
                            channel.sendMessageFormat(languageContext.get("commands.move.attempt"), EmoteReference.THINKING).queue();
                            AudioCmdUtils.openAudioConnection(event, link, vc, languageContext);
                            return;
                        }
                        
                        channel.sendMessageFormat(languageContext.get("commands.move.error_moving"), EmoteReference.ERROR).queue();
                        return;
                    } catch(Exception e) {
                        if(e instanceof PermissionException) {
                            channel.sendMessageFormat(languageContext.get("commands.move.cannot_connect"), EmoteReference.ERROR).queue();
                            return;
                        }
                        
                        channel.sendMessageFormat(languageContext.get("commands.move.non_existent_channel"), EmoteReference.ERROR).queue();
                        return;
                    }
                }
                
                try {
                    VoiceChannel vc = event.getGuild().getVoiceChannelsByName(content, true).get(0);
                    JdaLink link = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getLavaLink();
                    
                    AudioCmdUtils.openAudioConnection(event, link, vc, languageContext);
                    channel.sendMessageFormat(languageContext.get("commands.move.success"), EmoteReference.OK, vc.getName()).queue();
                } catch(IndexOutOfBoundsException e) {
                    channel.sendMessageFormat(languageContext.get("commands.move.vc_not_found"), EmoteReference.ERROR).queue();
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
    public void playnow(CommandRegistry cr) {
        cr.register("playnow", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                
                if(isDJ(event.getMember())) {
                    if(content.trim().isEmpty()) {
                        channel.sendMessageFormat(languageContext.get("commands.music_general.no_song"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    try {
                        new URL(content);
                    } catch(Exception e) {
                        if(content.startsWith("sonudcloud")) {
                            String name = content.substring("soundcloud".length()).trim();
                            if(name.isEmpty()) {
                                channel.sendMessageFormat(
                                        languageContext.get("commands.music_general.soundcloud_no_args"),
                                        EmoteReference.ERROR
                                ).queue();
                                return;
                            }
                            content = "scsearch: " + content;
                        } else content = "ytsearch: " + content;
                    }
                    
                    MantaroBot.getInstance().getAudioManager().loadAndPlay(event, content, false, true, languageContext);
                    TextChannelGround.of(event).dropItemWithChance(0, 5);
                } else {
                    channel.sendMessageFormat(languageContext.get("commands.music_general.dj_only"), EmoteReference.ERROR).queue();
                }
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Puts a song on the front of the queue. Run `~>skip` after this to play it.")
                               .setUsage("`~>playnow <song>`")
                               .addParameter("song", "The song to play, can be either a soundcloud or youtube link, or a search query.")
                               .build();
            }
        });
    }
    
    @Subscribe
    public void np(CommandRegistry cr) {
        cr.register("np", new SimpleCommand(Category.MUSIC) {
            @Override
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                
                final GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
                final TrackScheduler trackScheduler = musicManager.getTrackScheduler();
                final IPlayer audioPlayer = trackScheduler.getMusicPlayer();
                
                if(audioPlayer == null || audioPlayer.getPlayingTrack() == null) {
                    channel.sendMessageFormat(languageContext.get("commands.np.no_track"), EmoteReference.ERROR).queue();
                    return;
                }
                
                EmbedBuilder npEmbed = new EmbedBuilder();
                long now = audioPlayer.getTrackPosition();
                long total = audioPlayer.getPlayingTrack().getDuration();
                
                npEmbed.setAuthor(languageContext.get("commands.np.header"), null, event.getGuild().getIconUrl())
                        .setThumbnail("http://www.clipartbest.com/cliparts/jix/6zx/jix6zx4dT.png")
                        .setDescription("\n\u23ef " + AudioCmdUtils.getProgressBar(now, total) + "\n\n" +
                                                "**[" + musicManager.getTrackScheduler().getAudioPlayer().getPlayer().getPlayingTrack()
                                                                .getInfo().title + "]"
                                                + "(" + musicManager.getTrackScheduler().getAudioPlayer().getPlayer().getPlayingTrack()
                                                                .getInfo().uri + ")** "
                                                + String.format("`(%s/%s)`", Utils.getDurationMinutes(now), total == Long.MAX_VALUE ? "stream" : Utils.getDurationMinutes(total)))
                        .setFooter("Enjoy the music! <3", event.getAuthor().getAvatarUrl());
                
                channel.sendMessage(npEmbed.build()).queue();
                TextChannelGround.of(event).dropItemWithChance(0, 10);
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
        cr.register("pause", new SimpleCommand(Category.MUSIC) {
            @Override
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
                
                if(!isInConditionTo(event, musicManager.getLavaLink(), languageContext))
                    return;
                
                boolean paused = !musicManager.getTrackScheduler().getMusicPlayer().isPaused();
                String toSend = EmoteReference.MEGA + (paused ? languageContext.get("commands.pause.paused") : languageContext.get("commands.pause.unpaused"));
                musicManager.getTrackScheduler().getMusicPlayer().setPaused(paused);
                channel.sendMessage(toSend).queue();
                TextChannelGround.of(event).dropItemWithChance(0, 10);
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
        RateLimiter warningRatelimiter = new RateLimiter(TimeUnit.MINUTES, 3);
        
        cr.register("play", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                
                if(content.trim().isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.music_general.no_song"), EmoteReference.ERROR).queue();
                    return;
                }
                
                try {
                    new URL(content);
                } catch(Exception e) {
                    if(content.startsWith("soundcloud")) {
                        String name = content.substring("soundcloud".length()).trim();
                        if(name.isEmpty()) {
                            channel.sendMessageFormat(
                                    languageContext.get("commands.music_general.soundcloud_no_args"),
                                    EmoteReference.ERROR
                            ).queue();
                            return;
                        }
                        content = "scsearch: " + content;
                    } else content = "ytsearch: " + content;
                }
                
                MantaroBot.getInstance().getAudioManager().loadAndPlay(event, content, false, false, languageContext);
                TextChannelGround.of(event).dropItemWithChance(0, 5);
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Play songs! This connects to the voice channel the user that triggers it it's connected to, *only* if there is " +
                                                       "no song playing currently and Mantaro isn't bound to any channel. Basically this works as a join command on the first song. If you're lost, use `~>music` for examples.\n" +
                                                       "You can use `~>play soundcloud <search>` to search in soundcloud's library.")
                               .setUsage("~>play <song>")
                               .addParameter("song", "The song to play. Can be a youtube or soundcloud URL, or a search result (Example: `~>play despacito` or `~>play https://www.youtube.com/watch?v=jjDO91gNiCU`)")
                               .build();
            }
        });
    }
    
    @Subscribe
    public void forceplay(CommandRegistry cr) {
        cr.register("forceplay", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                
                if(content.trim().isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.music_general.no_song"), EmoteReference.ERROR).queue();
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
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("**This command doesn't put the song at the start of the queue, for that use `~>playnow`!**\n" +
                                                       "Play the first song I find in your search. This connects to the voice channel the user that triggers it it's connected to, *only* if there is " +
                                                       "no song playing currently and Mantaro isn't bound to any channel. Basically this works as a join command on the first song. If you're lost, use `~>music` for examples.\n" +
                                                       "You can use `~>forceplay soundcloud <search>` to search in soundcloud's library.")
                               .setUsage("~>forceplay <song>")
                               .addParameter("song", "The song to play. Can be a youtube or soundcloud URL, or a search result (Example: `~>play despacito` or `~>play https://www.youtube.com/watch?v=jjDO91gNiCU`)")
                               .build();
            }
        });
    }
    
    @Subscribe
    public void rewind(CommandRegistry cr) {
        cr.register("rewind", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                
                if(args.length == 0) {
                    channel.sendMessageFormat(languageContext.get("commands.rewind.no_time"), EmoteReference.ERROR).queue();
                    return;
                }
                
                GuildMusicManager manager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
                LavalinkPlayer lavalinkPlayer = manager.getLavaLink().getPlayer();
                if(lavalinkPlayer.getPlayingTrack() == null) {
                    channel.sendMessageFormat(languageContext.get("commands.music_general.not_playing"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(isDJ(event.getMember())) {
                    try {
                        long amt = Utils.parseTime(args[0]);
                        if(amt < 0) {
                            channel.sendMessageFormat(languageContext.get("commands.rewind.negative"), EmoteReference.ERROR).queue();
                            return;
                        }
                        
                        long position = lavalinkPlayer.getTrackPosition();
                        if(position - amt < 0) {
                            channel.sendMessageFormat(languageContext.get("commands.rewind.before_beginning"), EmoteReference.ERROR).queue();
                            return;
                        }
                        
                        lavalinkPlayer.seekTo(position - amt);
                        channel.sendMessageFormat(languageContext.get("commands.rewind.success"),
                                EmoteReference.CORRECT, AudioUtils.getLength(position - amt)
                        ).queue();
                    } catch(NumberFormatException ex) {
                        channel.sendMessageFormat(languageContext.get("general.invalid_number"), EmoteReference.ERROR).queue();
                    }
                } else
                    channel.sendMessageFormat(languageContext.get("commands.music_general.dj_only"), EmoteReference.ERROR).queue();
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Rewind the current song a specified amount of time.")
                               .setUsage("~>rewind <time>")
                               .addParameter("time", "The amount of minutes to rewind. Time is in this format: 1m29s (1 minute and 29s), for example.")
                               .build();
            }
        });
    }
    
    @Subscribe
    public void reset(CommandRegistry cr) {
        cr.register("restartsong", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                
                GuildMusicManager manager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
                LavalinkPlayer lavalinkPlayer = manager.getLavaLink().getPlayer();
                
                if(lavalinkPlayer.getPlayingTrack() == null) {
                    channel.sendMessageFormat(languageContext.get("commands.music.music_general.not_playing"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(isDJ(event.getMember())) {
                    lavalinkPlayer.seekTo(1);
                    channel.sendMessageFormat(languageContext.get("commands.restartsong.success"), EmoteReference.CORRECT).queue();
                } else {
                    channel.sendMessageFormat(languageContext.get("commands.music_general.dj_only"), EmoteReference.ERROR).queue();
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
        cr.register("forward", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                
                if(args.length == 0) {
                    channel.sendMessageFormat(languageContext.get("commands.skipahead.no_time"), EmoteReference.ERROR).queue();
                    return;
                }
                
                GuildMusicManager manager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
                LavalinkPlayer lavalinkPlayer = manager.getLavaLink().getPlayer();
                
                if(lavalinkPlayer.getPlayingTrack() == null) {
                    channel.sendMessageFormat(languageContext.get("commands.music_general.not_playing"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(isDJ(event.getMember())) {
                    try {
                        long amt = Utils.parseTime(args[0]);
                        if(amt < 0) {
                            //same as in rewind here
                            channel.sendMessageFormat(languageContext.get("commands.rewind.negative"), EmoteReference.ERROR).queue();
                            return;
                        }
                        
                        AudioTrack track = lavalinkPlayer.getPlayingTrack();
                        long position = lavalinkPlayer.getTrackPosition();
                        if(position + amt > track.getDuration()) {
                            channel.sendMessageFormat(languageContext.get("commands.skipahead.past_duration"), EmoteReference.ERROR).queue();
                            return;
                        }
                        
                        lavalinkPlayer.seekTo(position + amt);
                        channel.sendMessageFormat(languageContext.get("commands.skipahead.success"),
                                EmoteReference.CORRECT, AudioUtils.getLength(position + amt)
                        ).queue();
                    } catch(NumberFormatException ex) {
                        channel.sendMessageFormat(languageContext.get("general.invalid_number"), EmoteReference.ERROR).queue();
                    }
                } else
                    channel.sendMessageFormat(languageContext.get("commands.music_general.dj_only"), EmoteReference.ERROR).queue();
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Fast forwards the current song a specified amount of time.")
                               .setUsage("~>forward <time>")
                               .addParameter("time", "The amount of minutes to rewind. Time is in this format: 1m29s (1 minute and 29s), for example.")
                               .build();
            }
        });
        
        cr.registerAlias("forward", "skipahead");
    }
    
    
    @Subscribe
    public void queue(CommandRegistry cr) {
        TreeCommand queueCommand = (TreeCommand) cr.register("queue", new TreeCommand(Category.MUSIC) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                        GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
                        int page = 0;
                        
                        try {
                            page = Math.max(Integer.parseInt(content), 1);
                        } catch(Exception ignored) {
                        }
                        
                        embedForQueue(page, event, musicManager, languageContext);
                        TextChannelGround.of(event).dropItemWithChance(0, 10);
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
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                MantaroAudioManager mantaroAudioManager = MantaroBot.getInstance().getAudioManager();
                GuildMusicManager musicManager = mantaroAudioManager.getMusicManager(event.getGuild());
                
                if(!isInConditionTo(event, musicManager.getLavaLink(), languageContext))
                    return;
                
                if(isDJ(event.getMember())) {
                    musicManager.getLavaLink().getPlayer().stopTrack();
                    musicManager.getTrackScheduler().stop();
                    int tempLenght = musicManager.getTrackScheduler().getQueue().size();
                    event.getChannel().sendMessageFormat(languageContext.get("commands.music_general.queue.clear_success"), EmoteReference.CORRECT, tempLenght).queue();
                    
                    return;
                }
                
                event.getChannel().sendMessageFormat(languageContext.get("commands.music_general.queue.clear_error"), EmoteReference.ERROR).queue();
            }
        });
        
        cr.registerAlias("queue", "q");
    }
    
    @Subscribe
    public void removetrack(CommandRegistry cr) {
        cr.register("removetrack", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                
                GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
                
                if(!isInConditionTo(event, musicManager.getLavaLink(), languageContext))
                    return;
                
                if(!isDJ(event.getMember())) {
                    channel.sendMessageFormat(languageContext.get("commands.removetrack.not_dj"), EmoteReference.ERROR).queue();
                    return;
                }
                
                MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getTrackScheduler()
                        .getQueueAsList(list -> {
                            TIntHashSet selected = new TIntHashSet();
                            String last = Integer.toString(list.size() - 1);
                            
                            for(String param : args) {
                                String arg = replaceEach(
                                        param,
                                        new String[] {"first", "next", "last", "all"},
                                        new String[] {"0", "0", last, "0-" + last}
                                );
                                
                                if(arg.contains("-") || arg.contains("~")) {
                                    String[] range = content.split("[-~]");
                                    
                                    if(range.length != 2) {
                                        channel.sendMessageFormat(languageContext.get("commands.removetrack.invalid_range"),
                                                EmoteReference.ERROR, param
                                        ).queue();
                                        return;
                                    }
                                    
                                    try {
                                        int iStart = Integer.parseInt(range[0]) - 1, iEnd = Integer.parseInt(range[1]) - 1;
                                        
                                        if(iStart < 0 || iStart >= list.size()) {
                                            channel.sendMessageFormat(
                                                    languageContext.get("commands.removetrack.no_track"), EmoteReference.ERROR, iStart
                                            ).queue();
                                            return;
                                        }
                                        
                                        if(iEnd < 0 || iEnd >= list.size()) {
                                            channel.sendMessageFormat(
                                                    languageContext.get("commands.removetrack.no_track"), EmoteReference.ERROR, iEnd
                                            ).queue();
                                            return;
                                        }
                                        
                                        selected.addAll(IntStream.rangeClosed(iStart, iEnd).toArray());
                                    } catch(NumberFormatException ex) {
                                        channel.sendMessageFormat(
                                                languageContext.get("commands.removetrack.invalid_number"), EmoteReference.ERROR, param
                                        ).queue();
                                        return;
                                    }
                                } else {
                                    try {
                                        int i = Integer.parseInt(content) - 1;
                                        
                                        if(i < 0 || i >= list.size()) {
                                            channel.sendMessageFormat(
                                                    languageContext.get("commands.removetrack.no_track"), EmoteReference.ERROR, i
                                            ).queue();
                                            return;
                                        }
                                        
                                        selected.add(i);
                                    } catch(NumberFormatException ex) {
                                        channel.sendMessageFormat(
                                                languageContext.get("commands.removetrack.invalid_number_range"), EmoteReference.ERROR, arg
                                        ).queue();
                                        return;
                                    }
                                }
                            }
                            
                            TIntIterator i = selected.iterator();
                            while(i.hasNext()) list.remove(i.next());
                            
                            channel.sendMessageFormat(languageContext.get("commands.removetrack.success"), EmoteReference.CORRECT, selected.size()).queue();
                            
                            TextChannelGround.of(event).dropItemWithChance(0, 10);
                        });
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Remove the specified track from the queue.")
                               .setUsage("`~>removetrack <track number/first/next/last>` (Any of them, only one at a time)")
                               .addParameter("track number", "The position of the track in the current queue. You can also specify a range (1-10, for example) to delete the first 10 tracks of the queue.")
                               .addParameter("first", "The first track of the queue.")
                               .addParameter("next", "The next track of the queue.")
                               .addParameter("last", "The last track of the queue.")
                               .build();
            }
        });
    }
    
    @Subscribe
    public void repeat(CommandRegistry cr) {
        cr.register("repeat", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                
                GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
                
                if(!isInConditionTo(event, musicManager.getLavaLink(), languageContext))
                    return;
                final TrackScheduler trackScheduler = musicManager.getTrackScheduler();
                
                if(args.length == 0) {
                    if(trackScheduler.getRepeatMode() == TrackScheduler.Repeat.SONG) {
                        trackScheduler.setRepeatMode(null);
                        channel.sendMessageFormat(languageContext.get("commands.repeat.song_cancel"), EmoteReference.CORRECT).queue();
                    } else {
                        trackScheduler.setRepeatMode(TrackScheduler.Repeat.SONG);
                        channel.sendMessageFormat(languageContext.get("commands.repeat.song_repeat"), EmoteReference.CORRECT).queue();
                    }
                    
                    TextChannelGround.of(event).dropItemWithChance(0, 10);
                } else {
                    if(args[0].equalsIgnoreCase("queue")) {
                        if(trackScheduler.getRepeatMode() == TrackScheduler.Repeat.QUEUE) {
                            trackScheduler.setRepeatMode(null);
                            channel.sendMessageFormat(languageContext.get("commands.repeat.queue_cancel"), EmoteReference.CORRECT).queue();
                        } else {
                            trackScheduler.setRepeatMode(TrackScheduler.Repeat.QUEUE);
                            channel.sendMessageFormat(languageContext.get("commands.repeat.queue_repeat"), EmoteReference.CORRECT).queue();
                        }
                        
                        TextChannelGround.of(event).dropItemWithChance(0, 10);
                    }
                }
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Repeats a song.")
                               .setUsage("`~>repeat [queue]`")
                               .addParameter("queue", "Add this if you want to repeat the queue (`~>repeat queue`). This is optional.")
                               .build();
            }
        });
    }
    
    @Subscribe
    public void nextSong(CommandRegistry cr) {
        cr.register("ns", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                
                GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
                TrackScheduler scheduler = musicManager.getTrackScheduler();
                
                AudioTrack next = scheduler.getQueue().peek();
                
                if(next == null) {
                    channel.sendMessageFormat(languageContext.get("commands.nextsong.no_song_next"), EmoteReference.TALKING).queue();
                } else {
                    channel.sendMessageFormat(languageContext.get("commands.nextsong.format"),
                            EmoteReference.MEGA, next.getInfo().title, Utils.getDurationMinutes(next.getDuration()), scheduler.getQueue().size()
                    ).queue();
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
        cr.register("shuffle", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
                
                if(!isInConditionTo(event, musicManager.getLavaLink(), languageContext))
                    return;
                
                musicManager.getTrackScheduler().shuffle();
                event.getChannel().sendMessageFormat(languageContext.get("commands.shuffle.success"), EmoteReference.OK).queue();
                TextChannelGround.of(event).dropItemWithChance(0, 10);
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
        cr.register("skip", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                
                try {
                    GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
                    TrackScheduler scheduler = musicManager.getTrackScheduler();
                    
                    if(!isInConditionTo(event, musicManager.getLavaLink(), languageContext))
                        return;
                    
                    if(scheduler.getCurrentTrack().getUserData() != null &&
                               String.valueOf(scheduler.getCurrentTrack().getUserData()).equals(event.getAuthor().getId()) || isDJ(event.getMember())) {
                        channel.sendMessageFormat(languageContext.get("commands.skip.dj_skip"), EmoteReference.CORRECT).queue();
                        scheduler.nextTrack(true, true);
                        return;
                    }
                    
                    GuildData guildData = MantaroData.db().getGuild(event.getGuild()).getData();
                    
                    if(!guildData.isMusicVote()) {
                        channel.sendMessageFormat(languageContext.get("commands.skip.success"), EmoteReference.CORRECT).queue();
                        scheduler.nextTrack(true, true);
                    } else {
                        List<String> voteSkips = scheduler.getVoteSkips();
                        int requiredVotes = scheduler.getRequiredVotes();
                        if(voteSkips.contains(event.getAuthor().getId())) {
                            voteSkips.remove(event.getAuthor().getId());
                            channel.sendMessageFormat(languageContext.get("commands.skip.vote.remove"), EmoteReference.CORRECT, requiredVotes - voteSkips.size()).queue();
                        } else {
                            voteSkips.add(event.getAuthor().getId());
                            if(voteSkips.size() >= requiredVotes) {
                                channel.sendMessageFormat(languageContext.get("commands.skip.success"), EmoteReference.CORRECT).queue();
                                scheduler.nextTrack(true, true);
                                return;
                            }
                            
                            channel.sendMessageFormat(languageContext.get("commands.skip.vote.submit"), EmoteReference.OK, requiredVotes - voteSkips.size()).queue();
                        }
                    }
                    
                    TextChannelGround.of(event).dropItemWithChance(0, 10);
                } catch(NullPointerException e) {
                    channel.sendMessageFormat(languageContext.get("commands.skip.no_track"), EmoteReference.ERROR).queue();
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
        cr.register("stop", new SimpleCommand(Category.MUSIC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                
                try {
                    GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
                    TrackScheduler scheduler = musicManager.getTrackScheduler();
                    
                    if(!isInConditionTo(event, musicManager.getLavaLink(), languageContext))
                        return;
                    
                    if(isDJ(event.getMember())) {
                        channel.sendMessageFormat(languageContext.get("commands.stop.dj_stop"), EmoteReference.CORRECT).queue();
                        stop(event, languageContext);
                        return;
                    }
                    
                    GuildData guildData = MantaroData.db().getGuild(event.getGuild()).getData();
                    
                    if(!guildData.isMusicVote()) {
                        channel.sendMessageFormat(languageContext.get("commands.stop.success"), EmoteReference.CORRECT).queue();
                        stop(event, languageContext);
                    } else {
                        List<String> stopVotes = scheduler.getVoteStop();
                        int requiredVotes = scheduler.getRequiredVotes();
                        if(stopVotes.contains(event.getAuthor().getId())) {
                            stopVotes.remove(event.getAuthor().getId());
                            channel.sendMessageFormat(languageContext.get("commands.stop.vote.remove"), EmoteReference.CORRECT, requiredVotes - stopVotes.size()).queue();
                        } else {
                            stopVotes.add(event.getAuthor().getId());
                            if(stopVotes.size() >= requiredVotes) {
                                channel.sendMessageFormat(languageContext.get("commands.stop.success"), EmoteReference.CORRECT).queue();
                                stop(event, languageContext);
                                return;
                            }
                            
                            channel.sendMessageFormat(languageContext.get("commands.stop.vote.submit"), EmoteReference.OK, requiredVotes - stopVotes.size()).queue();
                        }
                    }
                } catch(NullPointerException e) {
                    channel.sendMessageFormat(languageContext.get("commands.stop.no_player"), EmoteReference.ERROR).queue();
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
        TreeCommand volumeCommand = (TreeCommand) cr.register("volume", new TreeCommand(Category.MUSIC) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                        TextChannel channel = event.getChannel();
                        final ManagedDatabase db = MantaroData.db();
                        
                        if(db.getUser(event.getMember()).isPremium() || db.getGuild(event.getMember()).isPremium() || MantaroData.config().get().getOwners().contains(event.getAuthor().getId())) {
                            GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
                            
                            if(!isInConditionTo(event, musicManager.getLavaLink(), languageContext))
                                return;
                            
                            if(content.isEmpty()) {
                                channel.sendMessageFormat(languageContext.get("commands.volume.no_args"), EmoteReference.ERROR).queue();
                                return;
                            }
                            
                            JdaLink player = musicManager.getLavaLink();
                            
                            int volume;
                            try {
                                volume = Math.max(4, Math.min(100, Integer.parseInt(content)));
                            } catch(Exception e) {
                                channel.sendMessageFormat(languageContext.get("general.invalid_number"), EmoteReference.ERROR).queue();
                                return;
                            }
                            
                            player.getPlayer().setVolume(volume);
                            channel.sendMessageFormat(languageContext.get("commands.volume.success"),
                                    EmoteReference.OK, volume, StatsManager.bar(volume, 50)
                            ).queue();
                        } else {
                            channel.sendMessageFormat(languageContext.get("commands.volume.premium_only"), EmoteReference.ERROR).queue();
                        }
                    }
                };
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Sets the playback volume. **This is a *donator-only* feature!**")
                               .setUsage("`~>volume <number>`")
                               .addParameter("number", "The number, 4 to 100 that you want to set the volume to.")
                               .build();
            }
        });
        
        volumeCommand.addSubCommand("check", new SubCommand() {
            @Override
            public String description() {
                return "Checks the current volume";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                JdaLink link = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild()).getLavaLink();
                IPlayer player = link.getPlayer();
                
                event.getChannel().sendMessageFormat(
                        languageContext.get("commands.volume.check"), EmoteReference.ZAP, player.getVolume(), StatsManager.bar(player.getVolume(), 50)
                ).queue();
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
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Tells you how to use music. Yes, this is only a guide.")
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
        try {
            GuildMusicManager musicManager = MantaroBot.getInstance().getAudioManager().getMusicManager(event.getGuild());
            TrackScheduler trackScheduler = musicManager.getTrackScheduler();
            IPlayer musicPlayer = trackScheduler.getMusicPlayer();
            
            if(musicPlayer.getPlayingTrack() != null && !musicPlayer.isPaused()) {
                musicPlayer.stopTrack();
            }
            
            int TEMP_QUEUE_LENGTH = trackScheduler.getQueue().size();
            trackScheduler.getQueue().clear();
            
            if(TEMP_QUEUE_LENGTH > 0) {
                event.getChannel().sendMessageFormat(lang.get("commands.stop.cleanup"), EmoteReference.OK, TEMP_QUEUE_LENGTH).queue();
            }
            
            //This ends up calling TrackScheduler#onTrackStart -> currentTrack == null -> TrackScheduler#onStop!
            //Beware to not close the connection twice...
            trackScheduler.nextTrack(true, true);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private boolean isInConditionTo(GuildMessageReceivedEvent event, JdaLink player, I18nContext lang) {
        try {
            if(!event.getMember().getVoiceState().inVoiceChannel() || !event.getMember().getVoiceState().getChannel().getId().equalsIgnoreCase(player.getChannel())) {
                if(isDJ(event.getMember())) {
                    return true;
                }
                
                sendNotConnectedToMyChannel(event.getChannel(), lang);
                return false;
            }
            
            return true;
        } catch(NullPointerException e) {
            if(event.getGuild().getSelfMember().getVoiceState().inVoiceChannel())
                log.error("Possible bug? No player even though bot is connected to a channel!", e);
            
            event.getChannel().sendMessageFormat(lang.get("commands.music_general.no_player"), EmoteReference.ERROR).queue();
            return false; //No player to stop/change?
        }
    }
}
