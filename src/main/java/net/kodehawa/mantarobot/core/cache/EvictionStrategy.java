package net.kodehawa.mantarobot.core.cache;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

public interface EvictionStrategy {
    long NO_REMOVAL_NEEDED = 0;
    
    /**
     * Adds a member to the cache, returning the ID of the member that should
     * be removed, or {@code 0} if not needed.
     *
     * @param id ID to add.
     *
     * @return ID to remove.
     */
    @CheckReturnValue
    long cache(long id);
    
    @Nonnull
    @CheckReturnValue
    static EvictionStrategy leastRecentlyUsed(@Nonnegative int size) {
        if (size < 1)
            throw new IllegalArgumentException("Size must be at least 1");

        return new EvictionStrategy() {
            private final long[] ids = new long[size];
            private int index;
            
            @Override
            public long cache(long id) {
                var idx = (index = inc(index, size));
                var old = ids[idx];
                ids[idx] = id;
                return old;
            }
        };
    }
    
    private static int inc(int i, int modulus) {
        if (++i >= modulus) i = 0;
        return i;
    }
}
