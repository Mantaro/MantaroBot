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
import net.kodehawa.mantarobot.module.Callback;
import net.kodehawa.mantarobot.module.CommandType;
import net.kodehawa.mantarobot.module.Module;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class Audio extends Module {

    private final AudioPlayerManager playerManager;
    private final Map<Long, MusicManager> musicManagers;

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
                loadAndPlay(event.getGuild(), event.getTextChannel(), args[0]);
            }

            @Override
            public String help() {
                return "";
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
                return "";
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
                return "";
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
                event.getChannel().sendMessage(embedQueueList(musicManager)).queue();
            }

            @Override
            public String help() {
                return "";
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
                return "";
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
                channel.sendMessage("Added new track to queue: **" + track.getInfo().title + "**").queue();
                if(Parameters.getMusicVChannelForServer(guild.getId()).isEmpty()){
                    play(channel.getGuild(), musicManager, track);
                } else {
                    play(Parameters.getMusicVChannelForServer(guild.getId()), channel.getGuild(), musicManager, track);
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                int i = 0;
                StringBuilder builder = new StringBuilder();
                for(AudioTrack audioTrack : playlist.getTracks()){
                    if(i <= 60){
                        builder.append("Added new track to queue: **" + audioTrack.getInfo().title + "**\n");
                        play(channel.getGuild(), musicManager, audioTrack);
                    } else {
                        break;
                    }
                    i++;
                }
                channel.sendMessage(builder.toString()).queue();
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Nothing found on " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("Couldn't play music: " + exception.getMessage()).queue();
            }
        });
    }

    private void play(Guild guild, MusicManager musicManager, AudioTrack track) {
        connectToFirstVoiceChannel(guild.getAudioManager());
        musicManager.getScheduler().queue(track);
    }

    private void play(String cid, Guild guild, MusicManager musicManager, AudioTrack track) {
        connectToNamedVoiceChannel(cid, guild.getAudioManager());
        musicManager.getScheduler().queue(track);
    }

    private void skipTrack(TextChannel channel) {
        MusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        musicManager.getScheduler().nextTrack();
        channel.sendMessage("Skipped to next track.").queue();
    }

    private static void connectToFirstVoiceChannel(AudioManager audioManager) {
        if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
                audioManager.openAudioConnection(voiceChannel);
                break;
            }
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
        channel.sendMessage("Closed audio connection.").queue();
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
        builder.setTitle("Track list.");
        builder.setColor(Color.CYAN);
        if(!toSend.isEmpty()){
            builder.setDescription(toSend);
        } else {
            builder.setDescription( "Nothing here, just dust.");
        }

        return builder.build();
    }
}