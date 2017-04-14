package net.kodehawa.mantarobot.modules;

import com.google.common.base.Preconditions;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {
	private static final Logger LOGGER = LoggerFactory.getLogger(CommandRegistry.class);

	private final Map<String, Command> commands;

	public CommandRegistry(Map<String, Command> commands) {
		this.commands = Preconditions.checkNotNull(commands);
	}

	public CommandRegistry() {
		this(new HashMap<>());
	}

	public Map<String, Command> commands() {
		return commands;
	}

	public boolean process(GuildMessageReceivedEvent event, String cmdname, String content) {
		Command cmd = commands.get(cmdname);
		if (cmd == null) return false;
		if (!cmd.permission().test(event.getMember())) {
			event.getChannel().sendMessage(EmoteReference.STOP + "You have no permissions to trigger this command").queue();
			return false;
		}
		try {
			cmd.run(event, cmdname, content);
			return true;
		} catch (Throwable t) {
			LOGGER.error("Error running command " + cmdname, t);
			event.getChannel().sendMessage("There was an unexpected " + t.getClass().getSimpleName() + " running the command").queue();
			return false;
		}
	}

	public void register(String s, Command c) {
		commands.putIfAbsent(s, c);
	}
}
