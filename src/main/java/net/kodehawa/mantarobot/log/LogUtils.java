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

package net.kodehawa.mantarobot.log;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.SentryHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class LogUtils {
    private final static String ICON_URL = "https://totally-not.a-sketchy.site/985414.png";
    private static final String WEBHOOK_START = "https://discordapp.com/api/webhooks/";
    private static WebhookClient LOGBACK_WEBHOOK;
    private static WebhookClient SHARD_WEBHOOK;
    private static WebhookClient SPAMBOT_WEBHOOK;
    
    static {
        String shardWebhook = MantaroData.config().get().getShardWebhookUrl();
        String logWebhook = MantaroData.config().get().getWebhookUrl();
        String spambotWebhook = MantaroData.config().get().getSpambotUrl();
        if(shardWebhook != null) {
            String[] parts = shardWebhook.replace(WEBHOOK_START, "").split("/");
            SHARD_WEBHOOK = new WebhookClientBuilder(Long.parseLong(parts[0]), parts[1]).build();
        }
        if(logWebhook != null) {
            String[] parts = logWebhook.replace(WEBHOOK_START, "").split("/");
            LOGBACK_WEBHOOK = new WebhookClientBuilder(Long.parseLong(parts[0]), parts[1]).build();
        }
        if(spambotWebhook != null) {
            String[] parts = spambotWebhook.replace(WEBHOOK_START, "").split("/");
            SPAMBOT_WEBHOOK = new WebhookClientBuilder(Long.parseLong(parts[0]), parts[1]).build();
        }
    }
    
    public static void shard(String message) {
        if(SHARD_WEBHOOK == null) return;
        
        try {
            SHARD_WEBHOOK.send(new WebhookEmbed(
                    null, Color.PINK.getRGB(), message,
                    null, null,
                    new WebhookEmbed.EmbedFooter(new Date(System.currentTimeMillis()).toString(), ICON_URL),
                    new WebhookEmbed.EmbedTitle("Shard", null), null,
                    new ArrayList<>()));
        } catch(Exception e) {
            SentryHelper.captureException("Cannot post to shard webhook", e, LogUtils.class);
        }
    }
    
    public static void log(String title, String message) {
        if(LOGBACK_WEBHOOK == null) return;
        
        try {
            LOGBACK_WEBHOOK.send(new WebhookEmbed(
                    null, Color.PINK.getRGB(), message,
                    null, null,
                    new WebhookEmbed.EmbedFooter(new Date(System.currentTimeMillis()).toString(), ICON_URL),
                    new WebhookEmbed.EmbedTitle(title, null), null,
                    new ArrayList<>()));
        } catch(Exception e) {
            SentryHelper.captureException("Cannot post to shard webhook", e, LogUtils.class);
        }
    }
    
    public static void log(String message) {
        if(LOGBACK_WEBHOOK == null) return;
        
        try {
            LOGBACK_WEBHOOK.send(new WebhookEmbed(
                    null, Color.PINK.getRGB(), message,
                    null, null,
                    new WebhookEmbed.EmbedFooter(new Date(System.currentTimeMillis()).toString(), ICON_URL),
                    new WebhookEmbed.EmbedTitle("Log", null), null,
                    new ArrayList<>()));
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
    
    public static void spambot(User user) {
        if(SPAMBOT_WEBHOOK == null) return;
        try {
            List<WebhookEmbed.EmbedField> fields = new ArrayList<>();
            fields.add(new WebhookEmbed.EmbedField(true, "Tag", String.format("%#s", user)));
            fields.add(new WebhookEmbed.EmbedField(true, "ID", user.getId()));
            fields.add(new WebhookEmbed.EmbedField(true, "Account Creation", user.getTimeCreated().toString()));
            fields.add(new WebhookEmbed.EmbedField(true, "Mutual Guilds", user.getMutualGuilds().stream().map(g ->
                                                                                                                      g.getId() + ": " + g.getMemberCache().size() + " members"
            ).collect(Collectors.joining("\n"))));
            
            SPAMBOT_WEBHOOK.send(new WebhookEmbed(null, Color.PINK.getRGB(),
                    null, user.getEffectiveAvatarUrl(),
                    null, new WebhookEmbed.EmbedFooter(new Date(System.currentTimeMillis()).toString(), ICON_URL),
                    new WebhookEmbed.EmbedTitle("Possible spambot detected", null), null, fields));
        } catch(Exception e) {
            SentryHelper.captureException("Cannot post to spambot webhook", e, LogUtils.class);
        }
    }
}
