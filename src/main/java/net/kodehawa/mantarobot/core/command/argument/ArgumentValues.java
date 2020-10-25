package net.kodehawa.mantarobot.core.command.argument;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.Iterator;

/**
 * Helper for reading arguments in order, without having to explicitly keep track of indexes.
 */
public class ArgumentValues implements Iterator<String> {
    private final Arguments arguments;

    public ArgumentValues(@Nonnull Arguments arguments) {
        this.arguments = arguments;
    }

    /**
     * Helper method for jumping back to a given offset, with support for nested marks.
     * <pre><code>
     * try(MarkedBlock block = args.marked()) {
     *     while(args.hasNext()) {
     *         handle(args.next());
     *         if (shouldAbort()) {
     *             block.reset();
     *             break;
     *         }
     *     }
     * }
     * </code></pre>
     *
     * @return A marker for a given offset, with support for back (and front) jumps.
     */
    public MarkedBlock marked() {
        return arguments.marked();
    }

    /**
     * The current offset in the arguments array.
     *
     * @return The current offset in the arguments array.
     */
    @Nonnegative
    @CheckReturnValue
    public int getOffset() {
        return arguments.getOffset();
    }

    /**
     * Sets the current offset in the arguments array.
     *
     * @param offset New offset to use.
     *
     * @apiNote This method should be avoided by parsers. Use the {@link #marked() marker} API instead.
     */
    public void setOffset(@Nonnegative int offset) {
        arguments.setOffset(offset);
    }

    /**
     * Returns a range of the underlying array, with absolute offsets.
     *
     * @param from Start index on the array, relative to the array start.
     * @param to End index on the array, relative to the array start.
     *
     * @return A range of the underlying array.
     *
     * @apiNote When possible, prefer using {@link #range(int, int) relative ranges} instead.
     */
    @Nonnull
    @CheckReturnValue
    public String[] absoluteRange(int from, int to) {
        String[] array = new String[to - from];
        for(int i = from; i < to; i++) {
            array[i - from] = get(i - from);
        }
        return array;
    }

    /**
     * Returns a range of the underlying array, with offsets relative to the {@link #getOffset() current offset}.
     *
     * @param from Start index on the array, relative to the {@link #getOffset() current offset}.
     * @param to End index on the array, relative to the {@link #getOffset() current offset}.
     *
     * @return A range of the underlying array.
     */
    @Nonnull
    @CheckReturnValue
    public String[] range(int from, int to) {
        String[] array = new String[to - from];
        for(int i = from; i < to; i++) {
            array[i - from] = get(i - from);
        }
        return array;
    }

    /**
     * Gets an element of the underlying array with an index relative to the {@link #getOffset() current offset}.
     *
     * @param i Index of the element.
     *
     * @return The element at the given index.
     *
     * @throws IllegalArgumentException If the given index is smaller than zero or outside the bounds of the underlying array.
     */
    @Nonnull
    @CheckReturnValue
    public String get(@Nonnegative int i) {
        return arguments.get(i).getValue();
    }

    /**
     * Steps back to the previous element and returns it.
     *
     * @return The previous element.
     *
     * @throws IllegalStateException If there is no previous element.
     *
     * @apiNote Parsers should avoid stepping back more times than they stepped forward, as that may result
     *          in an inconsistent state.
     */
    @Nonnull
    @CheckReturnValue
    public String previous() {
        return arguments.previous().getValue();
    }

    /**
     * Returns whether or not there is a previous element.
     *
     * @return True if there is a previous element.
     */
    @CheckReturnValue
    public boolean hasPrevious() {
        return arguments.hasPrevious();
    }

    /**
     * Steps back to the previous element
     *
     * @throws IllegalStateException If there is no previous element.
     *
     * @apiNote Parsers should avoid stepping back more times than they stepped forward, as that may result
     *          in an inconsistent state.
     */
    public void back() {
        arguments.back();
    }

    /**
     * Returns the next argument available.
     *
     * @return The next argument available.
     *
     * @throws IllegalStateException If there are no more arguments.
     *
     * @see #hasNext()
     */
    @Override
    @Nonnull
    @CheckReturnValue
    public String next() {
        return arguments.next().getValue();
    }

    /**
     * Returns whether or not there are more arguments to read.
     *
     * @return True if there are more arguments.
     */
    @Override
    @CheckReturnValue
    public boolean hasNext() {
        return arguments.hasNext();
    }

    /**
     * Creates a copy of this object. Changes made to the copy or {@code this} won't affect the other.
     *
     * @return A copy of this object.
     */
    @Nonnull
    @CheckReturnValue
    public ArgumentValues snapshot() {
        return arguments.snapshot().values();
    }
}
