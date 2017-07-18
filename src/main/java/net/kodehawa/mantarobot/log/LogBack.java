package net.kodehawa.mantarobot.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import net.kodehawa.mantarobot.utils.SentryHelper;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

public class LogBack extends AppenderBase<ILoggingEvent> {
    private static boolean enabled = false;
    private PatternLayout patternLayout;
    private PatternLayout patternLayoutSentry;
    private String[] filters = {
            "PermissionException", "Read timed out", "timeout", "RatelimitedException", "ResponseProcessCookies", "Could not find tracks from mix.",
            "Data truncation: Data too long for column", "RestAction queue returned failure: [ErrorResponseException] -1: ERROR",
            "RestAction queue returned failure: [ErrorResponseException] 10008: Unknown Message", "RestAction queue returned failure: [ErrorResponseException] 50001: Missing Access"
    };
    private String[] exactFilters = {
            "Encountered an exception:"
    };

    public static void disable() {
        enabled = false;
    }

    public static void enable() {
        enabled = true;
    }

    @Override
    protected void append(ILoggingEvent event) {
        if(!enabled) return;
        String toSend = patternLayout.doLayout(event);
        String sentry = patternLayoutSentry.doLayout(event);

        for(String filtered : filters) {
            if(toSend.contains(filtered)) {
                return;
            }
        }

        for(String filtered : exactFilters) {
            if(toSend.equalsIgnoreCase(filtered)) {
                return;
            }
        }

        if(event.getLevel().isGreaterOrEqual(Level.WARN)
                && !toSend.contains("Attempting to reconnect in 2s") && !toSend.contains("---- DISCONNECT")) {
            SentryHelper.captureMessageErrorContext(sentry, this.getClass(), "Log Back");
        }

        if(toSend.length() < 1920) {
            LogUtils.simple(toSend);
        } else {
            LogUtils.simple(EmoteReference.THINKING + "Received a log message larger than 1920 characters, so I pasted it (" + Utils.paste(toSend) + ")");
        }
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