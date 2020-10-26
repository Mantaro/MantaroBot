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

package net.kodehawa.mantarobot.commands.moderation;

import net.dv8tion.jda.api.entities.Member;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.MantaroObj;
import net.kodehawa.mantarobot.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MuteTask {
    private static final Logger log = LoggerFactory.getLogger(MuteTask.class);

    public static void handle() {
        try {
            MantaroObj data = MantaroData.db().getMantaroData();
            Map<Long, Pair<String, Long>> mutes = data.getMutes();
            log.debug("Checking mutes... data size {}", mutes.size());
            for (var entry : mutes.entrySet()) {
                try {
                    log.trace("Iteration: Mute check");
                    var id = entry.getKey();
                    var pair = entry.getValue();
                    var guildId = pair.getLeft();
                    var maxTime = pair.getRight();

                    var guild = MantaroBot.getInstance().getShardManager().getGuildById(guildId);
                    if (guild == null) {
                        //Might be in another instance, or the guild left, either way we can't check properly.
                        continue;
                    }

                    var dbGuild = MantaroData.db().getGuild(guildId);
                    var guildData = dbGuild.getData();

                    //I spent an entire month trying to figure out why this didn't work to then come to the conclusion that I'm completely stupid.
                    //I was checking against `id` instead of against the mute role id because I probably was high or something when I did this
                    //It literally took me a fucking month to figure this shit out
                    //What in the name of real fuck.
                    //Please hold me.
                    if (guild.getRoleById(guildData.getMutedRole()) == null) {
                        data.getMutes().remove(id);
                        data.saveAsync();
                        log.debug("Removed {} because role == null", id);
                    } else {
                        if (System.currentTimeMillis() > maxTime) {
                            log.debug("Unmuted {} because time ran out", id);
                            data.getMutes().remove(id);
                            data.save();
                            var roleById = guild.getRoleById(guildData.getMutedRole());

                            Member member = null;
                            try {
                                member = guild.retrieveMemberById(id, false).complete();
                            } catch (Exception ignored) { }

                            if (member == null) {
                                data.getMutes().remove(id);
                                data.saveAsync();
                                log.debug("Removed {} because member == null", id);
                                continue;
                            }

                            if (roleById != null) {
                                guild.removeRoleFromMember(member, roleById).queue();
                            }

                            guildData.setCases(guildData.getCases() + 1);
                            dbGuild.saveAsync();
                            ModLog.log(guild.getSelfMember(),
                                    member.getUser(),
                                    "Mute timeout expired", "none",
                                    ModLog.ModAction.UNMUTE,
                                    guildData.getCases()
                            );
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }
}
