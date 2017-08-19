package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
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
@ToString
@EqualsAndHashCode
public class DBGuild implements ManagedObject {
    public static final String DB_TABLE = "guilds";
    private GuildData data;
    private String id;
    private long premiumUntil;

    @ConstructorProperties({"id", "premiumUntil", "data"})
    public DBGuild(String id, long premiumUntil, GuildData data) {
        this.id = id;
        this.premiumUntil = premiumUntil;
        this.data = data;
    }

    @JsonIgnore
    public DBGuild() {

    }

    public static DBGuild of(String id) {
        return new DBGuild(id, 0, new GuildData());
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
        data = new GuildData();
        data.read(in);
    }

    public Guild getGuild(JDA jda) {
        return jda.getGuildById(getId());
    }

    @JsonIgnore
    public long getPremiumLeft() {
        return isPremium() ? this.premiumUntil - currentTimeMillis() : 0;
    }

    public DBGuild incrementPremium(long milliseconds) {
        if(isPremium()) {
            this.premiumUntil += milliseconds;
        } else {
            this.premiumUntil = currentTimeMillis() + milliseconds;
        }
        return this;
    }

    @JsonIgnore
    public boolean isPremium() {
        PremiumKey key = MantaroData.db().getPremiumKey(data.getPremiumKey());
        return currentTimeMillis() < premiumUntil || (key != null && currentTimeMillis() < key.getExpiration() && key.getParsedType().equals(PremiumKey.Type.GUILD));
    }

    @JsonIgnore
    public PremiumKey generateAndApplyPremiumKey(int days){
        String premiumId = UUID.randomUUID().toString();
        PremiumKey newKey = new PremiumKey(premiumId, TimeUnit.DAYS.toMillis(days),
                currentTimeMillis() + TimeUnit.DAYS.toMillis(days), PremiumKey.Type.GUILD, true, id);
        data.setPremiumKey(premiumId);
        newKey.save();
        save();
        return newKey;
    }
}
