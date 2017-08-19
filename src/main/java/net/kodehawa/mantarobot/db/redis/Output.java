package net.kodehawa.mantarobot.db.redis;

import com.google.common.io.ByteArrayDataOutput;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

public class Output implements ByteArrayDataOutput {
    private final ByteArrayOutputStream byteArrayOutputSteam;
    private final DataOutput output;

    public Output() {
        this.byteArrayOutputSteam = new ByteArrayOutputStream();
        output = new DataOutputStream(byteArrayOutputSteam);
    }

    public <T> void writeCollection(Collection<T> collection, BiConsumer<Output, T> consumer) {
        writeInt(collection.size());
        //System.out.println(Thread.currentThread().getStackTrace()[2] + " -> writeCollection -> " + collection.size());
        collection.forEach(e -> consumer.accept(this, e));
    }

    public <K, V> void writeMap(Map<K, V> map, BiConsumer<Output, K> keyConsumer, BiConsumer<Output, V> valueConsumer) {
        writeInt(map.size());
        //System.out.println(Thread.currentThread().getStackTrace()[2] + " -> writeMap -> " + map.size());
        map.forEach((k, v) -> {
            keyConsumer.accept(this, k);
            valueConsumer.accept(this, v);
        });
    }

    public void writeUTF(String s, boolean emptyIfNull) {
        writeUTF(emptyIfNull && s == null ? "" : s);
    }

    public void writeLong(Long l, boolean minusOneIfNull) {
        writeLong(l == null ? -1 : l);
    }

    @Override
    public void write(int b) {
        try {
            output.write(b);
        } catch(IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    @Override
    public void write(@Nonnull byte[] b) {
        try {
            output.write(b);
        } catch(IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    @Override
    public void write(@Nonnull byte[] b, int off, int len) {
        try {
            output.write(b, off, len);
        } catch(IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    @Override
    public void writeBoolean(boolean v) {
        try {
            output.writeBoolean(v);
        } catch(IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    @Override
    public void writeByte(int v) {
        try {
            output.writeByte(v);
        } catch(IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    @Override
    @Deprecated
    public void writeBytes(@Nonnull String s) {
        try {
            output.writeBytes(s);
        } catch(IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    @Override
    public void writeChar(int v) {
        try {
            output.writeChar(v);
        } catch(IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    @Override
    public void writeChars(@Nonnull String s) {
        try {
            output.writeChars(s);
        } catch(IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    @Override
    public void writeDouble(double v) {
        try {
            output.writeDouble(v);
        } catch(IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    @Override
    public void writeFloat(float v) {
        try {
            output.writeFloat(v);
        } catch(IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    @Override
    public void writeInt(int v) {
        try {
            output.writeInt(v);
        } catch(IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    @Override
    public void writeLong(long v) {
        try {
            output.writeLong(v);
        } catch(IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    @Override
    public void writeShort(int v) {
        try {
            output.writeShort(v);
        } catch(IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    @Override
    public void writeUTF(@Nonnull String s) {
        try {
            output.writeUTF(s);
        } catch(IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    @Override
    public byte[] toByteArray() {
        return byteArrayOutputSteam.toByteArray();
    }
}
