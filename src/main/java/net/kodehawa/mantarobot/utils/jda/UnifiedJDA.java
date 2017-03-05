package net.kodehawa.mantarobot.utils.jda;

import net.dv8tion.jda.bot.JDABot;
import net.dv8tion.jda.client.JDAClient;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.requests.RestAction;

public interface UnifiedJDA extends JDA {
	JDA getShard(int shard);

	int getShardAmount();

	Status[] getShardStatus();

	@Override
	default Status getStatus() {
		throw new UnsupportedOperationException();
	}

	@Override
	default RestAction<User> retrieveUserById(String id) {
		throw new UnsupportedOperationException();
	}

	@Override
	default ShardInfo getShardInfo() {
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
	default JDAClient asClient() {
		throw new UnsupportedOperationException();
	}

	@Override
	default JDABot asBot() {
		throw new UnsupportedOperationException();
	}
}
