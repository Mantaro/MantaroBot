package net.kodehawa.mantarobot.db.redis;

import com.rethinkdb.net.Connection;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.mantarobot.ExtraRuntimeOptions;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.ManagedObject;
import net.kodehawa.mantarobot.db.entities.*;
import net.kodehawa.mantarobot.utils.Utils;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class RedisCachedDatabase extends ManagedDatabase {
    private static final Map<Class<? extends ManagedObject>, String> PREFIXES = Collections.unmodifiableMap(Utils.map(
            CustomCommand.class, "cc",
            DBGuild.class, "guild",
            Player.class, "player",
            DBUser.class, "user",
            PremiumKey.class, "key"
    ));

    private final RMap<String, CustomCommand> ccMap;
    private final RMap<String, DBGuild> guildMap;
    private final RMap<String, Player> playerMap;
    private final RMap<String, DBUser> userMap;
    private final RMap<String, PremiumKey> keyMap;
    private final RBucket<MantaroObj> mantaroBucket;
    private final Map<Class<? extends ManagedObject>, RMap<String, ManagedObject>> map;

    public RedisCachedDatabase(@Nonnull Connection conn,
                               @Nonnull RMap<String, CustomCommand> ccMap,
                               @Nonnull RMap<String, DBGuild> guildMap,
                               @Nonnull RMap<String, Player> playerMap,
                               @Nonnull RMap<String, DBUser> userMap,
                               @Nonnull RMap<String, PremiumKey> keyMap,
                               @Nonnull RBucket<MantaroObj> mantaroBucket) {
        super(conn);
        this.ccMap = ccMap;
        this.guildMap = guildMap;
        this.playerMap = playerMap;
        this.userMap = userMap;
        this.keyMap = keyMap;
        this.mantaroBucket = mantaroBucket;
        this.map = Collections.unmodifiableMap(Utils.map(
                CustomCommand.class, ccMap,
                DBGuild.class, guildMap,
                Player.class, playerMap,
                DBUser.class, userMap,
                PremiumKey.class, keyMap
        ));
    }

    private static void log(String message, Object... fmtArgs) {
        if(ExtraRuntimeOptions.LOG_CACHE_ACCESS) {
            //using debug logs spams too much
            log.info(message, fmtArgs);
        }
    }

    private static void log(String message) {
        if(ExtraRuntimeOptions.LOG_CACHE_ACCESS) {
            log.info(message);
        }
    }

    @Override
    @Nonnull
    @CheckReturnValue
    public CustomCommand getCustomCommand(@Nonnull String guildId, @Nonnull String name) {
        log("Getting custom command {}:{} from cache", guildId, name);
        return ccMap.computeIfAbsent("cc:" + guildId + ":" + name, ignored->super.getCustomCommand(guildId, name));
    }

    @Override
    @Nonnull
    @CheckReturnValue
    public List<CustomCommand> getCustomCommands() {
        List<CustomCommand> list = super.getCustomCommands();
        log("Caching all custom commands");
        list.forEach(command->ccMap.fastPutAsync(command.getId(), command));
        return list;
    }

    @Override
    @Nonnull
    @CheckReturnValue
    public List<CustomCommand> getCustomCommands(@Nonnull String guildId) {
        List<CustomCommand> list = super.getCustomCommands(guildId);
        log("Caching all custom commands from guild {}", guildId);
        list.forEach(command->ccMap.fastPutAsync(command.getId(), command));
        return list;
    }

    @Override
    @Nonnull
    @CheckReturnValue
    public List<CustomCommand> getCustomCommandsByName(@Nonnull String name) {
        List<CustomCommand> list = super.getCustomCommandsByName(name);
        log("Caching all custom commands named {}", name);
        list.forEach(command->ccMap.fastPutAsync(command.getId(), command));
        return list;
    }

    @Override
    @Nonnull
    @CheckReturnValue
    public DBGuild getGuild(@Nonnull String guildId) {
        log("Getting guild {} from cache", guildId);
        return guildMap.computeIfAbsent("guild:" + guildId, ignored->super.getGuild(guildId));
    }

    @Override
    @Nonnull
    @CheckReturnValue
    public MantaroObj getMantaroData() {
        log("Getting MantaroObj from cache");
        MantaroObj o = mantaroBucket.get();
        if(o == null) {
            return getMantaroData(true);
        }
        return o;
    }

    @Nonnull
    @CheckReturnValue
    public MantaroObj getMantaroData(boolean updateFromRethink) {
        if(updateFromRethink) {
            MantaroObj o = super.getMantaroData();
            mantaroBucket.set(o);
            return o;
        } else {
            return getMantaroData();
        }
    }

    @Override
    @Nonnull
    @CheckReturnValue
    public Player getPlayer(@Nonnull String userId) {
        log("Getting player {} from cache", userId);
        return playerMap.computeIfAbsent("player:" + userId, ignored->super.getPlayer(userId));
    }

    @Override
    @Nonnull
    @CheckReturnValue
    public List<Player> getPlayers() {
        List<Player> list = super.getPlayers();
        log("Caching all players");
        list.forEach(player->playerMap.fastPutAsync(player.getId(), player));
        return list;
    }

    @Override
    @Nonnull
    @CheckReturnValue
    public PremiumKey getPremiumKey(@Nullable String id) {
        log("Getting premium key {} from cache", id);
        if(id == null) return null;
        return keyMap.computeIfAbsent("key:" + id, ignored->super.getPremiumKey(id));
    }

    @Override
    @Nonnull
    @CheckReturnValue
    public List<PremiumKey> getPremiumKeys() {
        List<PremiumKey> list = super.getPremiumKeys();
        log("Caching all premium keys");
        list.forEach(key->keyMap.fastPutAsync(key.getId(), key));
        return list;
    }

    @Override
    @Nonnull
    @CheckReturnValue
    public DBUser getUser(@Nonnull String userId) {
        log("Getting user {} from cache", userId);
        return userMap.computeIfAbsent("user:" + userId, ignored->super.getUser(userId));
    }

    @Override
    public void save(@Nonnull ManagedObject object) {
        if(object instanceof MantaroObj) {
            mantaroBucket.setAsync((MantaroObj)object);
        } else {
            Class<? extends ManagedObject> c = object.getClass();
            RMap<String, ManagedObject> m = map.get(c);
            if(m == null) throw new IllegalStateException("No map configured for " + c);
            String prefix = PREFIXES.get(c);
            if(prefix == null) throw new IllegalStateException("No prefix configured for " + c);
            log("Caching {} {}:{}", c.getSimpleName(), prefix, object.getDatabaseId());
            m.fastPutAsync(prefix + ":" + object.getDatabaseId(), object);
        }
        super.save(object);
    }

    @Override
    public void delete(@Nonnull ManagedObject object) {
        if(object instanceof MantaroObj) {
            mantaroBucket.deleteAsync();
        } else {
            Class<? extends ManagedObject> c = object.getClass();
            RMap<String, ManagedObject> m = map.get(c);
            if(m == null) throw new IllegalStateException("No map configured for " + c);
            String prefix = PREFIXES.get(c);
            if(prefix == null) throw new IllegalStateException("No prefix configured for " + c);
            log("Removing {} {}:{} from cache", c.getSimpleName(), prefix, object.getDatabaseId());
            m.fastRemoveAsync(prefix + ":" + object.getDatabaseId());
        }
        super.delete(object);
    }
}
