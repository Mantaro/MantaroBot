package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.moderation.ModLog;
import net.kodehawa.mantarobot.commands.rpg.TextChannelGround;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.DBGuild;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ModerationCmds extends Module {
	private static final Logger LOGGER = LoggerFactory.getLogger("Moderation");
	private static final Pattern pattern = Pattern.compile("\\d+?[a-zA-Z]");

	public static Iterable<String> iterate(Matcher matcher) {
		return new Iterable<String>() {
			@Override
			public Iterator<String> iterator() {
				return new Iterator<String>() {
					@Override
					public boolean hasNext() {
						return matcher.find();
					}

					@Override
					public String next() {
						return matcher.group();
					}
				};
			}

			@Override
			public void forEach(Consumer<? super String> action) {
				while (matcher.find()) {
					action.accept(matcher.group());
				}
			}
		};
	}

	private static long parse(String s) {
		s = s.toLowerCase();
		long[] time = {0};
		iterate(pattern.matcher(s)).forEach(string -> {
			String l = string.substring(0, string.length() - 1);
			TimeUnit unit;
			switch (string.charAt(string.length() - 1)) {
				case 's':
					unit = TimeUnit.SECONDS;
					break;
				case 'm':
					unit = TimeUnit.MINUTES;
					break;
				case 'h':
					unit = TimeUnit.HOURS;
					break;
				case 'd':
					unit = TimeUnit.DAYS;
					break;
				default:
					unit = TimeUnit.SECONDS;
					break;
			}
			time[0] += unit.toMillis(Long.parseLong(l));
		});
		return time[0];
	}

	public ModerationCmds() {
		super(Category.MODERATION);
		ban();
		kick();
		tempban();
		prune();
	}

	private void ban() {
		super.register("ban", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				Guild guild = event.getGuild();
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				Message receivedMessage = event.getMessage();
				String reason = content;

				if (!guild.getMember(author).hasPermission(net.dv8tion.jda.core.Permission.BAN_MEMBERS)) {
					channel.sendMessage(EmoteReference.ERROR + "Cannot ban: You have no Ban Members permission.").queue();
					return;
				}

				if (receivedMessage.getMentionedUsers().isEmpty()) {
					channel.sendMessage(EmoteReference.ERROR + "You need to mention at least one user to ban.").queue();
					return;
				}

				for (User user : event.getMessage().getMentionedUsers()) {
					reason = reason.replaceAll("(\\s+)?<@!?" + user.getId() + ">(\\s+)?", "");
				}

				if (reason.isEmpty()) {
					reason = "Not specified";
				}

				final String finalReason = reason;

				receivedMessage.getMentionedUsers().forEach(user -> {
					if (!event.getGuild().getMember(event.getAuthor()).canInteract(event.getGuild().getMember(user))) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot ban an user in a higher hierarchy than you").queue();
						return;
					}

					if (event.getAuthor().getId().equals(user.getId())) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "Why are you trying to ban yourself?").queue();
						return;
					}

					Member member = guild.getMember(user);
					if (member == null) return;
					if (!guild.getSelfMember().canInteract(member)) {
						channel.sendMessage(EmoteReference.ERROR + "Cannot ban member " + member.getEffectiveName() + ", they are higher or the same " + "hierachy than I am!").queue();
						return;
					}

					if (!guild.getSelfMember().hasPermission(net.dv8tion.jda.core.Permission.BAN_MEMBERS)) {
						channel.sendMessage(EmoteReference.ERROR + "Sorry! I don't have permission to ban members in this server!").queue();
						return;
					}
					final DBGuild db = MantaroData.db().getGuild(event.getGuild());

					guild.getController().ban(member, 7).queue(
						success -> {
							user.openPrivateChannel().complete().sendMessage(EmoteReference.MEGA + "You were **banned** by " + event.getAuthor().getName() + "#"
								+ event.getAuthor().getDiscriminator() + " with reason: " + finalReason + ".").queue();
							db.getData().setCases(db.getData().getCases() + 1);
							db.saveAsync();
							channel.sendMessage(EmoteReference.ZAP + "You will be missed... or not " + member.getEffectiveName()).queue();
							ModLog.log(event.getMember(), user, finalReason, ModLog.ModAction.BAN, db.getData().getCases());
							TextChannelGround.of(event).dropItemWithChance(1, 2);
						},
						error ->
						{
							if (error instanceof PermissionException) {
								channel.sendMessage(EmoteReference.ERROR + "Error banning " + member.getEffectiveName()
									+ ": " + "(No permission provided: " + ((PermissionException) error).getPermission() + ")").queue();
							} else {
								channel.sendMessage(EmoteReference.ERROR + "Unknown error while banning " + member.getEffectiveName()
									+ ": " + "<" + error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();

								LOGGER.warn("Unexpected error while banning someone.", error);
							}
						});
				});
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Ban")
					.setDescription("Bans the mentioned users.")
					.build();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

		});
	}

	private void kick() {
		super.register("kick", new SimpleCommand() {
			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				Guild guild = event.getGuild();
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				Message receivedMessage = event.getMessage();
				String reason = content;

				if (!guild.getMember(author).hasPermission(net.dv8tion.jda.core.Permission.KICK_MEMBERS)) {
					channel.sendMessage(EmoteReference.ERROR2 + "Cannot kick: You have no Kick Members permission.").queue();
					return;
				}

				if (receivedMessage.getMentionedUsers().isEmpty()) {
					channel.sendMessage(EmoteReference.ERROR + "You must mention 1 or more users to be kicked!").queue();
					return;
				}

				Member selfMember = guild.getSelfMember();

				if (!selfMember.hasPermission(net.dv8tion.jda.core.Permission.KICK_MEMBERS)) {
					channel.sendMessage(EmoteReference.ERROR2 + "Sorry! I don't have permission to kick members in this server!").queue();
					return;
				}

				for (User user : event.getMessage().getMentionedUsers()) {
					reason = reason.replaceAll("(\\s+)?<@!?" + user.getId() + ">(\\s+)?", "");
				}

				if (reason.isEmpty()) {
					reason = "Not specified";
				}

				final String finalReason = reason;

				receivedMessage.getMentionedUsers().forEach(user -> {
					if (!event.getGuild().getMember(event.getAuthor()).canInteract(event.getGuild().getMember(user))) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot kick an user in a higher hierarchy than you").queue();
						return;
					}

					if (event.getAuthor().getId().equals(user.getId())) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "Why are you trying to kick yourself?").queue();
						return;
					}

					Member member = guild.getMember(user);
					if (member == null) return;

					//If one of them is in a higher hierarchy than the bot, cannot kick.
					if (!selfMember.canInteract(member)) {
						channel.sendMessage(EmoteReference.ERROR2 + "Cannot kick member: " + member.getEffectiveName() + ", they are higher or the same " + "hierachy than I am!").queue();
						return;
					}
					final DBGuild db = MantaroData.db().getGuild(event.getGuild());

					//Proceed to kick them. Again, using queue so I don't get rate limited.
					guild.getController().kick(member).queue(
						success -> {
							user.openPrivateChannel().complete().sendMessage(EmoteReference.MEGA + "You were **banned** by " + event.getAuthor().getName() + "#"
								+ event.getAuthor().getDiscriminator() + " with reason: " + finalReason + ".").queue();
							db.getData().setCases(db.getData().getCases() + 1);
							db.saveAsync();
							channel.sendMessage(EmoteReference.ZAP + "You will be missed... or not " + member.getEffectiveName()).queue(); //Quite funny, I think.
							ModLog.log(event.getMember(), user, finalReason, ModLog.ModAction.KICK, db.getData().getCases());
							TextChannelGround.of(event).dropItemWithChance(2, 2);
						},
						error -> {
							if (error instanceof PermissionException) {
								channel.sendMessage(String.format(EmoteReference.ERROR + "Error kicking [%s]: (No permission provided: %s)", member.getEffectiveName(), ((PermissionException) error).getPermission())).queue();
							} else {
								channel.sendMessage(String.format(EmoteReference.ERROR + "Unknown error while kicking [%s]: <%s>: %s", member.getEffectiveName(), error.getClass().getSimpleName(), error.getMessage())).queue();
								LOGGER.warn("Unexpected error while kicking someone.", error);
							}
						});
				});
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Kick")
					.setDescription("Kicks the mentioned users.")
					.build();
			}
		});
	}

	private void opts() {

	}

	private void prune() {
		super.register("prune", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				TextChannel channel = event.getChannel();
				if (content.isEmpty()) {
					channel.sendMessage(EmoteReference.ERROR + "You specified no messages to prune.").queue();
					return;
				}

				if (!event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "I cannot prune on this server since I don't have permission: Manage Messages").queue();
					return;
				}

				if (content.startsWith("bot")) {
					channel.getHistory().retrievePast(100).queue(
						messageHistory -> {
							String prefix = MantaroData.db().getGuild(event.getGuild()).getData().getGuildCustomPrefix();
							messageHistory = messageHistory.stream().filter(message -> message.getAuthor().isBot() ||
								message.getContent().startsWith(prefix == null ? "~>" : prefix)).collect(Collectors.toList());

							if (messageHistory.isEmpty()) {
								event.getChannel().sendMessage(EmoteReference.ERROR + "There are no messages from bots or bot calls here.").queue();
								return;
							}

							final int size = messageHistory.size();

							channel.deleteMessages(messageHistory).queue(
								success -> channel.sendMessage(EmoteReference.PENCIL + "Successfully pruned " + size + " bot messages").queue(),
								error -> {
									if (error instanceof PermissionException) {
										PermissionException pe = (PermissionException) error;
										channel.sendMessage(EmoteReference.ERROR + "Lack of permission while pruning messages" +
											"(No permission provided: " + pe.getPermission() + ")").queue();
									} else {
										channel.sendMessage(EmoteReference.ERROR + "Unknown error while pruning messages" + "<"
											+ error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();
										error.printStackTrace();
									}
								});

						},
						error -> {
							channel.sendMessage(EmoteReference.ERROR + "Unknown error while retrieving the history to prune the messages" + "<"
								+ error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();
							error.printStackTrace();
						}
					);
					return;
				}
				int i = Integer.parseInt(content);

				if (i <= 5) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You need to provide at least 5 messages.").queue();
					return;
				}

				channel.getHistory().retrievePast(Math.min(i, 100)).queue(
					messageHistory -> {
						messageHistory = messageHistory.stream().filter(message -> !message.getCreationTime()
							.isBefore(OffsetDateTime.now().minusWeeks(2)))
							.collect(Collectors.toList());

						if (messageHistory.isEmpty()) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "There are no messages newer than 2 weeks old, discord won't let me delete them.").queue();
							return;
						}

						final int size = messageHistory.size();

						channel.deleteMessages(messageHistory).queue(
							success -> channel.sendMessage(EmoteReference.PENCIL + "Successfully pruned " + size + " messages").queue(),
							error -> {
								if (error instanceof PermissionException) {
									PermissionException pe = (PermissionException) error;
									channel.sendMessage(EmoteReference.ERROR + "Lack of permission while pruning messages" +
										"(No permission provided: " + pe.getPermission() + ")").queue();
								} else {
									channel.sendMessage(EmoteReference.ERROR + "Unknown error while pruning messages" + "<"
										+ error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();
									error.printStackTrace();
								}
							});
					},
					error -> {
						channel.sendMessage(EmoteReference.ERROR + "Unknown error while retrieving the history to prune the messages" + "<"
							+ error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();
						error.printStackTrace();
					}
				);
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.ADMIN;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Prune command")
					.setDescription("Prunes a specific amount of messages.")
					.addField("Usage", "~>prune <x> - Prunes messages", false)
					.addField("Parameters", "x = number of messages to delete", false)
					.addField("Important", "You need to provide at least 3 messages. I'd say better 10 or more.\nYou can use ~>prune bot to remove all bot messages and bot calls.", false)
					.build();
			}
		});
	}

	private void tempban() {
		super.register("tempban", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				String reason = content;
				Guild guild = event.getGuild();
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				Message receivedMessage = event.getMessage();

				if (!guild.getMember(author).hasPermission(net.dv8tion.jda.core.Permission.BAN_MEMBERS)) {
					channel.sendMessage(EmoteReference.ERROR + "Cannot ban: You have no Ban Members permission.").queue();
					return;
				}

				if (event.getMessage().getMentionedUsers().isEmpty()) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention an user!").queue();
					return;
				}

				for (User user : event.getMessage().getMentionedUsers()) {
					reason = reason.replaceAll("(\\s+)?<@!?" + user.getId() + ">(\\s+)?", "");
				}
				int index = reason.indexOf("time:");
				if (index < 0) {
					event.getChannel().sendMessage(EmoteReference.ERROR +
						"You cannot temp ban an user without giving me the time!").queue();
					return;
				}
				String time = reason.substring(index);
				reason = reason.replace(time, "").trim();
				time = time.replaceAll("time:(\\s+)?", "");
				if (reason.isEmpty()) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot temp ban someone without a reason.!").queue();
					return;
				}

				if (time.isEmpty()) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot temp ban someone without giving me the time!").queue();
					return;
				}

				final DBGuild db = MantaroData.db().getGuild(event.getGuild());
				long l = parse(time);
				String finalReason = reason;
				String sTime = StringUtils.parseTime(l);
				receivedMessage.getMentionedUsers().forEach(user -> {
					user.openPrivateChannel().complete().sendMessage(EmoteReference.MEGA + "You were **temporarly banned** by " + event.getAuthor().getName() + "#"
						+ event.getAuthor().getDiscriminator() + " with reason: " + finalReason + ".").queue();
					db.getData().setCases(db.getData().getCases() + 1);
					db.saveAsync();
					channel.sendMessage(EmoteReference.ZAP + "You will be missed... or not " + event.getMember().getEffectiveName()).queue();
					ModLog.log(event.getMember(), user, finalReason, ModLog.ModAction.TEMP_BAN, db.getData().getCases(), sTime);
					MantaroBot.getInstance().getTempBanManager().addTempban(
						guild.getId() + ":" + user.getId(), l + System.currentTimeMillis());
					TextChannelGround.of(event).dropItemWithChance(1, 2);
				});
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Tempban Command")
					.setDescription("Temporarly bans an user")
					.addField("Usage", "~>tempban <user> <reason> time:<time>", false)
					.addField("Example", "~>tempban @Kodehawa example time:1d", false)
					.addField("Extended usage", "time: can be used with the following parameters: " +
						"d (days), s (second), m (minutes), h (hour). For example time:1d1h will give a day and an hour.", false)
					.build();
			}
		});
	}
}
