package net.kodehawa.lib.mantarolang.jda;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.member.GenericGuildMemberEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.lib.mantarolang.java.StaticSerializator;
import net.kodehawa.lib.mantarolang.objects.LangCallable;
import net.kodehawa.lib.mantarolang.objects.LangString;

import java.util.Collections;

import static net.kodehawa.lib.mantarolang.MantaroLangCompiler.cast;

public class EventSerialization {
	public static final StaticSerializator<GenericGuildMemberEvent> GENERIC_GUILD_MEMBER_EVENT = new StaticSerializator<GenericGuildMemberEvent>();
	public static final StaticSerializator<Guild> GUILD = new StaticSerializator<Guild>();
	public static final StaticSerializator<GuildMessageReceivedEvent> GUILD_MESSAGE_RECEIVED_EVENT = new StaticSerializator<GuildMessageReceivedEvent>();
	public static final StaticSerializator<Member> MEMBER = new StaticSerializator<Member>();
	public static final StaticSerializator<Message> MESSAGE = new StaticSerializator<Message>();
	public static final StaticSerializator<PrivateChannel> PRIVATE_CHANNEL = new StaticSerializator<PrivateChannel>();
	public static final StaticSerializator<TextChannel> TEXT_CHANNEL = new StaticSerializator<TextChannel>();

	static {
		//GuildMessageReceivedEvent
		GUILD_MESSAGE_RECEIVED_EVENT.map("channel", event -> TEXT_CHANNEL.serialize(event.getChannel()));
		GUILD_MESSAGE_RECEIVED_EVENT.map("author", event -> MEMBER.serialize(event.getMember()));
		GUILD_MESSAGE_RECEIVED_EVENT.map("guild", event -> GUILD.serialize(event.getGuild()));
		GUILD_MESSAGE_RECEIVED_EVENT.map("message", event -> MESSAGE.serialize(event.getMessage()));

		//GenericGuildMemberEvent
		GENERIC_GUILD_MEMBER_EVENT.map("member", event -> MEMBER.serialize(event.getMember()));
		GENERIC_GUILD_MEMBER_EVENT.map("guild", event -> GUILD.serialize(event.getGuild()));

		//TextChannel
		TEXT_CHANNEL.map("topic", channel -> new LangString(channel.getTopic()));
		TEXT_CHANNEL.map("name", channel -> new LangString(channel.getName()));
		TEXT_CHANNEL.map("mention", channel -> new LangString(channel.getAsMention()));
		TEXT_CHANNEL.map("id", channel -> new LangString(channel.getId()));
		TEXT_CHANNEL.map("send", channel -> (LangCallable) args -> Collections.singletonList(MESSAGE.serialize(channel.sendMessage(cast(args.get(0), LangString.class).get()).complete())));
		TEXT_CHANNEL.map("sendTyping", channel -> (LangCallable) args -> {
			channel.sendTyping().complete();
			return Collections.emptyList();
		});

		//Member
		MEMBER.map("name", member -> new LangString(member.getEffectiveName()));
		MEMBER.map("username", member -> new LangString(member.getUser().getName()));
		MEMBER.map("mention", member -> new LangString(member.getAsMention()));
		MEMBER.map("id", member -> new LangString(member.getUser().getId()));
		MEMBER.map("avatar", member -> new LangString(member.getUser().getEffectiveAvatarUrl()));
	}
}
