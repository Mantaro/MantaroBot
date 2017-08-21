package net.kodehawa.mantarobot.commands.utils.birthday;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
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

    @Override
    public void run() {
        try{
            if(cache == null){
                cache = MantaroBot.getInstance().getBirthdayCacher();
                if(cache == null) return;
            }

            if(!cache.isDone) return;

            log.info("Checking birthdays to assign roles...");
            Map<String, String> cached = cache.cachedBirthdays;
            List<Guild> guilds = MantaroBot.getInstance().getGuilds();

            for(Guild guild : guilds){
                GuildData tempData = db.getGuild(guild).getData();
                if(tempData.getBirthdayChannel() != null && tempData.getBirthdayRole() != null){
                    Role birthdayRole = guild.getRoleById(tempData.getBirthdayRole());
                    TextChannel channel = guild.getTextChannelById(tempData.getBirthdayChannel());

                    if(channel != null && birthdayRole != null){
                        Map<String, String> guildMap = cached.entrySet().stream().filter(map -> guild.getMemberById(map.getKey()) != null)
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                        for(Map.Entry<String, String> data : guildMap.entrySet()){
                            Member member = guild.getMemberById(data.getKey());
                            UserData userData = db.getUser(member).getData();
                            if(userData.getBirthday() == null) continue;
                            //else start the assigning

                            Calendar cal = Calendar.getInstance();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

                            //tada!
                            if (userData.getBirthday().substring(0, 5).equals(dateFormat.format(cal.getTime()).substring(0, 5))) {
                                log.debug("Assigning birthday role on guild {} (M: {})", guild.getId(), member.getEffectiveName());
                                if (!member.getRoles().contains(birthdayRole)) {
                                    guild.getController().addSingleRoleToMember(member, birthdayRole).queue(s ->{
                                                channel.sendMessage(String.format(EmoteReference.POPPER + "**%s is a year older now! Wish them a happy birthday.** :tada:",
                                                        member.getEffectiveName())).queue();
                                                MantaroBot.getInstance().getStatsClient().increment("birthdays_logged");
                                            }
                                    );
                                }
                            } else {
                                //day passed
                                if (guild.getRoles().contains(birthdayRole)) {
                                    guild.getController().removeRolesFromMember(member, birthdayRole).queue();
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }
}