package net.kodehawa.mantarobot.commands.custom.v3.ast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public interface Node {
    <T, C> T accept(NodeVisitor<T, C> visitor, C context);

    Node simplify();

    static Node fromJSON(JSONObject serialized) {
        switch(serialized.getString("type")) {
            case "literal": return new LiteralNode(serialized.getString("value"));
            case "variable": return new VariableNode(fromJSON(serialized.getJSONObject("name")));
            case "op": {
                List<Node> args = new ArrayList<>();
                JSONArray raw = serialized.getJSONArray("args");
                for(int i = 0; i < raw.length(); i++) {
                    args.add(fromJSON(raw.getJSONObject(i)));
                }
                return new OperationNode(fromJSON(serialized.getJSONObject("name")), args);
            }
            case "multi": {
                List<Node> nodes = new ArrayList<>();
                JSONArray raw = serialized.getJSONArray("children");
                for(int i = 0; i < raw.length(); i++) {
                    nodes.add(fromJSON(raw.getJSONObject(i)));
                }
                return new MultiNode(nodes);
            }
            default: {
                throw new IllegalArgumentException("Unknown node name " + serialized.getString("name"));
            }
        }
    }
}
