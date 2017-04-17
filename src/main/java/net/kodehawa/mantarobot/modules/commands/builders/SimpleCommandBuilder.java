package net.kodehawa.mantarobot.modules.commands.builders;

import br.com.brjdevs.java.utils.functions.TriConsumer;
import com.google.common.base.Preconditions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.commands.Category;
import net.kodehawa.mantarobot.modules.commands.Command;
import net.kodehawa.mantarobot.modules.commands.CommandPermission;
import net.kodehawa.mantarobot.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.utils.QuadConsumer;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class SimpleCommandBuilder {
	private final Category category;
	private QuadConsumer<SimpleCommand, GuildMessageReceivedEvent, String, String[]> code;
	private BiFunction<SimpleCommand, GuildMessageReceivedEvent, MessageEmbed> help;
	private boolean hidden = false;
	private CommandPermission permission;
	private Function<String, String[]> splitter;

	public SimpleCommandBuilder(Category category) {
		this.category = category;
	}

	public Command build() {
		Preconditions.checkNotNull(code, "code");
		Preconditions.checkNotNull(permission, "permission");
		if (help == null)
			help = (t, e) -> new EmbedBuilder().setDescription("No help available for this command").build();
		return new SimpleCommand() {
			@Override
			public void call(GuildMessageReceivedEvent event, String cmdname, String[] args) {
				code.accept(this, event, cmdname, args);
			}

			@Override
			public String[] splitArgs(String content) {
				if (splitter == null)
					return SimpleCommand.super.splitArgs(content);
				return splitter.apply(content);
			}

			@Override
			public Category category() {
				return category;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return help.apply(this, event);
			}

			@Override
			public boolean hidden() {
				return hidden;
			}

			@Override
			public CommandPermission permission() {
				return permission;
			}
		};
	}

	public SimpleCommandBuilder code(QuadConsumer<SimpleCommand, GuildMessageReceivedEvent, String, String[]> code) {
		this.code = Preconditions.checkNotNull(code, "code");
		return this;
	}

	public SimpleCommandBuilder code(TriConsumer<GuildMessageReceivedEvent, String, String[]> code) {
		Preconditions.checkNotNull(code, "code");
		this.code = (thiz, event, name, args) -> code.accept(event, name, args);
		return this;
	}

	public SimpleCommandBuilder code(BiConsumer<GuildMessageReceivedEvent, String[]> code) {
		Preconditions.checkNotNull(code, "code");
		this.code = (thiz, event, name, args) -> code.accept(event, args);
		return this;
	}

	public SimpleCommandBuilder code(Consumer<GuildMessageReceivedEvent> code) {
		Preconditions.checkNotNull(code, "code");
		this.code = (thiz, event, name, args) -> code.accept(event);
		return this;
	}

	public SimpleCommandBuilder help(BiFunction<SimpleCommand, GuildMessageReceivedEvent, MessageEmbed> help) {
		this.help = Preconditions.checkNotNull(help, "help");
		return this;
	}

	public SimpleCommandBuilder help(Function<GuildMessageReceivedEvent, MessageEmbed> help) {
		Preconditions.checkNotNull(help, "help");
		this.help = (thiz, event) -> help.apply(event);
		return this;
	}

	public SimpleCommandBuilder hidden(boolean hidden) {
		this.hidden = hidden;
		return this;
	}

	public SimpleCommandBuilder permission(CommandPermission permission) {
		this.permission = Preconditions.checkNotNull(permission);
		return this;
	}

	public SimpleCommandBuilder splitter(Function<String, String[]> splitter) {
		this.splitter = splitter;
		return this;
	}
}
