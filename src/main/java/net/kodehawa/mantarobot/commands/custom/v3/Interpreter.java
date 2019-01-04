package net.kodehawa.mantarobot.commands.custom.v3;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Interpreter {
    private final Map<String, Object> custom = new HashMap<>();
    private final Map<String, String> vars;
    private final Map<String, Operation> operations;

    public Interpreter(Map<String, String> vars, Map<String, Operation> operations) {
        this.vars = vars;
        this.operations = operations;
    }

    public String exec(JSONArray code) {
        StringBuilder result = new StringBuilder();
        for(int i = 0; i < code.length(); i++) {
            JSONObject node = code.getJSONObject(i);
            switch(node.getString("type")) {
                case "literal": {
                    result.append(node.getString("value"));
                    break;
                }
                case "variable": {
                    result.append(vars.getOrDefault(exec(node.getJSONArray("name")), ""));
                    break;
                }
                case "op": {
                    String name = exec(node.getJSONArray("name"));
                    JSONArray data = node.getJSONArray("data");
                    String[] args = new String[data.length()];
                    for(int j = 0; j < args.length; j++) {
                        args[i] = exec(data.getJSONArray(i));
                    }
                    Operation operation = operations.get(name);
                    if(operation != null) {
                        result.append(operation.apply(this, args));
                    }
                }
            }
        }
        return result.toString();
    }

    public void set(String key, Object value) {
        custom.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T)custom.get(key);
    }
}
