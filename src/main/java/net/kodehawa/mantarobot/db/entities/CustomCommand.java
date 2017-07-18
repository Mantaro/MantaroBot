package net.kodehawa.mantarobot.db.entities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.entities.helpers.CustomCommandList;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

/**
 * <p>A Custom Command Object.</p>
 * <p><b>DON'T USE</b> {@link CustomCommand#getValues()}. Use {@link CustomCommand#values()} instead.</p>
 */
@Getter
@ToString
@RequiredArgsConstructor
public class CustomCommand implements ManagedObject {
    public static final String DB_TABLE = "commands";
    private final Set<String> authors;
    private final String id, commandName, guildId;
    private final List<String> values;

    public CustomCommand(String name, String guildId) {
        this.id = String.valueOf(ManagedDatabase.ID_WORKER.generate());

        this.commandName = name;
        this.guildId = guildId;

        this.authors = new HashSet<>();
        this.values = new LinkedList<>();
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

    public List<String> values() {
        return new CustomCommandList(values);
    }
}
