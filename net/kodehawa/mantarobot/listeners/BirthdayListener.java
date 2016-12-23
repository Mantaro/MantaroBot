package net.kodehawa.mantarobot.listeners;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.kodehawa.mantarobot.cmd.Birthday;
import net.kodehawa.mantarobot.cmd.guild.Parameters;
import net.kodehawa.mantarobot.log.LogType;
import net.kodehawa.mantarobot.log.Logger;
import net.kodehawa.mantarobot.thread.AsyncHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class BirthdayListener extends ListenerAdapter {
    private Guild guild;
    private MessageChannel channel;
    private Member membertoAssign;

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
                            Role birthdayRole = guild.getRoleById(Parameters.getBirthdayRoleForServer(guildId));
                            if (data[1].equals(event.getAuthor().getId())) {
                                membertoAssign = event.getGuild().getMember(event.getAuthor());
                                if (!membertoAssign.getRoles().contains(birthdayRole)) {
                                    guild.getController().addRolesToMember(membertoAssign, birthdayRole).queue(
                                            success -> {
                                                TextChannel tc = guild.getTextChannelById(Parameters.getBirthdayChannelForServer(guildId));
                                                tc.sendMessage(":tada: **" + membertoAssign.getEffectiveName() +
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
                        } else {
                            String guildId1 = data[0];
                            Member membertoRemove = event.getGuild().getMember(event.getAuthor());
                            Role birthdayRole1 = guild.getRoleById(Parameters.getBirthdayRoleForServer(guildId1));
                            if(membertoRemove.getRoles().contains(birthdayRole1)){
                                guild.getController().removeRolesFromMember(membertoRemove, birthdayRole1).queue();
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
