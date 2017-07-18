package net.kodehawa.mantarobot.db.entities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.ManagedObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

@Getter
@ToString
@RequiredArgsConstructor
public class Marriage implements ManagedObject {
    public static final String DB_TABLE = "marriages";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
    private final String id;
    private final long since;
    private final String user1, user2;

    public Marriage(User user1, User user2) {
        this(user1.getId(), user2.getId());
    }

    public Marriage(String user1, String user2) {
        this.id = String.valueOf(ManagedDatabase.ID_WORKER.generate());
        this.user1 = user1;
        this.user2 = user2;
        this.since = System.currentTimeMillis();
    }

    //TODO REMOVE THIS AFTER DATAPORT
    public Marriage(String user1, String user2, long since) {
        this.id = String.valueOf(ManagedDatabase.ID_WORKER.generate());
        this.user1 = user1;
        this.user2 = user2;
        this.since = since;
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

    public String anniversary() {
        Calendar anniversary = new GregorianCalendar(), today = new GregorianCalendar();
        anniversary.setTime(new Date(marriedSince()));
        anniversary.set(Calendar.YEAR, today.get(Calendar.YEAR));
        if(today.compareTo(anniversary) > 0)
            anniversary.add(Calendar.YEAR, 1);
        return DATE_FORMAT.format(anniversary.getTime());
    }

    public void divorce() {
        delete();
    }

    public long marriedSince() {
        return ManagedDatabase.MANTARO_FACTORY.creationTime(Long.parseLong(id));
    }

    public String marryDate() {
        return DATE_FORMAT.format(new Date(marriedSince()));
    }
}
