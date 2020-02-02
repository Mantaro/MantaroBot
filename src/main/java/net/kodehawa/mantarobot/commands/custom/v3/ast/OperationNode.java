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

import java.util.List;
import java.util.stream.Collectors;

public class OperationNode implements Node {
    private final Node name;
    private final List<Node> args;
    
    public OperationNode(Node name, List<Node> args) {
        this.name = name;
        this.args = args;
    }
    
    public Node name() {
        return name;
    }
    
    public List<Node> args() {
        return args;
    }
    
    @Override
    public <T, C> T accept(NodeVisitor<T, C> visitor, C context) {
        return visitor.visitOperation(this, context);
    }
    
    @Override
    public Node simplify() {
        return new OperationNode(
                name.simplify(),
                args.stream()
                        .map(Node::simplify)
                        .collect(Collectors.toList())
        );
    }
}
