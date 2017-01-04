package net.kodehawa.mantarobot.listeners;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.GuildBanEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.MessageUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.kodehawa.mantarobot.cmd.guild.Parameters;

public class LogListener extends ListenerAdapter {

	private final DateFormat df = new SimpleDateFormat("HH:mm:ss");
	private static int logTotal = 0;
	@Override
	public void onMessageDelete(MessageDeleteEvent event){
		try{
			String hour = df.format(new Date(System.currentTimeMillis()));
			if(Parameters.getLogHash().containsKey(event.getGuild().getId())){
				TextChannel tc = event.getGuild().getTextChannelsByName(Parameters.getLogChannelForServer(event.getGuild().getId()), true).get(0);
				Message deletedMessage = Listener.shortMessageHistory.get(event.getMessageId());
				if(!deletedMessage.getContent().isEmpty() && !event.getChannel().getName().equals(Parameters.getLogChannelForServer(event.getGuild().getId()))){
					logTotal++;
					tc.sendMessage(":warning: `[" + hour + "]` " + deletedMessage.getAuthor().getName() + "#" + deletedMessage.getAuthor().getDiscriminator() + " *deleted*"
							+ " a message in #" + event.getChannel().getName() + "\n" + "```diff\n-" + deletedMessage.getContent().replace("```", "") + "```").queue();
				}
			}
		} catch(Exception e){
			if(!(e instanceof NullPointerException)){
				//Unexpected exception, log.
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onMessageUpdate(MessageUpdateEvent event){
		try{
			String hour = df.format(new Date(System.currentTimeMillis()));
			if(Parameters.getLogHash().containsKey(event.getGuild().getId())){
				TextChannel tc = event.getGuild().getTextChannelsByName(Parameters.getLogChannelForServer(event.getGuild().getId()), true).get(0);
				User author = event.getAuthor();
				Message editedMessage = Listener.shortMessageHistory.get(event.getMessage().getId());
				if(!editedMessage.getContent().isEmpty() && !event.getChannel().getName().equals(Parameters.getLogChannelForServer(event.getGuild().getId()))){
				    tc.sendMessage(":warning: `[" + hour + "]` " + author.getName()+ "#" + author.getDiscriminator() + " *modified* a message in #" + event.getChannel().getName() + ".\n"
							+ "```diff\n-" + editedMessage.getContent().replace("```", "") +
							"\n+" + event.getMessage().getContent().replace("```", "") + "```").queue();
					//Update old message
					Listener.shortMessageHistory.put(event.getMessage().getId(), event.getMessage());
					logTotal++;
				}
			}
		} catch(Exception e){
			if(!(e instanceof NullPointerException)){
				//Unexpected exception, log.
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onGuildBan(GuildBanEvent event){
		String hour = df.format(new Date(System.currentTimeMillis()));
		if(Parameters.getLogHash().containsKey(event.getGuild().getId())){
			TextChannel tc = event.getGuild().getTextChannelsByName(Parameters.getLogChannelForServer(event.getGuild().getId()), true).get(0);
			tc.sendMessage(":warning: `[" + hour + "]` " + event.getUser().getName() + "#" + event.getUser().getDiscriminator() + " just got banned.").queue();
			logTotal++;
		}
	}
	
	@Override
	public void onGuildUnban(GuildUnbanEvent event){
		String hour = df.format(new Date(System.currentTimeMillis()));
		if(Parameters.getLogHash().containsKey(event.getGuild().getId())){
			TextChannel tc = event.getGuild().getTextChannelsByName(Parameters.getLogChannelForServer(event.getGuild().getId()), true).get(0);
			tc.sendMessage(":warning: `[" + hour + "]` " + event.getUser().getName() + "#" + event.getUser().getDiscriminator() + " just got unbanned.").queue();
			logTotal++;
		}
	}

	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		String hour = df.format(new Date(System.currentTimeMillis()));
		if(Parameters.getLogHash().containsKey(event.getGuild().getId())) {
			TextChannel tc = event.getGuild().getTextChannelsByName(Parameters.getLogChannelForServer(event.getGuild().getId()), true).get(0);
			tc.sendMessage(":mega: " + event.getMember().getEffectiveName() + " just joined").queue();
			logTotal++;
		}
	}

	public static String getLogTotal(){
		return String.valueOf(logTotal);
	}
}
