package net.kodehawa.lib.customfunc;

public class Token {
    private final String name;

    public Token(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Token && ((Token) obj).getName().equals(this.getName());
    }

    public String toString() {
        return this.name;
    }

    public String getName() {
        return this.name;
    }

    public Object resolve(Environiment environiment) {
        return environiment.containsResolvedToken(getName()) ? environiment.getResolvedToken(getName()) : this;
    }
}
