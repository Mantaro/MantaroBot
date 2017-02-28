package net.kodehawa.mantarobot.commands.music;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.listeners.OptimizedListener;

public class VoiceChannelListener extends OptimizedListener<GenericGuildVoiceEvent> {
    private static VoiceLeaveTimer timer;

    static {
         timer = new VoiceLeaveTimer();
    }

    public VoiceChannelListener() {
        super(GenericGuildVoiceEvent.class);
    }

    private void onJoin(VoiceChannel vc, Member member) {
        Guild guild = vc.getGuild();
        if (!timer.isExpiring(guild.getId())) return;
        VoiceChannel v = guild.getAudioManager().isAttemptingToConnect() ? guild.getAudioManager().getQueuedAudioConnection() : guild.getAudioManager().getConnectedChannel();
        if (vc != v) return;
        GuildMusicManager musicManager = MantaroBot.getAudioManager().getMusicManager(guild);
        musicManager.getTrackScheduler().getAudioPlayer().setPaused(false);
        TextChannel channel = musicManager.getTrackScheduler().getCurrentTrack().getRequestedChannel();
        if (channel != null && channel.canTalk())
            channel.sendMessage(member.equals(guild.getSelfMember()) ? "I was moved into a Voice Channel with users listening, resumed the player!" : "Someone joined the Voice Channel, resumed the player!").queue();
        timer.removeMusicPlayer(guild.getId());
    }

    private void onLeave(VoiceChannel vc) {
        Guild guild = vc.getGuild();
        if (timer.isExpiring(guild.getId())) return;
        GuildMusicManager musicManager = MantaroBot.getAudioManager().getMusicManager(guild);
        if (musicManager.getTrackScheduler().isStopped()) return;
        musicManager.getTrackScheduler().getAudioPlayer().setPaused(true);
        TextChannel channel = musicManager.getTrackScheduler().getCurrentTrack().getRequestedChannel();
        if (channel != null && channel.canTalk())
            channel.sendMessage("I was left alone in the Voice Channel so I paused the player. If nobody join this channel within 2 minutes I'll stop the player.").queue();
        timer.addMusicPlayer(guild.getId(), 2000);
    }

    @Override
    public void event(GenericGuildVoiceEvent event) {
        if (event instanceof GuildVoiceMoveEvent) {
            VoiceChannel joined = ((GuildVoiceMoveEvent) event).getChannelJoined();
            VoiceChannel left = ((GuildVoiceMoveEvent) event).getChannelLeft();
            boolean isSelf = event.getMember().equals(event.getGuild().getSelfMember());
            if (isSelf) {
                if (isAlone(joined))
                    onLeave(joined);
                else
                    onJoin(joined, event.getMember());
            } else {
                if (isAlone(left) && left == event.getGuild().getAudioManager().getConnectedChannel())
                    onLeave(left);
                else if (!isAlone(joined) && joined == event.getGuild().getAudioManager().getConnectedChannel())
                    onJoin(joined, event.getMember());
            }

        } else if (event instanceof GuildVoiceJoinEvent) {
            if (((GuildVoiceJoinEvent) event).getChannelJoined() == event.getGuild().getAudioManager().getConnectedChannel()) {
                onJoin(((GuildVoiceJoinEvent) event).getChannelJoined(), event.getMember());
            }
        } else if (event instanceof GuildVoiceLeaveEvent) {
            if (((GuildVoiceLeaveEvent) event).getChannelLeft() == event.getGuild().getAudioManager().getConnectedChannel())
                onLeave(((GuildVoiceLeaveEvent) event).getChannelLeft());
        }
    }
    private boolean isAlone(VoiceChannel voiceChannel) {
        for (Member member : voiceChannel.getMembers()) {
            if (!member.getUser().isBot() && !member.getVoiceState().isDeafened())
                return true;
        }
        return false;
    }
}
