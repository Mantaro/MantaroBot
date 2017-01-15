package net.kodehawa.mantarobot.cmd;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;
import net.kodehawa.mantarobot.audio.MusicManager;
import net.kodehawa.mantarobot.cmd.guild.Parameters;
import net.kodehawa.mantarobot.log.Log;
import net.kodehawa.mantarobot.log.Type;
import net.kodehawa.mantarobot.module.Callback;
import net.kodehawa.mantarobot.module.CommandType;
import net.kodehawa.mantarobot.module.Module;

import java.awt.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Audio extends Module {

    private final AudioPlayerManager playerManager;
    private final Map<Long, MusicManager> musicManagers;
    Member _member;

    public Audio(){
        this.musicManagers = new HashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
        this.registerCommands();
    }

    @Override
    public void registerCommands(){
        super.register("play", "Plays a song in the music voice channel.", new Callback() {
            @Override
            public void onCommand(String[] args, String content, MessageReceivedEvent event) {
                _member = event.getMember();
                try {
                    new URL(args[0]);
                }
                catch(Exception e) {
                    args[0] = "ytsearch: " + args[0];
                }

                loadAndPlay(event.getGuild(), event.getTextChannel(), args[0]);
            }

            @Override
            public String help() {
                return "Plays a song in the music voice channel.\n"
                        + "Usage:\n"
                        + "~>play [youtubesongurl] (Can be a song or a playlist)";
            }

            @Override
            public CommandType commandType() {
                return CommandType.USER;
            }
        });

        super.register("skip", "Stops the track and continues to the next one, if there is one.", new Callback() {
            @Override
            public void onCommand(String[] args, String content, MessageReceivedEvent event) {
                MusicManager musicManager = musicManagers.get(Long.parseLong(event.getGuild().getId()));
                if(nextTrackAvaliable(musicManager)){
                    skipTrack(event.getTextChannel());
                }
                else {
                    event.getChannel().sendMessage("No tracks next. Disconnecting...").queue();
                    closeConnection(musicManager, event.getGuild().getAudioManager(), event.getTextChannel());
                }
            }

            @Override
            public String help() {
                return "Stops the track and continues to the next one, if there is one.\n"
                        + "Usage:\n"
                        + "~>skip";
            }

            @Override
            public CommandType commandType() {
                return CommandType.USER;
            }
        });

        super.register("musicleave", "Leaves the voice channel.", new Callback() {
            @Override
            public void onCommand(String[] args, String content, MessageReceivedEvent event) {
                MusicManager musicManager = musicManagers.get(Long.parseLong(event.getGuild().getId()));
                closeConnection(musicManager, event.getGuild().getAudioManager(), event.getTextChannel());
            }

            @Override
            public String help() {
                return "Leaves the voice channel.\n"
                        + "Usage:\n"
                        + "~>musicleave";
            }

            @Override
            public CommandType commandType() {
                return CommandType.USER;
            }
        });

        super.register("tracklist", "Returns the current tracklist playing on the server.", new Callback() {
            @Override
            public void onCommand(String[] args, String content, MessageReceivedEvent event) {
                MusicManager musicManager = musicManagers.get(Long.parseLong(event.getGuild().getId()));
                if(content.isEmpty()){
                    event.getChannel().sendMessage(embedQueueList(musicManager)).queue();
                } else if(content.startsWith("clean")){
                    for(AudioTrack audioTrack : musicManager.getScheduler().getQueue()){
                        event.getChannel().sendMessage("Removed track: " + audioTrack.getInfo().title).queue();
                        musicManager.getScheduler().getQueue().remove(audioTrack);
                    }
                    skipTrack(event.getTextChannel());
                }
            }

            @Override
            public String help() {
                return "Returns the current tracklist playing on the server or clears it.\n"
                        + "Usage:\n"
                        + "~>tracklist"
                        + "~>tracklist clear";
            }

            @Override
            public CommandType commandType() {
                return CommandType.USER;
            }
        });

        super.register("removetrack", "Removes the specified track from the queue.", new Callback() {
            @Override
            public void onCommand(String[] args, String content, MessageReceivedEvent event) {
                MusicManager musicManager = musicManagers.get(Long.parseLong(event.getGuild().getId()));
                int n = 0;
                for(AudioTrack audioTrack : musicManager.getScheduler().getQueue()){
                    if(n == Integer.parseInt(content) - 1){
                        event.getChannel().sendMessage("Removed track: " + audioTrack.getInfo().title).queue();
                        musicManager.getScheduler().getQueue().remove(audioTrack);
                        break;
                    }
                    n++;
                }
            }

            @Override
            public String help() {
                return "Removes the specified track from the queue.\n"
                        + "Usage:\n"
                        + "~>removetrack [tracknumber] (as specified on the ~>tracklist command)";
            }

            @Override
            public CommandType commandType() {
                return CommandType.USER ;
            }
        });
    }

    private synchronized MusicManager getGuildAudioPlayer(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        MusicManager musicManager = musicManagers.get(guildId);
        if (musicManager == null) {
            musicManager = new MusicManager(playerManager);
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    private void loadAndPlay(final Guild guild, final TextChannel channel, final String trackUrl) {
        MusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                channel.sendMessage(":mega: Added new track to queue: **" + track.getInfo().title + "**").queue();
                if(Parameters.getMusicVChannelForServer(guild.getId()).isEmpty()){
                    play(channel.getGuild(), musicManager, track, _member);
                } else {
                    Log.instance().print("Joining voice: " + Parameters.getMusicVChannelForServer(guild.getId()), getClass(), Type.INFO);
                    play(Parameters.getMusicVChannelForServer(guild.getId()), channel.getGuild(), musicManager, track);
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                int i = 0;
                StringBuilder builder = new StringBuilder();
                if(!playlist.isSearchResult()){
                    for(AudioTrack audioTrack : playlist.getTracks()){
                        if(Parameters.getMusicVChannelForServer(guild.getId()).isEmpty()){
                            if(i <= 60){
                                builder.append("Added new track to queue: **" + audioTrack.getInfo().title + "**\n");
                                play(channel.getGuild(), musicManager, audioTrack, _member);
                            } else {
                                break;
                            }
                            i++;
                        } else {
                            Log.instance().print("Joining voice: " + Parameters.getMusicVChannelForServer(guild.getId()), getClass(), Type.INFO);
                            play(Parameters.getMusicVChannelForServer(guild.getId()), channel.getGuild(), musicManager, audioTrack);
                        }
                        channel.sendMessage(builder.toString()).queue();
                    }
                } else {
                    if(Parameters.getMusicVChannelForServer(guild.getId()).isEmpty()){
                        play(channel.getGuild(), musicManager, playlist.getTracks().get(0), _member);
                    } else {
                        Log.instance().print("Joining voice: " + Parameters.getMusicVChannelForServer(guild.getId()), getClass(), Type.INFO);
                        play(Parameters.getMusicVChannelForServer(guild.getId()), channel.getGuild(), musicManager, playlist.getTracks().get(0));
                    }
                    channel.sendMessage(builder.toString()).queue();
                }

                channel.sendMessage(":mega: " + builder.toString()).queue();
            }

            @Override
            public void noMatches() {
                channel.sendMessage(":heavy_multiplication_x: Nothing found on " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                Log.instance().print("Couldn't play music", this.getClass(), Type.WARNING, exception);
                channel.sendMessage(":heavy_multiplication_x: Couldn't play music: " + exception.getMessage()).queue();
            }
        });
    }

    private void play(Guild guild, MusicManager musicManager, AudioTrack track, Member member) {
        connectToUserVoiceChannel(guild.getAudioManager(), member);
        musicManager.getScheduler().queue(track);
    }

    private void play(String cid, Guild guild, MusicManager musicManager, AudioTrack track) {
        connectToNamedVoiceChannel(cid, guild.getAudioManager());
        musicManager.getScheduler().queue(track);
    }

    private void skipTrack(TextChannel channel) {
        MusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        musicManager.getScheduler().nextTrack();
        channel.sendMessage(":mega: Skipped to next track.").queue();
    }

    private static void connectToUserVoiceChannel(AudioManager audioManager, Member member) {
        if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            audioManager.openAudioConnection(member.getVoiceState().getChannel());
        }
    }

    private static void connectToNamedVoiceChannel(String voiceId, AudioManager audioManager){
        if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
                if(voiceChannel.getId().equals(voiceId)){
                    audioManager.openAudioConnection(voiceChannel);
                    break;
                }
            }
        }
    }

    private void closeConnection(MusicManager musicManager, AudioManager audioManager, TextChannel channel) {
        musicManager.getScheduler().getQueue().clear();
        audioManager.closeAudioConnection();
        channel.sendMessage(":mega: Closed audio connection.").queue();
    }

    private boolean nextTrackAvaliable(MusicManager musicManager){
        if(musicManager.getScheduler().getQueueSize() > 0){
            return true;
        }
        return false;
    }

    private MessageEmbed embedQueueList(MusicManager musicManager) {
        String toSend = musicManager.getScheduler().getQueueList();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Track list");
        builder.setColor(Color.CYAN);
        if(!toSend.isEmpty()){
            builder.setDescription(toSend);
        } else {
            builder.setDescription("Nothing here, just dust.");
        }

        return builder.build();
    }
}