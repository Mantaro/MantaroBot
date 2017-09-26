package net.kodehawa.mantarobot.db.redis;

import com.rethinkdb.net.Connection;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.*;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;

import java.util.List;

public class RedisCachedDatabase extends ManagedDatabase {
    private final RMap<String, CustomCommand> ccMap;
    private final RMap<String, DBGuild> guildMap;
    private final RMap<String, Player> playerMap;
    private final RMap<String, DBUser> userMap;
    private final RMap<String, PremiumKey> keyMap;
    private final RBucket<MantaroObj> mantaroBucket;

    public RedisCachedDatabase(Connection conn,
                               RMap<String, CustomCommand> ccMap,
                               RMap<String, DBGuild> guildMap,
                               RMap<String, Player> playerMap,
                               RMap<String, DBUser> userMap,
                               RMap<String, PremiumKey> keyMap,
                               RBucket<MantaroObj> mantaroBucket) {
        super(conn);
        this.ccMap = ccMap;
        this.guildMap = guildMap;
        this.playerMap = playerMap;
        this.userMap = userMap;
        this.keyMap = keyMap;
        this.mantaroBucket = mantaroBucket;
    }

    @Override
    public CustomCommand getCustomCommand(String guildId, String name) {
        return ccMap.computeIfAbsent("cc:" + guildId + ":" + name, ignored->super.getCustomCommand(guildId, name));
    }

    @Override
    public List<CustomCommand> getCustomCommands() {
        List<CustomCommand> list = super.getCustomCommands();
        list.forEach(command->ccMap.put(command.getId(), command));
        return list;
    }

    @Override
    public List<CustomCommand> getCustomCommands(String guildId) {
        List<CustomCommand> list = super.getCustomCommands(guildId);
        list.forEach(command->ccMap.put(command.getId(), command));
        return list;
    }

    @Override
    public List<CustomCommand> getCustomCommandsByName(String name) {
        List<CustomCommand> list = super.getCustomCommandsByName(name);
        list.forEach(command->ccMap.put(command.getId(), command));
        return list;
    }

    @Override
    public DBGuild getGuild(String guildId) {
        return guildMap.computeIfAbsent("guild:" + guildId, ignored->super.getGuild(guildId));
    }

    @Override
    public MantaroObj getMantaroData() {
        MantaroObj o = mantaroBucket.get();
        if(o == null) {
            return getMantaroData(true);
        }
        return o;
    }

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
    public Player getPlayer(String userId) {
        return playerMap.computeIfAbsent("player:" + userId, ignored->super.getPlayer(userId));
    }

    @Override
    public List<Player> getPlayers() {
        List<Player> list = super.getPlayers();
        list.forEach(player->playerMap.put(player.getId(), player));
        return list;
    }

    @Override
    public PremiumKey getPremiumKey(String id) {
        return keyMap.computeIfAbsent("key:" + id, ignored->super.getPremiumKey(id));
    }

    @Override
    public List<PremiumKey> getPremiumKeys() {
        List<PremiumKey> list = super.getPremiumKeys();
        list.forEach(key->keyMap.put(key.getId(), key));
        return list;
    }

    @Override
    public DBUser getUser(String userId) {
        return userMap.computeIfAbsent("user:" + userId, ignored->super.getUser(userId));
    }

    @Override
    public void save(CustomCommand command) {
        ccMap.put(command.getId(), command);
        super.delete(command);
    }

    @Override
    public void save(DBGuild guild) {
        guildMap.put(guild.getId(), guild);
        super.save(guild);
    }

    @Override
    public void save(DBUser user) {
        userMap.put(user.getId(), user);
        super.save(user);
    }

    @Override
    public void save(MantaroObj obj) {
        mantaroBucket.set(obj);
        super.save(obj);
    }

    @Override
    public void save(Player player) {
        playerMap.put(player.getId(), player);
        super.save(player);
    }

    @Override
    public void save(PremiumKey key) {
        keyMap.put(key.getId(), key);
        super.save(key);
    }

    @Override
    public void delete(CustomCommand command) {
        ccMap.remove(command.getId());
        super.delete(command);
    }

    @Override
    public void delete(DBGuild guild) {
        guildMap.remove(guild.getId());
        super.delete(guild);
    }

    @Override
    public void delete(DBUser user) {
        userMap.remove(user.getId());
        super.delete(user);
    }

    @Override
    public void delete(MantaroObj obj) {
        mantaroBucket.delete();
        super.delete(obj);
    }

    @Override
    public void delete(Player player) {
        playerMap.remove(player.getId());
        super.delete(player);
    }

    @Override
    public void delete(PremiumKey key) {
        keyMap.remove(key.getId());
        super.delete(key);
    }
}
