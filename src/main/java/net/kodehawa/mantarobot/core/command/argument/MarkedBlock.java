package net.kodehawa.mantarobot.core.command.argument;

/**
 * Allows resetting to a previous position, with support for nesting
 */
public class MarkedBlock {
    private final Arguments arguments;
    private int offset;

    public MarkedBlock(Arguments arguments) {
        this.arguments = arguments;
        mark();
    }

    /**
     * Updates the reset offset. After calling this method, any resets will return to the
     * current offset.
     */
    public void mark() {
        this.offset = arguments.getOffset();
    }

    /**
     * Resets to the currently marked offset.
     */
    public void reset() {
        arguments.setOffset(offset);
    }
}
