package net.kodehawa.mantarobot.utils.jda;

import net.dv8tion.jda.bot.JDABot;
import net.dv8tion.jda.client.JDAClient;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.SelfUser;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.managers.Presence;
import net.dv8tion.jda.core.requests.RestAction;
import org.apache.http.HttpHost;

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
	default HttpHost getGlobalProxy() {
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

	/**
	 * Who doesn't love to install a fresh auxiliary cable.
	 *
	 * @param port Where?
	 * @throws UnsupportedOperationException When you think it's even possible to.
	 */
	@Override
	default void installAuxiliaryCable(int port) throws UnsupportedOperationException {
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

    @Override
    default int getMaxReconnectDelay() {
	    throw new UnsupportedOperationException();
    }

    default Stream<JDA> stream() {
		return StreamSupport.stream(spliterator(), false).sorted(Comparator.comparingInt(jda -> jda.getShardInfo().getShardId()));
	}
}
