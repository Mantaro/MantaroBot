package net.kodehawa.mantarobot.cmd;

import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.core.Mantaro;

public class Help extends Command {

	private final Mantaro mantaro = Mantaro.instance();
	
	public Help(){
		setName("help");
		setCommandType("user");
	}
	
	@Override
	public void onCommand(String[] split, String content, MessageReceivedEvent event) {
		guild = event.getGuild();
		channel = event.getChannel();
		author = event.getAuthor();
		Member member = guild.getMember(author);
		String command = content;
		if(content.isEmpty()){
			StringBuilder builderuser = new StringBuilder();
			StringBuilder builderadmin = new StringBuilder();
			for(String cmd : mantaro.modules.keySet()){
				if(!mantaro.modules.get(cmd).getDescription().isEmpty() && mantaro.modules.get(cmd).getCommandType().equals("user"))
					builderuser.append(cmd + ": " + mantaro.modules.get(cmd).getDescription() + "\n");
				else if(!mantaro.modules.get(cmd).getDescription().isEmpty() && mantaro.modules.get(cmd).getCommandType().equals("servertool"))
					builderadmin.append(cmd + ": " + mantaro.modules.get(cmd).getDescription() + "\n");
			}
			
			channel.sendMessage(":mega: Delivered! Check your inbox.").queue(
					success ->
					{
						author.getPrivateChannel().sendMessage(
								":exclamation: Command help. For extended help use this command with a command name as argument.\n"
								+ ":exclamation: Remember: *all* commands as for now use the ~> prefix. So put that before the command name to execute it.\n\n"
								+ "**User commands:**\n"
								+ builderuser.toString() +"\n"
								+ ":star: Mantaro version: " + Mantaro.instance().getMetadata("build") +
								Mantaro.instance().getMetadata("date") + "_J" + JDAInfo.VERSION).queue();
						if(member.hasPermission(Permission.ADMINISTRATOR) || member.hasPermission(Permission.MESSAGE_MANAGE) 
								|| member.hasPermission(Permission.BAN_MEMBERS) || member.hasPermission(Permission.KICK_MEMBERS))
						{
							author.getPrivateChannel().sendMessage(
									"**Admin commands:**\n"
									+ builderadmin.toString()).queue();
						}
					});
		} else{
			if(mantaro.modules.containsKey(command)){
				if(!mantaro.modules.get(command).getExtendedHelp().isEmpty())
					channel.sendMessage(mantaro.modules.get(command).getExtendedHelp()).queue();
				else
					channel.sendMessage(":heavy_multiplication_x: No extended help set for this command.").queue();
			}
		}
	}

}
