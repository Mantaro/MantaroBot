package net.kodehawa.mantarobot.cmd.management;

import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.log.LogType;
import net.kodehawa.mantarobot.log.Logger;

/**
 * Threaded automatic command loader.
 * This class handles everything related to loading commands in the following order:
 * -> Loops through the classes in the {@link net.kodehawa.mantarobot.cmd} package and its subpackages which extends the {@link net.kodehawa.cmd.management.Command} class.
 * -> Initializes the instance of the command.
 * -> Puts the command in a {@link java.util.HashMap} containing the command name (cmd.getName()) and the command instance itself.
 * 
 * It is completely automated and needs no input from the programmer when a class which extends {@link net.kodehawa.cmd.management.Command} is added to the classpath.
 * 
 * Under the hood the {@link net.kodehawa.core.Mantaro#classes} HashMap is using the Reflections library, which puts all the classes on the specificed package which 
 * extends or implements the specified method, but without actually loading them.
 * 
 * Seen in Mantaro.class line 73 to 77.
 * 
 * This is a task which benefits from multiple cores and tries to be done as quick as possible.
 * @author Yomura
 * @since 24/11/2016
 * @see Reflections library.
 */
public class Loader {

	public Loader(){
		Runnable loaderthr = () ->
		{
			for(Class<? extends Command> c : Mantaro.instance().classes){
		   		try {
		   			//Gets the instance of every command. Basically initializing the constructor of it and calling the class itself, making myself able to call its 
		   			//methods and also itself.
		   			Command cmd = c.newInstance();
		   			//Adds the command to the commands hashmap, with key = command name and executes the command action. Later on in the command
		   			//call timeline this will be called as cmd.onCommand (called from the hashmap). Avoid adding the command if the name is null.
		   			if(cmd.getName() != null){
	    	   			Mantaro.instance().modules.put(cmd.getName(), cmd);
	    	   		}
		   		} catch (InstantiationException e) {
		   			Logger.instance().print("Cannot initialize a command", LogType.CRITICAL);
		   			e.printStackTrace();
		   		} catch (IllegalAccessException e) {
		   			Logger.instance().print("Cannot access a command class!", LogType.CRITICAL);
		   			e.printStackTrace();
		   		}
			}
			Mantaro.instance().classes.clear();
			Logger.instance().print("Loaded " + Mantaro.instance().modules.size() + " modules", LogType.INFO);
		};
		new Thread(loaderthr).start();
	}
}
