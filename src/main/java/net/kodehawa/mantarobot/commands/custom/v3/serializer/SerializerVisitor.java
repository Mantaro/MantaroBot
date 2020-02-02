/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.custom.v3.serializer;

import net.kodehawa.mantarobot.commands.custom.v3.ast.LiteralNode;
import net.kodehawa.mantarobot.commands.custom.v3.ast.MultiNode;
import net.kodehawa.mantarobot.commands.custom.v3.ast.Node;
import net.kodehawa.mantarobot.commands.custom.v3.ast.NodeVisitor;
import net.kodehawa.mantarobot.commands.custom.v3.ast.OperationNode;
import net.kodehawa.mantarobot.commands.custom.v3.ast.VariableNode;
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
