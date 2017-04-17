package net.kodehawa.mantarobot.modules.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.utils.StringUtils;

import static net.kodehawa.mantarobot.commands.info.CommandStatsManager.log;

public interface SimpleCommand extends Command {

	void call(GuildMessageReceivedEvent event, String content, String[] args);

	@Override
	default void run(GuildMessageReceivedEvent event, String commandName, String content) {
		call(event, content, splitArgs(content));
		log(commandName);
	}

	default EmbedBuilder baseEmbed(GuildMessageReceivedEvent event, String name) {
		return baseEmbed(event, name, event.getJDA().getSelfUser().getEffectiveAvatarUrl());
	}

	default EmbedBuilder baseEmbed(GuildMessageReceivedEvent event, String name, String image) {
		return new EmbedBuilder()
			.setAuthor(name, null, image)
			.setColor(event.getMember().getColor())
			.setFooter("Requested by " + event.getMember().getEffectiveName(), event.getAuthor().getEffectiveAvatarUrl());
	}

	default void doTimes(int times, Runnable runnable) {
		for (int i = 0; i < times; i++) runnable.run();
	}

	default EmbedBuilder helpEmbed(GuildMessageReceivedEvent event, String name) {
		return baseEmbed(event, name).addField("Permission required", permission().toString(), true);
	}

	default void onHelp(GuildMessageReceivedEvent event) {
		event.getChannel().sendMessage(help(event)).queue();
	}

	default String[] splitArgs(String content) {
		return StringUtils.advancedSplitArgs(content, 0);
	}
}
