package net.kodehawa.mantarobot.web;

import com.rethinkdb.model.MapObject;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.info.AsyncInfoMonitor;
import net.kodehawa.mantarobot.commands.info.CommandStatsManager;
import net.kodehawa.mantarobot.commands.info.GuildStatsManager;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.beans.Introspector;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.commands.info.StatsHelper.calculateDouble;
import static net.kodehawa.mantarobot.commands.info.StatsHelper.calculateInt;

@RestController
public class Controller {
	private Map<String, Callable<Object>> vpsMap = Arrays.stream(AsyncInfoMonitor.class.getDeclaredMethods())
		.filter(method -> Modifier.isStatic(method.getModifiers()))
		.filter(method -> method.getParameterCount() == 0)
		.filter(method -> method.getName().startsWith("get"))
		.collect(Collectors.toMap(
			method -> Introspector.decapitalize(method.getName()),
			m -> (Callable<Object>) () -> m.invoke(null)
		));

	@RequestMapping("/stats/cmds/{type}")
	public Object cmds(@PathVariable("type") String type) {
		switch (type) {
			case "now":
				return CommandStatsManager.MINUTE_CMDS;
			case "hourly":
				return CommandStatsManager.HOUR_CMDS;
			case "daily":
				return CommandStatsManager.DAY_CMDS;
			case "total":
				return CommandStatsManager.TOTAL_CMDS;
			default:
				return null;
		}
	}

	@RequestMapping("/stats/guilds/{type}")
	public Object guilds(@PathVariable("type") String type) {
		switch (type) {
			case "now":
				return GuildStatsManager.MINUTE_EVENTS;
			case "hourly":
				return GuildStatsManager.HOUR_EVENTS;
			case "daily":
				return GuildStatsManager.DAY_EVENTS;
			case "total":
				return GuildStatsManager.TOTAL_EVENTS;
			default:
				return null;
		}
	}

	@RequestMapping("/stats/shards")
	public Object shardinfo() {
		return MantaroBot.getInstance().getShardList().stream()
			.map(
				s -> new MapObject<>()
					.with("id", s.getShardInfo().getShardId())
					.with("status", s.getStatus())
					.with("users", s.getUsers().size())
					.with("guilds", s.getGuilds().size())
					.with("ping", s.getPing())
					.with("lastEvent", System.currentTimeMillis() - s.getEventManager().LAST_EVENT)
					.with("musicChannels", s.getVoiceChannels().stream().filter(
						c -> c.getMembers().contains(c.getGuild().getSelfMember())
					).count())
			)
			.collect(Collectors.toList());
	}

	@RequestMapping("/stats")
	public Object stats() {

		List<Guild> guilds = MantaroBot.getInstance().getGuilds();
		List<VoiceChannel> voiceChannels = MantaroBot.getInstance().getVoiceChannels();
		List<VoiceChannel> musicChannels = voiceChannels.parallelStream().filter(
			vc -> vc.getMembers().contains(vc.getGuild().getSelfMember())).collect(Collectors.toList());
		long musicConnections, exclusiveness;

		return new MapObject<>()
			.with(
				"usersPerGuild",
				calculateInt(guilds, value -> value.getMembers().size())
			)
			.with(
				"onlineUsersPerGuild",
				calculateInt(
					guilds,
					value -> (int) value.getMembers().stream()
						.filter(member -> !member.getOnlineStatus().equals(OnlineStatus.OFFLINE)).count()
				)
			)
			.with(
				"onlineUsersPerUserPerGuild",
				calculateDouble(
					guilds,
					value -> (double) value.getMembers()
						.stream().filter(member -> !member.getOnlineStatus().equals(OnlineStatus.OFFLINE))
						.count() / (double) value.getMembers().size() * 100
				)
			)
			.with(
				"listeningUsersPerUsersPerGuilds",
				calculateDouble(
					musicChannels,
					value -> (double) value.getMembers().size() / (double) value.getGuild().getMembers().size() * 100
				)
			)
			.with(
				"listeningUsersPerOnlineUsersPerGuilds",
				calculateDouble(
					musicChannels,
					value -> (double) value.getMembers().size() / (double) value.getGuild().getMembers().stream()
						.filter(member -> !member.getOnlineStatus().equals(OnlineStatus.OFFLINE)).count() * 100
				)
			)
			.with(
				"textChannelsPerGuild",
				calculateInt(guilds, value -> value.getTextChannels().size())
			)
			.with(
				"voiceChannelsPerGuild",
				calculateInt(guilds, value -> value.getVoiceChannels().size())
			)
			.with(
				"musicConnections",
				musicConnections = voiceChannels.stream()
					.filter(channel -> channel.getMembers().contains(channel.getGuild().getSelfMember()))
					.count()
			)
			.with(
				"exclusiveness",
				exclusiveness = MantaroBot.getInstance().getGuilds().stream()
					.filter(
						g -> g.getMembers().stream().filter(member -> member.getUser().isBot()).count() == 1
					).count()
			)
			.with(
				"musicConnectionsPerServer",
				(double) musicConnections / (double) guilds.size() * 100
			)
			.with(
				"exclusivenessPercent",
				(double) exclusiveness / (double) guilds.size() * 100
			)
			.with(
				"bigGuilds",
				MantaroBot.getInstance().getGuilds().stream().filter(g -> g.getMembers().size() > 500).count()
			);
	}

	@RequestMapping("/stats/resources")
	public Object vps() {
		Map<Object, Object> map = new MapObject<>();
		vpsMap.forEach((k, v) -> {
			try {
				map.put(k, v.call());
			} catch (Exception ignored) {}
		});

		return map;
	}
}
