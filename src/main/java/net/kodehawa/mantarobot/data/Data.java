package net.kodehawa.mantarobot.data;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.rpg.entity.player.EntityPlayer;
import net.kodehawa.mantarobot.commands.rpg.entity.player.EntityPlayerMP;
import net.kodehawa.mantarobot.data.data.GuildData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Data {
	public List<String> blacklistedGuilds = new CopyOnWriteArrayList<>();
	public List<String> blacklistedUsers = new CopyOnWriteArrayList<>();
	public String defaultPrefix = "~>";
	public Map<String, GuildData> guilds = new ConcurrentHashMap<>();
	public Map<String, EntityPlayerMP> users = new ConcurrentHashMap<>();

	public GuildData getGuild(Guild guild, boolean isRewritable) {
		if (isRewritable) return guilds.computeIfAbsent(guild.getId(), s -> new GuildData());
		return guilds.getOrDefault(guild.getId(), new GuildData());
	}

	public String getPrefix(Guild guild) {
		return Optional.ofNullable(getGuild(guild, false).prefix).orElse(defaultPrefix);
	}

	public EntityPlayerMP getUser(User user, boolean isRewritable) {
		if(user.getId() == null) return null;
		if (isRewritable) return users.computeIfAbsent(user.getId(), s -> new EntityPlayerMP());
		return users.getOrDefault(user.getId(), new EntityPlayerMP());
	}

	public EntityPlayer getUser(GuildMessageReceivedEvent event, boolean isRewritable) {
		if(event.getMember() == null) return null;
		return getUser(event.getMember(), isRewritable);
	}

	public EntityPlayer getUser(Guild guild, User user, boolean isRewritable) {
		if(user.getId() == null) return null;
		GuildData guildData = getGuild(guild, isRewritable);

		if (guildData.localMode) {
			if (isRewritable) return guildData.users.computeIfAbsent(user.getId(), s -> new EntityPlayer());
			return guildData.users.getOrDefault(user.getId(), new EntityPlayer());
		}

		return getUser(user, isRewritable);
	}

	public EntityPlayer getUser(Member member, boolean isRewritable) {
		if(member == null) return null;
		return getUser(member.getGuild(), member.getUser(), isRewritable);
	}
}
