package net.kodehawa.mantarobot.module;

import net.kodehawa.mantarobot.module.Parser.CommandArguments;

public interface Command {
	CommandType commandType();

	String help();

	void invoke(CommandArguments cmd);

	boolean isHiddenFromHelp();
}
