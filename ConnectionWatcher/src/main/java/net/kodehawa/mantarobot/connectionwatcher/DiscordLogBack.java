package net.kodehawa.mantarobot.connectionwatcher;

import net.dv8tion.jda.core.entities.TextChannel;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class DiscordLogBack extends AppenderBase<ILoggingEvent> {
    private static boolean enabled = false;

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
        ConnectionWatcher.getInstance().getConsole().sendMessage(toSend).queue();
        previousEvent = event;
    }

    @Override
    public void start() {
        patternLayout = new PatternLayout();
        patternLayout.setContext(getContext());
        patternLayout.setPattern("`[%d{HH:mm:ss}]` [%t/%level] [%logger{0}]: %msg%n");
        patternLayout.start();

        super.start();
    }
}