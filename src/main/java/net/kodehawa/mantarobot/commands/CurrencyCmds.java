package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.inventory.ItemStack;
import net.kodehawa.mantarobot.commands.currency.inventory.Items;
import net.kodehawa.mantarobot.commands.currency.inventory.TextChannelGround;
import net.kodehawa.mantarobot.data.Data.UserData;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.Async;
import net.kodehawa.mantarobot.utils.Expirator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CurrencyCmds extends Module {
	public CurrencyCmds() {
		super(Category.CURRENCY);

		profile();
		loot();
		summon();

		/*
		TODO NEXT:
		 - inventory command
		 - sell command
		 - loot command
		 - transfer command
		 - gamble command
		 */
	}

	private void gamble() {
		Random r = new Random();
		super.register("gamble", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				UserData user = MantaroData.getData().get().getUser(event.getAuthor(), true);

				double multiplier;
				int i;
				int luck;
				try {
					if (content.equals("all") || content.equals("everything") || content.equals("")) {
						i = user.money;
						multiplier = 1.5d + (r.nextInt(1500) / 1000d);
						luck = 60 + (int) (multiplier * 10) + r.nextInt(10);
					} else {
						i = Integer.parseInt(content);
						if (i > user.money) throw new UnsupportedOperationException();
						multiplier = 1.2d + (i / user.money * r.nextInt(1300) / 1000d);
						luck = 15 + (int) (multiplier * 30) + r.nextInt(10);
					}
				} catch (NumberFormatException e) {
					//TODO INVALID NUMBER
					return;
				} catch (UnsupportedOperationException e) {
					//TODO NOT ENOUGH MONEY
					return;
				}

				if (luck > r.nextInt(100)) {
					int gains = (int) (i * multiplier);
					if (user.addMoney(gains)) {

					} else {
						//TODO
					}
				} else {
					user.money = Math.min(0, user.money - i);
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return null;
			}
		});
	}

	private void loot() {
		Random r = new Random();
		Expirator expirator = new Expirator();
		List<String> usersRatelimited = new ArrayList<>();

		super.register("loot", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				String id = event.getAuthor().getId();

				if (usersRatelimited.contains(id)) {
					event.getChannel().sendMessage(":stopwatch:" +
						"Cooldown a lil bit, you're ratelimited right now so maybe wait a little bit more and let other people loot.").queue();
					return;
				}

				usersRatelimited.add(id);
				expirator.letExpire(System.currentTimeMillis() + 10000, () -> usersRatelimited.remove(id));

				UserData userData = MantaroData.getData().get().getUser(event.getAuthor(), true);
				List<ItemStack> loot = TextChannelGround.of(event).collect();
				int moneyFound = Math.min(0, r.nextInt(400) - 300);

				if (!loot.isEmpty()) {
					String s = ItemStack.toString(loot);
					if (moneyFound != 0) {

						if (userData.addMoney(moneyFound)) {
							event.getChannel().sendMessage("Digging through messages, you found " + s + ", along with" + moneyFound + " credits!").queue();
						} else {
							event.getChannel().sendMessage("Digging through messages, you found " + s + ", along with" + moneyFound + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java integer. Here's a buggy money bag for you.");
						}
					} else {
						event.getChannel().sendMessage("Digging through messages, you found " + s).queue();
					}
				} else {
					if (moneyFound != 0) {
						if (userData.addMoney(moneyFound)) {
							event.getChannel().sendMessage("Digging through messages, you found " + moneyFound + " credits!").queue();
						} else {
							event.getChannel().sendMessage("Digging through messages, you found " + moneyFound + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java integer. Here's a buggy money bag for you.");
						}
					} else {
						event.getChannel().sendMessage("Digging through messages, you found nothing but dust").queue();
					}
				}

				userData.getInventory().merge(loot);

				MantaroData.getData().update();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Loot command")
					.setDescription("Loots the current chat for items, for usage in Mantaro's currency system.\n"
						+ "Currently, there are ``" + Items.ALL.length + "`` items avaliable in chance," +
						"for which you have a random chance of getting one or more.")
					.addField("Usage", "~>loot", false)
					.build();
			}
		});
	}

	private void lottery() {
		//TODO @AdrianTodt re-do this with Currency system + Expirator
		List<User> users = new ArrayList<>();
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

	private void profile() {
		super.register("profile", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				UserData data = MantaroData.getData().get().getUser(event.getAuthor(), false);
				event.getChannel().sendMessage(baseEmbed(event, event.getMember().getEffectiveName() + "'s Profile", event.getAuthor().getEffectiveAvatarUrl())
					.addField(":credit_card: Credits", "$ " + data.money, false)
					.addField(":pouch: Inventory", ItemStack.toString(data.getInventory().asList()), false)
					.build()
				).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Profile command.")
					.setDescription("Retrieves your current user profile.")
					.build();
			}
		});
	}

	//TODO Remove before release
	private void summon() {
		super.register("summon", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				Random r = new Random(System.currentTimeMillis());
				List<ItemStack> toDrop = ItemStack.stackfy(Stream.of(Items.ALL).filter(item -> r.nextBoolean()).collect(Collectors.toList()));
				TextChannelGround.of(event).drop(toDrop);
				event.getChannel().sendMessage("Dropped " + ItemStack.toString(toDrop) + " in the channel").queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return null;
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.BOT_OWNER;
			}
		});
	}
}
