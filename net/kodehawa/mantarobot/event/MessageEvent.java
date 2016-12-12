package net.kodehawa.mantarobot.event;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class MessageEvent extends Event {

	MessageReceivedEvent evt;
	String messageContent;
	
	public MessageEvent(String name, MessageReceivedEvent event, String message) {
		super(name);
	}
	
	public MessageReceivedEvent getMessageEvent(){
		return evt;
	}
	
	public String getMessageContent(){
		return messageContent;
	}
}
