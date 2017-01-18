package net.kodehawa.mantarobot.module;

import net.dv8tion.jda.core.entities.*;

import java.util.HashMap;
import java.util.List;

public class Module extends Register {
    public static HashMap<String, Callback> modules = new HashMap<>();
    public static HashMap<String, String[]> moduleDescriptions = new HashMap<>();
    private static final Module inst = new Module();
    public Guild guild;
    public MessageChannel channel;
    public User author;
    public Message receivedMessage;
    private Category cat;

    public Module(){}

    public void setCategory(Category c){
        this.cat = c;
    }

    public void register(String name, String description, Callback callback) {
        System.out.printf("Loaded %s, %s (Cat %s) \n", name, getClass().getSimpleName(), cat);

        String[] descriptionBuilder = {
                description,
                getClass().getSimpleName(),
                cat.toString()
        };

        moduleDescriptions.put(name, descriptionBuilder);
        modules.put(name, callback);
    }

    public String[] getDescription(String cmdname){
        return moduleDescriptions.get(cmdname);
    }

    public static Module instance(){
        return inst;
    }
}
