package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.awt.Color;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ActionCmds extends Module {

	public ActionCmds() {
		super(Category.ACTION);
		pat();
		hug();
		action();
		greet();
		meow();
		kiss();
		tsundere();
		bloodsuck();
		lewd();
	}

	private void action() {
		super.register("action", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				String noArgs = content.split(" ")[0];
				TextChannel channel = event.getChannel();
				switch (noArgs) {
					case "facedesk":
						channel.sendMessage("http://puu.sh/rK6E7/0b745e5544.gif").queue();
						break;
					case "nom":
						channel.sendMessage("http://puu.sh/rK7t2/330182c282.gif").queue();
						break;
					case "bleach":
						channel.sendMessage(MantaroData.getBleach().get().get(new Random().nextInt(MantaroData.getBleach().get().size()))).queue();
						break;
					default:
						onHelp(event);
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Action commands")
					.addField("Description:", "~>action bleach: Random image of someone drinking bleach.\n" +
						"~>action facedesk: Facedesks.\n" +
						"~>action nom: nom nom.", false)
					.setColor(Color.PINK)
					.build();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

		});
	}

	private void bloodsuck() {
		super.register("bloodsuck", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				TextChannel channel = event.getChannel();
				if (event.getMessage().getMentionedUsers().isEmpty()) {
					channel.sendFile(Utils.toByteArray("http://imgur.com/ZR8Plmd.png"), "suck.png", null).queue();
				} else {
					String bString = event.getMessage().getMentionedUsers().stream().map(IMentionable::getAsMention).collect(Collectors.joining(" "));
					String bs = String.format(EmoteReference.TALKING + "%s sucks the blood of %s", event.getAuthor().getAsMention(), bString);
					channel.sendFile(Utils.toByteArray("http://imgur.com/ZR8Plmd.png"), "suck.png",
							new MessageBuilder().append(bs).build()).queue();
				}
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Bloodsuck")
					.setDescription("Sucks the blood of the mentioned user(s)")
					.build();
			}
		});
	}

	private void greet() {
		super.register("greet", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				event.getChannel().sendMessage(EmoteReference.TALKING + MantaroData.getGreeting().get().get(
					new Random().nextInt(MantaroData.getGreeting().get().size()))).queue();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Greeting")
					.setDescription("Sends a random greeting")
					.setColor(Color.DARK_GRAY)
					.build();
			}
		});
	}

	private void hug() {
		super.register("hug", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				List<String> hugs = MantaroData.getHugs().get();
				String hString = event.getMessage().getMentionedUsers().stream().map(IMentionable::getAsMention).collect(Collectors.joining(" "));
				byte[] toSend = Utils.toByteArray(hugs.get(new Random().nextInt(hugs.size())));
				if(toSend == null){
					event.getChannel().sendMessage(EmoteReference.ERROR + "Somehow we cannot convert the image to bytes. Maybe it;s down?").queue();
					return;
				}

				String hug = String.format(EmoteReference.TALKING + "%s you have been hugged by %s", hString, author.getAsMention());
				channel.sendFile(toSend, "hug.gif", new MessageBuilder().append(hug).build()).queue();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Hug command")
					.addField("Description:", "Hugs the specified user.", false)
					.setColor(Color.PINK)
					.build();
			}
		});
	}

	private void kiss() {
		super.register("kiss", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				List<String> kisses = MantaroData.getKisses().get();
				String kString = event.getMessage().getMentionedUsers().stream().map(IMentionable::getAsMention)
					.collect(Collectors.joining(" "));
				byte[] toSend = Utils.toByteArray(kisses.get(new Random().nextInt(kisses.size())));
				if(toSend == null){
					event.getChannel().sendMessage(EmoteReference.ERROR + "Somehow we cannot convert the image to bytes. Maybe it doesn't exist? Please report.").queue();
					return;
				}
				String kiss = String.format(EmoteReference.TALKING + "%s you have been kissed by %s", kString, author.getAsMention());
				channel.sendFile(toSend, "kiss.gif", new MessageBuilder().append(kiss).build()).queue();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Kiss command")
					.addField("Description:", "Kisses the specified user.", false)
					.setColor(Color.PINK)
					.build();
			}
		});
	}

	private void lewd() {
		super.register("lewd", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				String lood = event.getMessage().getMentionedUsers().stream().map(IMentionable::getAsMention).collect(Collectors.joining(" "));
				event.getChannel().sendFile(Utils.toByteArray("http://imgur.com/LJfZYau.png"), "lewd.png"
						, new MessageBuilder().append(lood).append(" Y-You lewdie!").build()).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Lewd")
					.setDescription("Y-You lewdie")
					.build();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

		});
	}

	private void meow() {
		super.register("mew", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				TextChannel channel = event.getChannel();
				Message receivedMessage = event.getMessage();
				if (!receivedMessage.getMentionedUsers().isEmpty()) {
					String mew = event.getMessage().getMentionedUsers().stream().map(IMentionable::getAsMention).collect(Collectors.joining(" "));
					channel.sendFile(Utils.toByteArray("http://imgur.com/yFGHvVR.gif"), "mew.gif",
							new MessageBuilder().append(EmoteReference.TALKING).append(String.format("*meows at %s.*", mew)).build()).queue();
				} else {
					channel.sendFile(Utils.toByteArray("http://imgur.com/yFGHvVR.gif"), "mew.gif",
							new MessageBuilder().append(":speech_balloon: Meeeeow.").build()).queue();
				}
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Meow command")
					.setDescription("Meows at a user or just meows.")
					.setColor(Color.cyan)
					.build();
			}
		});
	}

	private void pat() {
		super.register("pat", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				List<String> pats = MantaroData.getPatting().get();
				String pString = event.getMessage().getMentionedUsers().stream().map(IMentionable::getAsMention).collect(Collectors.joining(" "));
				byte[] toSend = Utils.toByteArray(pats.get(new Random().nextInt(pats.size())));
				if(toSend == null){
					event.getChannel().sendMessage(EmoteReference.ERROR + "Somehow we cannot convert the image to bytes. Maybe it doesn't exist? Please report.").queue();
					return;
				}

				String pat = String.format(EmoteReference.TALKING + "%s you have been patted by %s", pString, author.getAsMention());
				channel.sendFile(toSend, "pat.gif", new MessageBuilder().append(pat).build()).queue();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Pat command")
					.addField("Description:", "Pats the specified user.", false)
					.setColor(Color.PINK)
					.build();
			}
		});
	}

	private void tsundere() {
		super.register("tsundere", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				event.getChannel().sendMessage(EmoteReference.MEGA + MantaroData.getTsundereLines().get().get(new Random().nextInt(MantaroData.getTsundereLines().get().size()))).queue();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Tsundere command")
					.setDescription("Y-You baka!")
					.setColor(Color.pink)
					.build();
			}
		});
	}
}
