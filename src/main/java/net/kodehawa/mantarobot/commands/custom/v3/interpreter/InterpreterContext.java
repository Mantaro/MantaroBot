package net.kodehawa.mantarobot.commands.custom.v3.interpreter;

import java.util.HashMap;
import java.util.Map;

public class InterpreterContext {
    private final Map<String, Object> custom = new HashMap<>();
    private final Map<String, String> vars;
    private final Map<String, Operation> operations;

    public InterpreterContext(Map<String, String> vars, Map<String, Operation> operations) {
        this.vars = vars;
        this.operations = operations;
    }

    public Map<String, String> vars() {
        return vars;
    }

    public Map<String, Operation> operations() {
        return operations;
    }

    public void set(String key, Object value) {
        custom.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T)custom.get(key);
    }
}
