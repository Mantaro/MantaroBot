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

package net.kodehawa.mantarobot.commands.custom.v3;

import javax.annotation.Nonnull;
import java.util.Objects;

public enum TokenType {
    LITERAL(null), START_VAR("$("), START_OP("@{"),
    RIGHT_PAREN(")"), RIGHT_BRACE("}"), SEMICOLON(";");

    private final String literalValue;

    TokenType(String literalValue) {
        this.literalValue = literalValue;
    }

    @Nonnull
    public String literalValue() {
        return Objects.requireNonNull(literalValue, "This token type does not have a literal value");
    }
}
