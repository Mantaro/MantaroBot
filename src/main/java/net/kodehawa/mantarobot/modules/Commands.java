package net.kodehawa.mantarobot.modules;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.kodehawa.mantarobot.modules.commands.Category;
import net.kodehawa.mantarobot.modules.commands.CommandPermission;
import net.kodehawa.mantarobot.modules.commands.builders.SimpleCommandBuilder;

public class Commands {

	public static MessageEmbed helpEmbed(String name, CommandPermission permission, String description, String usage) {
		String cmdname = Character.toUpperCase(name.charAt(0)) + name.substring(1) + " Command";
		String p = permission.name().toLowerCase();
		String perm = Character.toUpperCase(p.charAt(0)) + p.substring(1);
		return new EmbedBuilder()
			.setTitle(cmdname, null)
			.setDescription("\u200B")
			.addField("Permission required", perm, false)
			.addField("Description", description, false)
			.addField("Usage", usage, false)
			.build();
	}

	public static SimpleCommandBuilder newSimple(Category category) {
		return new SimpleCommandBuilder(category);
	}
}
