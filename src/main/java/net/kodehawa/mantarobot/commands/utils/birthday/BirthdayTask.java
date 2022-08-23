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

package net.kodehawa.mantarobot.commands.utils.birthday;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.utils.SplitUtil;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.custom.EmbedJSON;
import net.kodehawa.mantarobot.commands.custom.legacy.DynamicModifiers;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.JsonDataManager;
import net.kodehawa.mantarobot.utils.exporters.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BirthdayTask {
    private static final Pattern MODIFIER_PATTERN = Pattern.compile("\\b\\p{L}*:\\b");
    private static final Logger log = LoggerFactory.getLogger(BirthdayTask.class);
    private static final DateTimeFormatter dayMonthFormat = DateTimeFormatter.ofPattern("dd-MM");
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("MM");

    private static final String modLogMessage = "Birthday assigner." +
            " If you see this happening for every member of your server, or in unintended ways, please do ~>opts birthday disable";

    private static final ScheduledExecutorService backOffPool = Executors.newScheduledThreadPool(2,
            new ThreadFactoryBuilder().setNameFormat("Birthday Backoff Message Thread").build()
    );

    private static final ScheduledExecutorService backOffRolePool = Executors.newScheduledThreadPool(4,
            new ThreadFactoryBuilder().setNameFormat("Birthday Backoff Role Thread").build()
    );

    public static void handle(int shardId) {
        final var bot = MantaroBot.getInstance();
        final var instant = Instant.now();
        try {
            final var cache = bot.getBirthdayCacher();
            // There's no cache to be seen here
            if (cache == null) {
                return;
            }

            // We haven't finished caching all members, somehow?
            if (!cache.isDone) {
                return;
            }

            final var start = System.currentTimeMillis();
            var membersAssigned = 0;
            var membersDivested = 0;

            final var jda = bot.getShardManager().getShardById(shardId);
            if (jda == null) { // To be fair, this shouldn't be possible as it only starts it with the shards it knows...
                return;
            }

            log.info("Checking birthdays in shard {} to assign roles...", jda.getShardInfo().getShardId());

            // Well, fuck, this was a day off. NYC time was 23:00 when Chicago time was at 00:00, so it checked the
            // birthdays for THE WRONG DAY. Heck.
            // 17-02-2022: Fuck again, I was using the wrong thing. Now it works, lol.
            final var timezone = ZonedDateTime.ofInstant(instant, ZoneId.of("America/Chicago"));
            // Example: 25-02
            final var now = timezone.format(dayMonthFormat);
            // Example: 02
            final var month = timezone.format(monthFormat);
            // Example: 01
            final var lastMonthTz = ZonedDateTime.ofInstant(instant, ZoneId.of("America/Chicago"))
                    .minusMonths(1);
            final var lastMonth = lastMonthTz.format(dateFormat);

            final var cached = cache.getCachedBirthdays();
            final var guilds = jda.getGuildCache();

            // Backoff sending: we need to backoff the birthday requests,
            // else we're gonna find ourselves quite often hitting ratelimits, which might slow the whole
            // bot down. Therefore, we're just gonna get all of the messages we need to send and *slowly*
            // send them over the course of a few minutes, instead of trying to send them all at once.
            Map<BirthdayGuildInfo, List<MessageCreateBuilder>> toSend = new HashMap<>();
            List<BirthdayRoleInfo> roleBackoffAdd = new ArrayList<>();
            List<BirthdayRoleInfo> roleBackoffRemove = new ArrayList<>();

            // For all current -cached- guilds.
            for (final var guild : guilds) {
                // This is quite a db spam, lol
                final var dbGuild = MantaroData.db().getGuild(guild);
                final var guildData = dbGuild.getData();
                final var guildLanguageContext = new I18nContext(guildData, null);

                // If we have a birthday guild and channel here, continue
                if (guildData.getBirthdayChannel() != null && guildData.getBirthdayRole() != null) {
                    final var birthdayRole = guild.getRoleById(guildData.getBirthdayRole());
                    final var channel = guild.getChannelById(StandardGuildMessageChannel.class, guildData.getBirthdayChannel());

                    if (channel != null && birthdayRole != null) {
                        if (!guild.getSelfMember().canInteract(birthdayRole))
                            continue; //Go to next guild...
                        if (!channel.canTalk())
                            continue; //cannot talk here...
                        if (guildData.getGuildAutoRole() != null && birthdayRole.getId().equals(guildData.getGuildAutoRole()))
                            continue; //Birthday role is autorole role
                        if (birthdayRole.isPublicRole())
                            continue; //Birthday role is public role
                        if (birthdayRole.isManaged())
                            continue; //This was meant to be a bot role?

                        // Guild map is now created from allowed birthdays. This is a little hacky, but we don't really care.
                        // The other solution would have been just disabling this completely, which would have been worse.
                        // @formatter:off
                        Map<Long, BirthdayCacher.BirthdayData> guildMap = cached.entrySet()
                                .stream()
                                .filter(map -> guildData.getAllowedBirthdays().contains(String.valueOf(map.getKey())))
                                .filter(map ->
                                        // Only check for current month or last month!
                                        map.getValue().getBirthday().substring(3, 5).equals(month) ||
                                        map.getValue().getBirthday().substring(3, 5).equals(lastMonth)
                                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                        // @formatter:on

                        int birthdayNumber = 0;
                        var birthdayAnnouncerText = new StringBuilder();
                        birthdayAnnouncerText.append(guildLanguageContext.get("general.birthday"))
                                .append("\n\n");
                        List<MessageCreateData> messageList = new ArrayList<>();
                        List<Long> nullMembers = new ArrayList<>();

                        for (var data : guildMap.entrySet()) {
                            var birthday = data.getValue().getBirthday();
                            if (guildData.getBirthdayBlockedIds().contains(String.valueOf(data.getKey()))) {
                                continue;
                            }

                            if (birthday == null) {
                                log.debug("Birthday is null? Continuing...");
                                nullMembers.add(data.getKey());
                                continue;
                            }

                            // This needs to be a retrieveMemberById call, sadly. This will get cached, though.
                            Member member;
                            try {
                                // This is expensive!
                                member = guild.retrieveMemberById(data.getKey()).useCache(true).complete();
                            } catch (Exception ex) {
                                nullMembers.add(data.getKey());
                                continue;
                            }

                            // Make sure we announce on March 1st for birthdays on February 29 if the current
                            // year is not a leap year.
                            var compare = birthday.substring(0, 5);
                            if (compare.equals("29-02") && !Year.isLeap(LocalDate.now().getYear())) {
                                compare = "28-02";
                            }

                            if (compare.equals(now)) {
                                log.debug("Assigning birthday role on guild {} (M: {})", guild.getId(), member.getEffectiveName());
                                var tempBirthdayMessage =
                                        String.format(EmoteReference.POPPER + "**%s is a year older now! Wish them a happy birthday.** :tada:",
                                        member.getEffectiveName());

                                if (guildData.getBirthdayMessage() != null) {
                                    tempBirthdayMessage = guildData.getBirthdayMessage()
                                            .replace("$(user)", member.getEffectiveName())
                                            .replace("$(usermention)", member.getAsMention())
                                            .replace("$(tag)", member.getUser().getAsTag());
                                }

                                // Variable used in lambda expression should be final or effectively final...
                                final var birthdayMessage = tempBirthdayMessage;
                                if (!member.getRoles().contains(birthdayRole)) {
                                    log.debug("Backing off adding birthday role on guild {} (M: {})", guild.getId(), member.getEffectiveName());

                                    // We can pretty much do all of this only based on the IDs
                                    roleBackoffAdd.add(new BirthdayRoleInfo(guild.getId(), member.getId(), birthdayRole));
                                    messageList.add(buildBirthdayMessage(birthdayMessage, channel, member));
                                    membersAssigned++;
                                    birthdayNumber++;

                                    Metrics.BIRTHDAY_COUNTER.inc();
                                }
                            } else {
                                //day passed
                                if (member.getRoles().contains(birthdayRole)) {
                                    log.debug("Backing off removing birthday role on guild {} (M: {})", guild.getId(), member.getEffectiveName());
                                    roleBackoffRemove.add(new BirthdayRoleInfo(guild.getId(), member.getId(), birthdayRole));
                                    membersDivested++;
                                }
                            }
                        }

                        if (birthdayNumber != 0) {
                            var holder = new BirthdayMessageHolder(messageList);
                            birthdayAnnouncerText.append(holder.getMessage());
                            var splitMessage = SplitUtil.split(birthdayAnnouncerText.toString(), 2000, true, SplitUtil.Strategy.NEWLINE);
                            var counter = new AtomicInteger();
                            var splitEmbeds = holder.getEmbeds().stream()
                                    .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / 10))
                                    .values().stream().toList();

                            if (splitMessage.size() == 1 && splitEmbeds.size() == 1) {
                                toSend.put(
                                        new BirthdayGuildInfo(guild.getId(), channel.getId()),
                                        List.of(new MessageCreateBuilder().setContent(splitMessage.get(0)).setEmbeds(splitEmbeds.get(0)))
                                );
                            } else {
                                List<MessageCreateBuilder> birthdayMessageList = new ArrayList<>();
                                // There's probably, definitely a better way to handle this.
                                if (splitMessage.size() > 1) {
                                    splitMessage.forEach(s -> new MessageCreateBuilder().setContent(s));
                                }

                                if (splitEmbeds.size() > 1) {
                                    var counterEmbed = new AtomicInteger();
                                    for(var e : splitEmbeds) {
                                        var current = counterEmbed.getAndIncrement();
                                        if (current < birthdayMessageList.size() - 1) {
                                            birthdayMessageList.get(current).addEmbeds(e);
                                        } else {
                                            birthdayMessageList.add(new MessageCreateBuilder().setEmbeds(e));
                                        }
                                    }
                                }

                                toSend.put(new BirthdayGuildInfo(guild.getId(), channel.getId()), birthdayMessageList);
                            }
                        }

                        // If any of the member lookups to discord returned null, remove them.
                        if (!nullMembers.isEmpty()) {
                            guildData.getAllowedBirthdays().removeAll(nullMembers.stream().map(String::valueOf).toList());
                            dbGuild.save();
                        }
                    }
                }
            }

            final var end = System.currentTimeMillis();
            log.info("{} (birthdays): people assigned: {}, people divested: {}, took {}ms",
                    jda.getShardInfo(), membersAssigned, membersDivested, (end - start)
            );

            // A pool inside a pool?
            // Send the backoff sending comment above, this basically avoids hitting
            // discord with one billion requests at once.
            final var backoff = 400;
            final var roleBackoff = 100;
            backOffPool.submit(() -> {
                log.info("{} (birthdays): Backoff messages: {}. Sending them with {}ms backoff.",
                        jda.getShardInfo(), toSend.size(), backoff
                );

                final var startMessage = System.currentTimeMillis();
                for (var entry : toSend.entrySet()) {
                    try {
                        final var info = entry.getKey();
                        final var guildId = info.guildId;
                        final var channelId = info.channelId;
                        final var messages = entry.getValue();

                        final var guild = bot.getShardManager().getGuildById(guildId);
                        if (guild == null)
                            continue;

                        final var channel = guild.getTextChannelById(channelId);
                        if (channel == null)
                            continue;

                        messages.forEach(message -> channel.sendMessage(message.build())
                                .setAllowedMentions(EnumSet.of(
                                        Message.MentionType.USER, Message.MentionType.CHANNEL,
                                        Message.MentionType.ROLE, Message.MentionType.EMOJI)
                                )
                                .queue()
                        );
                        // If 100 guilds (about 1/10th of all the shard guilds! so very unlikely) do
                        // get a birthday now, the maximum delay will be 40,000ms, which is 40 seconds.
                        // Not much of an issue for the end user, but avoid sending too many requests
                        // to discord at once. If half of all the guilds in the shard do, the delay
                        // will be about 200,000ms, so 2 minutes.
                        Thread.sleep(backoff);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                final var endMessage = System.currentTimeMillis();
                toSend.clear();
                log.info("Sent all birthday backoff messages, backoff was {}ms, took {}ms", backoff, endMessage - startMessage);
            });

            backOffRolePool.submit(() -> {
                log.info("{} (birthdays): Backoff roles (add): {}. Sending them with {}ms backoff.",
                        jda.getShardInfo(), roleBackoffAdd.size(), roleBackoff
                );

                final var startRole = System.currentTimeMillis();
                for (var roleInfo : roleBackoffAdd) {
                    try {
                        var guild = bot.getShardManager().getGuildById(roleInfo.guildId);
                        if (guild == null)
                            continue;

                        guild.addRoleToMember(UserSnowflake.fromId(roleInfo.memberId), roleInfo.role)
                                .reason(modLogMessage)
                                .queue();

                        Thread.sleep(roleBackoff);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                log.info("{} (birthdays): Backoff roles (remove): {}. Sending them with {}ms backoff.",
                        jda.getShardInfo(), roleBackoffRemove.size(), roleBackoff
                );

                for (var roleInfo : roleBackoffRemove) {
                    try {
                        var guild = bot.getShardManager().getGuildById(roleInfo.guildId);
                        if (guild == null)
                            continue;

                        guild.removeRoleFromMember(UserSnowflake.fromId(roleInfo.memberId), roleInfo.role)
                                .reason(modLogMessage)
                                .queue();

                        Thread.sleep(roleBackoff);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                final var endRole = System.currentTimeMillis();
                roleBackoffAdd.clear();
                roleBackoffRemove.clear();

                log.info("{} (birthdays): All roles done (add and removal), backoff was {}ms. Took {}ms",
                        jda.getShardInfo(), roleBackoff, endRole - startRole
                );
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static MessageCreateData buildBirthdayMessage(String message, StandardGuildMessageChannel channel, Member user) {
        MessageCreateBuilder builder = new MessageCreateBuilder();
        if (message.contains("$(")) {
            message = new DynamicModifiers()
                    .mapFromBirthday("event", channel, user, channel.getGuild())
                    .resolve(message);
        }

        // copy-pasted from welcome messages
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
                        builder.addContent(EmoteReference.ERROR2.toHeaderString())
                                .addContent("The string\n```json\n{")
                                .addContent(json)
                                .addContent("}```\n")
                                .addContent("Is not a valid birthday message (failed to Convert to EmbedJSON). Check the wiki for more information.\n");

                        // So I know what is going on, regardless.
                        e.printStackTrace();
                        return builder.build();
                    }

                    var msgBuilder = new MessageCreateBuilder().setEmbeds(embed.gen(null));
                    if (!extra.isEmpty()) {
                        msgBuilder.addContent(extra).addContent("\n");
                    }

                    return msgBuilder.build();
                }
            } catch (Exception e) {
                if (e.getMessage().toLowerCase().contains("url must be a valid")) {
                    builder.addContent("Failed to send birthday message: Wrong image URL in thumbnail, image, footer and/or author.\n");
                } else {
                    builder.addContent("Failed to send birthday message: Unknown error, try checking your message.\n");
                }

                return builder.build();
            }
        }

        // No match.
        builder.addContent(message).addContent("\n");
        return builder.build();
    }

    private static class BirthdayMessageHolder {
        public List<MessageCreateData> message;

        BirthdayMessageHolder(List<MessageCreateData> message) {
            this.message = message;
        }

        public String getMessage() {
            StringBuilder builder = new StringBuilder();
            for (var msg : message) {
                builder.append(msg.getContent());
            }

            return builder.toString();
        }

        public List<MessageEmbed> getEmbeds() {
            List<MessageEmbed> embeds = new ArrayList<>();
            for (var msg : message) {
                embeds.addAll(msg.getEmbeds());
            }

            return embeds;
        }
    }

    private static class BirthdayGuildInfo {
        public String guildId;
        public String channelId;

        BirthdayGuildInfo(String guildId, String channelId) {
            this.guildId = guildId;
            this.channelId = channelId;
        }
    }

    private static class BirthdayRoleInfo {
        public String guildId;
        public String memberId;
        public Role role;

        BirthdayRoleInfo(String guildId, String memberId, Role role) {
            this.guildId = guildId;
            this.memberId = memberId;
            this.role = role;
        }
    }
}
