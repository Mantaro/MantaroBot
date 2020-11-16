/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.log;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LogUtils {
    private static final Logger log = LoggerFactory.getLogger(LogUtils.class);

    private final static String ICON_URL = "https://i.imgur.com/h5FQyuf.png";
    private static final String WEBHOOK_START = "https://discordapp.com/api/webhooks/";
    private static WebhookClient LOGBACK_WEBHOOK;
    private static WebhookClient SHARD_WEBHOOK;
    private static WebhookClient SPAMBOT_WEBHOOK;

    static {
        var shardWebhook = MantaroData.config().get().getShardWebhookUrl();
        var logWebhook = MantaroData.config().get().getWebhookUrl();
        var spambotWebhook = MantaroData.config().get().getSpambotUrl();

        if (shardWebhook != null) {
            var parts = shardWebhook.replace(WEBHOOK_START, "").split("/");
            SHARD_WEBHOOK = new WebhookClientBuilder(Long.parseLong(parts[0]), parts[1]).build();
        }

        if (logWebhook != null) {
            var parts = logWebhook.replace(WEBHOOK_START, "").split("/");
            LOGBACK_WEBHOOK = new WebhookClientBuilder(Long.parseLong(parts[0]), parts[1]).build();
        } else {
            log.error("Webhook URL is null. Webhooks won't be posted at all to status channels.");
        }

        if (spambotWebhook != null) {
            var parts = spambotWebhook.replace(WEBHOOK_START, "").split("/");
            SPAMBOT_WEBHOOK = new WebhookClientBuilder(Long.parseLong(parts[0]), parts[1]).build();
        } else {
            log("Warning", "Spambot Webhook URL is null. Spam bots won't be logged.");
            log.error("Spambot Webhook URL is null. Spam bots won't be logged to status channels.");
        }
    }

    public static void shard(String message) {
        if (SHARD_WEBHOOK == null) {
            return;
        }

        try {
            SHARD_WEBHOOK.send(new WebhookEmbed(
                    null, Color.PINK.getRGB(), message,
                    null, null,
                    new WebhookEmbed.EmbedFooter(Utils.formatDate(OffsetDateTime.now()), ICON_URL),
                    new WebhookEmbed.EmbedTitle("Shard", null), null,
                    new ArrayList<>()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void log(String title, String message) {
        if (LOGBACK_WEBHOOK == null) {
            return;
        }

        try {
            LOGBACK_WEBHOOK.send(new WebhookEmbed(
                    null, Color.PINK.getRGB(), message,
                    null, null,
                    new WebhookEmbed.EmbedFooter(Utils.formatDate(OffsetDateTime.now()), ICON_URL),
                    new WebhookEmbed.EmbedTitle(title, null), null,
                    new ArrayList<>()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void log(String message) {
        if (LOGBACK_WEBHOOK == null) {
            return;
        }

        try {
            LOGBACK_WEBHOOK.send(new WebhookEmbed(
                    null, Color.PINK.getRGB(), message,
                    null, null,
                    new WebhookEmbed.EmbedFooter(Utils.formatDate(OffsetDateTime.now()), ICON_URL),
                    new WebhookEmbed.EmbedTitle("Log", null), null,
                    new ArrayList<>()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void simple(String message) {
        if (LOGBACK_WEBHOOK == null) {
            return;
        }

        try {
            LOGBACK_WEBHOOK.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void shardSimple(String message) {
        if (SHARD_WEBHOOK == null) {
            return;
        }

        try {
            SHARD_WEBHOOK.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void spambot(User user, String guildId, String channelId, String messageId, SpamType type) {
        if (SPAMBOT_WEBHOOK == null) {
            log.warn("---- Spambot detected! ID: {}, Reason: {}", user.getId(), type);
            return;
        }

        try {
            List<WebhookEmbed.EmbedField> fields = new ArrayList<>();
            fields.add(new WebhookEmbed.EmbedField(false, "Tag", String.format("%#s", user)));
            fields.add(new WebhookEmbed.EmbedField(true, "ID", user.getId()));
            fields.add(new WebhookEmbed.EmbedField(true, "Guild ID", guildId));
            fields.add(new WebhookEmbed.EmbedField(true, "Channel ID", channelId));
            fields.add(new WebhookEmbed.EmbedField(true, "Message ID", messageId));

            fields.add(new WebhookEmbed.EmbedField(true, "Account Creation", user.getTimeCreated().toString()));
            fields.add(new WebhookEmbed.EmbedField(true, "Mutual Guilds", user.getMutualGuilds().stream().map(g ->
                    g.getId() + ": " + g.getMemberCount() + " members"
            ).collect(Collectors.joining("\n"))));

            fields.add(new WebhookEmbed.EmbedField(false, "Type", type.toString()));

            if (type == SpamType.BLATANT) {
                var mantaroData = MantaroData.db().getMantaroData();
                mantaroData.getBlackListedUsers().add(user.getId());
                mantaroData.save();

                fields.add(new WebhookEmbed.EmbedField(false, "Info", "User has been blacklisted automatically. " +
                        "For more information use the investigate command.")
                );
            }

            SPAMBOT_WEBHOOK.send(new WebhookEmbed(null, Color.PINK.getRGB(),
                    null, user.getEffectiveAvatarUrl(),
                    null, new WebhookEmbed.EmbedFooter(Utils.formatDate(OffsetDateTime.now()), ICON_URL),
                    new WebhookEmbed.EmbedTitle("Possible spambot detected", null), null, fields));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public enum SpamType {
        BLATANT, OVER_SPAM_LIMIT
    }
}
