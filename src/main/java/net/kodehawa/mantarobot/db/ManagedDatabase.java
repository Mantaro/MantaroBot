package net.kodehawa.mantarobot.db;

import br.com.brjdevs.java.snowflakes.Snowflakes;
import br.com.brjdevs.java.snowflakes.entities.Config;
import br.com.brjdevs.java.snowflakes.entities.Worker;
import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Cursor;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.dataporter.oldentities.*;
import net.kodehawa.mantarobot.db.entities.PremiumKey;

import java.util.List;

import static com.rethinkdb.RethinkDB.r;

public class ManagedDatabase {
    public static final Config MANTARO_FACTORY = Snowflakes.config(1495900000L, 2L, 2L, 12L);
    public static final Worker ID_WORKER = MANTARO_FACTORY.worker(0, 0), LOG_WORKER = MANTARO_FACTORY.worker(0, 2);

    private final Connection conn;
    private Guild guild;

    public ManagedDatabase(Connection conn) {
        this.conn = conn;
    }

    public OldCustomCommand getCustomCommand(String guildId, String name) {
        return r.table(OldCustomCommand.DB_TABLE).get(guildId + ":" + name).run(conn, OldCustomCommand.class);
    }

    public OldCustomCommand getCustomCommand(Guild guild, String name) {
        return getCustomCommand(guild.getId(), name);
    }

    public OldCustomCommand getCustomCommand(OldGuild guild, String name) {
        return getCustomCommand(guild.getId(), name);
    }

    public OldCustomCommand getCustomCommand(GuildMessageReceivedEvent event, String cmd) {
        return getCustomCommand(event.getGuild(), cmd);
    }

    public List<OldCustomCommand> getCustomCommands() {
        Cursor<OldCustomCommand> c = r.table(OldCustomCommand.DB_TABLE).run(conn, OldCustomCommand.class);
        return c.toList();
    }

    public List<OldCustomCommand> getCustomCommands(String guildId) {
        String pattern = '^' + guildId + ':';
        Cursor<OldCustomCommand> c = r.table(OldCustomCommand.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, OldCustomCommand.class);
        return c.toList();
    }

    public List<OldCustomCommand> getCustomCommands(Guild guild) {
        return getCustomCommands(guild.getId());
    }

    public List<OldCustomCommand> getCustomCommands(OldGuild guild) {
        return getCustomCommands(guild.getId());
    }

    public List<OldCustomCommand> getCustomCommandsByName(String name) {
        String pattern = ':' + name + '$';
        Cursor<OldCustomCommand> c = r.table(OldCustomCommand.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, OldCustomCommand.class);
        return c.toList();
    }

    public OldGuild getGuild(String guildId) {
        OldGuild guild = r.table(OldGuild.DB_TABLE).get(guildId).run(conn, OldGuild.class);
        return guild == null ? OldGuild.of(guildId) : guild;
    }

    public OldGuild getGuild(Guild guild) {
        return getGuild(guild.getId());
    }

    public OldGuild getGuild(Member member) {
        return getGuild(member.getGuild());
    }

    public OldGuild getGuild(GuildMessageReceivedEvent event) {
        return getGuild(event.getGuild());
    }

    public OldMantaroObj getMantaroData() {
        OldMantaroObj obj = r.table(OldMantaroObj.DB_TABLE).get("mantaro").run(conn, OldMantaroObj.class);
        return obj == null ? OldMantaroObj.create() : obj;
    }

    public OldPlayer getPlayer(String userId) {
        OldPlayer player = r.table(OldPlayer.DB_TABLE).get(userId + ":g").run(conn, OldPlayer.class);
        return player == null ? OldPlayer.of(userId) : player;
    }

    public OldPlayer getPlayer(User user) {
        return getPlayer(user.getId());
    }

    public OldPlayer getPlayer(Member member) {
        return getPlayer(member.getUser());
    }

    public List<OldPlayer> getPlayers() {
        String pattern = ":g$";
        Cursor<OldPlayer> c = r.table(OldPlayer.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, OldPlayer.class);
        return c.toList();
    }

    public List<PremiumKey> getPremiumKeys() {
        Cursor<PremiumKey> c = r.table(PremiumKey.DB_TABLE).run(conn, PremiumKey.class);
        return c.toList();
    }

    public List<OldQuote> getQuotes(String guildId) {
        String pattern = '^' + guildId + ':';
        Cursor<OldQuote> c = r.table(OldQuote.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, OldQuote.class);
        return c.toList();
    }

    public List<OldQuote> getQuotes(Guild guild) {
        return getQuotes(guild.getId());
    }

    public List<OldQuote> getQuotes(OldGuild guild) {
        return getQuotes(guild.getId());
    }

    public OldUser getUser(String userId) {
        OldUser user = r.table(OldUser.DB_TABLE).get(userId).run(conn, OldUser.class);
        return user == null ? OldUser.of(userId) : user;
    }

    public OldUser getUser(User user) {
        return getUser(user.getId());
    }

    public OldUser getUser(Member member) {
        return getUser(member.getUser());
    }
}
