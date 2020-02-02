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

package net.kodehawa.mantarobot.commands.custom.v3.ast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public interface Node {
    static Node fromJSON(JSONObject serialized) {
        switch(serialized.getString("type")) {
            case "literal":
                return new LiteralNode(serialized.getString("value"));
            case "variable":
                return new VariableNode(fromJSON(serialized.getJSONObject("name")));
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
    
    <T, C> T accept(NodeVisitor<T, C> visitor, C context);
    
    Node simplify();
}
