package net.kodehawa.mantarobot.db;

import com.rethinkdb.gen.exc.ReqlDriverError;
import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Cursor;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.db.entities.*;
import net.kodehawa.mantarobot.db.redis.RedisCache;

import java.util.List;

import static com.rethinkdb.RethinkDB.r;

public class ManagedDatabase {
    private final RedisCache cache;
    private final Connection conn;

    public ManagedDatabase(Connection conn, RedisCache cache) {
        this.conn = conn;
        this.cache = cache;
    }

    public CustomCommand getCustomCommand(String guildId, String name) {
        String key = guildId + ":" + name;
        return cache.getOrElse(key, () -> r.table(CustomCommand.DB_TABLE).get(guildId + ":" + name).run(conn, CustomCommand.class));
    }

    public CustomCommand getCustomCommand(Guild guild, String name) {
        return getCustomCommand(guild.getId(), name);
    }

    public CustomCommand getCustomCommand(DBGuild guild, String name) {
        return getCustomCommand(guild.getId(), name);
    }

    public CustomCommand getCustomCommand(GuildMessageReceivedEvent event, String cmd) {
        return getCustomCommand(event.getGuild(), cmd);
    }

    //no way to optimize this ¯\_(ツ)_/¯
    public List<CustomCommand> getCustomCommands() {
        Cursor<CustomCommand> c = r.table(CustomCommand.DB_TABLE).run(conn, CustomCommand.class);
        return c.toList();
    }

    //no way to optimize this without a full cc rewrite ¯\_(ツ)_/¯
    public List<CustomCommand> getCustomCommands(String guildId) {
        String pattern = '^' + guildId + ':';
        Cursor<CustomCommand> c = r.table(CustomCommand.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, CustomCommand.class);
        return c.toList();
    }

    public List<CustomCommand> getCustomCommands(Guild guild) {
        return getCustomCommands(guild.getId());
    }

    public List<CustomCommand> getCustomCommands(DBGuild guild) {
        return getCustomCommands(guild.getId());
    }

    //same thing
    public List<CustomCommand> getCustomCommandsByName(String name) {
        String pattern = ':' + name + '$';
        Cursor<CustomCommand> c = r.table(CustomCommand.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, CustomCommand.class);
        return c.toList();
    }

    public DBGuild getGuild(String guildId) {
        if(guildId == null) return null;
        return cache.getOrElse(guildId, () -> {
            DBGuild guild = r.table(DBGuild.DB_TABLE).get(guildId).run(conn, DBGuild.class);
            return guild == null ? DBGuild.of(guildId) : guild;
        });
    }

    public DBGuild getGuild(Guild guild) {
        return getGuild(guild.getId());
    }

    public DBGuild getGuild(Member member) {
        return getGuild(member.getGuild());
    }

    public DBGuild getGuild(GuildMessageReceivedEvent event) {
        return getGuild(event.getGuild());
    }

    public MantaroObj getMantaroData() {
        try {
            MantaroObj obj = r.table(MantaroObj.DB_TABLE).get("mantaro").run(conn, MantaroObj.class);
            MantaroObj o = obj == null ? MantaroObj.create() : obj;
            cache.set("mantaroobj", o);
            return o;
        } catch(ReqlDriverError e) {
            return cache.getOrElse("mantaroobj", MantaroObj::create);
        }
    }

    public Player getPlayer(String userId) {
        if(userId == null) return null;
        String id = userId + ":g";
        return cache.getOrElse(id, () -> {
            Player player = r.table(Player.DB_TABLE).get(id).run(conn, Player.class);
            return player == null ? Player.of(userId) : player;
        });
    }

    public Player getPlayer(User user) {
        return getPlayer(user.getId());
    }

    public Player getPlayer(Member member) {
        return getPlayer(member.getUser());
    }

    public List<Player> getPlayers() {
        String pattern = ":g$";
        Cursor<Player> c = r.table(Player.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, Player.class);
        return c.toList();
    }

    public List<PremiumKey> getPremiumKeys() {
        Cursor<PremiumKey> c = r.table(PremiumKey.DB_TABLE).run(conn, PremiumKey.class);
        return c.toList();
    }

    //Also tests if the key is valid or not!
    public PremiumKey getPremiumKey(String id){
        if(id == null) return null;
        return cache.getOrElse(id, () -> r.table(PremiumKey.DB_TABLE).get(id).run(conn, PremiumKey.class));
    }

    public List<Quote> getQuotes(String guildId) {
        String pattern = '^' + guildId + ':';
        Cursor<Quote> c = r.table(Quote.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, Quote.class);
        return c.toList();
    }

    public List<Quote> getQuotes(Guild guild) {
        return getQuotes(guild.getId());
    }

    public List<Quote> getQuotes(DBGuild guild) {
        return getQuotes(guild.getId());
    }

    public DBUser getUser(String userId) {
        if(userId == null) return null;
        return cache.getOrElse(userId, () -> {
            DBUser user = r.table(DBUser.DB_TABLE).get(userId).run(conn, DBUser.class);
            return user == null ? DBUser.of(userId) : user;
        });
    }

    public DBUser getUser(User user) {
        return getUser(user.getId());
    }

    public DBUser getUser(Member member) {
        return getUser(member.getUser());
    }
}
