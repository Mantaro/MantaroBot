package net.kodehawa.mantarobot.commands.music;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.managers.AudioManager;
import net.kodehawa.mantarobot.MantaroBot;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceLeaveTimer {
	private final Map<String, Long> expiring;
	private boolean updated = false;

	public VoiceLeaveTimer() {
		this(new ConcurrentHashMap<>());
	}

	public VoiceLeaveTimer(Map<String, Long> timingOut) {
		this.expiring = Collections.synchronizedMap(timingOut);

		Thread thread = new Thread(this::threadcode, "VoiceLeaveTimer");
		thread.setDaemon(true);
		thread.start();
	}

	public void addMusicPlayer(String id, long millis) {
		expiring.put(id, millis);
		updated = true;
		synchronized (this) {
			notify();
		}
	}

	public Long get(String key) {
		return expiring.get(key);
	}

	public boolean isExpiring(String key) {
		return expiring.containsKey(key);
	}

	public boolean removeMusicPlayer(String id) {
		if (expiring.containsKey(id)) {
			expiring.remove(id);
			updated = true;
			synchronized (this) {
				notify();
			}
			return true;
		}
		return false;
	}

	private void threadcode() {
		//noinspection InfiniteLoopStatement
		while (true) {
			if (expiring.isEmpty()) {
				try {
					synchronized (this) {
						wait();
						updated = false;
					}
				} catch (InterruptedException ignored) {
				}
			}

			//noinspection OptionalGetWithoutIsPresent
			Map.Entry<String, Long> closestEntry = expiring.entrySet().stream().sorted(Comparator.comparingLong(Map.Entry::getValue)).findFirst().get();

			try {
				long timeout = closestEntry.getValue() - System.currentTimeMillis();
				if (timeout > 0) {
					synchronized (this) {
						wait(timeout);
					}
				}
			} catch (InterruptedException ignored) {
			}

			if (!updated) {
				String id = closestEntry.getKey();
				expiring.remove(id);

				Guild guild = MantaroBot.getJDA().getGuildById(id);
				if (guild == null) continue;
				AudioManager am = guild.getAudioManager();
				if (am.isConnected() || am.isAttemptingToConnect()) {
					GuildMusicManager musicManager = MantaroBot.getAudioManager().getMusicManager(guild);
					am.closeAudioConnection();
					if (musicManager.getTrackScheduler().isStopped()) {
						continue;
					}
					TextChannel channel = musicManager.getTrackScheduler().getCurrentTrack().getRequestedChannel();
					if (channel != null && channel.canTalk()) {
						channel.sendMessage("Nobody joined the VoiceChannel after 2 minutes, stopping the player...").queue();
					}
					musicManager.getTrackScheduler().stop();
				}
			} else updated = false; //and the loop will restart and resolve it
		}
	}
}
