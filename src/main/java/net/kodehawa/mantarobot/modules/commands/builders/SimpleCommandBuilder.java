package net.kodehawa.mantarobot.modules.commands.builders;

import br.com.brjdevs.java.utils.functions.TriConsumer;
import com.google.common.base.Preconditions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.commands.CommandPermission;
import net.kodehawa.mantarobot.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.modules.commands.base.Command;
import net.kodehawa.mantarobot.utils.QuadConsumer;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class SimpleCommandBuilder {
	private final Category category;
	private QuadConsumer<SimpleCommand, GuildMessageReceivedEvent, String, String[]> code;
	private BiFunction<SimpleCommand, GuildMessageReceivedEvent, MessageEmbed> help;
	private CommandPermission permission;
	private Function<String, String[]> splitter;

	public SimpleCommandBuilder(Category category) {
		this.category = category;
	}

	public Command build() {
		Preconditions.checkNotNull(code, "code");
		if (help == null)
			help = (t, e) -> new EmbedBuilder().setDescription("No help available for this command").build();
		return new SimpleCommand(category) {
			@Override
			public Category category() {
				return category;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return help.apply(this, event);
			}

			@Override
			public CommandPermission permission() {
				return permission;
			}

			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {
				code.accept(this, event, content, args);
			}

			@Override
			public String[] splitArgs(String content) {
				return splitter == null ? super.splitArgs(content) : splitter.apply(content);
			}
		};
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

	public SimpleCommandBuilder onCall(BiConsumer<GuildMessageReceivedEvent, String[]> code) {
		Preconditions.checkNotNull(code, "code");
		this.code = (thiz, event, content, args) -> code.accept(event, args);
		return this;
	}

	public SimpleCommandBuilder onCall(Consumer<GuildMessageReceivedEvent> code) {
		Preconditions.checkNotNull(code, "code");
		this.code = (thiz, event, content, args) -> code.accept(event);
		return this;
	}

	public SimpleCommandBuilder onCall(QuadConsumer<SimpleCommand, GuildMessageReceivedEvent, String, String[]> code) {
		this.code = Preconditions.checkNotNull(code, "code");
		return this;
	}

	public SimpleCommandBuilder onCall(TriConsumer<GuildMessageReceivedEvent, String, String[]> code) {
		Preconditions.checkNotNull(code, "code");
		this.code = (thiz, event, content, args) -> code.accept(event, content, args);
		return this;
	}

	public SimpleCommandBuilder permission(CommandPermission permission) {
		this.permission = permission;
		return this;
	}

	public SimpleCommandBuilder splitter(Function<String, String[]> splitter) {
		this.splitter = splitter;
		return this;
	}
}
