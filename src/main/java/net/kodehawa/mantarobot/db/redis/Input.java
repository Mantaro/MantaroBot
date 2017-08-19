package net.kodehawa.mantarobot.db.redis;

import com.google.common.io.ByteArrayDataInput;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class Input implements ByteArrayDataInput {
    private final DataInput input;

    public Input(byte[] array) {
        this.input = new DataInputStream(new ByteArrayInputStream(array));
    }

    public <T> ArrayList<T> readList(Function<Input, T> function) {
        return readCollection(ArrayList::new, function);
    }

    public <T> HashSet<T> readSet(Function<Input, T> function) {
        return readCollection(HashSet::new, function);
    }

    public <T, C extends Collection<T>> C readCollection(Supplier<C> supplier, Function<Input, T> function) {
        int size = readInt();
        //System.out.println(Thread.currentThread().getStackTrace()[3] + " -> readCollection -> " + size);
        C collection = supplier.get();
        for(int i = 0; i < size; i++) {
            collection.add(function.apply(this));
        }
        return collection;
    }

    public <K, V> HashMap<K, V> readMap(Function<Input, K> keyFunction, Function<Input, V> valueFunction) {
        return readMap(HashMap::new, keyFunction, valueFunction);
    }

    public <K, V, M extends Map<K, V>> M readMap(Supplier<M> supplier, Function<Input, K> keyFunction, Function<Input, V> valueFunction) {
        int size = readInt();
        //System.out.println(Thread.currentThread().getStackTrace()[3] + " -> readMap -> " + size);
        M map = supplier.get();
        for(int i = 0; i < size; i++) {
            map.put(keyFunction.apply(this), valueFunction.apply(this));
        }
        return map;
    }

    public String readUTF(boolean nullIfEmpty) {
        String s = readUTF();
        return nullIfEmpty && s.isEmpty() ? null : s;
    }

    public Long readLong(boolean nullIfMinusOne) {
        long l = readLong();
        return nullIfMinusOne && l == -1 ? null : l;
    }

    @Override
    public void readFully(@Nonnull byte[] b) {
        try {
            input.readFully(b);
        } catch(IOException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void readFully(@Nonnull byte[] b, int off, int len) {
        try {
            input.readFully(b, off, len);
        } catch(IOException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public int skipBytes(int n) {
        try {
            return input.skipBytes(n);
        } catch(IOException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean readBoolean() {
        try {
            return input.readBoolean();
        } catch(IOException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public byte readByte() {
        try {
            return input.readByte();
        } catch(IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    @Override
    public int readUnsignedByte() {
        try {
            return input.readUnsignedByte();
        } catch(IOException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public short readShort() {
        try {
            return input.readShort();
        } catch(IOException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public int readUnsignedShort() {
        try {
            return input.readUnsignedShort();
        } catch(IOException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public char readChar() {
        try {
            return input.readChar();
        } catch(IOException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public int readInt() {
        try {
            return input.readInt();
        } catch(IOException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public long readLong() {
        try {
            return input.readLong();
        } catch(IOException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public float readFloat() {
        try {
            return input.readFloat();
        } catch(IOException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public double readDouble() {
        try {
            return input.readDouble();
        } catch(IOException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public String readLine() {
        try {
            return input.readLine();
        } catch(IOException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public @Nonnull
    String readUTF() {
        try {
            return input.readUTF();
        } catch(IOException e) {
            throw new AssertionError(e);
        }
    }
}
