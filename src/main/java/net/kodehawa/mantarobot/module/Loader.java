package net.kodehawa.mantarobot.module;

import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.log.Type;
import net.kodehawa.mantarobot.log.Log;

/**
 * Threaded automatic command loader.
 * This class handles everything related to loading commands in the following order:
 * -> Loops through the classes in the {@link net.kodehawa.mantarobot.cmd} package and its subpackages which extends the {@link net.kodehawa.mantarobot.management.Command} class.
 * -> Initializes the instance of the command.
 * -> Puts the command in a {@link java.util.HashMap} containing the command name (cmd.getName()) and the command instance itself.
 *
 * It is completely automated and needs no input from the programmer when a class which extends {@link net.kodehawa.mantarobot.management.Command} is added to the classpath.
 *
 * Under the hood the  HashMap is using the Reflections library, which puts all the classes on the specificed package which
 * extends or implements the specified method, but without actually loading them.
 *
 * Seen in Mantaro.class line 73 to 77.
 *
 * This is a task which benefits from multiple cores and tries to be done as quick as possible.
 * @author Yomura
 * @since 24/11/2016
 * @see org.reflections.Reflections
 */
public class Loader {

    public Loader(){
        Runnable loaderthr = () ->
        {
            for(Class<? extends Module> c : Mantaro.instance().classes){
                try {
                    c.newInstance();
                } catch (InstantiationException e) {
                    Log.instance().print("Cannot initialize a command", this.getClass(), Type.CRITICAL, e);
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    Log.instance().print("Cannot access a command class!", this.getClass(), Type.CRITICAL, e);
                    e.printStackTrace();
                }
            }
            Mantaro.instance().classes.clear();
            Log.instance().print("Loaded " + Module.modules.size() + " commands", this.getClass(), Type.INFO);
        };
        new Thread(loaderthr).start();
    }
}
