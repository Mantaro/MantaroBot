package net.kodehawa.mantarobot.cmd;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.kodehawa.mantarobot.cmd.servertools.Parameters;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.log.LogType;
import net.kodehawa.mantarobot.log.Logger;
import net.kodehawa.mantarobot.management.Command;
import net.kodehawa.mantarobot.util.HashMapUtils;

public class Birthday extends Command {

	private HashMap<String, String> bd = new HashMap<>();
	private String FILE_SIGN = "d41d8cd98f00b204e9800998ecf8427e";

	public Birthday()
	{
		setName("birthday");
		setDescription("Sets your birthday date.");
		setCommandType("user");
		new HashMapUtils("mantaro", "bd", bd, FILE_SIGN, false);
		Mantaro.instance().getSelf().addEventListener(new BirthdayListener());
	}
	
	@Override
	public void onCommand(String[] split, String content, MessageReceivedEvent event) {
		guild = event.getGuild();
		channel = event.getChannel();
		String userId = event.getMessage().getAuthor().getId();
		SimpleDateFormat format1 = new SimpleDateFormat("dd-MM-yyyy");
		Date bd1 = null;
		//So they don't input something that isn't a date...
		try{
			bd1 = format1.parse(split[0]);
		} catch(Exception e){
			channel.sendMessage(":heavy_multiplication_x: a valid date.").queue();
			e.printStackTrace();
		}
		
		if(bd1 != null){
			if(!bd.containsKey(userId)){
				String finalBirthday = format1.format(bd1);
				
				bd.put(event.getGuild().getId()+ ":" +userId, finalBirthday);
				new HashMapUtils("mantaro", "bd", bd, FILE_SIGN, true);
				channel.sendMessage(":mega: Added birthday date.").queue();
			}
			else{
				String finalBirthday = format1.format(bd1);
				
				bd.remove(userId);
				bd.put(event.getGuild().getId()+ ":" +userId, finalBirthday);
				new HashMapUtils("mantaro", "bd", bd, FILE_SIGN, true);
				channel.sendMessage(":mega: Changed birthday date.").queue();
			}
		}
	}

	private class BirthdayListener extends ListenerAdapter{
		BirthdayListener(){
			Logger.instance().print("Birthday Logger started.", this.getClass(), LogType.INFO);
		}

		Member userToAssign;
		public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
			guild = event.getGuild();
			channel = event.getChannel();
			if(Parameters.getBirthdayHash().containsKey(guild.getId())) {
				String userKey = event.getGuild().getId() + ":" + event.getAuthor().getId();
				String[] data = userKey.split(":");
				if(bd.containsKey(userKey)){
					if(!bd.get(userKey).isEmpty()){
						try{
							Calendar cal = Calendar.getInstance();
							SimpleDateFormat format1 = new SimpleDateFormat("dd-MM-yyyy");
							if(bd.get(userKey).substring(0, 5).equals(format1.format(cal.getTime()).substring(0, 5))) {
								String guildId = data[0];
								List<Member> user = event.getGuild().getMembers();
								Role birthdayRole =
										guild.getRolesByName(Parameters.getBirthdayRoleForServer(guildId), true).get(0);
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
								Role birthdayRole = guild.getRolesByName(Parameters.getBirthdayRoleForServer(guildId), true).get(0);
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
}