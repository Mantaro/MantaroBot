package net.kodehawa.mantarobot.modules;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.CommandProcessor.Arguments;

public abstract class SimpleCommand implements Command {

	protected abstract void onCommand(String[] args, String content, GuildMessageReceivedEvent event);

	@Override
	public void invoke(Arguments cmd) {
		onCommand(cmd.args, cmd.content, cmd.event);
	}

	@Override
	public boolean isHiddenFromHelp() {
		return false;
	}

	public EmbedBuilder baseEmbed(GuildMessageReceivedEvent event, String name) {
		return new EmbedBuilder()
			.setAuthor(name, null, event.getAuthor().getEffectiveAvatarUrl())
			.setColor(event.getMember().getColor())
			.setFooter("Requested by " + event.getMember().getEffectiveName(), event.getAuthor().getEffectiveAvatarUrl());
	}

	protected void onHelp(GuildMessageReceivedEvent event) {
		event.getChannel().sendMessage(help(event)).queue();
	}
}
