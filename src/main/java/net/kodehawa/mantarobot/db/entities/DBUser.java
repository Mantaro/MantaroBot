package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.db.redis.Input;
import net.kodehawa.mantarobot.db.redis.Output;

import java.beans.ConstructorProperties;

import static com.rethinkdb.RethinkDB.r;
import static java.lang.System.currentTimeMillis;
import static net.kodehawa.mantarobot.data.MantaroData.cache;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

@Getter
@ToString
@EqualsAndHashCode
public class DBUser implements ManagedObject {
    public static final String DB_TABLE = "users";
    private UserData data;
    private String id;
    private long premiumUntil;

    @ConstructorProperties({"id", "premiumUntil", "data"})
    public DBUser(String id, long premiumUntil, UserData data) {
        this.id = id;
        this.premiumUntil = premiumUntil;
        this.data = data;
    }

    @JsonIgnore
    public DBUser() {

    }

    public static DBUser of(String id) {
        return new DBUser(id, 0, new UserData());
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
        out.writeLong(premiumUntil);
        data.write(out);
    }

    @Override
    public void read(Input in) {
        id = String.valueOf(in.readLong());
        premiumUntil = in.readLong();
        data = new UserData();
        data.read(in);
    }

    @JsonIgnore
    public long getPremiumLeft() {
        return isPremium() ? this.premiumUntil - currentTimeMillis() : 0;
    }

    public User getUser(JDA jda) {
        return jda.getUserById(getId());
    }

    public DBUser incrementPremium(long milliseconds) {
        if(isPremium()) {
            this.premiumUntil += milliseconds;
        } else {
            this.premiumUntil = currentTimeMillis() + milliseconds;
        }
        return this;
    }

    @JsonIgnore
    public boolean isPremium() {
        return currentTimeMillis() < premiumUntil;
    }
}
