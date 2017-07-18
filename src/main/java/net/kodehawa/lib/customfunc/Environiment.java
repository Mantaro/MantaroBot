package net.kodehawa.lib.customfunc;

import java.util.Map;

public interface Environiment {
    static Environiment of(Map<String, CustomFunction> functions, Map<String, Object> tokens) {
        return new Environiment() {
            @Override
            public boolean containsFunction(String functionName) {
                return functions.containsKey(functionName);
            }

            @Override
            public boolean containsResolvedToken(String token) {
                return tokens.containsKey(token);
            }

            @Override
            public CustomFunction getFunction(String functionName) {
                return functions.get(functionName);
            }

            @Override
            public Object getResolvedToken(String token) {
                return tokens.get(token);
            }
        };
    }

    boolean containsFunction(String functionName);

    boolean containsResolvedToken(String token);

    CustomFunction getFunction(String functionName);

    Object getResolvedToken(String token);
}
