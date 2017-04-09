package net.kodehawa.mantarobot.modules;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.function.Consumer;
import java.util.function.Function;

import static net.kodehawa.mantarobot.commands.info.CommandStatsManager.log;
import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;

public abstract class SimpleCommand implements Command {
	protected abstract void call(String[] args, String content, GuildMessageReceivedEvent event);

	@Override
	public void invoke(GuildMessageReceivedEvent event, String cmdName, String content) {
		call(splitArgs(content), content, event);
		log(cmdName);
	}

	@Override
	public boolean isHiddenFromHelp() {
		return false;
	}

	@Override
	public CommandPermission permissionRequired() {
		return CommandPermission.USER;
	}

	protected EmbedBuilder baseEmbed(GuildMessageReceivedEvent event, String name) {
		return baseEmbed(event, name, event.getJDA().getSelfUser().getEffectiveAvatarUrl());
	}

	protected EmbedBuilder baseEmbed(GuildMessageReceivedEvent event, String name, String image) {
		return new EmbedBuilder()
			.setAuthor(name, null, image)
			.setColor(event.getMember().getColor())
			.setFooter("Requested by " + event.getMember().getEffectiveName(), event.getAuthor().getEffectiveAvatarUrl());
	}

	protected EmbedBuilder helpEmbed(GuildMessageReceivedEvent event, String name) {
		return baseEmbed(event, name).addField("Permission required", permissionRequired().toString(), true);
	}

	protected void onHelp(GuildMessageReceivedEvent event) {
		event.getChannel().sendMessage(help(event)).queue();
	}

	protected String[] splitArgs(String content) {
		return SPLIT_PATTERN.split(content);
	}

	protected String contentFrom(String content, int argsToSkip) {
		String[] arrayed = content.split(" ");
		StringBuilder toReplace = new StringBuilder();
		for (int i = 0; i < argsToSkip; i++) {
			toReplace.append(arrayed[i]).append(" ");
		}
		return content.replace(toReplace.toString(), "");
	}

	protected void doTimes(int times, Runnable runnable) {
		for (int i = 0; i < times; i++) {
			runnable.run();
		}
	}

}
