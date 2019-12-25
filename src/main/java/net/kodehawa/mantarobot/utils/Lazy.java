package net.kodehawa.mantarobot.utils;

import java.util.function.Supplier;

public class Lazy<T> implements Supplier<T> {
    private final Supplier<T> supplier;
    private T value;
    private boolean set;
    
    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }
    
    public T get() {
        if(set) {
            return value;
        }
        synchronized(this) {
            if(set) return value;
            T v = supplier.get();
            value = v;
            set = true;
            return v;
        }
    }
}
