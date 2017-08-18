package net.kodehawa.mantarobot.commands;

import br.com.brjdevs.java.utils.texts.StringUtils;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.info.CommandStatsManager;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.ITreeCommand;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Module
public class FunCmds {

	private final Random r = new Random();

	@Subscribe
	public void coinflip(CommandRegistry cr) {
		cr.register("coinflip", new SimpleCommand(Category.FUN) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				int times;
				if (args.length == 0 || content.length() == 0) times = 1;
				else {
					try {
						times = Integer.parseInt(args[0]);
						if (times > 1000) {
							event.getChannel().sendMessage(
								EmoteReference.ERROR + "Whoa there! The limit is 1,000 coinflips").queue();
							return;
						}
					} catch (NumberFormatException nfe) {
						event.getChannel().sendMessage(
							EmoteReference.ERROR + "You need to specify an Integer for the amount of " +
								"repetitions").queue();
						return;
					}
				}

				final int[] heads = {0};
				final int[] tails = {0};
				doTimes(times, () -> {
					if (new Random().nextBoolean()) heads[0]++;
					else tails[0]++;
				});
				String flips = times == 1 ? "time" : "times";
				event.getChannel().sendMessage(
					EmoteReference.PENNY + " Your result from **" + times + "** " + flips + " yielded " +
						"**" + heads[0] + "** heads and **" + tails[0] + "** tails").queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Coinflip command")
					.setDescription("**Flips a coin with a defined number of repetitions**")
					.addField("Usage", "`~>coinflip <number of times>` - **Flips a coin x number of times**", false)
					.build();
			}
		});
	}

	@Subscribe
	public void marry(CommandRegistry cr) {
		ITreeCommand marry = (ITreeCommand) cr.register("marry", new TreeCommand(Category.FUN) {
			@Override
			public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
				return new SubCommand() {
					@Override
					protected void call(GuildMessageReceivedEvent event, String content) {
						if (event.getMessage().getMentionedUsers().isEmpty()) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "Mention the user you want to marry.")
									.queue();
							return;
						}

						User member = event.getAuthor();
						User user = event.getMessage().getMentionedUsers().get(0);
						Player player = MantaroData.db().getPlayer(event.getMember());
						User user1 = player.getData().getMarriedWith() == null
								? null : MantaroBot.getInstance().getUserById(player.getData().getMarriedWith());

						if (user.getId().equals(event.getAuthor().getId())) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot marry yourself, as much as you may want to.").queue();
							return;
						}

						if (user.isBot()) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot marry a bot.").queue();
							return;
						}

						if (player.getData().isMarried()) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "That user is married already.").queue();
							return;
						}

						if (player.getData().isMarried() && user1 != null) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "You are married already.").queue();
							return;
						}

						if (InteractiveOperations.create(
								event.getChannel(), 120,
								(e) -> {
									if (!e.getAuthor().getId().equals(user.getId())) return Operation.IGNORED;

									if (e.getMessage().getContent().equalsIgnoreCase("yes")) {
										Player user11 = MantaroData.db().getPlayer(e.getMember());
										Player marry = MantaroData.db().getPlayer(e.getGuild().getMember(member));
										user11.getData().setMarriedWith(member.getId());
										marry.getData().setMarriedWith(e.getAuthor().getId());
										e.getChannel().sendMessage(EmoteReference.POPPER + e.getMember()
												.getEffectiveName() + " accepted the proposal of " + member.getName() + "!").queue();
										user11.save();
										marry.save();
										return Operation.COMPLETED;
									}

									if (e.getMessage().getContent().equalsIgnoreCase("no")) {
										e.getChannel().sendMessage(EmoteReference.CORRECT + "Denied proposal.").queue();
										return Operation.COMPLETED;
									}

									return Operation.IGNORED;
								}
						) != null) {
							TextChannelGround.of(event).dropItemWithChance(Items.LOVE_LETTER, 2);
							event.getChannel().sendMessage(EmoteReference.MEGA + user
									.getName() + ", respond with **yes** or **no** to the marriage proposal from " + event
									.getAuthor().getName() + ".").queue();

						} else {
							event.getChannel().sendMessage(EmoteReference.ERROR + "Another Interactive Operation is already running here").queue();
						}
					}
				};
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Marriage command")
					.setDescription("**Basically marries you with a user.**")
					.addField("Usage", "`~>marry <@mention>` - **Propose to someone**", false)
					.addField(
						"Divorcing", "Well, if you don't want to be married anymore you can just do `~>marry divorce`",
						false
					)
					.build();
			}
		});

		marry.addSubCommand("divorce", new SubCommand() {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content) {
				Player user = MantaroData.db().getPlayer(event.getMember());

				if (user.getData().getMarriedWith() == null) {
					event.getChannel().sendMessage(
							EmoteReference.ERROR + "You aren't married with anyone, why don't you find that special someone?")
							.queue();
					return;
				}

				User user1 = user.getData().getMarriedWith() == null
						? null : MantaroBot.getInstance().getUserById(user.getData().getMarriedWith());

				if (user1 == null) {
					user.getData().setMarriedWith(null);
					user.getData().setMarriedSince(0L);
					user.saveAsync();
					event.getChannel().sendMessage(
							EmoteReference.CORRECT + "Now you're single. That's nice I guess.").queue();
					return;
				}

				Player marriedWith = MantaroData.db().getPlayer(user1);

				marriedWith.getData().setMarriedWith(null);
				marriedWith.getData().setMarriedSince(0L);
				marriedWith.saveAsync();

				user.getData().setMarriedWith(null);
				user.getData().setMarriedSince(0L);
				user.saveAsync();
				event.getChannel().sendMessage(EmoteReference.CORRECT + "Now you're single. That's nice I guess.")
						.queue();
			}
		});
	}

	@Subscribe
	public void ratewaifu(CommandRegistry cr) {
		cr.register("ratewaifu", new SimpleCommand(Category.FUN) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {

				if (args.length == 0) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "Give me a waifu to rate!").queue();
					return;
				}

				int waifuRate = r.nextInt(100);
				if (content.equalsIgnoreCase("mantaro")) waifuRate = 100;

				event.getChannel().sendMessage(
					EmoteReference.THINKING + "I rate " + content + " with a **" + waifuRate + "/100**").queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Rate your waifu")
					.setDescription("**Just rates your waifu from zero to 100. Results may vary.**")
					.build();
			}
		});

		cr.registerAlias("ratewaifu", "rw");
	}

	@Subscribe
	public void roll(CommandRegistry registry) {
		registry.register("roll", new SimpleCommand(Category.FUN) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				Map<String, Optional<String>> opts = StringUtils.parse(args);

				int size = 6, amount = 1;

				if (opts.containsKey("size")) {
					try {
						size = Integer.parseInt(opts.get("size").orElse(""));
					} catch (Exception ignored) {}
				}

				if (opts.containsKey("amount")) {
					try {
						amount = Integer.parseInt(opts.get("amount").orElse(""));
					} catch (Exception ignored) {}
				} else if (opts.containsKey(null)) { //Backwards Compatibility
					try {
						amount = Integer.parseInt(opts.get(null).orElse(""));
					} catch (Exception ignored) {}
				}

				if (amount >= 100) amount = 100;
				event.getChannel().sendMessage(
					EmoteReference.DICE + "You got **" + diceRoll(size, amount) + "**" +
						(amount == 1 ? "!" : (", doing **" + amount + "** rolls."))
				).queue();

				TextChannelGround.of(event.getChannel()).dropItemWithChance(Items.LOADED_DICE, 5);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Dice command")
					.setDescription(
						"Roll a any-sided dice a 1 or more times\n" +
							"`~>roll [-amount <number>] [-size <number>]`: Rolls a dice of the specified size the specified times.\n" +
							"(By default, this command will roll a 6-sized dice 1 time.)"
					)
					.build();
			}
		});
	}

	@Subscribe
	public void love(CommandRegistry registry) {
		Random r = new Random();
		String[] usersToMax = {"155867458203287552;132584525296435200",
				"132584525296435200;155867458203287552","1558674582032875522;213466096718708737", "213466096718708737;1558674582032875522",
				"267207628965281792;251260900252712962", "251260900252712962;267207628965281792"};

		registry.register("love", new SimpleCommand(Category.FUN) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				List<User> mentioned = event.getMessage().getMentionedUsers();
				int percentage = r.nextInt(100);
				String result;

				if(mentioned.size() < 1) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention at least 1 user.").queue();
					return;
				}

				String ids = mentioned.get(0).getId() + ";" + event.getAuthor().getId();
				List<String> listDisplay = new ArrayList<>();
				String toDisplay;
				listDisplay.add("\uD83D\uDC97  " + mentioned.get(0).getName() + "#" + mentioned.get(0).getDiscriminator());
				listDisplay.add("\uD83D\uDC97  " + event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator());
				toDisplay = listDisplay.stream().collect(Collectors.joining("\n"));

				if(mentioned.size() == 2) {
					ids = mentioned.get(0).getId() + ";" + mentioned.get(1).getId();
					toDisplay = mentioned.stream()
							.map(user -> "\uD83D\uDC97  " + user.getName() + "#" + user.getDiscriminator()).collect(Collectors.joining("\n"));
				}

				final String matcher = ids;
				String[] yChecker = ids.split(";");
				boolean yCheck = yChecker[0].equalsIgnoreCase(yChecker[1]);
				if(Stream.of(usersToMax).anyMatch(s -> s.equals(matcher)) || yCheck) {
					percentage = 100;
				}

				if(percentage < 45) {
					result = "Try again next time...";
				} else if (percentage >= 45 && percentage < 75) {
					result = "Good enough!";
				} else if (percentage >= 75 && percentage < 100) {
					result = "Good match!";
				}  else {
					result = "Perfect match!";
					if (yCheck) {
						result = "You're a special creature and you should love yourself more than anyone <3";
					}
				}

				MessageEmbed loveEmbed = new EmbedBuilder()
						.setAuthor("\u2764 Love Meter \u2764", null, event.getAuthor().getEffectiveAvatarUrl())
						.setThumbnail("http://www.hey.fr/fun/emoji/twitter/en/twitter/469-emoji_twitter_sparkling_heart.png")
						.setDescription("\n**" + toDisplay + "**\n\n" +
								percentage + "% ||  " + CommandStatsManager.bar(percentage, 40) + "  **||** \n\n" +
								"**Result:** `"
								+ result + "`")
						.setColor(event.getMember().getColor())
						.build();

				event.getChannel().sendMessage(loveEmbed).queue();
			}
			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Love Meter")
						.setDescription("**Calculates the love between 2 discord users**")
						.addField("Considerations", "You can either mention one user (matches with yourself) or two (matches 2 users)", false)
						.build();
			}
		});
	}

	private long diceRoll(int size, int amount) {
		long sum = 0;
		for (int i = 0; i < amount; i++) sum += r.nextInt(size) + 1;
		return sum;
	}
}
