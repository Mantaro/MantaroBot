package net.kodehawa.mantarobot.commands.music.debug;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.hook.AudioOutputHook;
import com.sedmelluq.discord.lavaplayer.player.hook.AudioOutputHookFactory;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.core.listeners.MantaroListener;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeedingTicketFactory implements AudioOutputHookFactory {
    private static final Logger log = LoggerFactory.getLogger(SpeedingTicketFactory.class);
    private static Guild guild;

    public SpeedingTicketFactory(Guild guild){
        this.guild = guild;
    }

    @Override
    public AudioOutputHook createOutputHook() {
        return new SpeedingTicketIssuer();
    }

    private static class SpeedingTicketIssuer implements AudioOutputHook {
        private final long[] timestamps;
        private boolean suspected;
        private int samplesCollected;
        private int start;
        private int count;

        private SpeedingTicketIssuer() {
            this.timestamps = new long[500];
        }

        @Override
        public AudioFrame outgoingFrame(AudioPlayer player, AudioFrame frame) {
            long time = System.currentTimeMillis();

            synchronized (timestamps) {
                if (suspected) {
                    if(samplesCollected++ > 6){
                        if(MantaroBot.getInstance().getGuildById(guild.getId()) != null){
                            MantaroAudioManager manager = MantaroBot.getInstance().getAudioManager();
                            AudioManager audioManager = guild.getAudioManager();
                            if(!audioManager.isAttemptingToConnect()) {
                                VoiceChannel previousVc = audioManager.getConnectedChannel();
                                audioManager.closeAudioConnection();
                                manager.getMusicManagers().remove(guild.getId());
                                audioManager.openAudioConnection(previousVc);
                                MantaroListener.getLogChannel().sendMessage(EmoteReference.THINKING + "Performed automatic music speedup fix on guild " + guild.getId()).queue();
                                suspected = false;
                            }
                        }
                    } else {
                        log.warn("Sample #{} for {}.", samplesCollected, System.identityHashCode(player), new Throwable());
                    }
                } else {
                    if (count < timestamps.length) {
                        timestamps[count++] = time;
                    } else {
                        timestamps[start] = time;
                        start = (start + 1) % timestamps.length;
                        measureSpeed(player);
                    }
                }
            }

            return frame;
        }

        private void measureSpeed(AudioPlayer player) {
            long first = timestamps[start];
            long last = timestamps[(start + count - 1) % timestamps.length];
            long average = (last - first) / count;

            if (average <= 11) {
                suspected = true;
                log.warn("Detected possible multi-polling on {}. Will log stack traces for the next 6 provide calls.", System.identityHashCode(player));
            }
        }
    }
}