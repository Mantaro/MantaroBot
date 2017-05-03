package net.kodehawa.mantarobot.commands.music.listener;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.GuildVoiceState;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.kodehawa.mantarobot.MantaroBot;

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
                            if (voiceState.isGuildMuted()) {
                                channel.sendMessage("Pausing player now because you muted me!").queue();
                                player.setPaused(true);
                            }
                            if (voiceChannel.getMembers().size() == 1) {
                                channel.sendMessage("I decided to leave **" + voiceChannel.getName() + "** because I was all " +
                                        "alone!").queue();
                                guild.getAudioManager().closeAudioConnection();
                            }
                        }
                    }
                }
            }
            catch (Exception ignored) {
            }
        });
    }
}
