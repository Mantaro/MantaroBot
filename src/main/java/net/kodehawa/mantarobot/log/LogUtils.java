/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.SentryHelper;

import java.awt.*;
import java.util.Date;

public class LogUtils {
    private final static String ICON_URL = "https://totally-not.a-sketchy.site/985414.png";
    private static final String WEBHOOK_START = "https://discordapp.com/api/webhooks/";
    private static WebhookClient LOGBACK_WEBHOOK = null;
    private static WebhookClient SHARD_WEBHOOK = null;

    static {
        String shardWebhook = MantaroData.config().get().getShardWebhookUrl();
        String logWebhook = MantaroData.config().get().getWebhookUrl();
        if(shardWebhook != null) {
            String[] parts1 = shardWebhook.replace(WEBHOOK_START, "").split("/");
            SHARD_WEBHOOK = new WebhookClientBuilder(Long.parseLong(parts1[0]), parts1[1]).build();
        }

        if(logWebhook != null) {
            String[] parts2 = logWebhook.replace(WEBHOOK_START, "").split("/");
            LOGBACK_WEBHOOK = new WebhookClientBuilder(Long.parseLong(parts2[0]), parts2[1]).build();
        }
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
        if(SHARD_WEBHOOK == null) return;

        try {
            SHARD_WEBHOOK.send(new EmbedBuilder()
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
        if(LOGBACK_WEBHOOK == null) return;

        try {
            LOGBACK_WEBHOOK.send(new EmbedBuilder()
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
        if(LOGBACK_WEBHOOK == null) return;

        try {
            LOGBACK_WEBHOOK.send(new EmbedBuilder()
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
        if(LOGBACK_WEBHOOK == null) return;

        try {
            LOGBACK_WEBHOOK.send(message);
        } catch(Exception e) {
            SentryHelper.captureException("Cannot post to shard webhook", e, LogUtils.class);
        }
    }

    public static void shardSimple(String message) {
        if(SHARD_WEBHOOK == null) return;

        try {
            SHARD_WEBHOOK.send(message);
        } catch(Exception e) {
            SentryHelper.captureException("Cannot post to shard webhook", e, LogUtils.class);
        }
    }
}
