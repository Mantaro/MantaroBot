package net.kodehawa.mantarobot.commands.music;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

class AudioUtils {
	static String getLength(long length) {
		return String.format("%02d:%02d",
			TimeUnit.MILLISECONDS.toMinutes(length),
			TimeUnit.MILLISECONDS.toSeconds(length) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(length))
		);
	}

	static String getQueueList(BlockingQueue<AudioTrackContext> queue) {
		StringBuilder sb = new StringBuilder();
		int n = 1;
		for (AudioTrackContext audioTrack : queue) {
			long aDuration = audioTrack.getDuration();
			String duration = String.format("%02d:%02d",
				TimeUnit.MILLISECONDS.toMinutes(aDuration),
				TimeUnit.MILLISECONDS.toSeconds(aDuration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(aDuration))
			);

			String title = audioTrack.getInfo().title;
			if(title.length() > 30) title = title.substring(0, 30) + "...";
			//.append intensifies
			sb.append("**")
				.append(n)
				.append(".** [")
				.append(title)
				.append("](")
				.append(audioTrack.getInfo().uri)
				.append(") (")
				.append(duration)
				.append(")")
				.append(audioTrack.getDJ() != null ? "  **[" + audioTrack.getDJ().getName() + "]**" : "")
				.append("\n");
			n++;
		}
		return sb.toString();
	}

}
