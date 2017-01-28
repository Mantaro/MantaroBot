package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandType;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;

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
		tsundere();
	}

	private void action() {
		super.register("action", new SimpleCommand() {
			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
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
						channel.sendMessage(MantaroData.getBleach().get().get(new Random().nextInt(MantaroData.getBleach().get().size() - 1))).queue();
						break;
					default:
						channel.sendMessage(help(event)).queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Action commands")
					.addField("Description:", "~>action bleach: Random image of someone drinking bleach.\n" +
						"~>action facedesk: Facedesks.\n" +
						"~>action nom: nom nom.", false)
					.setColor(Color.PINK)
					.build();
			}
		});
	}

	private void greet() {
		super.register("greet", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				event.getChannel().sendMessage(":speech_balloon: " + MantaroData.getGreeting().get().get(
					new Random().nextInt(MantaroData.getGreeting().get().size()))).queue();
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Greeting")
					.setDescription("Sends a random greeting")
					.setColor(Color.DARK_GRAY)
					.build();
			}
		});
	}

	private void hug() {
		super.register("hug", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				List<String> hugs = MantaroData.getHugs().get();
				String hString = event.getMessage().getMentionedUsers().stream().map(IMentionable::getAsMention).collect(Collectors.joining(" "));
				String hug = String.format(":speech_balloon: %s you have been hugged by %s \n %s", hString, author.getAsMention(), hugs.get(new Random().nextInt(hugs.size())));
				channel.sendMessage(hug).queue();
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Hug command")
					.addField("Description:", "Hugs the specified user.", false)
					.setColor(Color.PINK)
					.build();
			}
		});
	}

	private void meow() {
		super.register("mew", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				TextChannel channel = event.getChannel();
				Message receivedMessage = event.getMessage();
				if (!receivedMessage.getMentionedUsers().isEmpty()) {
					String mew = event.getMessage().getMentionedUsers().stream().map(IMentionable::getAsMention).collect(Collectors.joining(" "));
					channel.sendMessage(String.format(":speech_balloon: *meows at* %s.* \n http://puu.sh/rK5Nf/63d90628c2.gif", mew)).queue();
				} else {
					channel.sendMessage(":speech_balloon: Meeeeow.\n http://puu.sh/rK5K7/034039286e.gif").queue();
				}
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Meow command")
					.setDescription("Meows at a user or just meows.")
					.setColor(Color.cyan)
					.build();
			}
		});
	}

	private void pat() {
		super.register("pat", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				List<String> pats = MantaroData.getPatting().get();
				Random rand = new Random();
				String pString = event.getMessage().getMentionedUsers().stream().map(IMentionable::getAsMention).collect(Collectors.joining(" "));
				String pat = String.format(":speech_balloon: %s you have been patted by %s \n %s", pString, author.getAsMention(), pats.get(new Random().nextInt(pats.size())));
				channel.sendMessage(pat).queue();
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Pat command")
					.addField("Description:", "Pats the specified user	.", false)
					.setColor(Color.PINK)
					.build();
			}
		});
	}

	private void tsundere() {
		super.register("tsundere", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				event.getChannel().sendMessage(":mega: " + MantaroData.getTsundereLines().get().get(new Random().nextInt(MantaroData.getTsundereLines().get().size()))).queue();
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Tsundere command")
					.setDescription("Y-You baka!")
					.setColor(Color.pink)
					.build();
			}
		});
	}
}
