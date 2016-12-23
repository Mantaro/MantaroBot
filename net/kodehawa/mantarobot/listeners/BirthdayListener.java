package net.kodehawa.mantarobot.listeners;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.kodehawa.mantarobot.cmd.Birthday;
import net.kodehawa.mantarobot.cmd.guild.Parameters;
import net.kodehawa.mantarobot.log.LogType;
import net.kodehawa.mantarobot.log.Logger;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class BirthdayListener extends ListenerAdapter {
    private Guild guild;
    private MessageChannel channel;
    private Member userToAssign;

    public BirthdayListener(){
        Logger.instance().print("Birthday Logger started.", this.getClass(), LogType.INFO);
    }

    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        guild = event.getGuild();
        channel = event.getChannel();
        if(Parameters.getBirthdayHash().containsKey(guild.getId())) {
            String userKey = event.getGuild().getId() + ":" + event.getAuthor().getId();
            String[] data = userKey.split(":");
            if(Birthday.bd.containsKey(userKey)){
                if(!Birthday.bd.get(userKey).isEmpty()){
                    try{
                        Calendar cal = Calendar.getInstance();
                        SimpleDateFormat format1 = new SimpleDateFormat("dd-MM-yyyy");
                        if(Birthday.bd.get(userKey).substring(0, 5).equals(format1.format(cal.getTime()).substring(0, 5))) {
                            String guildId = data[0];
                            List<Member> user = event.getGuild().getMembers();
                            Role birthdayRole =
                                    guild.getRoleById(Parameters.getBirthdayRoleForServer(guildId));
                            for (Member finalMember : user) {
                                if (finalMember.getUser().getId().equals(event.getAuthor().getId())) {
                                    userToAssign = finalMember;
                                    if (!guild.getMembersWithRoles(birthdayRole).contains(finalMember)) {
                                        guild.getController().addRolesToMember(finalMember, birthdayRole).queue(
                                                success -> {
                                                    TextChannel tc = guild.getTextChannelById(Parameters.getBirthdayChannelForServer(guildId));
                                                    tc.sendMessage(":tada: **" + userToAssign.getEffectiveName() +
                                                            " is a year older now! Wish them a happy birthday.** :tada:").queue();
                                                },
                                                error ->{
                                                    if(error instanceof PermissionException){
                                                        PermissionException pe = (PermissionException) error;
                                                        TextChannel tc = guild.getTextChannelById(Parameters.getBirthdayChannelForServer(guildId));
                                                        tc.sendMessage(":big_multiplication_x: PermissionError while appling roles, (No permission provided: " + pe.getPermission() + ")").queue();
                                                    } else {
                                                        channel.sendMessage(":heavy_multiplication_x:" + "Unknown error while applying roles [" + birthdayRole.getName()
                                                                + "]: " + "<" + error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();
                                                        error.printStackTrace();														}
                                                });
                                    }
                                }
                            }
                        } else {
                            String guildId = data[0];
                            Role birthdayRole = guild.getRoleById(Parameters.getBirthdayRoleForServer(guildId));
                            List<Member> user = event.getGuild().getMembers();
                            for (Member finalMember : user) {
                                if(guild.getMembersWithRoles(birthdayRole).contains(finalMember)){
                                    guild.getController().removeRolesFromMember(finalMember, birthdayRole).queue();
                                }
                            }
                        }
                    } catch(Exception e){
                        Logger.instance().print("Cannot process birthday for: " + userKey + " program will be still running.", this.getClass(), LogType.WARNING);
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
