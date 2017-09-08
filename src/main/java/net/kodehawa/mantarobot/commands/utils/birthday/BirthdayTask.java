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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class BirthdayTask implements Runnable {

    private BirthdayCacher cache = MantaroBot.getInstance().getBirthdayCacher();
    private ManagedDatabase db = MantaroData.db();
    //just in case shit goes massively boom
    @Setter
    public static boolean isEnabled = true;

    @Override
    public void run() {
        try {
            if (!isEnabled) return;

            if (cache == null) {
                cache = MantaroBot.getInstance().getBirthdayCacher();
                if (cache == null) return;
            }

            if (!cache.isDone) return;

            log.info("Checking birthdays to assign roles...");
            long start = System.currentTimeMillis();
            Map<String, String> cached = cache.cachedBirthdays;
            int i = 0;
            int r = 0;
            List<Guild> guilds = MantaroBot.getInstance().getGuilds();

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
                            if(birthday == null) continue; //shouldnt happen
                            //else start the assigning

                            Calendar cal = Calendar.getInstance();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

                            //tada!
                            if(birthday.substring(0, 5).equals(dateFormat.format(cal.getTime()).substring(0, 5))) {
                                log.debug("Assigning birthday role on guild {} (M: {})", guild.getId(), member.getEffectiveName());
                                if(!member.getRoles().contains(birthdayRole)) {
                                    try {
                                        guild.getController().addSingleRoleToMember(member, birthdayRole).queue(s -> {
                                                    channel.sendMessage(String.format(EmoteReference.POPPER + "**%s is a year older now! Wish them a happy birthday.** :tada:",
                                                            member.getEffectiveName())).queue();
                                                    MantaroBot.getInstance().getStatsClient().increment("birthdays_logged");
                                                }
                                        );
                                        i++;
                                    } catch (Exception ignored) {}
                                }
                            } else {
                                //day passed
                                if(member.getRoles().contains(birthdayRole)) {
                                    try {
                                        guild.getController().removeRolesFromMember(member, birthdayRole).queue();
                                        r++;
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                    }
                }
            }

            long end = System.currentTimeMillis();

            String toSend = String.format("Finished checking birthdays, people assigned: %d, people divested: %d, took %dms", i, r, (end - start));

            log.info(toSend);
            LogUtils.log("Birthday", toSend);

        } catch(Exception e) {
            e.printStackTrace();
            Sentry.capture(e);
        }
    }
}
