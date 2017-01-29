package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandType;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;

import java.util.List;

public class ModerationCmds extends Module {

	public ModerationCmds() {
		super(Category.MODERATION);
		ban();
		kick();
	}

	private void kick() {
		super.register("kick", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				Guild guild = event.getGuild();
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				Message receivedMessage = event.getMessage();

				//We need to check if the member trying to kick the person has KICK_MEMBERS permission.
				if (guild.getMember(author).hasPermission(Permission.KICK_MEMBERS)) {
					//If they mentioned a user this gets passed, if they didn't it just doesn't.
					if (receivedMessage.getMentionedUsers().isEmpty()) {
						channel.sendMessage("\u274C" + "You must mention 1 or more users to be kicked!").queue();
					} else {
						Member selfMember = guild.getSelfMember();

						//Do I have permissions to kick members, if yes continue, if no end command.
						if (!selfMember.hasPermission(Permission.KICK_MEMBERS)) {
							channel.sendMessage("\u274C" + "Sorry! I don't have permission to kick members in this server!").queue();
							return;
						}

						List<User> mentionedUsers = receivedMessage.getMentionedUsers();
						//For all mentioned users in the command.
						for (User user : mentionedUsers) {
							Member member = guild.getMember(user);
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
										error.printStackTrace(); //TODO LOG THAT SHIT
									}
								});
						}
					}
				} else {
					channel.sendMessage("❌ Cannot kick. Possible errors: You have no Kick Members permission or this was triggered outside of a guild.").queue();
				}
			}

			@Override
			public CommandType commandType() {
				return CommandType.ADMIN;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Kick")
					.setDescription("Kicks the mentioned users.")
					.build();
			}
		});
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
				if (guild.getMember(author).hasPermission(Permission.BAN_MEMBERS)) {
					//If you mentioned someone to ban, continue.
					if (receivedMessage.getMentionedUsers().isEmpty()) {
						channel.sendMessage("\u274C" + "You need to mention at least one user to ban.").queue();
						return;
					}

					//For all mentioned members..
					List<User> mentionedUsers = receivedMessage.getMentionedUsers();
					for (User user : mentionedUsers) {
						Member member = guild.getMember(user);
						//If one of them is in a higher hierarchy than the bot, I cannot ban them.
						if (!guild.getSelfMember().canInteract(member)) {
							channel.sendMessage("\u274C" + "Cannot ban member " + member.getEffectiveName() + ", they are higher or the same " + "hierachy than I am!").queue();
							return;
						}

						//If I cannot ban, well..
						if (!guild.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
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
									error.printStackTrace(); //TODO LOG THAT SHIT
								}
							});
					}
				} else {
					channel.sendMessage("\u274C " + "Cannot ban. Possible errors: You have no Tools Members permission or this was triggered outside of a guild.").queue();
				}
			}

			@Override
			public CommandType commandType() {
				return CommandType.ADMIN;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Ban")
					.setDescription("Bans the mentioned users.")
					.build();
			}
		});
	}
}
