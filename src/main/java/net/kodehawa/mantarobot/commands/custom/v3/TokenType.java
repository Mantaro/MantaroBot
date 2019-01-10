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
