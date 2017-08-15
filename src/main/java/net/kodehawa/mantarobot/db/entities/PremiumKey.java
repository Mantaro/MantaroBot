package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.redis.Input;
import net.kodehawa.mantarobot.db.redis.Output;

import java.beans.ConstructorProperties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.rethinkdb.RethinkDB.r;
import static java.lang.System.currentTimeMillis;
import static net.kodehawa.mantarobot.data.MantaroData.cache;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

@Getter
public class PremiumKey implements ManagedObject {
    public static final String DB_TABLE = "keys";
    private long duration;
    private long expiration;
    private String id;
    private int type;
    private boolean enabled;
    private String owner;

    public enum Type {
        MASTER, USER, GUILD
    }

    @ConstructorProperties({"id", "duration", "expiration", "type", "enabled", "owner"})
    public PremiumKey(String id, long duration, long expiration, Type type, boolean enabled, String owner) {
        this.id = id;
        this.duration = duration;
        this.expiration = expiration;
        this.type = type.ordinal();
        this.enabled = enabled;
        this.owner = owner;
    }

    @JsonIgnore
    public PremiumKey() {}

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
        out.writeUTF(id);
        out.writeLong(duration);
        out.writeLong(expiration);
        out.writeInt(type);
        out.writeBoolean(enabled);
        out.writeUTF(owner);
    }

    @Override
    public void read(Input in) {
        id = in.readUTF();
        duration = in.readLong();
        expiration = in.readLong();
        type = in.readInt();
        enabled = in.readBoolean();
        owner = in.readUTF();
    }

    @JsonIgnore
    public static PremiumKey generatePremiumKey(String owner, Type type){
        String premiumId = UUID.randomUUID().toString();
        PremiumKey newKey = new PremiumKey(premiumId, -1, -1, type, false, owner);
        newKey.save();
        return newKey;
    }

    @JsonIgnore
    public Type getParsedType(){
        return Type.values()[type];
    }

    @JsonIgnore
    public long getDurationDays(){
        return TimeUnit.MILLISECONDS.toDays(duration);
    }

    @JsonIgnore
    public long validFor(){
        return TimeUnit.MILLISECONDS.toDays(getExpiration() - currentTimeMillis());
    }

    @JsonIgnore
    public long validForMs(){
        return getExpiration() - currentTimeMillis();
    }

    @JsonIgnore
    public void activate(int days){
        this.enabled = true;
        this.duration = TimeUnit.DAYS.toMillis(days);
        this.expiration = currentTimeMillis() + TimeUnit.DAYS.toMillis(days);
        save();
    }
}
