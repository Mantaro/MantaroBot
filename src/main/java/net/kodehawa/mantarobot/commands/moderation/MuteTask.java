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

package net.kodehawa.mantarobot.commands.moderation;

import net.dv8tion.jda.core.entities.Guild;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.MantaroCore;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.MantaroObj;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public class MuteTask implements Runnable {

    @Override
    public void run() {
        try {
            if(!MantaroCore.hasLoadedCompletely()) return;
            MantaroObj data = MantaroData.db().getMantaroData();
            Map<Long, Pair<String, Long>> mutes = data.getMutes();
            for (Map.Entry<Long, Pair<String, Long>> entry : mutes.entrySet())
            {
                Long id = entry.getKey();
                Pair<String, Long> pair = entry.getValue();
                String guildId = pair.getKey();
                long maxTime = pair.getValue();
                Guild guild = MantaroBot.getInstance().getGuildById(guildId);
                DBGuild dbGuild = MantaroData.db().getGuild(guildId);
                GuildData guildData = dbGuild.getData();

                if (guild == null) {
                    data.getMutes().remove(id);
                    data.save();
                    return;
                } else if (guild.getMemberById(id) == null) {
                    data.getMutes().remove(id);
                    data.save();
                    return;
                } if(guild.getRoleById(id) == null){
                    data.getMutes().remove(id);
                    data.save();
                    return;
                } else {
                    if (System.currentTimeMillis() > maxTime) {
                        data.getMutes().remove(id);
                        data.save();
                        guild.getController().removeRolesFromMember(guild.getMemberById(id), guild.getRoleById(guildData.getMutedRole())).queue();
                        guildData.setCases(guildData.getCases() + 1);
                        dbGuild.save();
                        ModLog.log(guild.getSelfMember(), MantaroBot.getInstance().getUserById(id), "Mute timeout expired", ModLog.ModAction.UNMUTE, guildData.getCases());
                    }
                }
            }
        } catch (Exception e1) {}
    }
}
