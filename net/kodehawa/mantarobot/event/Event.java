package net.kodehawa.mantarobot.event;

public abstract class Event {
	
	String name;

	public Event(String name){
		this.name = name;
	}
	
	public String getName(){
		return this.name;
	}
}
