package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandType;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ActionCmds extends Module {

	public ActionCmds(){
		super(Category.ACTION);
		pat();
		hug();
		action();
		greet();
		meow();
		tsundere();
	}

	private void action(){
		super.register("action", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				String noArgs = content.split(" ")[0];
				TextChannel channel = event.getChannel();
				Random rd = new Random();
				switch (noArgs) {
					case "facedesk":
						channel.sendMessage("http://puu.sh/rK6E7/0b745e5544.gif").queue();
						break;
					case "nom":
						channel.sendMessage("http://puu.sh/rK7t2/330182c282.gif").queue();
						break;
					case "bleach":
						channel.sendMessage(MantaroData.getData().get().bleach.get(new Random().nextInt(MantaroData.getData().get().bleach.size()))).queue();
						break;
					default:
						channel.sendMessage(help(event));
				}
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "ActionCmds commands")
						.addField("Description:", "~>action bleach: Random image of someone drinking bleach.\n" +
								"~>action facedesk: Facedesks.\n" +
								"~>action nom: nom nom.", false)
						.setColor(Color.PINK)
						.build();
			}
		});
	}

	private void pat(){
		super.register("pat", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				List<String> pats = MantaroData.getData().get().pat;
				Random rand = new Random();
				List<User> mentions = event.getMessage().getMentionedUsers();
				StringBuilder pString = new StringBuilder();
				mentions.forEach(pString::append);
				int i = rand.nextInt(pats.size());
				String pat = String.format(":speech_balloon: %s you have been patted by %s \n %s", pString, author.getAsMention(), pats.get(i));
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

	private void hug(){
		super.register("hug", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				List<String> hugs = MantaroData.getData().get().hugs;
				List<User> mentions = event.getMessage().getMentionedUsers();
				Random rand = new Random();
				StringBuilder hString = new StringBuilder();
				mentions.forEach(hString::append);
				int i = rand.nextInt(hugs.size());
				String hug = String.format(":speech_balloon: %s you have been hugged by %s \n %s", hString, author.getAsMention(), hugs.get(i));
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
						.build();			}
		});
	}

	private void greet(){
		super.register("greet", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				event.getChannel().sendMessage(":speech_balloon: " + MantaroData.getData().get().greet.get(
						new Random().nextInt(MantaroData.getData().get().greet.size()))).queue();
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

	private void meow(){
		super.register("mew", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				TextChannel channel = event.getChannel();
				Message receivedMessage = event.getMessage();
				if (!receivedMessage.getMentionedUsers().isEmpty()) {
					List<User> mentions = receivedMessage.getMentionedUsers();
					StringBuilder builder = new StringBuilder();
					mentions.forEach(builder::append);
					channel.sendMessage(String.format(":speech_balloon: *meows at* %s.* \n http://puu.sh/rK5Nf/63d90628c2.gif", builder.toString())).queue();
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

	private void tsundere(){
		super.register("tsundere", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				event.getChannel().sendMessage(":mega: " + MantaroData.getData().get().tsun.get(
						new Random().nextInt(MantaroData.getData().get().tsun.size()))).queue();
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
