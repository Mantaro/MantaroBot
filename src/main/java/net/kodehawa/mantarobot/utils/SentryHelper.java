package net.kodehawa.mantarobot.utils;

import io.sentry.Sentry;
import io.sentry.event.BreadcrumbBuilder;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import io.sentry.event.UserBuilder;
import io.sentry.event.interfaces.ExceptionInterface;

public class SentryHelper {

    public static void captureException(String message, Throwable t, Class clazz) {
        EventBuilder eventBuilder = new EventBuilder()
                .withMessage(message)
                .withLevel(Event.Level.ERROR)
                .withLogger(clazz.getName())
                .withSentryInterface(new ExceptionInterface(t));
        Sentry.capture(eventBuilder);
    }

    public static void captureMessage(String message, Class clazz) {
        EventBuilder eventBuilder = new EventBuilder()
                .withMessage(message)
                .withLevel(Event.Level.INFO)
                .withLogger(clazz.getName());

        Sentry.capture(eventBuilder);
    }

    public static void breadcrumb(String breadcrumb) {
        Sentry.record(
                new BreadcrumbBuilder().setMessage(breadcrumb).build()
        );
    }

    public static void captureExceptionContext(String message, Throwable t, Class clazz, String user) {
        Sentry.setUser(new UserBuilder().setUsername(user).build());
        EventBuilder eventBuilder = new EventBuilder()
                .withMessage(message)
                .withLevel(Event.Level.ERROR)
                .withLogger(clazz.getName())
                .withSentryInterface(new ExceptionInterface(t));
        Sentry.capture(eventBuilder);
        Sentry.clearContext();
    }

    public static void captureMessageContext(String message, Class clazz, String user) {
        Sentry.setUser(new UserBuilder().setUsername(user).build());
        EventBuilder eventBuilder = new EventBuilder()
                .withMessage(message)
                .withLevel(Event.Level.INFO)
                .withLogger(clazz.getName());

        Sentry.capture(eventBuilder);
        Sentry.clearContext();
    }

    public static void captureMessageErrorContext(String message, Class clazz, String user) {
        Sentry.setUser(new UserBuilder().setUsername(user).build());
        EventBuilder eventBuilder = new EventBuilder()
                .withMessage(message)
                .withLevel(Event.Level.ERROR)
                .withLogger(clazz.getName());

        Sentry.capture(eventBuilder);
        Sentry.clearContext();
    }

    public static void breadcrumbContext(String breadcrumb, String user) {
        Sentry.setUser(new UserBuilder().setUsername(user).build());
        Sentry.record(
                new BreadcrumbBuilder().setMessage(breadcrumb).build()
        );
        Sentry.clearContext();
    }
}