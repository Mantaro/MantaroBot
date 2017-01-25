package net.kodehawa.mantarobot.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import net.kodehawa.mantarobot.MantaroBot;

public class DiscordLogBack extends AppenderBase<ILoggingEvent> {
	private static boolean enabled = false;

	public static void disable() {
		enabled = false;
	}

	public static void enable() {
		enabled = true;
	}

	private PatternLayout patternLayout;

	@Override
	protected void append(ILoggingEvent event) {
		if (!enabled) return;
		if (!event.getLevel().isGreaterOrEqual(Level.DEBUG)) return;

		MantaroBot.getJDA().getTextChannelById("266231083341840385").sendMessage(patternLayout.doLayout(event)).queue();
	}

	@Override
	public void start() {
		patternLayout = new PatternLayout();
		patternLayout.setContext(getContext());
		patternLayout.setPattern("[%d{HH:mm:ss}] [%t/%level] [%logger{0}]: %msg%n");
		patternLayout.start();

		super.start();
	}
}