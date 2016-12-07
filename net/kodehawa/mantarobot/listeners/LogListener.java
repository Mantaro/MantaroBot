package net.kodehawa.mantarobot.listeners;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.GuildBanEvent;
import net.dv8tion.jda.core.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.MessageUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.kodehawa.mantarobot.cmd.servertools.Parameters;
import net.kodehawa.mantarobot.core.Mantaro;

public class LogListener extends ListenerAdapter {

	Date date = new Date();
	private DateFormat df = new SimpleDateFormat("HH:mm:ss");
	
	@Override
	public void onMessageDelete(MessageDeleteEvent event){
		try{
			String hour = df.format(new Date(System.currentTimeMillis()));
			if(Parameters.getLogHash().containsKey(event.getGuild().getId())){
				TextChannel tc = event.getGuild().getTextChannelsByName(Parameters.getLogChannelForServer(event.getGuild().getId()), true).get(0);
				Message deletedMessage = Listener.shortMessageHistory.get(event.getMessageId());
				if(!deletedMessage.getContent().isEmpty() && !event.getChannel().getName().equals(Parameters.getLogChannelForServer(event.getGuild().getId()))){
					tc.sendMessage(":warning: `[" + hour + "]` " + deletedMessage.getAuthor().getName() + "#" + deletedMessage.getAuthor().getDiscriminator() + " *deleted*"
							+ " a message in #" + event.getChannel().getName() + "\n" + "**Message deleted:** " + deletedMessage.getContent()).queue();
					if(Mantaro.instance().isDebugEnabled){ System.out.println("Caught LogListener::onMessageDelete on server " + event.getGuild().getName());}

				}
			}
		} catch(Exception ignored){} //Fails without logging
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
				    tc.sendMessage(":warning: `[" + hour + "]` " + author.getName()+ "#" + author.getDiscriminator() + " *modified* a message in #" + event.getChannel().getName() + ".\n" + "**Previous content:** " + editedMessage.getContent() + "\n**New content:** "
					+ event.getMessage().getContent()).queue();
					if(Mantaro.instance().isDebugEnabled){ System.out.println("Caught LogListener::onMessageUpdate on server " + event.getGuild().getName());}
					//Update old message
					Listener.shortMessageHistory.put(event.getMessage().getId(), event.getMessage());
				}
			}
		} catch(Exception ignored){} //Fails without logging
	}
	
	@Override
	public void onGuildBan(GuildBanEvent event){
		String hour = df.format(new Date(System.currentTimeMillis()));
		if(Parameters.getLogHash().containsKey(event.getGuild().getId())){
			TextChannel tc = event.getGuild().getTextChannelsByName(Parameters.getLogChannelForServer(event.getGuild().getId()), true).get(0);
			tc.sendMessage(":warning: `[" + hour + "]` " + event.getUser().getName() + "#" + event.getUser().getDiscriminator() + " just got banned.").queue();
		}
	}
	
	@Override
	public void onGuildUnban(GuildUnbanEvent event){
		String hour = df.format(new Date(System.currentTimeMillis()));
		if(Parameters.getLogHash().containsKey(event.getGuild().getId())){
			TextChannel tc = event.getGuild().getTextChannelsByName(Parameters.getLogChannelForServer(event.getGuild().getId()), true).get(0);
			tc.sendMessage(":warning: `[" + hour + "]` " + event.getUser().getName() + "#" + event.getUser().getDiscriminator() + " just got unbanned.").queue();
		}
	}
}
