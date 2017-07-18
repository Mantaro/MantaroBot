package net.kodehawa.mantarobot.commands.custom;

import java.util.Iterator;
import java.util.function.Consumer;

public abstract class IteratorAdapter<T, U> implements Iterator<T> {
    private final Iterator<U> original;

    protected IteratorAdapter(Iterator<U> original) {
        this.original = original;
    }

    protected abstract T wrap(U u);

    @Override
    public boolean hasNext() {
        return original.hasNext();
    }

    @Override
    public T next() {
        return wrap(original.next());
    }

    @Override
    public void remove() {
        original.remove();
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        original.forEachRemaining(u -> action.accept(wrap(u)));
    }
}
