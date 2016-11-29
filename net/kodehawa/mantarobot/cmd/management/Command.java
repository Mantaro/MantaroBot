package net.kodehawa.mantarobot.cmd.management;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public abstract class Command{

	private String name;
	private String commandType;
	private String alias = "";
	private String help = "";
	private String description = "";
	protected Guild guild;
	protected User author;
	protected MessageChannel channel;
	protected Message receivedMessage;

	public abstract void onCommand(String[] split, String content, MessageReceivedEvent event);	

	public void setName(String name){
		this.name = name;
	}
	
	public String getName(){
		return name;
	}
	
	public void setAlias(String alias){
		this.alias = alias;
	}
	
	public String getAlias(){
		return alias;
	}
	
	public void setHelp(String help){
		this.help = help;
	}
	
	public String getHelp(){
		return help;
	}
	
	public void setDescription(String description){
		this.description = description;
	}
	
	public String getDescription(){
		return this.description;
	}
	
	public void setCommandType(String type){
		this.commandType = type;
	}
	
	public String getCommandType(){
		return commandType;
	}
}
