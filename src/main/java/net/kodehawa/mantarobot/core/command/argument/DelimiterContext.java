package net.kodehawa.mantarobot.core.command.argument;

import net.kodehawa.mantarobot.core.command.argument.split.SplitString;

class DelimiterContext {
    private final StringBuilder builder = new StringBuilder();
    private final char delimiter;
    private final boolean allowEscaping;
    private boolean escaped;
    private boolean insideBlock;

    DelimiterContext(char delimiter, boolean allowEscaping) {
        this.delimiter = delimiter;
        this.allowEscaping = allowEscaping;
    }

    String result() {
        return builder.toString();
    }

    boolean handle(SplitString string) {
        builder.append(string.getPreviousWhitespace());
        String value = string.getValue();
        for(int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                escaped = false;
                builder.append(c);
                continue;
            }
            if (c == delimiter) {
                insideBlock = !insideBlock;
                continue;
            }
            if (allowEscaping && c == '\\') {
                escaped = true;
                continue;
            }

            builder.append(c);
        }
        return insideBlock || escaped;
    }
}
