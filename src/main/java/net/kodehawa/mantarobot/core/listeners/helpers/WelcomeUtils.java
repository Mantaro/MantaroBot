/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.core.listeners.helpers;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.kodehawa.mantarobot.commands.custom.EmbedJSON;
import net.kodehawa.mantarobot.commands.custom.legacy.DynamicModifiers;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.JsonDataManager;

import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

public class WelcomeUtils {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern MODIFIER_PATTERN = Pattern.compile("\\p{L}*:");

    public static void sendJoinLeaveMessage(User user, Guild guild, TextChannel tc, List<String> extraMessages, String msg) {
        sendJoinLeaveMessage(user, guild, tc, extraMessages, msg, false);
    }

    public static void sendJoinLeaveMessage(User user, Guild guild, TextChannel tc, List<String> extraMessages, String msg, boolean test) {
        var message = msg;
        if (!extraMessages.isEmpty()) {
            if (msg != null) {
                extraMessages.add(msg);
            }

            message = extraMessages.get(RANDOM.nextInt(extraMessages.size()));
        }

        if (tc != null && message != null) {
            if (!tc.canTalk()) {
                return;
            }

            if (message.contains("$(")) {
                message = new DynamicModifiers()
                        .mapFromJoinLeave("event", tc, user, guild)
                        .resolve(message);
            }

            var modIndex = message.indexOf(':');
            if (modIndex != -1) {
                // Wonky?
                var matcher = MODIFIER_PATTERN.matcher(message);
                var modifier = "none";
                // Find the first occurrence of a modifier (word:)
                if (matcher.find()) {
                    modifier = matcher.group().replace(":", "");
                }

                var json = message.substring(modIndex + 1);
                var extra = "";

                // Somehow (?) this fails sometimes? I really dunno how, but sure.
                try {
                    extra = message.substring(0, modIndex - modifier.length()).trim();
                } catch (Exception ignored) { }

                try {
                    if (modifier.equals("embed")) {
                        EmbedJSON embed;
                        try {
                            embed = JsonDataManager.fromJson('{' + json + '}', EmbedJSON.class);
                        } catch (Exception e) {
                            tc.sendMessage(EmoteReference.ERROR2 +
                                    "The string\n```json\n{" + json + "}```\n" +
                                    "Is not a valid welcome/leave message (failed to Convert to EmbedJSON). Check the wiki for more information."
                            ).queue();

                            // So I know what is going on, regardless.
                            e.printStackTrace();
                            return;
                        }

                        var builder = new MessageCreateBuilder().setEmbeds(embed.gen(null));
                        if (!extra.isEmpty()) {
                            builder.addContent(extra);
                        }

                        if (test) {
                            builder.addContent("\n**This is a test message. No mentions will be shown or resolved.**");
                        }

                        tc.sendMessage(builder.build())
                                // Allow role mentions here, per popular request :P
                                .setAllowedMentions(test ? EnumSet.noneOf(Message.MentionType.class) : EnumSet.of(Message.MentionType.USER, Message.MentionType.ROLE))
                                .queue(success -> { }, error -> tc.sendMessage("Failed to send join/leave message.").queue()
                        );

                        return;
                    }
                } catch (Exception e) {
                    if (e.getMessage().toLowerCase().contains("url must be a valid")) {
                        tc.sendMessage("Failed to send join/leave message: Wrong image URL in thumbnail, image, footer and/or author.").queue();
                    } else {
                        tc.sendMessage("Failed to send join/leave message: Unknown error, try checking your message.").queue();
                        e.printStackTrace();
                    }
                }
            }

            if (test) {
                message += "\n**This is a test message. No mentions will be shown or resolved.**";
            }

            tc.sendMessage(message)
                    .setAllowedMentions(test ? EnumSet.noneOf(Message.MentionType.class) : EnumSet.of(Message.MentionType.USER, Message.MentionType.ROLE))
                    .queue(success -> { }, failure -> tc.sendMessage("Failed to send join/leave message.").queue());
        }
    }

}
