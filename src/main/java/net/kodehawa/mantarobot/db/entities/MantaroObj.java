package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.redis.Input;
import net.kodehawa.mantarobot.db.redis.Output;
import org.apache.commons.lang3.tuple.Pair;

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.data.MantaroData.cache;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

@Data
public class MantaroObj implements ManagedObject {
    public static final String DB_TABLE = "mantaro";
    public final String id = "mantaro";
    public List<String> blackListedGuilds = null;
    public List<String> blackListedUsers = null;
    public List<String> patreonUsers = null;
    private Map<Long, Pair<String, Long>> mutes = null;
    private Map<String, Long> tempBans = null;

    @ConstructorProperties({"blackListedGuilds", "blackListedUsers", "patreonUsers", "tempbans", "mutes"})
    public MantaroObj(List<String> blackListedGuilds, List<String> blackListedUsers, List<String> patreonUsers, Map<String, Long> tempBans, Map<Long, Pair<String, Long>> mutes) {
        this.blackListedGuilds = blackListedGuilds;
        this.blackListedUsers = blackListedUsers;
        this.patreonUsers = patreonUsers;
        this.tempBans = tempBans;
        this.mutes = mutes;
    }

    @JsonIgnore
    public MantaroObj() {

    }

    public static MantaroObj create() {
        return new MantaroObj(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new HashMap<>());
    }

    @Override
    public void delete() {
        cache().invalidate("mantaroobj");
        r.table(DB_TABLE).get(id).delete().runNoReply(conn());
    }

    @Override
    public void save() {
        cache().set("mantaroobj", this);
        r.table(DB_TABLE).insert(this)
                .optArg("conflict", "replace")
                .runNoReply(conn());
    }

    @Override
    public void write(Output out) {
        out.writeCollection(blackListedGuilds, Output::writeUTF);
        out.writeCollection(blackListedUsers, Output::writeUTF);
        out.writeCollection(patreonUsers, Output::writeUTF);
        out.writeMap(tempBans, Output::writeUTF, Output::writeLong);
        out.writeMap(mutes, Output::writeLong, (out2, pair) -> {
            out2.writeUTF(pair.getLeft());
            out2.writeLong(pair.getRight());
        });
    }

    @Override
    public void read(Input in) {
        blackListedGuilds = in.readList(Input::readUTF);
        blackListedUsers = in.readList(Input::readUTF);
        patreonUsers = in.readList(Input::readUTF);
        tempBans = in.readMap(Input::readUTF, Input::readLong);
        mutes = in.readMap(Input::readLong, in2 ->
                Pair.of(in2.readUTF(), in2.readLong())
        );
    }
}
