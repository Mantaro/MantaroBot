package net.kodehawa.mantarobot.commands.custom.v3.serializer;

import net.kodehawa.mantarobot.commands.custom.v3.ast.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class SerializerVisitor implements NodeVisitor<JSONObject, Void> {
    @Override
    public JSONObject visitLiteral(LiteralNode node, Void context) {
        return new JSONObject().put("type", "literal").put("value", node.value());
    }

    @Override
    public JSONObject visitVariable(VariableNode node, Void context) {
        return new JSONObject().put("type", "variable").put("name", node.name().accept(this, null));
    }

    @Override
    public JSONObject visitOperation(OperationNode node, Void context) {
        JSONArray args = new JSONArray();
        for(Node arg : node.args()) {
            args.put(arg.accept(this, null));
        }
        return new JSONObject().put("type", "op").put("name", node.name().accept(this, null))
                .put("args", args);
    }

    @Override
    public JSONObject visitMulti(MultiNode node, Void context) {
        JSONArray array = new JSONArray();
        for(Node child : node.children()) {
            array.put(child.accept(this, null));
        }
        return new JSONObject().put("type", "multi").put("children", array);
    }
}
