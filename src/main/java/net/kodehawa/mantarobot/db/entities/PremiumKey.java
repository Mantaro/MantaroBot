package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.redis.Input;
import net.kodehawa.mantarobot.db.redis.Output;

import java.beans.ConstructorProperties;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.data.MantaroData.cache;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

@Getter
public class PremiumKey implements ManagedObject {
    public static final String DB_TABLE = "keys";
    private long duration;
    private long expiration;
    private String id;

    @ConstructorProperties({"id", "duration", "expiration"})
    public PremiumKey(String id, long duration, long expiration) {
        this.id = id;
        this.duration = duration;
        this.expiration = expiration;
    }

    @JsonIgnore
    public PremiumKey() {

    }

    @Override
    public void delete() {
        r.table(DB_TABLE).get(id).delete().runNoReply(conn());
        cache().invalidate(id);
    }

    @Override
    public void save() {
        cache().set(id, this);
        r.table(DB_TABLE).insert(this)
                .optArg("conflict", "replace")
                .runNoReply(conn());
    }

    @Override
    public void write(Output out) {
        out.writeLong(Long.parseLong(id));
        out.writeLong(duration);
        out.writeLong(expiration);
    }

    @Override
    public void read(Input in) {
        id = String.valueOf(in.readLong());
        duration = in.readLong();
        expiration = in.readLong();
    }
}
