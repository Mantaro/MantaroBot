package net.kodehawa.mantarobot.modules;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import static net.kodehawa.mantarobot.commands.info.CommandStatsManager.log;

public abstract class NoArgsCommand implements Command {
	protected abstract void call(GuildMessageReceivedEvent event);

	@Override
	public void invoke(GuildMessageReceivedEvent event, String cmdName, String content) {
		call(event);
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
}
