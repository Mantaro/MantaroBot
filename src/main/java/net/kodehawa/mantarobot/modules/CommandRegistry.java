package net.kodehawa.mantarobot.modules;

import com.google.common.base.Preconditions;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.DBGuild;
import net.kodehawa.mantarobot.modules.commands.AliasCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.modules.commands.base.Command;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {

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
		Config conf = MantaroData.config().get();
		DBGuild dbg = MantaroData.db().getGuild(event.getGuild());

		if (cmd == null) return false;

		if (MantaroData.db().getGuild(event.getGuild()).getData().getDisabledChannels().contains(event.getChannel().getId()) && cmd.category() != Category.MODERATION) {
			return false;
		}

		if(MantaroData.config().get().isPremiumBot() && cmd.category() == Category.CURRENCY){
			return false;
		}

		//If we are in the patreon bot, deny all requests from unknown guilds.
		if(conf.isPremiumBot() && !conf.isOwner(event.getAuthor()) && !dbg.isPremium()){
			event.getChannel().sendMessage(EmoteReference.ERROR + "Seems like you're trying to use the Patreon bot when this guild is **not** marked as premium. " +
					"**If you think this is an error please contact Kodehawa#3457 or poke me on #donators in the support guild**").queue();
			return false;
		}

		if (!cmd.permission().test(event.getMember())) {
			event.getChannel().sendMessage(EmoteReference.STOP + "You have no permissions to trigger this command").queue();
			return false;
		}

		cmd.run(event, cmdname, content);
		return true;
	}

	public void register(String s, Command c) {
		commands.putIfAbsent(s, c);
	}

	public void registerAlias(String c, String o) {
		Preconditions.checkArgument(commands.containsKey(o), "Command don't exists");
		register(c, new AliasCommand(o, commands.get(o)));
	}
}
