package net.kodehawa.mantarobot.commands.action;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.NoArgsCommand;

import java.awt.Color;
import java.util.List;

import static br.com.brjdevs.java.utils.extensions.CollectionUtils.random;

public class TextActionCmd extends NoArgsCommand {
	private final Color color;
	private final String desc;
	private final String format;
	private final String name;
	private final List<String> strings;

	public TextActionCmd(String name, String desc, Color color, String format, List<String> strings) {
		this.name = name;
		this.desc = desc;
		this.color = color;
		this.format = format;
		this.strings = strings;
	}

	@Override
	protected void call(GuildMessageReceivedEvent event) {
		event.getChannel().sendMessage(String.format(format, random(strings))).queue();
	}

	@Override
	public CommandPermission permissionRequired() {
		return CommandPermission.USER;
	}

	@Override
	public MessageEmbed help(GuildMessageReceivedEvent event) {
		return helpEmbed(event, name)
			.setDescription(desc)
			.setColor(color)
			.build();
	}
}
