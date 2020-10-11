/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *  
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.custom.v3.interpreter;

import net.kodehawa.mantarobot.commands.custom.v3.ast.*;

import java.util.stream.Collectors;

public class InterpreterVisitor implements NodeVisitor<String, InterpreterContext> {
    @Override
    public String visitLiteral(LiteralNode node, InterpreterContext context) {
        return node.value();
    }

    @Override
    public String visitVariable(VariableNode node, InterpreterContext context) {
        String key = node.name().accept(this, context);
        String value = context.vars().get(key);
        if (value == null) {
            return "{Unresolved variable " + key + "}";
        }
        return value;
    }

    @Override
    public String visitOperation(OperationNode node, InterpreterContext context) {
        String type = node.name().accept(this, context);
        Operation op = context.operations().get(type);
        if (op == null) {
            return "{Unknown operation " + type + "}";
        }
        return op.apply(context, node.args().stream()
                .map(n -> (Operation.Argument) () -> n.accept(this, context))
                .collect(Collectors.toList())
        );
    }

    @Override
    public String visitMulti(MultiNode node, InterpreterContext context) {
        StringBuilder sb = new StringBuilder();
        for (Node n : node.children()) {
            sb.append(n.accept(this, context));
        }
        return sb.toString();
    }
}
