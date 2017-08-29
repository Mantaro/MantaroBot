/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.log;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.SentryHelper;
import net.kodehawa.mantarobot.utils.http.Webhook;

import java.awt.*;
import java.util.Date;

public class LogUtils {
    private static final Webhook SHARD_WEBHOOK;
    private static final Webhook LOGBACK_WEBHOOK;
    private final static String ICON_URL = "https://totally-not.a-sketchy.site/985414.png";

    static {
        SHARD_WEBHOOK = new Webhook(MantaroData.config().get().getShardWebhookUrl());
        LOGBACK_WEBHOOK = new Webhook(MantaroData.config().get().getWebhookUrl());
    }

    public static MessageEmbed createLogEmbed(String title, String message) {
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(message)
                .setColor(Color.PINK)
                .setFooter(new Date(System.currentTimeMillis()).toString(), ICON_URL)
                .build();
    }

    public static void shard(String message) {
        try {
            SHARD_WEBHOOK.post(new EmbedBuilder()
                    .setTitle("Shard")
                    .setDescription(message)
                    .setColor(Color.PINK)
                    .setFooter(new Date(System.currentTimeMillis()).toString(), ICON_URL)
                    .build());
        } catch(Exception e) {
            SentryHelper.captureException("Cannot post to shard webhook", e, LogUtils.class);
        }
    }

    public static void log(String title, String message) {
        try {
            LOGBACK_WEBHOOK.post(new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(message)
                    .setColor(Color.PINK)
                    .setFooter(new Date(System.currentTimeMillis()).toString(), ICON_URL)
                    .build());
        } catch(Exception e) {
            SentryHelper.captureException("Cannot post to shard webhook", e, LogUtils.class);
        }
    }

    public static void log(String message) {
        try {
            LOGBACK_WEBHOOK.post(new EmbedBuilder()
                    .setTitle("Log")
                    .setDescription(message)
                    .setColor(Color.PINK)
                    .setFooter(new Date(System.currentTimeMillis()).toString(), ICON_URL)
                    .build());
        } catch(Exception e) {
            SentryHelper.captureException("Cannot post to shard webhook", e, LogUtils.class);
        }
    }

    public static void simple(String message) {
        try {
            LOGBACK_WEBHOOK.post(message);
        } catch(Exception e) {
            SentryHelper.captureException("Cannot post to shard webhook", e, LogUtils.class);
        }
    }

    public static void shardSimple(String message) {
        try {
            SHARD_WEBHOOK.post(message);
        } catch(Exception e) {
            SentryHelper.captureException("Cannot post to shard webhook", e, LogUtils.class);
        }
    }
}
