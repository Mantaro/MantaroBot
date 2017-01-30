package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModerationCmds extends Module {
	private static final Logger LOGGER = LoggerFactory.getLogger("osu!");

	public ModerationCmds() {
		super(Category.MODERATION);
		ban();
		kick();
	}

	private void ban() {
		super.register("ban", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				//Initialize the variables I'll need.
				Guild guild = event.getGuild();
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				Message receivedMessage = event.getMessage();

				//We need to check if this is in a guild AND if the member trying to kick the person has KICK_MEMBERS permission.
				if (!guild.getMember(author).hasPermission(net.dv8tion.jda.core.Permission.BAN_MEMBERS)) {
					channel.sendMessage("\u274C " + "Cannot ban: You have no Ban Members permission.").queue();
					return;
				}

				if (receivedMessage.getMentionedUsers().isEmpty()) {
					channel.sendMessage("\u274C" + "You need to mention at least one user to ban.").queue();
					return;
				}

				//For all mentioned members..
				receivedMessage.getMentionedUsers().forEach(user -> {
					Member member = guild.getMember(user);
					if(member == null) return;
					//If one of them is in a higher hierarchy than the bot, I cannot ban them.
					if (!guild.getSelfMember().canInteract(member)) {
						channel.sendMessage("\u274C" + "Cannot ban member " + member.getEffectiveName() + ", they are higher or the same " + "hierachy than I am!").queue();
						return;
					}

					//If I cannot ban, well..
					if (!guild.getSelfMember().hasPermission(net.dv8tion.jda.core.Permission.BAN_MEMBERS)) {
						channel.sendMessage("\u274C" + "Sorry! I don't have permission to ban members in this server!").queue();
						return;
					}

					//Proceed to ban them. Again, using queue so I don't get rate limited.
					//Also delete all messages from past 7 days.
					guild.getController().ban(member, 7).queue(
						success -> channel.sendMessage(":zap: You will be missed... or not " + member.getEffectiveName()).queue(),
						error ->
						{
							if (error instanceof PermissionException) {
								channel.sendMessage("\u274C" + "Error banning " + member.getEffectiveName()
									+ ": " + "(No permission provided: " + ((PermissionException) error).getPermission() + ")").queue();
							} else {
								channel.sendMessage("\u274C" + "Unknown error while banning " + member.getEffectiveName()
									+ ": " + "<" + error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();

								//I need more information in the case of an unexpected error.

								LOGGER.warn("Unexpected error while banning someone.", error);
							}
						});
				});
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Ban")
					.setDescription("Bans the mentioned users.")
					.build();
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
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				Guild guild = event.getGuild();
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				Message receivedMessage = event.getMessage();

				//We need to check if the member trying to kick the person has KICK_MEMBERS permission.
				if (!guild.getMember(author).hasPermission(net.dv8tion.jda.core.Permission.KICK_MEMBERS)) {
					channel.sendMessage("❌ Cannot kick: You have no Kick Members permission.").queue();
					return;
				}

				//If they mentioned a user this gets passed, if they didn't it just doesn't.
				if (receivedMessage.getMentionedUsers().isEmpty()) {
					channel.sendMessage("\u274C" + "You must mention 1 or more users to be kicked!").queue();
					return;
				}

				Member selfMember = guild.getSelfMember();

				//Do I have permissions to kick members, if yes continue, if no end command.
				if (!selfMember.hasPermission(net.dv8tion.jda.core.Permission.KICK_MEMBERS)) {
					channel.sendMessage("\u274C" + "Sorry! I don't have permission to kick members in this server!").queue();
					return;
				}

				//For all mentioned users in the command.
				receivedMessage.getMentionedUsers().forEach(user -> {
					Member member = guild.getMember(user);
					if(member == null) return;

					//If one of them is in a higher hierarchy than the bot, cannot kick.
					if (!selfMember.canInteract(member)) {
						channel.sendMessage("\u274C" + "Cannot kick member: " + member.getEffectiveName() + ", they are higher or the same " + "hierachy than I am!").queue();
						return;
					}

					//Proceed to kick them. Again, using queue so I don't get rate limited.
					guild.getController().kick(member).queue(
						success -> channel.sendMessage(":zap: You will be missed... or not " + member.getEffectiveName()).queue(), //Quite funny, I think.
						error -> {
							if (error instanceof PermissionException) {
								channel.sendMessage(String.format("❌ Error kicking [%s]: (No permission provided: %s)", member.getEffectiveName(), ((PermissionException) error).getPermission())).queue();
							} else {
								channel.sendMessage(String.format("❌ Unknown error while kicking [%s]: <%s>: %s", member.getEffectiveName(), error.getClass().getSimpleName(), error.getMessage())).queue();

								//Just so I get more info in the case of an unexpected error.
								LOGGER.warn("Unexpected error while kicking someone.", error);
							}
						});
				});
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Kick")
					.setDescription("Kicks the mentioned users.")
					.build();
			}
		});
	}

	private void opts() {
		super.register("opts", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				if (args.length < 2) {
					onHelp(event);
					return;
				}

				String option = args[0];
				String action = args[1];

				if (option.equals("logs")) {
					if (action.equals("enable")) {
						return;
					}

					if (action.equals("disable")) {
						return;
					}
					onHelp(event);
					return;
				}

				if (option.equals("prefix")) {
					if (action.equals("set")) {
						return;
					}

					if (action.equals("clear")) {
						return;
					}
					onHelp(event);
					return;
				}

				if (option.equals("nsfw")) {
					if (action.equals("setchannel")) {
						return;
					}

					if (action.equals("disable")) {
						return;
					}
					onHelp(event);
					return;
				}

				if (option.equals("birthday")) {
					if (action.equals("enable")) {
						return;
					}

					if (action.equals("disable")) {
						return;
					}
					onHelp(event);
					return;
				}

				if (option.equals("music")) {
					if (action.equals("limit")) {
						return;
					}

					if (action.equals("clear")) {
						return;
					}
					onHelp(event);
					return;
				}

				onHelp(event);
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.ADMIN;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return null; //TODO Uhhh that one will be BIG.
			}
		});
	}
}
