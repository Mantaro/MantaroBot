package net.kodehawa.mantarobot.modules.commands.core;

import com.google.common.base.Preconditions;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.modules.commands.AliasCommand;
import net.kodehawa.mantarobot.modules.commands.SubCommand;
import net.kodehawa.mantarobot.modules.commands.SimpleTreeCommand;
import net.kodehawa.mantarobot.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.modules.commands.base.Command;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.ArrayList;
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
		GuildData data = dbg.getData();

		if (cmd == null) return false;

		if (data.getDisabledCommands().contains(cmdname)) {
			return false;
		}

		if(data.getChannelSpecificDisabledCommands().get(event.getChannel().getId()) != null &&
				data.getChannelSpecificDisabledCommands().get(event.getChannel().getId()).contains(cmdname)) {
			return false;
		}

		if(data.getDisabledUsers().contains(event.getAuthor().getId())) {
			return false;
		}

		if (MantaroData.db().getGuild(event.getGuild()).getData().getDisabledChannels().contains(event.getChannel().getId()) && cmd.category() != Category.MODERATION) {
			return false;
		}

		if(MantaroData.config().get().isPremiumBot() && cmd.category() == Category.CURRENCY) {
			return false;
		}

		if (data.getDisabledCategories().contains(cmd.category())) {
			return false;
		}

		if (data.getChannelSpecificDisabledCategories().computeIfAbsent(event.getChannel().getId(), wew -> new ArrayList<>()).contains(cmd.category())) {
			return false;
		}

		if(!data.getDisabledRoles().isEmpty() && event.getMember().getRoles().stream().map(ISnowflake::getId).anyMatch(s -> data.getDisabledRoles().contains(s))){
			return false;
		}

		//If we are in the patreon bot, deny all requests from unknown guilds.
		if(conf.isPremiumBot() && !conf.isOwner(event.getAuthor()) && !dbg.isPremium()) {
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

	public Command register(String s, Command c) {
		commands.putIfAbsent(s, c);
		return c;
	}

	public void registerAlias(String c, String o) {
		if(!commands.containsKey(c)) {
			System.out.println(c + " isn't in the command map...");
		}

		register(o, new AliasCommand(o, commands.get(c)));
	}

	public void addSubCommandTo(TreeCommand command, String name, SubCommand subCommand){
		command.addSubCommand(name, subCommand);
	}

	public void addSubCommandTo(SimpleTreeCommand command, String name, SubCommand subCommand){
		command.addSubCommand(name, subCommand);
	}
}
