package net.kodehawa.mantarobot.modules.commands;

import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.modules.commands.builders.SimpleCommandBuilder;

public class Commands {
	@Deprecated
	public static SimpleCommandBuilder newSimple(Category category) {
		return new SimpleCommandBuilder(category);
	}
}
