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

package net.kodehawa.mantarobot.commands.utils.birthday;

import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.utils.cache.SnowflakeCacheView;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.apache.commons.lang3.time.FastDateFormat;

import java.util.Calendar;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class BirthdayTask {
    private ManagedDatabase db = MantaroData.db();
    private static FastDateFormat dateFormat = FastDateFormat.getInstance("dd-MM-yyyy");

    public void handle(int shardId) {
        try {
            BirthdayCacher cache = MantaroBot.getInstance().getBirthdayCacher();
            if(cache == null) return;
            if (!cache.isDone) return;
            int i = 0;
            int r = 0;

            JDA jda = MantaroBot.getInstance().getShard(shardId);

            log.info("Checking birthdays in shard {} to assign roles...", jda.getShardInfo() == null ? 0 : jda.getShardInfo().getShardId());
            long start = System.currentTimeMillis();
            Calendar cal = Calendar.getInstance();
            String now = dateFormat.format(cal.getTime()).substring(0, 5);
            Map<String, String> cached = cache.cachedBirthdays;
            SnowflakeCacheView<Guild> guilds = jda.getGuildCache();

            for(Guild guild : guilds) {
                GuildData tempData = db.getGuild(guild).getData();
                if(tempData.getBirthdayChannel() != null && tempData.getBirthdayRole() != null) {
                    Role birthdayRole = guild.getRoleById(tempData.getBirthdayRole());
                    TextChannel channel = guild.getTextChannelById(tempData.getBirthdayChannel());

                    if(channel != null && birthdayRole != null) {
                        if(!guild.getSelfMember().canInteract(birthdayRole)) continue;

                        Map<String, String> guildMap = cached.entrySet().stream().filter(map -> guild.getMemberById(map.getKey()) != null)
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                        for(Map.Entry<String, String> data : guildMap.entrySet()) {
                            Member member = guild.getMemberById(data.getKey());
                            String birthday = data.getValue();

                            if(birthday == null){
                                log.debug("Birthday is null? Continuing to next iteration...");
                                continue; //shouldnt happen
                            }
                            //else start the assigning

                            //tada!
                            if(birthday.substring(0, 5).equals(now)) {
                                log.debug("Assigning birthday role on guild {} (M: {})", guild.getId(), member.getEffectiveName());
                                if(!member.getRoles().contains(birthdayRole)) {
                                    try {
                                        guild.getController().addSingleRoleToMember(member, birthdayRole).queue(s -> {
                                                    channel.sendMessage(String.format(EmoteReference.POPPER + "**%s is a year older now! Wish them a happy birthday.** :tada:",
                                                            member.getEffectiveName())).queue();
                                                    MantaroBot.getInstance().getStatsClient().increment("birthdays_logged");
                                                }
                                        );
                                        log.debug("Assigned birthday role on guild {} (M: {})", guild.getId(), member.getEffectiveName());
                                        i++;
                                        //Something went boom, ignore and continue
                                    } catch (Exception e) {
                                        log.debug("Something went boom while assigning a birthday role?...");
                                    }
                                }
                            } else {
                                //day passed
                                if(member.getRoles().contains(birthdayRole)) {
                                    try {
                                        log.debug("Removing birthday role on guild {} (M: {})", guild.getId(), member.getEffectiveName());
                                        guild.getController().removeRolesFromMember(member, birthdayRole).queue();
                                        r++;
                                        //Something went boom, ignore and continue
                                    } catch (Exception e) {
                                        log.debug("Something went boom while removing a birthday role?...");
                                    }
                                }
                            }
                        }
                    }
                }
            }

            long end = System.currentTimeMillis();

            String toSend = String.format("Finished checking birthdays for shard %s, people assigned: %d, people divested: %d, took %dms",
                    jda.getShardInfo() == null ? 0 : jda.getShardInfo().getShardId(), i, r, (end - start));

            log.info(toSend);
        } catch(Exception e) {
            e.printStackTrace();
            Sentry.capture(e);
        }
    }
}
