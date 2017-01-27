package net.kodehawa.mantarobot.commands.custom;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.utils.GeneralUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.capitalize;

public class Mapifier {
	private static final Pattern compiledPattern = Pattern.compile("\\$\\([A-Za-z0-9.]+?\\)");

	public static String dynamicResolve(String string, Map<String, String> dynamicMap) {
		if (!string.contains("$(")) return string;

		Set<String> skipIfIterated = new HashSet<>();
		for (String key : GeneralUtils.iterate(compiledPattern.matcher(string))) {
			if (skipIfIterated.contains(key)) continue;
			String mapKey = key.substring(2, key.length() - 1);
			string = string.replace(key, dynamicMap.getOrDefault(mapKey, mapKey));
			if (!string.contains("$(")) break;
			skipIfIterated.add(key);
		}

		return string;
	}

	public static void map(String prefix, Map<String, String> map, Guild guild) {
		map.put(prefix, guild.getName());
		prefix = prefix + ".";
		map.put(prefix + "name", guild.getName());
		map(prefix + "owner", map, guild.getOwner());
		map.put(prefix + "region", guild.getRegion().getName());
		map(prefix + "publicChannel", map, guild.getPublicChannel());
		//map(prefix + "me", map, guild.getSelfMember());
	}

	public static void map(String prefix, Map<String, String> map, Member member) {
		map.put(prefix, member.getAsMention());
		prefix = prefix + ".";
		map.put(prefix + "username", member.getUser().getName());
		map.put(prefix + "discriminator", member.getUser().getDiscriminator());
		map.put(prefix + "name", member.getEffectiveName());
		map.put(prefix + "game", member.getGame().getName());
		map.put(prefix + "status", capitalize(member.getOnlineStatus().getKey()));
		map.put(prefix + "mention", member.getAsMention());
	}

	public static void map(String prefix, Map<String, String> map, GuildMessageReceivedEvent event) {
		map.put(prefix, event.getMember().getAsMention() + "@" + event.getChannel().getAsMention());
		prefix = prefix + ".";
		map(prefix + "channel", map, event.getChannel());
		map(prefix + "guild", map, event.getGuild());
		map(prefix + "me", map, event.getGuild().getSelfMember());
		map(prefix + "author", map, event.getMember());
		map(prefix + "message", map, event.getMessage());
	}

	public static void map(String prefix, Map<String, String> map, Message message) {
		map.put(prefix, message.getRawContent());
		prefix = prefix + ".";
		map.put(prefix + "raw", message.getRawContent());
		map.put(prefix + "textual", message.getContent());
		map.put(prefix + "stripped", message.getStrippedContent());
	}

	public static void map(String prefix, Map<String, String> map, TextChannel channel) {
		map.put(prefix, channel.getAsMention());
		prefix = prefix + ".";
		map.put(prefix + "topic", channel.getTopic());
		map.put(prefix + "name", channel.getName());
		map.put(prefix + "id", channel.getId());
		map.put(prefix + "mention", channel.getAsMention());
	}
}
