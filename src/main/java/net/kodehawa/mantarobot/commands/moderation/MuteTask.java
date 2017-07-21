package net.kodehawa.mantarobot.commands.moderation;

import net.dv8tion.jda.core.entities.Guild;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.MantaroObj;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public class MuteTask implements Runnable {

    @Override
    public void run() {
        MantaroObj data = MantaroData.db().getMantaroData();
        System.out.println(data.getMutes());
        for (Map.Entry<Long, Pair<String, Long>> entry : data.getMutes().entrySet())
        {
            try {
                Long id = entry.getKey();
                Pair<String, Long> pair = entry.getValue();
                String guildId = pair.getKey();
                long maxTime = pair.getValue();
                Guild guild = MantaroBot.getInstance().getGuildById(guildId);
                DBGuild dbGuild = MantaroData.db().getGuild(guildId);
                GuildData guildData = dbGuild.getData();

                System.out.println(id + " | {" + guildId + " " + maxTime + "}");
                if (guild == null) {
                    data.getMutes().remove(id);
                    data.save();
                } else if (guild.getMemberById(id) == null) {
                    data.getMutes().remove(id);
                    data.save();
                } else {
                    System.out.println(System.currentTimeMillis() > maxTime);
                    if (System.currentTimeMillis() > maxTime) {
                        data.getMutes().remove(id);
                        data.save();
                        guild.getController().removeRolesFromMember(guild.getMemberById(id), guild.getRoleById(guildData.getMutedRole())).queue();
                        guildData.setCases(guildData.getCases() + 1);
                        dbGuild.save();
                        ModLog.log(guild.getSelfMember(), MantaroBot.getInstance().getUserById(id), "Mute timeout expired", ModLog.ModAction.UNMUTE, guildData.getCases());
                    }
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }
}
