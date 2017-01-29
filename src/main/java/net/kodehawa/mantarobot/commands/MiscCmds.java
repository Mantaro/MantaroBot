package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.utils.UrbanData;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandType;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.Async;
import net.kodehawa.mantarobot.utils.GeneralUtils;
import net.kodehawa.mantarobot.utils.GsonDataManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MiscCmds extends Module {
	private static final Logger LOGGER = LoggerFactory.getLogger("Audio");
	private List<User> users = new ArrayList<>();

	public MiscCmds(){
		super(Category.MISC);
		misc();
		urban();
		lottery();
		eightBall();
		randomFact();
	}

	private void misc(){
		super.register("misc", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				String mentioned = "";
				if(!event.getMessage().getMentionedUsers().isEmpty()) mentioned = event.getMessage().getMentionedUsers().get(0).getAsMention();
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
						channel.sendMessage(help(event)).queue();
						break;
				}
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

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
		});
	}

	private void lottery(){
		super.register("lottery", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
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
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "lottery")
						.setDescription("Retrieves a random amount of money.")
						.build();
			}
		});
	}

	private void eightBall(){
		super.register("8ball", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				if (content.isEmpty()) {
					event.getChannel().sendMessage(help(event)).queue();
					return;
				}

				String textEncoded;
				String url2;

				try {
					textEncoded = URLEncoder.encode(content, "UTF-8");
				} catch (UnsupportedEncodingException exception) {
					event.getChannel().sendMessage("Error while encoding URL. My owners have been notified.").queue();
					LOGGER.warn("Error while encoding URL", exception);
					return;
				}

				String URL = String.format("https://8ball.delegator.com/magic/JSON/%1s", textEncoded);
				url2 = GeneralUtils.instance().restyGetObjectFromUrl(URL, event);

				JSONObject jObject = new JSONObject(url2);
				JSONObject data = jObject.getJSONObject("magic");

				event.getChannel().sendMessage("\uD83D\uDCAC " + data.getString("answer") + ".").queue();
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "8ball")
						.setDescription("Retrieves an answer from 8Ball. Requires a sentence.\n"
								+ "~>8ball [question]. Retrieves an answer from 8ball based on the question provided.")
						.build();
			}
		});
	}

	private void urban(){
		super.register("urban", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				//First split is definition, second one is number. I would use space but we need the ability to fetch with spaces too.
				String beheadedSplit[] = content.split("->");
				EmbedBuilder embed = new EmbedBuilder();

				if (!content.isEmpty()) {
					long start = System.currentTimeMillis();
					String url = null;
					try {
						url = "http://api.urbandictionary.com/v0/define?term=" + URLEncoder.encode(beheadedSplit[0], "UTF-8");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace(); //TODO LOG THAT SHIT
					}
					String json = GeneralUtils.instance().restyGetObjectFromUrl(url, event);
					UrbanData data = GsonDataManager.GSON.fromJson(json, UrbanData.class);

					long end = System.currentTimeMillis() - start;
					switch (beheadedSplit.length) {
						case 1:
							embed.setAuthor("Urban Dictionary definition for " + content, data.list.get(0).permalink, null)
									.setDescription("Main definition.")
									.setThumbnail("https://everythingfat.files.wordpress.com/2013/01/ud-logo.jpg")
									.setColor(Color.GREEN)
									.addField("Definition", data.list.get(0).definition, false)
									.addField("Thumbs up", data.list.get(0).thumbs_up, true)
									.addField("Thumbs down", data.list.get(0).thumbs_down, true)
									.addField("Example", data.list.get(0).example, false)
									.setFooter("Information by Urban Dictionary (Process time: " + end + "ms)", null);
							event.getChannel().sendMessage(embed.build()).queue();
							break;
						case 2:
							int defn = Integer.parseInt(beheadedSplit[1]) - 1;
							String defns = String.valueOf(defn + 1);
							embed.setAuthor("Urban Dictionary definition for " + beheadedSplit[0], data.list.get(defn).permalink, null)
									.setThumbnail("https://everythingfat.files.wordpress.com/2013/01/ud-logo.jpg")
									.setDescription("Definition " + defns)
									.setColor(Color.PINK)
									.addField("Definition", data.list.get(defn).definition, false)
									.addField("Thumbs up", data.list.get(defn).thumbs_up, true)
									.addField("Thumbs down", data.list.get(defn).thumbs_down, true)
									.addField("Example", data.list.get(defn).example, false)
									.setFooter("Information by Urban Dictionary", null);
							event.getChannel().sendMessage(embed.build()).queue();
							break;
						default:
							event.getChannel().sendMessage(help(event)).queue();
							break;
					}
				}
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Urban dictionary")
						.setColor(Color.CYAN)
						.setDescription("Retrieves definitions from *wo*Urban Dictionary**.\n"
								+ "Usage: \n"
								+ "~>urban [term]->[number]: Gets a definition based on parameters.\n"
								+ "Parameter description:\n"
								+ "[term]: The term you want to look up the urban definition for.\n"
								+ "[number]: **OPTIONAL** Parameter defined with the modifier '->' after the term. You don't need to use it.\n"
								+ "For example putting 2 will fetch the second result on Urban Dictionary")
						.build();
			}
		});
	}

	//small af
	private void randomFact(){
		super.register("randomfact", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				event.getChannel().sendMessage("\uD83D\uDCAC " + MantaroData.getFacts().get().get(new Random().nextInt(MantaroData.getFacts().get().size() - 1))).queue();
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Random Fact")
						.setDescription("Sends a random fact.")
						.build();
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
}
