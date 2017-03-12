package net.kodehawa.mantarobot.commands.moderation;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.SimpleCommand;

import java.util.Map;

import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;

public class CategorizedCommand extends SimpleCommand {
	private final Map<String, Map<String, Callable>> categories;

	public CategorizedCommand(Map<String, Map<String, Callable>> categories) {
		this.categories = categories;
	}

	@Override
	protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
		if (args.length < 2) {
			onHelp(event);
			return;
		}

		String category = args[0], task = args[1];

		Map<String, Callable> categoryMap = categories.get(category);

		if (categoryMap == null) {
			onHelp(event);
			return;
		}

		Callable callable = categoryMap.get(task);

		if (callable == null) {
			onHelp(event);
			return;
		}

		if (callable.call(event, args.length > 2 ? args[3] : null)) {
			onHelp(event);
		}
	}

	@Override
	protected String[] splitArgs(String content) {
		return SPLIT_PATTERN.split(content, 3);
	}

	@Override
	public MessageEmbed help(GuildMessageReceivedEvent event) {
		return null; //TODO
	}
}
