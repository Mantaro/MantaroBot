package net.kodehawa.mantarobot.exception;

import net.dv8tion.jda.core.entities.TextChannel;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.log.State;
import net.kodehawa.mantarobot.log.Type;

public class ExceptionHandler {

	public void handle(String content, Type t, State s, Exception ex) {
		if (s.equals(State.POSTLOAD)) {
			TextChannel txc = Mantaro.instance().getSelf().getTextChannelById("255479987409387520");
			StringBuilder sb = new StringBuilder();
			sb.append(ex.toString()).append("\n");
			for (StackTraceElement ste : ex.getStackTrace()) {
				sb.append("      ").
					append(ste).
					append("\n");
			}

			switch (t) {
				case WARNING:
					if (ex != null) {
						txc.sendMessage(content).queue();
						txc.sendMessage("[WARNING] Exception occurred on: " + Thread.currentThread().getName() + ".\n").queue();
						txc.sendMessage(sb.toString()).queue();
						break;
					} else {
						break;
					}
				case CRITICAL:
					if (ex != null) {
						txc.sendMessage(content).queue();
						txc.sendMessage("[CRITICAL] Exception occurred on: " + Thread.currentThread().getName() + ".\n").queue();
						txc.sendMessage(sb.toString()).queue();
						break;
					} else {
						break;
					}
				case INFO:
					break;
			}
		}
	}
}
