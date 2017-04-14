package net.kodehawa.mantarobot.modules;

import br.com.brjdevs.java.utils.functions.TriConsumer;
import com.google.common.base.Preconditions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.utils.QuadConsumer;
import net.kodehawa.mantarobot.utils.StringUtils;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface SimpleCommand extends Command {
	class Builder {
		private final Category category;
		private QuadConsumer<SimpleCommand, GuildMessageReceivedEvent, String, String[]> code;
		private String description;
		private BiFunction<SimpleCommand, GuildMessageReceivedEvent, MessageEmbed> help;
		private boolean hidden = false;
		private CommandPermission permission;
		private Function<String, String[]> splitter;

		public Builder(Category category) {
			this.category = category;
		}

		public Command build() {
			Preconditions.checkNotNull(code, "code");
			Preconditions.checkNotNull(permission, "permission");
			Preconditions.checkNotNull(description, "description");
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
				public String description() {
					return description;
				}

				@Override
				public MessageEmbed help(GuildMessageReceivedEvent event) {
					return help.apply(this, event);
				}

				@Override
				public boolean isHiddenFromHelp() {
					return hidden;
				}

				@Override
				public CommandPermission permission() {
					return permission;
				}
			};
		}

		public Builder code(QuadConsumer<SimpleCommand, GuildMessageReceivedEvent, String, String[]> code) {
			this.code = Preconditions.checkNotNull(code, "code");
			return this;
		}

		public Builder code(TriConsumer<GuildMessageReceivedEvent, String, String[]> code) {
			Preconditions.checkNotNull(code, "code");
			this.code = (thiz, event, name, args) -> code.accept(event, name, args);
			return this;
		}

		public Builder code(BiConsumer<GuildMessageReceivedEvent, String[]> code) {
			Preconditions.checkNotNull(code, "code");
			this.code = (thiz, event, name, args) -> code.accept(event, args);
			return this;
		}

		public Builder code(Consumer<GuildMessageReceivedEvent> code) {
			Preconditions.checkNotNull(code, "code");
			this.code = (thiz, event, name, args) -> code.accept(event);
			return this;
		}

		public Builder description(String description) {
			this.description = Preconditions.checkNotNull(description, "description");
			return this;
		}

		public Builder help(BiFunction<SimpleCommand, GuildMessageReceivedEvent, MessageEmbed> help) {
			this.help = Preconditions.checkNotNull(help, "help");
			return this;
		}

		public Builder help(Function<GuildMessageReceivedEvent, MessageEmbed> help) {
			Preconditions.checkNotNull(help, "help");
			this.help = (thiz, event) -> help.apply(event);
			return this;
		}

		public Builder hidden(boolean hidden) {
			this.hidden = hidden;
			return this;
		}

		public Builder permission(CommandPermission permission) {
			this.permission = Preconditions.checkNotNull(permission);
			return this;
		}

		public Builder splitter(Function<String, String[]> splitter) {
			this.splitter = splitter;
			return this;
		}
	}

	static Builder builder(Category category) {
		return new Builder(category);
	}

	@Override
	default void run(GuildMessageReceivedEvent event, String commandName, String content) {
		call(event, commandName, splitArgs(content));
	}

	static MessageEmbed helpEmbed(String name, CommandPermission permission, String description, String usage) {
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

	void call(GuildMessageReceivedEvent event, String commandName, String[] args);

	default EmbedBuilder baseEmbed(GuildMessageReceivedEvent event, String name) {
		return baseEmbed(event, name, event.getJDA().getSelfUser().getEffectiveAvatarUrl());
	}

	default EmbedBuilder baseEmbed(GuildMessageReceivedEvent event, String name, String image) {
		return new EmbedBuilder()
			.setAuthor(name, null, image)
			.setColor(event.getMember().getColor())
			.setFooter("Requested by " + event.getMember().getEffectiveName(), event.getAuthor().getEffectiveAvatarUrl());
	}

	default void doTimes(int times, Runnable runnable) {
		for (int i = 0; i < times; i++) {
			runnable.run();
		}
	}

	default EmbedBuilder helpEmbed(GuildMessageReceivedEvent event, String name) {
		return baseEmbed(event, name).addField("Permission required", permission().toString(), true);
	}

	default void onHelp(GuildMessageReceivedEvent event) {
		event.getChannel().sendMessage(help(event)).queue();
	}

	default String[] splitArgs(String content) {
		return StringUtils.advancedSplitArgs(content, 0);
	}




}
