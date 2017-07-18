package net.kodehawa.mantarobot.log;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.SentryHelper;
import net.kodehawa.mantarobot.utils.http.Webhook;

import java.awt.*;
import java.util.Date;

public class LogUtils {
    private final static String ICON_URL = "https://totally-not.a-sketchy.site/985414.png";
    private static Webhook SHARD_WEBHOOK;
    private static Webhook LOGBACK_WEBHOOK;

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
