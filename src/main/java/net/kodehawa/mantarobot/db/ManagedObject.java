package net.kodehawa.mantarobot.db;

import net.dv8tion.jda.core.utils.tuple.Pair;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.*;
import net.kodehawa.mantarobot.db.redis.Input;
import net.kodehawa.mantarobot.db.redis.Output;
import net.kodehawa.mantarobot.utils.Utils;

import java.util.Base64;
import java.util.Map;
import java.util.function.Supplier;

public interface ManagedObject {
    Map<Class<? extends ManagedObject>, Pair<Integer, Supplier<ManagedObject>>> ids = Utils.map(
            CustomCommand.class, Pair.of(1, (Supplier) CustomCommand::new),
            DBGuild.class, Pair.of(2, (Supplier) DBGuild::new),
            DBUser.class, Pair.of(3, (Supplier) DBUser::new),
            Player.class, Pair.of(4, (Supplier) Player::new),
            PremiumKey.class, Pair.of(5, (Supplier) PremiumKey::new),
            Quote.class, Pair.of(6, (Supplier) Quote::new),
            MantaroObj.class, Pair.of(7, (Supplier) MantaroObj::new)
    );

    static ManagedObject fromBase64(String b64) {
        Input in = new Input(Base64.getDecoder().decode(b64));
        int i = in.readUnsignedByte();
        ManagedObject o = null;
        for(Pair<Integer, Supplier<ManagedObject>> p : ids.values()) {
            if(p.getLeft() == i) {
                o = p.getRight().get();
                break;
            }
        }
        if(o == null) throw new IllegalStateException("Entity with id " + i + " not registered in the id map");
        o.read(in);
        return o;
    }

    void delete();

    void save();

    void write(Output out);

    void read(Input in);

    default void deleteAsync() {
        MantaroData.queue(this::delete);
    }

    default void saveAsync() {
        MantaroData.queue(this::save);
    }

    default String toBase64() {
        Output out = new Output();
        Pair<Integer, ?> p = ids.get(getClass());
        if(p == null) {
            throw new IllegalStateException("Entity " + getClass().getName() + " not registered in the id map");
        }
        out.writeByte(p.getLeft());
        write(out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }
}
