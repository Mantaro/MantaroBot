package net.kodehawa.mantarobot.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import net.dv8tion.jda.core.entities.TextChannel;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.SentryHelper;
import net.kodehawa.mantarobot.utils.Utils;

public class LogBack extends AppenderBase<ILoggingEvent> {
	private static boolean enabled = false;

	public static void disable() {
		enabled = false;
	}

	public static void enable() {
		enabled = true;
	}

	private static TextChannel consoleChannel() {
		return MantaroBot.getInstance().getTextChannelById(MantaroData.config().get().consoleChannel);
	}

	private PatternLayout patternLayout;
	private PatternLayout patternLayoutSentry;
	private ILoggingEvent previousEvent;

	@Override
	protected void append(ILoggingEvent event) {
		if (!enabled) return;
		if (!event.getLevel().isGreaterOrEqual(Level.INFO)) return;
		String toSend = patternLayout.doLayout(event);
		String sentry = patternLayoutSentry.doLayout(event);
		if (previousEvent != null && event.getMessage().equals(previousEvent.getMessage())) return;
		if (toSend.contains("INFO") && toSend.contains("RemoteNodeProcessor")) return;
		if (toSend.contains("PermissionException")) return;
		if (toSend.contains("ResponseProcessCookies")) return;
		if (toSend.contains("Read timed out")) return;
		if(toSend.contains("RateLimitedException")) return;

		if(event.getLevel().isGreaterOrEqual(Level.WARN)
				&& !toSend.contains("Attempting to reconnect in 2s") && !toSend.equalsIgnoreCase("Encountered an exception:")
				&& !toSend.contains("---- DISCONNECT")){
			SentryHelper.captureMessageErrorContext(sentry, this.getClass(), "Log Back");
		} else if (event.getLevel() == Level.INFO){
			SentryHelper.breadcrumb(sentry);
		}


		if (toSend.length() > 1920)
			toSend = ":warning: Received a message but it was too long, Hastebin: " + Utils.paste(toSend);
		consoleChannel().sendMessage(toSend).queue();
		previousEvent = event;
	}

	@Override
	public void start() {
		patternLayout = new PatternLayout();
		patternLayout.setContext(getContext());
		patternLayout.setPattern("[`%d{HH:mm:ss}`] [`%t/%level`] [`%logger{0}`]: %msg");
		patternLayout.start();


		patternLayoutSentry = new PatternLayout();
		patternLayoutSentry.setContext(getContext());
		patternLayoutSentry.setPattern("%msg");
		patternLayoutSentry.start();
		super.start();
	}
}