package net.kodehawa.mantarobot.cmd.custom;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.kodehawa.mantarobot.cmd.guild.Parameters;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.log.Log;
import net.kodehawa.mantarobot.log.Type;
import net.kodehawa.mantarobot.module.Callback;
import net.kodehawa.mantarobot.module.Category;
import net.kodehawa.mantarobot.module.CommandType;
import net.kodehawa.mantarobot.module.Module;
import net.kodehawa.mantarobot.util.JSONUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

public class UserCommands extends Module {

    private static Map<String, Map<String, List<String>>> custom = new HashMap<>();
    private File file;

    public UserCommands(){
        if(Mantaro.instance().isWindows()){
            this.file = new File("C:/mantaro/cc.json");
        }
        else if(Mantaro.instance().isUnix()){
            this.file = new File("/home/mantaro/cc.json");
        }
        read();
        this.setCategory(Category.FUN);
        this.registerCommands();
        Mantaro.instance().getSelf().addEventListener(new CustomCommandListener());
    }

    @Override
    public void registerCommands(){
        super.register("addcustom", "Adds a custom command", new Callback() {
            @Override
            public void onCommand(String[] args, String content, MessageReceivedEvent event) {
                if(!content.startsWith("debug")){
                    String guild = event.getGuild().getId();
                    String name = args[0];
                    String responses[] = content.replaceAll(args[0] + " ", "").split(",");

                    List<String> responses1 = new ArrayList<>(Arrays.asList(responses));
                    if(custom.get(guild) == null){
                        Map<String, List<String>> responsesMap = new HashMap<>();
                        responsesMap.put(name, responses1);
                        custom.put(guild, responsesMap);
                    } else {
                        custom.get(guild).put(name, responses1);
                    }

                    JSONObject jsonObject = new JSONObject(toJson(custom));
                    JSONUtils.instance().write(file, jsonObject);
                    read();

                    String sResponses = String.join(", ", responses1);
                    event.getChannel().sendMessage("``Added custom command: " + name + " with responses: " + sResponses + " -> Guild: " + guild + "``").queue();
                } else {
                    event.getChannel().sendMessage(toJson(custom)).queue();
                }
            }

            @Override
            public String help() {
                return "Creates a custom command. Only works on the guild where it was created.";
            }

            @Override
            public CommandType commandType() {
                return CommandType.USER;
            }
        });

        super.register("deletecustom", "Deletes a custom command", new Callback() {
            @Override
            public void onCommand(String[] args, String content, MessageReceivedEvent event) {
                String guild = event.getGuild().getId();
                String name = args[0];

                if(custom.get(guild).get(name) != null){
                    custom.get(guild).remove(name);
                    JSONObject jsonObject = new JSONObject(toJson(custom));
                    JSONUtils.instance().write(file, jsonObject);
                    read();

                    event.getChannel().sendMessage("``Deleted custom command: " + name +  " -> Guild: " + event.getGuild().getId() + "``").queue();
                } else {
                    event.getChannel().sendMessage("``Command doesn't exist!``").queue();
                }
            }

            @Override
            public String help() {
                return "Deletes a custom command.";
            }

            @Override
            public CommandType commandType() {
                return CommandType.USER;
            }
        });
    }

    private static Map<String, Map<String, List<String>>> getCustomCommands(){
        return custom;
    }

    private static Map<String, Map<String, List<String>>> fromJson(String json) {
        JsonElement element = new JsonParser().parse(json);

        if (!element.isJsonObject()) throw new IllegalStateException("\"ROOT\" element MUST BE a JsonObject");

        Map<String, Map<String, List<String>>> result = new HashMap<>();

        element.getAsJsonObject().entrySet().forEach(entry -> {
            if (!entry.getValue().isJsonObject())
                throw new IllegalStateException("\"ROOT -> *\" Element MUST BE a JsonObject");

            Map<String, List<String>> map = new HashMap<>();

            entry.getValue().getAsJsonObject().entrySet().forEach(entry2 -> {
                if (!entry2.getValue().isJsonArray())
                    throw new IllegalStateException("\"ROOT -> * -> *\" Element MUST BE a JsonArray");

                List<String> list = new ArrayList<>();

                entry2.getValue().getAsJsonArray().forEach(arrayElement -> {
                    if (!arrayElement.isJsonPrimitive() || !arrayElement.getAsJsonPrimitive().isString())
                        throw new IllegalStateException("\"ROOT -> * -> * -> *\" Element MUST BE a String");

                    list.add(arrayElement.getAsString());
                });

                map.put(entry2.getKey(), list);
            });

            result.put(entry.getKey(), map);
        });

        return result;
    }

    private static String toJson(Map<String, Map<String, List<String>>> map) {
        return new Gson().toJson(map);
    }

    private void read(){
        try{
            Log.instance().print("Loading custom commands...", this.getClass(), Type.INFO);
            BufferedReader br = new BufferedReader(new FileReader(file));
            JsonParser parser = new JsonParser();
            JsonObject object = parser.parse(br).getAsJsonObject();
            custom = fromJson(object.toString());
        } catch (FileNotFoundException | UnsupportedOperationException e) {
            e.printStackTrace();
            Log.instance().print("Cannot load custom commands!", this.getClass(), Type.WARNING, e);
        }
    }

    public class CustomCommandListener extends ListenerAdapter {
        private String defaultPx;

        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            defaultPx = Parameters.getPrefixForServer("default");

            if (event.getMessage().getContent().startsWith(defaultPx)) {
                if(UserCommands.getCustomCommands().containsKey(event.getGuild().getId())){
                    if (UserCommands.getCustomCommands().get(event.getGuild().getId()).containsKey(event.getMessage().getContent().replaceAll(defaultPx, ""))) {
                        Random random = new Random();
                        List<String> responses = UserCommands.getCustomCommands().get(event.getGuild().getId()).get(event.getMessage().getContent().replace(defaultPx, ""));
                        event.getChannel().sendMessage(responses.get(random.nextInt(responses.size() - 1))).queue();
                    }
                }
            }
        }
    }
 }
