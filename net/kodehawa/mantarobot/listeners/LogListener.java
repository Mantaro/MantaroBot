package net.kodehawa.mantarobot.listeners;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.MessageUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.kodehawa.mantarobot.cmd.servertools.Parameters;

public class LogListener extends ListenerAdapter {
	@Override
	public void onMessageDelete(MessageDeleteEvent event){
		Thread thread = new Thread(){
			public void run(){
				if(Parameters.getLogHash().containsKey(event.getGuild().getId())){
					TextChannel tc = event.getGuild().getTextChannelsByName(Parameters.getLogChannelForServer(event.getGuild().getId()), true).get(0);
					Message deletedMessage = Listener.shortMessageHistory.get(event.getMessageId());
					tc.sendMessage(":warning: " + deletedMessage.getAuthor().getName() + " deleted a message.\r" + "Message deleted: " + deletedMessage.getContent()).queue();
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
				if(Parameters.getLogHash().containsKey(event.getGuild().getId()) && !event.getAuthor().isBot() /*So it doesnt log when I edit the message*/){
					TextChannel tc = event.getGuild().getTextChannelsByName(Parameters.getLogChannelForServer(event.getGuild().getId()), true).get(0);
					User author = event.getAuthor();
					Message editedMessage = Listener.shortMessageHistory.get(event.getMessage().getId());
				    tc.sendMessage(":warning: " + author.getName() + " modified a message" + ".\r" + "Message changed from -> to: " + editedMessage.getContent() + " -> " + event.getMessage().getContent()).queue();
				}
			}
		};
		thread.run();
		thread.interrupt();
	}
}
