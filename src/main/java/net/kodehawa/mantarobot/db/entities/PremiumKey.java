package net.kodehawa.mantarobot.db.entities;

import lombok.Getter;
import net.kodehawa.mantarobot.db.ManagedObject;

import java.beans.ConstructorProperties;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

@Getter
public class PremiumKey implements ManagedObject {
    public static final String DB_TABLE = "keys";
    private final long duration;
    private final long expiration;
    private final String id;

    @ConstructorProperties({"id", "duration", "expiration"})
    public PremiumKey(String id, long duration, long expiration) {
        this.id = id;
        this.duration = duration;
        this.expiration = expiration;
    }

    @Override
    public void delete() {
        r.table(DB_TABLE).get(getId()).delete().runNoReply(conn());
    }

    @Override
    public void save() {
        r.table(DB_TABLE).insert(this)
                .optArg("conflict", "replace")
                .runNoReply(conn());
    }
}
