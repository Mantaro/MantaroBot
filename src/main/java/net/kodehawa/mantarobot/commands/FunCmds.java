package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.modules.CommandRegistry;
import net.kodehawa.mantarobot.modules.Event;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.OptionalInt;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Module
public class FunCmds {

	@Event
	public static void coinflip(CommandRegistry cr) {
		cr.register("coinflip", new SimpleCommand(Category.FUN) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				int times;
				if (args.length == 0 || content.length() == 0) times = 1;
				else {
					try {
						times = Integer.parseInt(args[0]);
						if (times > 1000) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "Whoah there! The limit is 1,000 coinflips").queue();
							return;
						}
					} catch (NumberFormatException nfe) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify an Integer for the amount of " +
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
				event.getChannel().sendMessage(EmoteReference.PENNY + " Your result from **" + times + "** " + flips + " yielded " +
					"**" + heads[0] + "** heads and **" + tails[0] + "** tails").queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Coinflip command")
					.setDescription("Flips a coin with a defined number of repetitions")
					.build();
			}
		});
	}

	@Event
	public static void dice(CommandRegistry cr) {
		cr.register("roll", new SimpleCommand(Category.FUN) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				int roll;
				try {
					roll = Integer.parseInt(args[0]);
				} catch (Exception e) {
					roll = 1;
				}
				if (roll >= 100) roll = 100;
				event.getChannel().sendMessage(EmoteReference.DICE + "You got **" + diceRoll(roll) + "** with a total of **" + roll
					+ "** repetitions.").queue();
				TextChannelGround.of(event).dropItemWithChance(6, 5);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Dice command")
					.setDescription("Roll a 6-sided dice a specified number of times")
					.build();
			}
		});
	}

	private static int diceRoll(int repetitions) {
		int num = 0;
		int roll;
		for (int i = 0; i < repetitions; i++) {
			roll = new Random().nextInt(6) + 1;
			num = num + roll;
		}
		return num;
	}

	@Event
	public static void marry(CommandRegistry cr) {
		cr.register("marry", new SimpleCommand(Category.FUN) {
			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (args.length > 0 && args[0].equals("divorce") || args[0].equals("anniversarystart")) {
					try {
						Player user = MantaroData.db().getPlayer(event.getMember());

						if (user.getData().getMarriedWith() == null) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "You aren't married with anyone, why don't you get started?").queue();
							return;
						}

						User user1 = user.getData().getMarriedWith() == null
								? null : MantaroBot.getInstance().getUserById(user.getData().getMarriedWith());
						Player marriedWith = MantaroData.db().getGlobalPlayer(user1);

						if(args[0].equals("anniversarystart")){
							if(user.getData().getMarriedSince() == null && user1 != null) {
								user.getData().setMarriedSince(System.currentTimeMillis());
								marriedWith.getData().setMarriedSince(System.currentTimeMillis());
								user.saveAsync();
								marriedWith.saveAsync();
								event.getChannel().sendMessage(EmoteReference.CORRECT + "Set anniversary date.").queue();
								return;
							}

							event.getChannel().sendMessage(EmoteReference.ERROR + "Either you're already married and your date is set or you're single :(").queue();
							return;
						}

						marriedWith.getData().setMarriedWith(null);
						marriedWith.getData().setMarriedSince(0L);
						user.getData().setMarriedWith(null);
						user.getData().setMarriedSince(0L);
						event.getChannel().sendMessage(EmoteReference.CORRECT + "Now you're single. I guess that's nice?").queue();
						marriedWith.save();
						user.save();
					} catch (NullPointerException e) {
						MantaroData.db().getPlayer(event.getMember()).getData().setMarriedWith(null);
						MantaroData.db().getPlayer(event.getMember()).getData().setMarriedSince(0L);
						MantaroData.db().getPlayer(event.getMember()).save();
						event.getChannel().sendMessage(EmoteReference.CORRECT + "Now you're single. I guess that's nice?").queue();
					}

					return;
				}

				if (event.getMessage().getMentionedUsers().isEmpty()) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "Mention the user you want to marry with.").queue();
					return;
				}

				User member = event.getAuthor();
				User user = event.getMessage().getMentionedUsers().get(0);

				if (user.getId().equals(event.getAuthor().getId())) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot marry with yourself.").queue();
					return;
				}

				if (user.isBot()) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot marry a bot.").queue();
					return;
				}

				if (MantaroData.db().getPlayer(event.getGuild().getMember(user)).getData().isMarried()) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "That user is married already.").queue();
					return;
				}

				if (MantaroData.db().getPlayer(event.getGuild().getMember(member)).getData().isMarried()) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You are married already.").queue();
					return;
				}

				event.getChannel().sendMessage(EmoteReference.MEGA + user.getName() + ", respond with **yes** or **no** to the marriage proposal from " + event.getAuthor().getName() + ".").queue();

				InteractiveOperations.create(event.getChannel(), "Marriage Proposal", (int) TimeUnit.SECONDS.toMillis(120), OptionalInt.empty(), (e) -> {
					if (!e.getAuthor().getId().equals(user.getId())) return false;

					if (e.getMessage().getContent().equalsIgnoreCase("yes")) {
						Player user1 = MantaroData.db().getPlayer(e.getMember());
						Player marry = MantaroData.db().getPlayer(e.getGuild().getMember(member));
						user1.getData().setMarriedWith(member.getId());
						marry.getData().setMarriedWith(e.getAuthor().getId());
						e.getChannel().sendMessage(EmoteReference.POPPER + e.getMember().getEffectiveName() + " accepted the proposal of " + member.getName() + "!").queue();
						user1.save();
						marry.save();
						return true;
					}

					if (e.getMessage().getContent().equalsIgnoreCase("no")) {
						e.getChannel().sendMessage(EmoteReference.CORRECT + "Denied proposal.").queue();
						return true;
					}

					return false;
				});
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Marriage command")
						.setDescription("Basically marries you with a user.")
						.addField("Usage", "~>marry <@mention>", false)
						.addField("Divorcing", "Well, if you don't want to be married anymore you can just do ~>marry divorce", false)
						.build();
			}
		});
	}
}
