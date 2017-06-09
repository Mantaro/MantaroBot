package net.kodehawa.mantarobot.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.listeners.operations.ReactionOperations;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.DBGuild;
import net.kodehawa.mantarobot.data.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.awt.Color;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

@Slf4j
public class AudioRequester implements AudioLoadResultHandler {
	public static final int MAX_QUEUE_LENGTH = 300;
	public static final long MAX_SONG_LENGTH = 1260000;
	private GuildMessageReceivedEvent event;
	private GuildMusicManager musicManager;
	private String trackUrl;
	private boolean skipSelection;

	AudioRequester(GuildMusicManager musicManager, GuildMessageReceivedEvent event, String trackUrl, boolean skipSelection) {
		this.musicManager = musicManager;
		this.trackUrl = trackUrl;
		this.event = event;
		this.skipSelection = skipSelection;
	}

	@Override
	public void trackLoaded(AudioTrack track) {
		loadSingle(track, false);
	}

	@Override
	public void playlistLoaded(AudioPlaylist playlist) {
		if (playlist.isSearchResult()) {
			if (!skipSelection) onSearchResult(playlist);
			else loadSingle(playlist.getTracks().get(0), false);
			return;
		}

		int i = 0;
		for (AudioTrack track : playlist.getTracks()) {
			if (MantaroData.db().getGuild(event.getGuild()).getData().getMusicQueueSizeLimit() != null) {
				if (i < MantaroData.db().getGuild(event.getGuild()).getData().getMusicQueueSizeLimit()) {
					loadSingle(track, true);
				} else {
					event.getChannel().sendMessage(String.format(":warning: The queue you added had more than %d songs, so we added songs until this limit and ignored the rest.", MantaroData.db().getGuild(event.getGuild()).getData().getMusicQueueSizeLimit())).queue();
					break;
				}
			} else {
				if (i < MAX_QUEUE_LENGTH) {
					loadSingle(track, true);
				} else {
					event.getChannel().sendMessage(":warning: The queue you added had more than 300 songs, so we added songs until this limit and ignored the rest.").queue();
					break;
				}
			}
			i++;
		}

		event.getChannel().sendMessage(String.format(
			"Added **%d songs** to queue on playlist: **%s** *(%s)*", i,
			playlist.getName(),
			Utils.getDurationMinutes(playlist.getTracks().stream().mapToLong(temp -> temp.getInfo().length).sum())
		)).queue();

	}

	@Override
	public void noMatches() {
		event.getChannel().sendMessage(String.format("Nothing found by %s.", trackUrl.startsWith("ytsearch:") ? trackUrl.substring(9) : trackUrl)).queue();
		if (musicManager.getTrackScheduler().isStopped())
			event.getGuild().getAudioManager().closeAudioConnection();
	}

	@Override
	public void loadFailed(FriendlyException exception) {
		if (!exception.severity.equals(FriendlyException.Severity.FAULT)) {
			event.getChannel().sendMessage("\u274C Error while fetching music: " + exception.getMessage()).queue();
		} else {
			log.warn("Error caught while playing audio, the bot might be able to continue playing music.", exception);
		}
		if (musicManager.getTrackScheduler().isStopped())
			event.getGuild().getAudioManager().closeAudioConnection();
	}

	public GuildMusicManager getMusicManager() {
		return musicManager;
	}

	private void loadSingle(AudioTrack audioTrack, boolean silent) {
		AudioTrackInfo trackInfo = audioTrack.getInfo();
		DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
		GuildData guildData = dbGuild.getData();

		String title = trackInfo.title;
		long length = trackInfo.length;

		long queueLimit = !Optional.ofNullable(dbGuild.getData().getMusicQueueSizeLimit()).
			isPresent() ? MAX_QUEUE_LENGTH : dbGuild.getData().getMusicQueueSizeLimit();

		int fqSize = guildData.getMaxFairQueue();

		if (getMusicManager().getTrackScheduler().getQueue().size() > queueLimit
			&& !MantaroData.db().getUser(event.getMember()).isPremium()
			&& !dbGuild.isPremium()) {
			if (!silent)
				event.getChannel().sendMessage(String.format(":warning: Could not queue %s: Surpassed queue song limit!", title)).queue(
					message -> message.delete().queueAfter(30, TimeUnit.SECONDS)
				);
			if (musicManager.getTrackScheduler().isStopped()) event.getGuild().getAudioManager().closeAudioConnection();
			return;
		}

		if (audioTrack.getInfo().length > MAX_SONG_LENGTH && !MantaroData.db().getUser(event.getMember()).isPremium()
			&& !dbGuild.isPremium()) {
			event.getChannel().sendMessage(String.format(":warning: Could not queue %s: Track is longer than 21 minutes! (%s)", title, AudioUtils.getLength(length))).queue();
			if (musicManager.getTrackScheduler().isStopped())
				event.getGuild().getAudioManager().closeAudioConnection(); //do you?
			return;
		}

		//Comparing if the URLs are the same to be 100% sure they're just not spamming the same url over and over again.
		if(musicManager.getTrackScheduler().getQueue().stream().filter(track -> track.getInfo().uri.equals(audioTrack.getInfo().uri)).count() > fqSize){
			event.getChannel().sendMessage(EmoteReference.ERROR + String.format("**Surpassed fair queue level of %d (Too many songs which are exactly equal)**", fqSize + 1)).queue();
			return;
		}

		musicManager.getTrackScheduler().queue(new AudioTrackContext(event.getAuthor(), event.getChannel(), audioTrack.getSourceManager() instanceof YoutubeAudioSourceManager ? "https://www.youtube.com/watch?v=" + audioTrack.getIdentifier() : trackUrl, audioTrack));

		if (!silent) {
			event.getChannel().sendMessage(
					String.format("\uD83D\uDCE3 Added to queue -> **%s** **!(%s)**", title, AudioUtils.getLength(length))
			).queue();
		}
	}

	private void onSearchResult(AudioPlaylist playlist) {
		EmbedBuilder builder = new EmbedBuilder().setColor(Color.CYAN).setTitle("Song selection." +
				(MantaroData.db().getGuild(event.getGuild()).getData().isReactionMenus() ? "React to the desired number to select a song." : "Type the song number to continue."), null)
				.setThumbnail("http://www.clipartbest.com/cliparts/jix/6zx/jix6zx4dT.png")
				.setFooter("This timeouts in 10 seconds.", null);
		java.util.List<AudioTrack> tracks = playlist.getTracks();
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < 4 && i < tracks.size(); i++) {
			AudioTrack at = tracks.get(i);
			b.append(i + 1)
					.append(".** [")
					.append(at.getInfo().title)
					.append("](")
					.append(at.getInfo().uri)
					.append(")**" +
							" (")
					.append(Utils.getDurationMinutes(at.getInfo().length))
					.append(")")
					.append("\n");
		}

		builder.setDescription(b);

		if(!event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION) ||
				!MantaroData.db().getGuild(event.getGuild()).getData().isReactionMenus()) {
            event.getChannel().sendMessage(builder.setDescription(b.toString()).build()).queue();
            IntConsumer consumer = (c) -> loadSingle(playlist.getTracks().get(c - 1), false);
            DiscordUtils.selectInt(event, 5, consumer);
            return;
        }

        long id = event.getAuthor().getIdLong(); //just in case someone else uses play before timing out
        ReactionOperations.create(event.getChannel().sendMessage(builder.build()).complete(), 15, (e)->{
            if(e.getUser().getIdLong() != id) return false;
			int i = e.getReactionEmote().getName().charAt(0)-'\u0030';
            if(i < 1 || i > 4) return false;
            loadSingle(playlist.getTracks().get(i - 1), false);
            try{
				if(event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)){
					event.getChannel().getMessageById(e.getMessageIdLong()).queue(m -> m.clearReactions().queue());
				}
			} catch (Exception ignored){}
            return true;
        }, "\u0031\u20e3", "\u0032\u20e3", "\u0033\u20e3", "\u0034\u20e3");
	}
}
