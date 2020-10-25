package net.kodehawa.mantarobot.core.command.argument;

import net.kodehawa.mantarobot.core.command.argument.split.SplitString;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Helper for reading arguments in order, without having to explicitly keep track of indexes.
 */
public class Arguments implements Iterator<SplitString> {
    private final SplitString[] array;
    private final ArgumentValues strings;
    private int offset;

    public Arguments(@Nonnull SplitString[] array, @Nonnegative int offset) {
        this.array = array;
        this.offset = offset;
        this.strings = new ArgumentValues(this);
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
    @Nonnull
    @CheckReturnValue
    public MarkedBlock marked() {
        return new MarkedBlock(this);
    }

    /**
     * The current offset in the arguments array.
     *
     * @return The current offset in the arguments array.
     */
    @Nonnegative
    @CheckReturnValue
    public int getOffset() {
        return offset;
    }

    /**
     * Sets the current offset in the arguments array.
     *
     * @param offset New offset to use.
     *
     * @apiNote This method should be avoided by parsers. Use the {@link #marked() marker} API instead.
     */
    public void setOffset(@Nonnegative int offset) {
        if (offset > array.length) {
            throw new IllegalArgumentException("Offset > length");
        }
        this.offset = offset;
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
    public SplitString[] absoluteRange(int from, int to) {
        return Arrays.copyOfRange(array, from, to);
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
    public SplitString[] range(int from, int to) {
        return absoluteRange(offset + from, offset + to);
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
    public SplitString get(@Nonnegative int i) {
        //noinspection ConstantConditions
        if (i < 0) {
            throw new IllegalArgumentException("Negative index");
        }
        if (i + offset >= array.length) {
            throw new IllegalArgumentException("Out of bounds! Remaining values = " + (array.length - offset) + ", requested = " + i);
        }
        return array[offset + i];
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
    public SplitString previous() {
        if (offset == 0) {
            throw new IllegalStateException("Already at the beginning");
        }
        return array[--offset];
    }

    /**
     * Returns whether or not there is a previous element.
     *
     * @return True if there is a previous element.
     */
    @CheckReturnValue
    public boolean hasPrevious() {
        return offset > 0;
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
        //noinspection ResultOfMethodCallIgnored
        previous();
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
    public SplitString next() {
        if (offset == array.length) {
            throw new IllegalStateException("No more arguments to read");
        }
        return array[offset++];
    }

    /**
     * Returns whether or not there are more arguments to read.
     *
     * @return True if there are more arguments.
     */
    @Override
    @CheckReturnValue
    public boolean hasNext() {
        return offset < array.length;
    }

    /**
     * Returns a view to the arguments' values, useful when the delimiters aren't needed.
     *
     * @return A view to the arguments' values.
     */
    @Nonnull
    @CheckReturnValue
    public ArgumentValues values() {
        return strings;
    }

    /**
     * Creates a copy of this object. Changes made to the copy or {@code this} won't affect the other.
     *
     * @return A copy of this object.
     */
    @Nonnull
    @CheckReturnValue
    public Arguments snapshot() {
        return new Arguments(array, offset);
    }
}
