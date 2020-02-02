/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.db;

import com.rethinkdb.model.OptArgs;
import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Cursor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.ExtraRuntimeOptions;
import net.kodehawa.mantarobot.commands.currency.seasons.Season;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.db.entities.CustomCommand;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.MantaroObj;
import net.kodehawa.mantarobot.db.entities.Marriage;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.PlayerStats;
import net.kodehawa.mantarobot.db.entities.PremiumKey;
import org.slf4j.Logger;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.rethinkdb.RethinkDB.r;

public class ManagedDatabase {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ManagedDatabase.class);
    private final Connection conn;
    
    public ManagedDatabase(@Nonnull Connection conn) {
        this.conn = conn;
    }
    
    private static void log(String message, Object... fmtArgs) {
        if(ExtraRuntimeOptions.LOG_DB_ACCESS) {
            log.info(message, fmtArgs);
        }
    }
    
    private static void log(String message) {
        if(ExtraRuntimeOptions.LOG_DB_ACCESS) {
            log.info(message);
        }
    }
    
    @Nullable
    @CheckReturnValue
    public CustomCommand getCustomCommand(@Nonnull String guildId, @Nonnull String name) {
        log("Requesting custom command {}:{} from rethink", guildId, name);
        return r.table(CustomCommand.DB_TABLE).get(guildId + ":" + name).run(conn, CustomCommand.class);
    }
    
    @Nullable
    @CheckReturnValue
    public CustomCommand getCustomCommand(@Nonnull Guild guild, @Nonnull String name) {
        return getCustomCommand(guild.getId(), name);
    }
    
    @Nullable
    @CheckReturnValue
    public CustomCommand getCustomCommand(@Nonnull DBGuild guild, @Nonnull String name) {
        return getCustomCommand(guild.getId(), name);
    }
    
    @Nullable
    @CheckReturnValue
    public CustomCommand getCustomCommand(@Nonnull GuildMessageReceivedEvent event, @Nonnull String cmd) {
        return getCustomCommand(event.getGuild(), cmd);
    }
    
    @Nonnull
    @CheckReturnValue
    public List<CustomCommand> getCustomCommands() {
        log("Requesting all custom commands from rethink");
        Cursor<CustomCommand> c = r.table(CustomCommand.DB_TABLE).run(conn, CustomCommand.class);
        return c.toList();
    }
    
    @Nonnull
    @CheckReturnValue
    public List<CustomCommand> getCustomCommands(@Nonnull String guildId) {
        log("Requesting all custom commands from guild {} from rethink", guildId);
        Cursor<CustomCommand> c = r.table(CustomCommand.DB_TABLE)
                                          .getAll(guildId)
                                          .optArg("index", "guild")
                                          .run(conn, CustomCommand.class);
        return c.toList();
    }
    
    @Nonnull
    @CheckReturnValue
    public List<CustomCommand> getCustomCommands(@Nonnull Guild guild) {
        return getCustomCommands(guild.getId());
    }
    
    @Nonnull
    @CheckReturnValue
    public List<CustomCommand> getCustomCommands(@Nonnull DBGuild guild) {
        return getCustomCommands(guild.getId());
    }
    
    @Nonnull
    @CheckReturnValue
    public List<CustomCommand> getCustomCommandsByName(@Nonnull String name) {
        log("Requesting all custom commands named {} from rethink", name);
        String pattern = ':' + name + '$';
        Cursor<CustomCommand> c = r.table(CustomCommand.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, CustomCommand.class);
        return c.toList();
    }
    
    @Nonnull
    @CheckReturnValue
    public DBGuild getGuild(@Nonnull String guildId) {
        log("Requesting guild {} from rethink", guildId);
        DBGuild guild = r.table(DBGuild.DB_TABLE).get(guildId).run(conn, DBGuild.class);
        return guild == null ? DBGuild.of(guildId) : guild;
    }
    
    @Nonnull
    @CheckReturnValue
    public DBGuild getGuild(@Nonnull Guild guild) {
        return getGuild(guild.getId());
    }
    
    @Nonnull
    @CheckReturnValue
    public DBGuild getGuild(@Nonnull Member member) {
        return getGuild(member.getGuild());
    }
    
    @Nonnull
    @CheckReturnValue
    public DBGuild getGuild(@Nonnull GuildMessageReceivedEvent event) {
        return getGuild(event.getGuild());
    }
    
    @Nonnull
    @CheckReturnValue
    public MantaroObj getMantaroData() {
        log("Requesting MantaroObj from rethink");
        MantaroObj obj = r.table(MantaroObj.DB_TABLE).get("mantaro").run(conn, MantaroObj.class);
        return obj == null ? MantaroObj.create() : obj;
    }
    
    @Nonnull
    @CheckReturnValue
    public Player getPlayer(@Nonnull String userId) {
        log("Requesting player {} from rethink", userId);
        Player player = r.table(Player.DB_TABLE).get(userId + ":g").run(conn, Player.class);
        return player == null ? Player.of(userId) : player;
    }
    
    @Nonnull
    @CheckReturnValue
    public Player getPlayer(@Nonnull User user) {
        return getPlayer(user.getId());
    }
    
    @Nonnull
    @CheckReturnValue
    public Player getPlayer(@Nonnull Member member) {
        return getPlayer(member.getUser());
    }
    
    @Nonnull
    @CheckReturnValue
    public SeasonPlayer getPlayerForSeason(@Nonnull String userId, Season season) {
        log("Requesting player {} (season {}) from rethink", userId, season);
        SeasonPlayer player = r.table(SeasonPlayer.DB_TABLE).get(userId + ":" + season).run(conn, SeasonPlayer.class);
        return player == null ? SeasonPlayer.of(userId, season) : player;
    }
    
    @Nonnull
    @CheckReturnValue
    public SeasonPlayer getPlayerForSeason(@Nonnull User user, Season season) {
        return getPlayerForSeason(user.getId(), season);
    }
    
    @Nonnull
    @CheckReturnValue
    public SeasonPlayer getPlayerForSeason(@Nonnull Member member, Season season) {
        return getPlayerForSeason(member.getUser(), season);
    }
    
    @Nonnull
    @CheckReturnValue
    public long getAmountSeasonalPlayers() {
        return r.table(SeasonPlayer.DB_TABLE).count().run(conn, OptArgs.of("read_mode", "outdated"));
    }
    
    @Nonnull
    @CheckReturnValue
    public PlayerStats getPlayerStats(@Nonnull String userId) {
        log("Requesting player STATS {} from rethink", userId);
        PlayerStats playerStats = r.table(PlayerStats.DB_TABLE).get(userId).run(conn, PlayerStats.class);
        return playerStats == null ? PlayerStats.of(userId) : playerStats;
    }
    
    @Nonnull
    @CheckReturnValue
    public PlayerStats getPlayerStats(@Nonnull User user) {
        return getPlayerStats(user.getId());
    }
    
    @Nonnull
    @CheckReturnValue
    public PlayerStats getPlayerStats(@Nonnull Member member) {
        return getPlayerStats(member.getUser());
    }
    
    @Nonnull
    @CheckReturnValue
    public List<Player> getPlayers() {
        log("Requesting all players from rethink");
        String pattern = ":g$";
        Cursor<Player> c = r.table(Player.DB_TABLE).filter(quote -> quote.g("id").match(pattern)).run(conn, Player.class);
        return c.toList();
    }
    
    //Can be null and it's perfectly valid.
    public Marriage getMarriage(String marriageId) {
        if(marriageId == null)
            return null;
        log("Requesting marriage {} from rethink", marriageId);
        return r.table(Marriage.DB_TABLE).get(marriageId).run(conn, Marriage.class);
    }
    
    @Nonnull
    @CheckReturnValue
    public List<Marriage> getMarriages() {
        log("Requesting all marriages from rethink");
        Cursor<Marriage> c = r.table(Marriage.DB_TABLE).run(conn, Marriage.class);
        return c.toList();
    }
    
    @Nonnull
    @CheckReturnValue
    public List<PremiumKey> getPremiumKeys() {
        log("Requesting all premium keys from rethink");
        Cursor<PremiumKey> c = r.table(PremiumKey.DB_TABLE).run(conn, PremiumKey.class);
        return c.toList();
    }
    
    //Also tests if the key is valid or not!
    @Nullable
    @CheckReturnValue
    public PremiumKey getPremiumKey(@Nullable String id) {
        log("Requesting premium key {} from rethink", id);
        if(id == null) return null;
        return r.table(PremiumKey.DB_TABLE).get(id).run(conn, PremiumKey.class);
    }
    
    @Nonnull
    @CheckReturnValue
    public DBUser getUser(@Nonnull String userId) {
        log("Requesting user {} from rethink", userId);
        DBUser user = r.table(DBUser.DB_TABLE).get(userId).run(conn, DBUser.class);
        return user == null ? DBUser.of(userId) : user;
    }
    
    @Nonnull
    @CheckReturnValue
    public DBUser getUser(@Nonnull User user) {
        return getUser(user.getId());
    }
    
    @Nonnull
    @CheckReturnValue
    public DBUser getUser(@Nonnull Member member) {
        return getUser(member.getUser());
    }
    
    public void save(@Nonnull ManagedObject object) {
        log("Saving {} {}:{} to rethink", object.getClass().getSimpleName(), object.getTableName(), object.getDatabaseId());
        r.table(object.getTableName())
                .insert(object)
                .optArg("conflict", "replace")
                .runNoReply(conn);
    }
    
    public void delete(@Nonnull ManagedObject object) {
        log("Deleting {} {}:{} from rethink", object.getClass().getSimpleName(), object.getTableName(), object.getDatabaseId());
        r.table(object.getTableName())
                .get(object.getId())
                .delete()
                .runNoReply(conn);
    }
}
