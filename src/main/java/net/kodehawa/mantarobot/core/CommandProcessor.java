package net.kodehawa.mantarobot.core;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.CommandRegistry;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static net.kodehawa.mantarobot.utils.StringUtils.splitArgs;

public class CommandProcessor {
	public static final CommandRegistry REGISTRY = new CommandRegistry();

	private static final Logger LOGGER = LoggerFactory.getLogger("CommandProcessor");

	public boolean run(GuildMessageReceivedEvent event) {
		if (MantaroBot.getLoadState() != LoadState.POSTLOAD) return false;
		if (MantaroData.db().getMantaroData().getBlackListedUsers().contains(event.getAuthor().getId())) return false;
		long start = System.currentTimeMillis();
		String rawCmd = event.getMessage().getRawContent();
		String[] prefix = MantaroData.config().get().prefix;
		String customPrefix = MantaroData.db().getGuild(event.getGuild()).getData().getGuildCustomPrefix();

		String usedPrefix = null;
		for(String s : prefix) {
			if(rawCmd.startsWith(s)) usedPrefix = s;
		}

		if (usedPrefix != null && rawCmd.startsWith(usedPrefix)) rawCmd = rawCmd.substring(usedPrefix.length());
		else if (customPrefix != null && rawCmd.startsWith(customPrefix)) rawCmd = rawCmd.substring(customPrefix.length());
		else if (usedPrefix == null) return false;

		String[] parts = splitArgs(rawCmd, 2);
		String cmdName = parts[0], content = parts[1];

		if (!event.getGuild().getSelfMember().getPermissions(event.getChannel()).contains(Permission.MESSAGE_EMBED_LINKS)) {
			event.getChannel().sendMessage(EmoteReference.STOP + "I require the permission ``Embed Links``. " +
					"All Commands will be refused until you give me that permission.\n" +
					"http://i.imgur.com/Ydykxcy.gifv Refer to this on instructions on how to give the bot the permissions. " +
					"Also check all the other roles the bot has have that permissions and remember to check channel-specific permissions. Thanks you.").queue();
			return false;
		}

		MantaroBot.getInstance().getStatsClient().increment("commands");
		REGISTRY.process(event, cmdName, content);
		LOGGER.debug("Command invoked: {}, by {}#{} with timestamp {}", cmdName, event.getAuthor().getName(), event.getAuthor().getDiscriminator(), new Date(System.currentTimeMillis()));

		long end = System.currentTimeMillis();
		MantaroBot.getInstance().getStatsClient().histogram("command_query_time", end - start);
		return true;
	}
}