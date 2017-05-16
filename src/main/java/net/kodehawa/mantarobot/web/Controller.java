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
import java.util.*;
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

	@RequestMapping("/stats")
	public Object stats() {
		Map<Object, Object> map = new MapObject<>();

		List<Guild> guilds = MantaroBot.getInstance().getGuilds();

		List<VoiceChannel> voiceChannels = MantaroBot.getInstance().getVoiceChannels();
		List<VoiceChannel> musicChannels = voiceChannels.parallelStream().filter(
			vc -> vc.getMembers().contains(vc.getGuild().getSelfMember())).collect(Collectors.toList());

		map.put("usersPerGuild", calculateInt(guilds, value -> value.getMembers().size()));
		IntSummaryStatistics onlineUsersPerGuild = calculateInt(
			guilds, value -> (int) value.getMembers().stream().filter(member -> !member.getOnlineStatus().equals(
				OnlineStatus.OFFLINE)).count());
		DoubleSummaryStatistics onlineUsersPerUserPerGuild = calculateDouble(
			guilds, value -> (double) value.getMembers().stream().filter(
				member -> !member.getOnlineStatus().equals(OnlineStatus.OFFLINE)).count() / (double) value.getMembers()
				.size() * 100);
		DoubleSummaryStatistics listeningUsersPerUsersPerGuilds = calculateDouble(
			musicChannels,
			value -> (double) value.getMembers().size() / (double) value.getGuild().getMembers().size() * 100
		);
		DoubleSummaryStatistics listeningUsersPerOnlineUsersPerGuilds = calculateDouble(
			musicChannels,
			value -> (double) value.getMembers().size() / (double) value.getGuild().getMembers().stream().filter(
				member -> !member.getOnlineStatus().equals(OnlineStatus.OFFLINE)).count() * 100
		);
		IntSummaryStatistics textChannelsPerGuild = calculateInt(guilds, value -> value.getTextChannels().size());
		IntSummaryStatistics voiceChannelsPerGuild = calculateInt(guilds, value -> value.getVoiceChannels().size());

		int musicConnections = (int) voiceChannels.stream().filter(voiceChannel -> voiceChannel.getMembers().contains(
			voiceChannel.getGuild().getSelfMember())).count();
		long exclusiveness = MantaroBot.getInstance().getGuilds().stream().filter(g -> g.getMembers().stream().filter(
			member -> member.getUser().isBot()).count() == 1).count();
		double musicConnectionsPerServer = (double) musicConnections / (double) guilds.size() * 100;
		double exclusivenessPercent = (double) exclusiveness / (double) guilds.size() * 100;
		long bigGuilds = MantaroBot.getInstance().getGuilds().stream().filter(g -> g.getMembers().size() > 500).count();

		return map;
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
