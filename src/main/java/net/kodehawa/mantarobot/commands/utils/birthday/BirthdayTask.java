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

package net.kodehawa.mantarobot.commands.utils.birthday;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.exporters.Metrics;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

public class BirthdayTask {
    private static final Logger log = LoggerFactory.getLogger(BirthdayTask.class);
    private static final FastDateFormat dateFormat = FastDateFormat.getInstance("dd-MM-yyyy");
    private static final String modLogMessage = "Birthday assigner." +
            " If you see this happening for every member of your server, or in unintended ways, please do ~>opts birthday disable";

    private static final ScheduledExecutorService backOffPool = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("Birthday Backoff Message Thread").build()
    );

    private static final ScheduledExecutorService backOffRolePool = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("Birthday Backoff Role Thread").build()
    );


    public static void handle(int shardId) {
        final var bot = MantaroBot.getInstance();

        try {
            BirthdayCacher cache = bot.getBirthdayCacher();
            // There's no cache to be seen here
            if (cache == null) {
                return;
            }

            // We haven't finished caching all members, somehow?
            if (!cache.isDone) {
                return;
            }

            var membersAssigned = 0;
            var membersDivested = 0;

            var jda = bot.getShardManager().getShardById(shardId);
            if (jda == null) { // To be fair, this shouldn't be possible as it only starts it with the shards it knows...
                return;
            }

            log.info("Checking birthdays in shard {} to assign roles...", jda.getShardInfo().getShardId());

            var start = System.currentTimeMillis();

            var cal = Calendar.getInstance();
            var now = dateFormat.format(cal.getTime()).substring(0, 5);
            var cached = cache.getCachedBirthdays();
            var guilds = jda.getGuildCache();

            // Backoff sending: we need to backoff the birthday requests,
            // else we're gonna find ourselves quite often hitting ratelimits, which might slow the whole
            // bot down. Therefore, we're just gonna get all of the messages we need to send and *slowly*
            // send them over the course of a few minutes, instead of trying to send them all at once.
            Map<BirthdayGuildInfo, Queue<Message>> toSend = new HashMap<>();
            List<BirthdayRoleInfo> roleBackoffAdd = new ArrayList<>();
            List<BirthdayRoleInfo> roleBackoffRemove = new ArrayList<>();

            // For all current -cached- guilds.
            for (var guild : guilds) {
                // This is quite a db spam, lol
                var dbGuild = MantaroData.db().getGuild(guild);
                var guildData = dbGuild.getData();

                // If we have a birthday guild and channel here, continue
                if (guildData.getBirthdayChannel() != null && guildData.getBirthdayRole() != null) {
                    var birthdayRole = guild.getRoleById(guildData.getBirthdayRole());
                    var channel = guild.getTextChannelById(guildData.getBirthdayChannel());

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
                        Map<String, BirthdayCacher.BirthdayData> guildMap =
                                cached.entrySet().stream().filter(map -> guildData.getAllowedBirthdays().contains(map.getKey()))
                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                        var birthdayAnnouncerText = new MessageBuilder();
                        birthdayAnnouncerText.append("**New birthdays for today, wish them Happy Birthday!**").append("\n\n");
                        int birthdayNumber = 0;

                        List<String> nullMembers = new ArrayList<>();

                        for (var data : guildMap.entrySet()) {
                            // This needs to be a retrieveMemberById call, sadly. This will get cached, though.
                            Member member = null;
                            try {
                                member = guild.retrieveMemberById(data.getKey(), false).complete();
                            } catch (Exception ignored) { }

                            var birthday = data.getValue().birthday;

                            // shut up warnings
                            if (member == null) {
                                nullMembers.add(data.getKey());
                                continue;
                            }

                            if (guildData.getBirthdayBlockedIds().contains(member.getId())) {
                                continue;
                            }

                            if (birthday == null) {
                                log.debug("Birthday is null? Removing role if present and continuing to next iteration...");
                                if (member.getRoles().contains(birthdayRole)) {
                                    guild.removeRoleFromMember(member, birthdayRole)
                                            .reason(modLogMessage)
                                            .queue();
                                }
                                continue; //shouldn't happen
                            }
                            //else start the assigning

                            //:tada:!
                            if (birthday.substring(0, 5).equals(now)) {
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

                                //Variable used in lambda expression should be final or effectively final...
                                final var birthdayMessage = tempBirthdayMessage;

                                if (!member.getRoles().contains(birthdayRole)) {
                                    try {
                                        log.debug("Backing off adding birthday role on guild {} (M: {})", guild.getId(), member.getEffectiveName());
                                        // We can pretty much do all of this only based on the IDs
                                        roleBackoffAdd.add(new BirthdayRoleInfo(guild.getId(), member.getId(), birthdayRole));
                                        Metrics.BIRTHDAY_COUNTER.inc();
                                        birthdayAnnouncerText.append(birthdayMessage).append("\n");
                                        membersAssigned++;
                                        birthdayNumber++;
                                    } catch (Exception e) { //Something went boom, ignore and continue
                                        log.debug("Something went boom while assigning a birthday role?...", e);
                                    }
                                }
                            } else {
                                //day passed
                                if (member.getRoles().contains(birthdayRole)) {
                                    try {
                                        log.debug("Backing off removing birthday role on guild {} (M: {})", guild.getId(), member.getEffectiveName());
                                        roleBackoffRemove.add(new BirthdayRoleInfo(guild.getId(), member.getId(), birthdayRole));
                                        membersDivested++;
                                    } catch (Exception e) { //Something went boom, ignore and continue
                                        log.debug("Something went boom while removing a birthday role?...", e);
                                    }
                                }
                            }
                        }

                        if (birthdayNumber != 0) {
                            toSend.put(
                                    new BirthdayGuildInfo(guild.getId(), channel.getId()),
                                    birthdayAnnouncerText.buildAll(MessageBuilder.SplitPolicy.NEWLINE)
                            );
                        } else {
                            if (!guildData.isNotifiedFromBirthdayChange()) {
                                birthdayAnnouncerText.append("\n")
                                        .append("**No birthdays? We've just changed how the birthday system works!**\n")
                                        .append("Give the changes a read on:" +
                                                " https://github.com/Mantaro/MantaroBot/wiki/Changes-to-the-birthday-announcement-system ")
                                        .append("and if you don't understand," +
                                                " join the support server at <https://support.mantaro.site> and ask in #support.\n")
                                        .append("Thanks for using Mantaro! " +
                                                "If you don't remember setting up birthday announcements, you can disable them.\n")
                                        .append("This warning message will only appear once.");

                                guildData.setNotifiedFromBirthdayChange(true);
                                dbGuild.save();

                                toSend.put(
                                        new BirthdayGuildInfo(guild.getId(), channel.getId()),
                                        birthdayAnnouncerText.buildAll(MessageBuilder.SplitPolicy.NEWLINE)
                                );
                            } //If it was notified, no need.
                        }

                        // If any of the member lookups to discord returned null, remove them.
                        if (!nullMembers.isEmpty()) {
                            guildData.getAllowedBirthdays().removeAll(nullMembers);
                            dbGuild.save();
                        }
                    }
                }
            }

            var end = System.currentTimeMillis();
            log.info("Finished checking birthdays for shard {}, people assigned: {}, people divested: {}, took {}ms",
                    jda.getShardInfo().getShardId(), membersAssigned, membersDivested, (end - start)
            );

            // A poll inside a pool?
            // Send the backoff sending comment above, this basically avoids hitting
            // discord with one billion requests at once.
            var backoff = 400;
            var roleBackoff = 100;
            backOffPool.submit(() -> {
                log.info("Backoff messages: {}. Sending them with {}ms backoff.", toSend.size(), backoff);
                var startMessage = System.currentTimeMillis();

                for (var entry : toSend.entrySet()) {
                    try {
                        var info = entry.getKey();
                        var guildId = info.guildId;
                        var channelId = info.channelId;
                        var messages = entry.getValue();

                        var guild = bot.getShardManager().getGuildById(guildId);
                        if (guild == null)
                            continue;

                        var channel = guild.getTextChannelById(channelId);
                        if (channel == null)
                            continue;

                        entry.getValue().forEach(message -> channel.sendMessage(message).queue());
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

                var endMessage = System.currentTimeMillis();
                toSend.clear();

                log.info("Sent all birthday backoff messages, backoff was {}ms, took {}ms", backoff, endMessage - startMessage);
            });

            backOffRolePool.submit(() -> {
                log.info("Backoff roles (add): {}. Sending them with {}ms backoff.", roleBackoffAdd.size(), roleBackoff);
                var startRole = System.currentTimeMillis();
                for (var roleInfo : roleBackoffAdd) {
                    try {
                        var guild = bot.getShardManager().getGuildById(roleInfo.guildId);
                        if (guild == null)
                            continue;

                        guild.addRoleToMember(roleInfo.memberId, roleInfo.role)
                                .reason(modLogMessage)
                                .queue();

                        Thread.sleep(roleBackoff);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                log.info("Backoff roles (remove): {}. Sending them with {}ms backoff.", roleBackoffRemove.size(), roleBackoff);
                for (var roleInfo : roleBackoffRemove) {
                    try {
                        var guild = bot.getShardManager().getGuildById(roleInfo.guildId);
                        if (guild == null)
                            continue;

                        guild.removeRoleFromMember(roleInfo.memberId, roleInfo.role)
                                .reason(modLogMessage)
                                .queue();

                        Thread.sleep(roleBackoff);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                var endRole = System.currentTimeMillis();
                roleBackoffAdd.clear();
                roleBackoffRemove.clear();

                log.info("All roles done (add and removal), backoff was {}ms. Took {}ms", roleBackoff, endRole - startRole);
            });
        } catch (Exception e) {
            e.printStackTrace();
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
