package net.kodehawa.mantarobot.utils.jda;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.hooks.IEventManager;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public abstract class ShardedJDA implements UnifiedJDA {

	@Override
	public long getPing() {
		return ((long) stream().mapToLong(JDA::getPing).average().orElseThrow(() -> new IllegalStateException("no JDA instances")));
	}

	@Override
	public void setEventManager(IEventManager manager) {
		forEach(jda -> jda.setEventManager(manager));
	}

	@Override
	public void addEventListener(Object... listeners) {
		forEach(jda -> jda.addEventListener(listeners));
	}

	@Override
	public void removeEventListener(Object... listeners) {
		forEach(jda -> jda.removeEventListener(listeners));
	}

	@Override
	public List<User> getUsers() {
		return distinct(stream().map(JDA::getUsers).flatMap(Collection::stream).collect(Collectors.toList()));
	}

	@Override
	public User getUserById(String id) {
		List<User> users = distinct(stream().map(jda -> jda.getUserById(id)).collect(Collectors.toList()));
		return users.size() == 0 ? null : users.get(0);
	}

	@Override
	public List<Guild> getMutualGuilds(User... users) {
		return stream().flatMap(jda -> jda.getMutualGuilds(users).stream()).collect(Collectors.toList());
	}

	@Override
	public List<Guild> getMutualGuilds(Collection<User> users) {
		return stream().flatMap(jda -> jda.getMutualGuilds(users).stream()).collect(Collectors.toList());
	}

	@Override
	public List<User> getUsersByName(String name, boolean ignoreCase) {
		return distinct(stream().flatMap(jda -> jda.getUsersByName(name, ignoreCase).stream()).collect(Collectors.toList()));
	}

	@Override
	public List<Guild> getGuilds() {
		return stream().map(JDA::getGuilds).flatMap(Collection::stream).distinct().collect(Collectors.toList());
	}

	@Override
	public Guild getGuildById(String id) {
		return stream().map(jda -> jda.getGuildById(id)).filter(Objects::nonNull).findFirst().orElse(null);
	}

	@Override
	public List<Guild> getGuildsByName(String name, boolean ignoreCase) {
		return stream().flatMap(jda -> jda.getGuildsByName(name, ignoreCase).stream()).collect(Collectors.toList());
	}

	@Override
	public List<TextChannel> getTextChannels() {
		return stream().map(JDA::getTextChannels).flatMap(Collection::stream).distinct().collect(Collectors.toList());
	}

	@Override
	public TextChannel getTextChannelById(String id) {
		return stream().map(jda -> jda.getTextChannelById(id)).filter(Objects::nonNull).findFirst().orElse(null);
	}

	@Override
	public List<TextChannel> getTextChannelsByName(String name, boolean ignoreCase) {
		return stream().flatMap(jda -> jda.getTextChannelsByName(name, ignoreCase).stream()).collect(Collectors.toList());
	}

	@Override
	public List<VoiceChannel> getVoiceChannels() {
		return stream().map(JDA::getVoiceChannels).flatMap(Collection::stream).distinct().collect(Collectors.toList());
	}

	@Override
	public VoiceChannel getVoiceChannelById(String id) {
		return stream().map(jda -> jda.getVoiceChannelById(id)).filter(Objects::nonNull).findFirst().orElse(null);
	}

	@Override
	public List<VoiceChannel> getVoiceChannelByName(String name, boolean ignoreCase) {
		return stream().flatMap(jda -> jda.getVoiceChannelByName(name, ignoreCase).stream()).collect(Collectors.toList());
	}

	@Override
	public List<PrivateChannel> getPrivateChannels() {
		return stream().map(JDA::getPrivateChannels).flatMap(Collection::stream).distinct().collect(Collectors.toList());
	}

	@Override
	public PrivateChannel getPrivateChannelById(String id) {
		return stream().map(jda -> jda.getPrivateChannelById(id)).filter(Objects::nonNull).findFirst().orElse(null);
	}

	@Override
	public List<Emote> getEmotes() {
		return stream().map(JDA::getEmotes).flatMap(Collection::stream).distinct().collect(Collectors.toList());
	}

	@Override
	public Emote getEmoteById(String id) {
		return stream().map(jda -> jda.getEmoteById(id)).filter(Objects::nonNull).findFirst().orElse(null);
	}

	@Override
	public List<Emote> getEmotesByName(String name, boolean ignoreCase) {
		return stream().flatMap(jda -> jda.getEmotesByName(name, ignoreCase).stream()).collect(Collectors.toList());
	}

	@Override
	public long getResponseTotal() {
		return stream().mapToLong(JDA::getResponseTotal).sum();
	}

	@Override
	public void setAutoReconnect(boolean reconnect) {
		forEach(jda -> jda.setAutoReconnect(reconnect));
	}

	@Override
	public void shutdown() {
		forEach(JDA::shutdown);
	}

	@Override
	public void shutdown(boolean free) {
		forEach(jda -> jda.shutdown(free));
	}

	@Override
	public Status[] getShardStatus() {
		return stream().map(JDA::getStatus).toArray(Status[]::new);
	}

	private List<User> distinct(List<User> list) {
		Map<String, List<User>> map = new HashMap<>();
		list.forEach(user -> map.computeIfAbsent(user.getId() != null ? user.getId() : null, k -> new ArrayList<>()).add(user));

		return map.values().stream()
			.map(users -> users.size() == 0 ? null : users.size() == 1 ? users.get(0) : new ShardedUser(users, this))
			.filter(Objects::nonNull)
			.sorted(Comparator.comparing(ISnowflake::getId))
			.collect(Collectors.toList());
	}

	private <T extends ISnowflake> Map<String, T> map(List<T> list) {
		return list.stream().collect(Collectors.toMap(ISnowflake::getId, UnaryOperator.identity()));
	}
}
