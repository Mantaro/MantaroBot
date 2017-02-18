package net.kodehawa.mantarobot.commands;

import com.mashape.unirest.http.Unirest;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MiscCmds extends Module {
	private static final Logger LOGGER = LoggerFactory.getLogger("Audio");
	private List<User> users = new ArrayList<>();

	public MiscCmds() {
		super(Category.MISC);
		misc();
		lottery();
		eightBall();
		randomFact();
	}

	private void eightBall() {
		super.register("8ball", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (content.isEmpty()) {
					onHelp(event);
					return;
				}

				String textEncoded;
				String answer;
				try {
					textEncoded = URLEncoder.encode(content, "UTF-8");
					answer = Unirest.get(String.format("https://8ball.delegator.com/magic/JSON/%1s", textEncoded))
						.asJson()
						.getBody()
						.getObject()
						.getJSONObject("magic")
						.getString("answer");
				} catch (Exception exception) {
					event.getChannel().sendMessage("Error while fetching results. My owners have been notified.").queue();
					LOGGER.warn("Error while processing answer <@155867458203287552>", exception);
					return;
				}

				event.getChannel().sendMessage("\uD83D\uDCAC " + answer + ".").queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "8ball")
					.setDescription("Retrieves an answer from 8Ball. Requires a sentence.\n"
						+ "~>8ball [question]. Retrieves an answer from 8ball based on the question provided.")
					.build();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

		});
	}

	private void lottery() {
		super.register("lottery", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				User author = event.getAuthor();
				if (!users.contains(author)) {
					event.getChannel().sendMessage("\uD83D\uDCAC " + "You won **" + new Random().nextInt(350) + "USD**, congrats!").queue();
					users.add(author);
				} else {
					event.getChannel().sendMessage("\uD83D\uDCAC " + "Try again later! (Usable every 24 hours)").queue();
				}
				Async.asyncSleepThen(86400000, () -> users.remove(author));
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "lottery")
					.setDescription("Retrieves a random amount of money.")
					.build();
			}
		});
	}

	private void misc() {
		super.register("misc", new SimpleCommand() {
			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Misc Commands")
					.setDescription("Miscellaneous funny/useful commands. Ranges from funny commands and random colors to bot hardware information\n"
						+ "Usage:\n"
						+ "~>misc rob [@user]: Rob random amount of money from a user.\n"
						+ "~>misc reverse [sentence]: Reverses any given sentence.\n"
						+ "~>misc noble: Random Lost Pause quote.\n"
						+ "~>misc rndcolor: Gives you a random hex color.\n"
						+ "Parameter explanation:\n"
						+ "[sentence]: A sentence to reverse."
						+ "[@user]: A user to mention.")
					.build();
			}

			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				String mentioned = "";
				if (!event.getMessage().getMentionedUsers().isEmpty())
					mentioned = event.getMessage().getMentionedUsers().get(0).getAsMention();
				TextChannel channel = event.getChannel();
				String noArgs = content.split(" ")[0];
				switch (noArgs) {
					case "rob":
						channel.sendMessage("\uD83D\uDCAC " + "You robbed **" + new Random().nextInt(1000) + "USD** from " + mentioned).queue();
						break;
					case "reverse":
						String stringToReverse = content.replace("reverse ", "");
						String reversed = new StringBuilder(stringToReverse).reverse().toString();
						channel.sendMessage(reversed).queue();
						break;
					case "rndcolor":
						String s = String.format("\uD83D\uDCAC Your random color is %s", randomColor());
						channel.sendMessage(s).queue();
						break;
					case "noble":
						channel.sendMessage("\uD83D\uDCAC " + MantaroData.getNoble().get().get(new Random().nextInt(MantaroData.getNoble().get().size() - 1)) + " -Noble").queue();
						break;
					default:
						onHelp(event);
						break;
				}
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

		});
	}

	/**
	 * @return a random hex color.
	 */
	private String randomColor() {
		String[] letters = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
		String color = "#";
		for (int i = 0; i < 6; i++) {
			color += letters[(int) Math.floor(Math.random() * 16)];
		}
		return color;
	}

	//small af
	private void randomFact() {
		super.register("randomfact", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				event.getChannel().sendMessage("\uD83D\uDCAC " + MantaroData.getFacts().get().get(new Random().nextInt(MantaroData.getFacts().get().size() - 1))).queue();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Random Fact")
					.setDescription("Sends a random fact.")
					.build();
			}
		});
	}

}
