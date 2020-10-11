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

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.exporters.Metrics;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BirthdayTask {
    private static final Logger log = LoggerFactory.getLogger(BirthdayTask.class);
    private static final FastDateFormat dateFormat = FastDateFormat.getInstance("dd-MM-yyyy");
    private static final String modLogMessage = "Birthday assigner." +
            " If you see this happening for every member of your server, or in unintended ways, please do ~>opts birthday disable";

    public static void handle(int shardId) {
        try {
            BirthdayCacher cache = MantaroBot.getInstance().getBirthdayCacher();
            // There's no cache to be seen here
            if (cache == null)
                return;
            // We haven't finished caching all members, somehow?
            if (!cache.isDone)
                return;

            int membersAssigned = 0;
            int membersDivested = 0;

            JDA jda = MantaroBot.getInstance().getShardManager().getShardById(shardId);
            if(jda == null) // To be fair, this shouldn't be possible as it only starts it with the shards it knows...
                return;

            log.info("Checking birthdays in shard {} to assign roles...", jda.getShardInfo().getShardId());

            long start = System.currentTimeMillis();

            Calendar cal = Calendar.getInstance();
            String now = dateFormat.format(cal.getTime()).substring(0, 5);

            Map<String, BirthdayCacher.BirthdayData> cached = cache.cachedBirthdays;
            SnowflakeCacheView<Guild> guilds = jda.getGuildCache();

            // For all current -cached- guilds.
            for (Guild guild : guilds) {
                DBGuild dbGuild = MantaroData.db().getGuild(guild);
                GuildData guildData = dbGuild.getData();
                // If we have a birthday guild and channel here, continue
                if (guildData.getBirthdayChannel() != null && guildData.getBirthdayRole() != null) {
                    Role birthdayRole = guild.getRoleById(guildData.getBirthdayRole());
                    TextChannel channel = guild.getTextChannelById(guildData.getBirthdayChannel());

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

                        MessageBuilder birthdayAnnouncerText = new MessageBuilder();
                        birthdayAnnouncerText.append("**New birthdays for today, wish them Happy Birthday!**").append("\n\n");
                        int birthdayNumber = 0;

                        List<String> nullMembers = new ArrayList<>();

                        for (Map.Entry<String, BirthdayCacher.BirthdayData> data : guildMap.entrySet()) {
                            // This needs to be a retrieveMemberById call, sadly. This will get cached, though.
                            Member member = null;
                            try {
                                member = guild.retrieveMemberById(data.getKey(), false).complete();
                            } catch (Exception ignored) { }

                            String birthday = data.getValue().birthday;

                            // shut up warnings
                            if (member == null) {
                                nullMembers.add(data.getKey());
                                continue;
                            }

                            if(guildData.getBirthdayBlockedIds().contains(member.getId()))
                                continue;

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
                                String tempBirthdayMessage = String.format(EmoteReference.POPPER + "**%s is a year older now! Wish them a happy birthday.** :tada:",
                                        member.getEffectiveName());

                                if (guildData.getBirthdayMessage() != null) {
                                    tempBirthdayMessage = guildData.getBirthdayMessage()
                                            .replace("$(user)", member.getEffectiveName())
                                            .replace("$(usermention)", member.getAsMention())
                                            .replace("$(tag)", member.getUser().getAsTag());
                                }

                                //Variable used in lambda expression should be final or effectively final...
                                final String birthdayMessage = tempBirthdayMessage;

                                if (!member.getRoles().contains(birthdayRole)) {
                                    try {
                                        log.debug("Assigned birthday role on guild {} (M: {})", guild.getId(), member.getEffectiveName());
                                        guild.addRoleToMember(member, birthdayRole)
                                                .reason(modLogMessage)
                                                .queue();

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
                                        log.debug("Removing birthday role on guild {} (M: {})", guild.getId(), member.getEffectiveName());
                                        guild.removeRoleFromMember(member, birthdayRole)
                                                .reason(modLogMessage)
                                                .queue();

                                        membersDivested++;
                                    } catch (Exception e) { //Something went boom, ignore and continue
                                        log.debug("Something went boom while removing a birthday role?...", e);
                                    }
                                }
                            }
                        }

                        if(birthdayNumber != 0) {
                            // Don't send one message per birthday, only send a single one or multiple as needed, but not a billion.
                            // This is to avoid spamming calls to Discord.
                            birthdayAnnouncerText.buildAll(MessageBuilder.SplitPolicy.NEWLINE).forEach(message -> channel.sendMessage(message).queue());
                        } else {
                            if(!guildData.isNotifiedFromBirthdayChange()) {
                                birthdayAnnouncerText.append("\n")
                                        .append("**No birthdays? We've just changed how the birthday system works!**\n")
                                        .append("Give the changes a read on: https://github.com/Mantaro/MantaroBot/wiki/Changes-to-the-birthday-announcement-system ")
                                        .append("and if you don't understand, join the support server at <https://support.mantaro.site> and ask in #support.\n")
                                        .append("Thanks for using Mantaro! If you don't remember setting up birthday announcements, you can disable them.\n")
                                        .append("This warning message will only appear once.");

                                guildData.setNotifiedFromBirthdayChange(true);
                                dbGuild.save();

                                birthdayAnnouncerText.buildAll(MessageBuilder.SplitPolicy.NEWLINE).forEach(message -> channel.sendMessage(message).queue());
                            } //If it was notified, no need.
                        }

                        // If any of the member lookups to discord returned null, remove them.
                        if(!nullMembers.isEmpty()) {
                            guildData.getAllowedBirthdays().removeAll(nullMembers);
                            dbGuild.save();
                        }
                    }
                }
            }

            long end = System.currentTimeMillis();

            String toSend = String.format("Finished checking birthdays for shard %s, people assigned: %d, people divested: %d, took %dms",
                    jda.getShardInfo().getShardId(), membersAssigned, membersDivested, (end - start));

            log.info(toSend);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
