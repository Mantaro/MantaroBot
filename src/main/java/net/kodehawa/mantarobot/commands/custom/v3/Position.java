package net.kodehawa.mantarobot.commands.custom.v3;

public class Position {
    private final int line;
    private final int column;
    private final int start;
    private final int end;

    public Position(int line, int column, int start, int end) {
        this.line = line;
        this.column = column;
        this.start = start;
        this.end = end;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    @Override
    public int hashCode() {
        return line ^ column ^ start ^ end;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Position)) {
            return false;
        }
        Position p = (Position)obj;
        return p.line == line && p.column == column && p.start == start && p.end == end;
    }

    @Override
    public String toString() {
        return "Position(" + line + ":" + column + ", " + start + "-" + end + ")";
    }
}
