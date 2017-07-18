package net.kodehawa.mantarobot.shard.jda;

import net.dv8tion.jda.bot.JDABot;
import net.dv8tion.jda.client.JDAClient;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.SelfUser;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.managers.Presence;
import net.dv8tion.jda.core.requests.RestAction;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface UnifiedJDA extends JDA, Iterable<JDA> {
    JDA getShard(int shard);

    int getShardAmount();

    Status[] getShardStatus();

    @Override
    default Status getStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    default List<Object> getRegisteredListeners() {
        throw new UnsupportedOperationException();
    }

    @Override
    default RestAction<User> retrieveUserById(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    default SelfUser getSelfUser() {
        throw new UnsupportedOperationException();
    }

    @Override
    default Presence getPresence() {
        throw new UnsupportedOperationException();
    }

    @Override
    default ShardInfo getShardInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    default String getToken() {
        throw new UnsupportedOperationException();
    }

    @Override
    default int getMaxReconnectDelay() {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean isAutoReconnect() {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean isAudioEnabled() {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean isBulkDeleteSplittingEnabled() {
        throw new UnsupportedOperationException();
    }

    @Override
    default AccountType getAccountType() {
        throw new UnsupportedOperationException();
    }

    @Override
    default JDAClient asClient() {
        throw new UnsupportedOperationException();
    }

    @Override
    default JDABot asBot() {
        throw new UnsupportedOperationException();
    }

    default Stream<JDA> stream() {
        return StreamSupport.stream(spliterator(), false).sorted(Comparator.comparingInt(jda -> jda.getShardInfo().getShardId()));
    }
}
