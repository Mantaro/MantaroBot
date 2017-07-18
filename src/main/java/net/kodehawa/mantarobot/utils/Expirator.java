package net.kodehawa.mantarobot.utils;

import br.com.brjdevs.java.utils.async.Async;
import com.google.common.primitives.Longs;
import gnu.trove.TCollections;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import lombok.Getter;
import net.kodehawa.mantarobot.utils.Expirator.Expirable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//TODO Isn't this a bit overcomplicated? Need less overhead pls.
public class Expirator<T extends Expirable> {
    @Getter
    private final Set<Expirable> expirables = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final TLongObjectMap<Set<Expirable>> expirations = TCollections.synchronizedMap(new TLongObjectHashMap<>());
    private volatile boolean updated = false;
    public Expirator() {
        Thread thread = new Thread(this::threadcode, "ExpirationManager Thread");
        thread.setDaemon(true);
        thread.start();
    }

    public void put(long milis, Expirable expirable) {
        Objects.requireNonNull(expirable);

        synchronized(expirations) {
            if(expirables.contains(expirable)) {
                expirations.valueCollection().forEach(list -> list.remove(expirable));
                expirables.remove(expirable);
            }

            if(!expirations.containsKey(milis)) expirations.put(milis, new LinkedHashSet<>());

            expirations.get(milis).add(expirable);
            expirables.add(expirable);
        }

        updated = true;
        synchronized(this) {
            notify();
        }
    }

    public void putRelative(long millisToAwait, Expirable expirable) {
        put(System.currentTimeMillis() + millisToAwait, expirable);
    }

    public void remove(Expirable expirable) {
        Objects.requireNonNull(expirable);

        synchronized(expirations) {
            if(expirables.contains(expirable)) {
                expirations.valueCollection().forEach(list -> list.remove(expirable));
                expirables.remove(expirable);
            }
        }

        updated = true;
        synchronized(this) {
            notify();
        }
    }

    private void threadcode() {
        //noinspection InfiniteLoopStatement
        while(true) {
            if(expirations.isEmpty()) {
                try {
                    synchronized(this) {
                        wait();
                        updated = false;
                    }
                } catch(InterruptedException ignored) {
                }
            }

            //noinspection OptionalGetWithoutIsPresent

            long firstEntry = Longs.min(expirations.keys());

            long timeout = firstEntry - System.currentTimeMillis();
            if(timeout > 0) {
                synchronized(this) {
                    try {
                        wait(timeout);
                    } catch(InterruptedException ignored) {
                    }
                }
            }

            if(!updated) {
                Set<Expirable> firstExpirables = expirations.remove(firstEntry);
                firstExpirables.remove(null);
                firstExpirables.forEach(expirable -> Async.thread("Expiration Executable", expirable::onExpire));
            } else updated = false; //and the loop will restart and resolve it
        }
    }

    public interface Expirable {
        static Expirable asExpirable(Runnable runnable) {
            return runnable::run;
        }

        void onExpire();
    }
}