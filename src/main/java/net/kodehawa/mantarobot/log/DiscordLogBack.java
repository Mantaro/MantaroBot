package net.kodehawa.mantarobot.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import net.dv8tion.jda.core.entities.TextChannel;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;

public class DiscordLogBack extends AppenderBase<ILoggingEvent> {
	private static boolean enabled = false;

	private static TextChannel consoleChannel() {
		return MantaroBot.getInstance().getTextChannelById(MantaroData.config().get().consoleChannel);
	}

	public static void disable() {
		enabled = false;
	}

	public static void enable() {
		enabled = true;
	}

	private PatternLayout patternLayout;
	private ILoggingEvent previousEvent;

	@Override
	protected void append(ILoggingEvent event) {
		if (!enabled) return;
		if (!event.getLevel().isGreaterOrEqual(Level.INFO)) return;
		String toSend = patternLayout.doLayout(event);
		if (previousEvent != null && event.getMessage().equals(previousEvent.getMessage())) return;
		if (toSend.contains("INFO") && toSend.contains("RemoteNodeProcessor")) return;
		if (toSend.length() > 1920) toSend = "Received a message but it was too long, Hastebin:" + Utils.paste(toSend);
		consoleChannel().sendMessage(toSend).queue();
		previousEvent = event;
	}

	@Override
	public void start() {
		patternLayout = new PatternLayout();
		patternLayout.setContext(getContext());
		patternLayout.setPattern("[`%d{HH:mm:ss}`] [`%t/%level`] [`%logger{0}`]: %msg");
		patternLayout.start();

		super.start();
	}
}