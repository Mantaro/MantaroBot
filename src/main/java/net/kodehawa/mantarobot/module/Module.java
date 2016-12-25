package net.kodehawa.mantarobot.module;

import net.dv8tion.jda.core.entities.*;

import java.util.HashMap;

public class Module extends Register {
    public static HashMap<String, Callback> modules = new HashMap<>();
    public static HashMap<String, String> moduleDescriptions = new HashMap<>();
    private static final Module inst = new Module();
    public Guild guild;
    public MessageChannel channel;
    public User author;
    public Message receivedMessage;

    public Module(){}

    public void register(String name, String description, Callback callback) {
        //Log.instance().print("Added command " + name + " in module " + getModuleName(), this.getClass(), Type.INFO);
        moduleDescriptions.put(name, description);
        modules.put(name, callback);
    }

    public String getModuleName(){
        return this.getClass().getSimpleName();
    }

    public String getDescription(String cmdname){
        return moduleDescriptions.get(cmdname);
    }

    public static Module instance(){
        return inst;
    }
}
