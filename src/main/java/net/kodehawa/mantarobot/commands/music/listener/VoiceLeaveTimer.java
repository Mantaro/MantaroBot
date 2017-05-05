package net.kodehawa.mantarobot.commands.music.listener;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.core.entities.*;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.GuildMusicManager;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

public class VoiceLeaveTimer implements Runnable {
    @Override
    public void run() {
        MantaroBot.getInstance().getAudioManager().getMusicManagers().forEach((guildId, manager) -> {
            try {
                Guild guild = MantaroBot.getInstance().getGuildById(guildId);
                GuildVoiceState voiceState = guild.getSelfMember().getVoiceState();
                if (voiceState.inVoiceChannel()) {
                    TextChannel channel = guild.getPublicChannel();
                    if (channel != null) {
                        if (channel.canTalk()) {
                            VoiceChannel voiceChannel = voiceState.getChannel();
                            AudioPlayer player = manager.getAudioPlayer();
                            GuildMusicManager mm = MantaroBot.getInstance().getAudioManager().getMusicManager(guild);
                            if(mm.getTrackScheduler().getCurrentTrack().getRequestedChannel() != null){
                                channel = mm.getTrackScheduler().getCurrentTrack().getRequestedChannel();
                            }

                            if (voiceState.isGuildMuted()) {
                                channel.sendMessage(EmoteReference.SAD + "Pausing player because I got muted :(").queue();
                                player.setPaused(true);
                            }

                            if (voiceChannel.getMembers().size() == 1) {
                                channel.sendMessage(EmoteReference.THINKING + "I decided to leave **" + voiceChannel.getName() + "** because I was left all " +
                                        "alone :<").queue();
                                guild.getAudioManager().closeAudioConnection();
                            }
                        }
                    }
                }
            }
            catch (Exception ignored) {}
        });
    }
}
