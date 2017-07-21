package net.kodehawa.mantarobot.db.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.db.ManagedObject;

import java.beans.ConstructorProperties;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.data.MantaroData.conn;

@Getter
public class Quote implements ManagedObject {
	public static final String DB_TABLE = "quotes";

	public static Quote of(Member member, TextChannel channel, Message message) {
		return new Quote(
			member.getGuild().getId() + ":",
			member.getUser().getId(),
			channel.getId(),
			message.getRawContent(),
			member.getGuild().getName(),
			member.getEffectiveName(),
			member.getUser().getEffectiveAvatarUrl(),
			channel.getName()
		);
	}

	public static Quote of(GuildMessageReceivedEvent event) {
		return of(event.getMember(), event.getChannel(), event.getMessage());
	}

	private final String channelId;
	private final String channelName;
	private final String content;
	private final String guildName;
	private final String id;
	private final String userAvatar;
	private final String userId;
	private final String userName;

	@ConstructorProperties({"id", "userId", "channelId", "content", "guildName", "userName", "userAvatar", "channelName"})
	public Quote(String id, String userId, String channelId, String content, String guildName, String userName, String userAvatar, String channelName) {
		this.id = id;
		this.userId = userId;
		this.channelId = channelId;
		this.content = content;
		this.guildName = guildName;
		this.userName = userName;
		this.userAvatar = userAvatar;
		this.channelName = channelName;
	}

	@Override
	public void delete() {
		r.table(DB_TABLE).get(getId()).delete().run(conn());
		MantaroBot.getInstance().getStatsClient().increment("database_hits");
	}

	@Override
	public void save() {
		r.table(DB_TABLE).insert(this)
			.optArg("conflict", "replace")
			.run(conn());
		MantaroBot.getInstance().getStatsClient().increment("database_hits");
	}

	@JsonIgnore
	public String getGuildId() {
		return getId().split(":")[0];
	}

	@JsonIgnore
	public String getQuoteId() {
		return getId().split(":")[1];
	}

}
