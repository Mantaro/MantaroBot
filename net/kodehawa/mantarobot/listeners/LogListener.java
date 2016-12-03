package net.kodehawa.mantarobot.listeners;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.GuildBanEvent;
import net.dv8tion.jda.core.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.MessageUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.kodehawa.mantarobot.cmd.servertools.Logs;

public class LogListener extends ListenerAdapter {
	@Override
	public void onMessageDelete(MessageDeleteEvent event){
		Thread thread = new Thread(){
			public void run(){
				if(Logs.getLogHash().containsKey(event.getGuild().getId())){
					TextChannel tc = event.getGuild().getTextChannelsByName(Logs.getLogChannelForServer(event.getGuild().getId()), true).get(0);
					Message deletedMessage = Listener.shortMessageHistory.get(event.getMessageId());
					tc.sendMessage(":warning: *" + deletedMessage.getAuthor().getName() + "#" + deletedMessage.getAuthor().getDiscriminator() + "* deleted a message.\n" + "**Message deleted:** " + deletedMessage.getContent()).queue();
				}
			}
		};
		thread.run();
		thread.interrupt();
	}
	
	@Override
	public void onMessageUpdate(MessageUpdateEvent event){
		Thread thread = new Thread(){
			public void run(){
				if(Logs.getLogHash().containsKey(event.getGuild().getId()) && !event.getAuthor().isBot() /*So it doesnt log when I edit the message*/){
					TextChannel tc = event.getGuild().getTextChannelsByName(Logs.getLogChannelForServer(event.getGuild().getId()), true).get(0);
					User author = event.getAuthor();
					Message editedMessage = Listener.shortMessageHistory.get(event.getMessage().getId());
				    tc.sendMessage(":warning: *" + author.getName()+ "#" + author.getDiscriminator() + "* modified a message" + ".\n" + "**Previous content:** " + editedMessage.getContent() + "\n**New content:** " + event.getMessage().getContent()).queue();
				}
			}
		};
		thread.run();
		thread.interrupt();
	}
	
	@Override
	public void onGuildBan(GuildBanEvent event){
		Thread thread = new Thread(){
			public void run(){
				if(Logs.getLogHash().containsKey(event.getGuild().getId())){
					TextChannel tc = event.getGuild().getTextChannelsByName(Logs.getLogChannelForServer(event.getGuild().getId()), true).get(0);
					tc.sendMessage(":warning: " + event.getUser().getName() + "#" + event.getUser().getDiscriminator() + " just got banned.").queue();
				}
			}
		};
		thread.run();
		thread.interrupt();
	}
	
	@Override
	public void onGuildUnban(GuildUnbanEvent event){
		Thread thread = new Thread(){
			public void run(){
				if(Logs.getLogHash().containsKey(event.getGuild().getId())){
					TextChannel tc = event.getGuild().getTextChannelsByName(Logs.getLogChannelForServer(event.getGuild().getId()), true).get(0);
					tc.sendMessage(":warning: " + event.getUser().getName() + "#" + event.getUser().getDiscriminator() + " just got unbanned.").queue();
				}
			}
		};
		thread.run();
		thread.interrupt();
	}
}
