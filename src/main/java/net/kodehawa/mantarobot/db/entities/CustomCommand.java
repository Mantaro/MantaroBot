package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.redis.Input;
import net.kodehawa.mantarobot.db.redis.Output;
import net.kodehawa.mantarobot.utils.URLEncoding;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.stream.Collectors;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.data.MantaroData.cache;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

@Getter
public class CustomCommand implements ManagedObject {
    public static final String DB_TABLE = "commands";
    private String id;
    private List<String> values;

    @ConstructorProperties({"id", "values"})
    public CustomCommand(String id, List<String> values) {
        this.id = id;
        this.values = values.stream().map(URLEncoding::decode).collect(Collectors.toList());
    }

    @JsonIgnore
    public CustomCommand() {

    }

    public static CustomCommand of(String guildId, String cmdName, List<String> responses) {
        return new CustomCommand(guildId + ":" + cmdName, responses.stream().map(URLEncoding::encode).collect(Collectors.toList()));
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
        out.writeUTF(id);
        out.writeCollection(values, Output::writeUTF);
    }

    @Override
    public void read(Input in) {
        id = in.readUTF();
        values = in.readList(Input::readUTF);
    }

    @JsonProperty("values")
    public List<String> encodedValues() {
        return values.stream().map(URLEncoding::encode).collect(Collectors.toList());
    }

    @JsonIgnore
    public String getGuildId() {
        return getId().split(":", 2)[0];
    }

    @JsonIgnore
    public String getName() {
        return getId().split(":", 2)[1];
    }

    @JsonIgnore
    public List<String> getValues() {
        return values;
    }
}
