package net.kodehawa.dataporter;

import com.rethinkdb.model.MapObject;
import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Cursor;
import net.kodehawa.dataporter.oldentities.*;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.*;
import net.kodehawa.mantarobot.db.entities.logging.CommandLog;
import net.kodehawa.mantarobot.db.entities.logging.PlayedSong;
import org.apache.commons.collections4.BidiMap;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static br.com.brjdevs.java.utils.texts.StringUtils.parse;
import static com.rethinkdb.RethinkDB.r;

public class MantaroDataPorter {
    public static void log(String s) {
        System.out.println(s);
    }

    public static void logln() {
        System.out.println();
    }

    public static void main(String[] args) {
        log("MantaroDataPorter for Database v4.x.x");
        logln();
        Map<String, Optional<String>> map = parse(args);

        //login
        String db = map.getOrDefault("d", Optional.empty()).orElse("mantaro"), oldDb = "old_" + db;

        log("Connecting to RethinkDB...");
        Connection c = connect(map, db);
        logln();

        log("Creating Tables and Indexes...");
        initDatabase(c, db, oldDb);
        logln();

        log("Converting Data...");
        convertMantaroObject(c, oldDb);
        convertCommands(c, oldDb);
        convertUsersAndPlayers(c, oldDb);
        convertGuilds(c, oldDb);
        convertQuotes(c, oldDb);
        logln();

        log("Dropping Old Database...");
        r.dbDrop(oldDb).run(c);
        logln();

        log("Databased Ported.");
        System.exit(0);
    }

    private static Connection connect(Map<String, Optional<String>> map, String db) {
        return r.connection()
                .hostname(map.getOrDefault("h", Optional.empty()).orElse("localhost"))
                .db(db)
                .connect();
    }

    private static void convertCommands(Connection c, String oldDb) {
        log("Converting Commands...");
        AtomicInteger i = new AtomicInteger();

        Cursor<OldCustomCommand> commands = r.db(oldDb).table(OldCustomCommand.DB_TABLE).run(c, OldCustomCommand.class);
        commands.forEach(command -> {
            i.getAndIncrement();
            CustomCommand newCommand = new CustomCommand(command.getGuildId(), command.getName());
            newCommand.values().addAll(command.getValues());
            newCommand.saveAsync();
        });

        log("Converted " + i.get() + " Commands.");
        logln();
    }

    private static void convertGuilds(Connection c, String oldDb) {
        log("Converting Guilds...");
        AtomicInteger i = new AtomicInteger();

        Cursor<OldGuild> guilds = r.db(oldDb).table(OldGuild.DB_TABLE).run(c, OldGuild.class);
        guilds.forEach(guild -> {
            i.getAndIncrement();
            GuildData newGuild = new GuildData(guild.getData(), guild.getId()); //We actually use the Jackson constructor bc we left the GuildData unchanged.
            newGuild.setPremiumUntil(guild.getPremiumUntil());
            newGuild.saveAsync();
        });

        log("Converted " + i.get() + " Guilds.");
        logln();
    }

    private static void convertMantaroObject(Connection c, String oldDb) {
        System.out.print("Converting MantaroObject...");
        AtomicInteger i = new AtomicInteger();

        OldMantaroObj obj = r.db(oldDb).table("mantaro").get("mantaro").run(c, OldMantaroObj.class);

        if(obj == null) {
            log(" SKIPPED.");
            logln();
            return;
        }

        MantaroObject mantaro = new MantaroObject();
        mantaro.getBlackListedGuilds().addAll(obj.getBlackListedGuilds());
        mantaro.getBlackListedUsers().addAll(obj.getBlackListedUsers());
        mantaro.getTempBans().putAll(obj.getTempBans());
        mantaro.saveAsync();

        log(" DONE.");
        logln();
    }

    private static void convertQuotes(Connection c, String oldDb) {
        log("Converting Quotes...");
        AtomicInteger i = new AtomicInteger();

        Cursor<OldQuote> quotes = r.db(oldDb).table(OldQuote.DB_TABLE).run(c, OldQuote.class);

        quotes.forEach(quote -> {
            i.getAndIncrement();
            new QuotedMessage(
                    quote.getChannelId(),
                    quote.getChannelName(),
                    quote.getContent(),
                    quote.getGuildId(),
                    quote.getGuildName(),
                    String.valueOf(ManagedDatabase.ID_WORKER.generate()),
                    quote.getUserAvatar(),
                    quote.getUserId(),
                    quote.getUserName()
            ).saveAsync();
        });

        log("Converted " + i.get() + " Quotes.");
        logln();
    }

    private static void convertUsersAndPlayers(Connection c, String oldDb) {
        log("Converting Users and Players...");
        AtomicInteger i = new AtomicInteger(), m = new AtomicInteger();

        Map<String, OldUser> users = new HashMap<>();
        Cursor<OldUser> userCursor = r.db(oldDb).table(OldUser.DB_TABLE).run(c, OldUser.class);
        userCursor.forEach(u -> users.put(u.getId(), u));

        Map<String, OldPlayer> players = new HashMap<>();
        Cursor<OldPlayer> playerCursor = r.db(oldDb).table(OldPlayer.DB_TABLE).run(c, OldPlayer.class);
        playerCursor.forEach(p -> players.put(p.getUserId(), p));

        Set<String> ids = new HashSet<>();
        ids.addAll(users.keySet());
        ids.addAll(players.keySet());

        BidiMap<String, String> marriages = new MirrorMap<>(new HashMap<>());

        ids.forEach(id -> {
            i.getAndIncrement();
            UserData userData = new UserData(id);

            if(users.containsKey(id)) {
                OldUser user = users.get(id);
                userData.setBirthday(user.getData().getBirthday());
                userData.setPremiumUntil(user.getPremiumUntil());
                userData.setTimezone(user.getData().getTimezone());
            }

            if(players.containsKey(id)) {
                OldPlayer player = players.get(id);
                userData.setMoney(player.getMoney());
                userData.setLevel(player.getLevel());
                userData.setReputation(player.getReputation());
                userData.getInventory().putAll(player.rawInventory());
                if(player.getData() != null) {
                    userData.setXp(player.getData().getExperience());
                    userData.setDescription(player.getData().getDescription());
                    String marriedWith = player.getData().getMarriedWith();
                    if(marriedWith != null) {
                        marriages.put(id, marriedWith);
                    }
                }
            }

            userData.saveAsync();
        });

        log("Created " + i.get() + " Users from " + users.size() + " Users and " + players.size() + " Players.");
        logln();
        log("Processing Marriages...");

        //Process Marriages
        Set<String> processed = new HashSet<>();

        marriages.forEach((user1, user2) -> {
            if(processed.contains(user1) || processed.contains(user2)) return;
            processed.add(user1);
            processed.add(user2);

            long since = 0;
            if(players.containsKey(user1)) {
                Long l = players.get(user1).getData().getMarriedSince();
                if(l != null && l != 0) {
                    since = l;
                }
            }

            if(since == 0 && players.containsKey(user2)) {
                Long l = players.get(user2).getData().getMarriedSince();
                if(l != null && l != 0) {
                    since = l;
                }
            }

            if(since == 0) since = 1499180400000L; //2017-07-04 00:00:00 JUST FOR THE LOLs

            m.getAndIncrement();
            new Marriage(user1, user2, since).saveAsync();
        });

        log("Created " + m.get() + " Marriages.");
        logln();
    }

    private static void initDatabase(Connection c, String db, String oldDb) {
        //renaming
        r.db(db).config().update(new MapObject<>("name", oldDb)).run(c);

        r.dbCreate(db).run(c);

        //tables
        r.tableCreate(CustomCommand.DB_TABLE).run(c);
        r.table(CustomCommand.DB_TABLE).indexCreate("guildId").run(c);
        r.table(CustomCommand.DB_TABLE).indexCreate("author", arg1 -> arg1.g("authors")).optArg("multi", true).run(c);

        r.tableCreate(QuotedMessage.DB_TABLE).run(c);
        r.table(QuotedMessage.DB_TABLE).indexCreate("guildId").run(c);

        r.tableCreate(Marriage.DB_TABLE).run(c); //new
        r.table(Marriage.DB_TABLE).indexCreate("users", row -> r.array(row.g("user1"), row.g("user2")))
                .optArg("multi", true).run(c);

        r.tableCreate(MantaroObject.DB_TABLE).run(c);
        r.tableCreate(UserData.DB_TABLE).run(c);
        r.tableCreate(GuildData.DB_TABLE).run(c);
        r.tableCreate(PremiumKey.DB_TABLE).run(c); //new
        r.tableCreate(CommandLog.DB_TABLE).run(c); //new
        r.tableCreate(PlayedSong.DB_TABLE).run(c); //new

        r.table(CustomCommand.DB_TABLE).indexWait("guildId", "author").run(c);
        r.table(QuotedMessage.DB_TABLE).indexWait("guildId").run(c);
        r.table(Marriage.DB_TABLE).indexWait("users").run(c);
    }
}
