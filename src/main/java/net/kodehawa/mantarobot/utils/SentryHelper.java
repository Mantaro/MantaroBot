/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.utils;

import io.sentry.Sentry;
import io.sentry.context.Context;
import io.sentry.event.BreadcrumbBuilder;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import io.sentry.event.UserBuilder;
import io.sentry.event.interfaces.ExceptionInterface;

public class SentryHelper {
    
    public static void captureException(String message, Throwable t, Class<?> clazz) {
        EventBuilder eventBuilder = new EventBuilder()
                                            .withMessage(message)
                                            .withLevel(Event.Level.ERROR)
                                            .withLogger(clazz.getName())
                                            .withSentryInterface(new ExceptionInterface(t));
        Sentry.capture(eventBuilder);
    }
    
    public static void captureMessage(String message, Class<?> clazz) {
        EventBuilder eventBuilder = new EventBuilder()
                                            .withMessage(message)
                                            .withLevel(Event.Level.INFO)
                                            .withLogger(clazz.getName());
        
        Sentry.capture(eventBuilder);
    }
    
    public static void breadcrumb(String breadcrumb) {
        final Context context = Sentry.getContext();
        context.recordBreadcrumb(
                new BreadcrumbBuilder().setMessage(breadcrumb).build()
        );
    }
    
    public static void captureExceptionContext(String message, Throwable t, Class<?> clazz, String user) {
        final Context context = Sentry.getContext();
        context.setUser(new UserBuilder().setUsername(user).build());
        EventBuilder eventBuilder = new EventBuilder()
                                            .withMessage(message)
                                            .withLevel(Event.Level.ERROR)
                                            .withLogger(clazz.getName())
                                            .withSentryInterface(new ExceptionInterface(t));
        Sentry.capture(eventBuilder);
        Sentry.clearContext();
    }
    
    public static void captureMessageContext(String message, Class<?> clazz, String user) {
        final Context context = Sentry.getContext();
        context.setUser(new UserBuilder().setUsername(user).build());
        EventBuilder eventBuilder = new EventBuilder()
                                            .withMessage(message)
                                            .withLevel(Event.Level.INFO)
                                            .withLogger(clazz.getName());
        
        Sentry.capture(eventBuilder);
        Sentry.clearContext();
    }
    
    public static void captureMessageErrorContext(String message, Class<?> clazz, String user) {
        final Context context = Sentry.getContext();
        context.setUser(new UserBuilder().setUsername(user).build());
        EventBuilder eventBuilder = new EventBuilder()
                                            .withMessage(message)
                                            .withLevel(Event.Level.ERROR)
                                            .withLogger(clazz.getName());
        
        Sentry.capture(eventBuilder);
        Sentry.clearContext();
    }
    
    public static void breadcrumbContext(String breadcrumb, String user) {
        final Context context = Sentry.getContext();
        context.setUser(new UserBuilder().setUsername(user).build());
        context.recordBreadcrumb(
                new BreadcrumbBuilder().setMessage(breadcrumb).build()
        );
        Sentry.clearContext();
    }
}
