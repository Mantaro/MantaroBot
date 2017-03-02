package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.RateLimiter;
import net.kodehawa.mantarobot.commands.currency.inventory.Item;
import net.kodehawa.mantarobot.commands.currency.inventory.ItemStack;
import net.kodehawa.mantarobot.commands.currency.inventory.Items;
import net.kodehawa.mantarobot.commands.currency.inventory.TextChannelGround;
import net.kodehawa.mantarobot.data.data.GlobalUserData;
import net.kodehawa.mantarobot.data.data.UserData;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CurrencyCmds extends Module {
	public CurrencyCmds() {
		super(Category.CURRENCY);

		profile();
		loot();
		//summon();
		gamble();
		mine();
		richest();
		inventory();
		market();

		/*
		TODO NEXT:
		 - transfer command
		 */
	}

	private void gamble() {
		RateLimiter rateLimiter = new RateLimiter(2000);
		Random r = new Random();

		super.register("gamble", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				String id = event.getAuthor().getId();

				if (!rateLimiter.process(id)) {
					event.getChannel().sendMessage(EmoteReference.STOPWATCH +
						"Cooldown a lil bit, you're gambling so fast that I can't print enough money!").queue();
					return;
				}

				UserData user = MantaroData.getData().get().getUser(event, true);

				if (user.money <= 0) {
					event.getChannel().sendMessage(EmoteReference.ERROR2 + "You're broke. Search for some credits first!").queue();
					return;
				}

				double multiplier;
				long i;
				int luck;
				try {
					switch (content) {
						case "all":
						case "everything":
							i = user.money;
							multiplier = 1.5d + (r.nextInt(1500) / 1000d);
							luck = 42 + (int) (multiplier * 10) + r.nextInt(20);
							break;
						case "half":
							i = user.money == 1 ? 1 : user.money / 2;
							multiplier = 1d + (r.nextInt(1500) / 1000d);
							luck = 35 + (int) (multiplier * 15) + r.nextInt(20);
							break;
						case "quarter":
							i = user.money == 1 ? 1 : user.money / 4;
							multiplier = 1d + (r.nextInt(1000) / 1000d);
							luck = 40 + (int) (multiplier * 15) + r.nextInt(20);
							break;
						default:
							i = Integer.parseInt(content);
							if (i > user.money || i < 0) throw new UnsupportedOperationException();
							multiplier = 1.2d + (i / user.money * r.nextInt(1300) / 1000d);
							luck = 45 + (int) (multiplier * 15) + r.nextInt(10);
							break;
					}
				} catch (NumberFormatException e) {
					event.getChannel().sendMessage(EmoteReference.ERROR2 + "Please type a valid number equal or less than your credits or `all` to gamble all your credits.").queue();
					return;
				} catch (UnsupportedOperationException e) {
					event.getChannel().sendMessage(EmoteReference.ERROR2 + "Please type a value within your credits amount.").queue();
					return;
				}

				if (luck > r.nextInt(100)) {
					long gains = (long) (i * multiplier);
					gains = Math.round(gains * 0.55);

					if (user.money >= Integer.MAX_VALUE) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "You have too many credits. Maybe you should spend some before getting more.").queue();
						return;
					}

					if (user.addMoney(gains)) {
						event.getChannel().sendMessage(EmoteReference.DICE + "Congrats, you won " + gains + " credits and got to keep what you had!").queue();
					} else {
						event.getChannel().sendMessage(EmoteReference.DICE + "Congrats, you won " + gains + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java long. Here's a buggy money bag for you.").queue();
					}
				} else {
					user.money = Math.max(0, user.money - i);
					event.getChannel().sendMessage("\uD83C\uDFB2 Sadly, you lost " + (user.money == 0 ? "all your" : i) + " credits! \uD83D\uDE26").queue();
				}

				MantaroData.getData().save();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return null;
			}
		});
	}

	private void inventory() {
		super.register("inventory", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				UserData user = MantaroData.getData().get().getUser(event, true);

				EmbedBuilder builder = baseEmbed(event, event.getMember().getEffectiveName() + "'s Inventory", event.getAuthor().getEffectiveAvatarUrl());

				List<ItemStack> list = user.getInventory().asList();
				if (list.isEmpty()) builder.setDescription("There is only dust.");
				else
					user.getInventory().asList().forEach(stack -> {
						long buyValue = stack.getItem().isBuyable() ? (long) (stack.getItem().getValue() * 1.1) : 0;
						long sellValue = stack.getItem().isSellable() ? (long) (stack.getItem().getValue() * 0.9) : 0;
						builder.addField(stack.getItem().getEmoji() + " " + stack.getItem().getName() + " x " + stack.getAmount(), String.format("**Price**: \uD83D\uDCE5 %d \uD83D\uDCE4 %d\n%s", buyValue, sellValue, stack.getItem().getDesc()), false);
					});

				event.getChannel().sendMessage(builder.build()).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Inventory command")
					.setDescription("Shows your current inventory.")
					.build();
			}
		});
	}

	private void loot() {
		RateLimiter rateLimiter = new RateLimiter(5000);
		Random r = new Random();

		super.register("loot", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {

				String id = event.getAuthor().getId();

				if (!rateLimiter.process(id)) {
					event.getChannel().sendMessage(EmoteReference.STOPWATCH +
						"Cooldown a lil bit, you're ratelimited right now so maybe wait a little bit more and let other people loot.").queue();
					return;
				}

				UserData userData = MantaroData.getData().get().getUser(event, true);
				TextChannelGround ground = TextChannelGround.of(event);
				List<ItemStack> loot = ground.collectItems();
				int moneyFound = ground.collectMoney() + Math.max(0, r.nextInt(400) - 300);

				if (!loot.isEmpty()) {
					String s = ItemStack.toString(ItemStack.reduce(loot));
					userData.getInventory().merge(loot);
					if (moneyFound != 0) {
						if (userData.addMoney(moneyFound)) {
							event.getChannel().sendMessage(EmoteReference.POPPER + "Digging through messages, you found " + s + ", along with " + moneyFound + " credits!").queue();
						} else {
							event.getChannel().sendMessage(EmoteReference.POPPER + "Digging through messages, you found " + s + ", along with " + moneyFound + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java long. Here's a buggy money bag for you.").queue();
						}
					} else {
						event.getChannel().sendMessage(EmoteReference.MEGA + "Digging through messages, you found " + s).queue();
					}
				} else {
					if (moneyFound != 0) {
						if (userData.money >= Integer.MAX_VALUE) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "You have too many credits. Maybe you should spend some before getting more.").queue();
							return;
						}

						if (userData.addMoney(moneyFound)) {
							event.getChannel().sendMessage(EmoteReference.POPPER + "Digging through messages, you found " + moneyFound + " credits!").queue();
						} else {
							event.getChannel().sendMessage(EmoteReference.POPPER + "Digging through messages, you found " + moneyFound + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java long. Here's a buggy money bag for you.").queue();
						}
					} else {
						event.getChannel().sendMessage(EmoteReference.SAD + "Digging through messages, you found nothing but dust").queue();
					}
				}

				MantaroData.getData().save();
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

	private void market() {
		super.register("market", new SimpleCommand() {
			RateLimiter rateLimiter = new RateLimiter(4500);

			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (!rateLimiter.process(event.getAuthor().getId())) {
					event.getChannel().sendMessage(EmoteReference.STOPWATCH +
							"Cooldown a lil bit, you're calling me so fast that I can't get enough items!").queue();
					return;
				}

				TextChannelGround.of(event).dropItemWithChance(Items.BROM_PICKAXE, 10);
				UserData user = MantaroData.getData().get().getUser(event, true);

				if (args.length > 0) {
					int itemNumber = 1;
					String itemName = content.replace(args[0] + " ", "");
					boolean isMassive = itemName.split(" ")[0].matches("^[0-9]*$");
					if(isMassive){
						try{
							itemNumber = Math.abs(Integer.valueOf(itemName.split(" ")[0]));
							itemName = itemName.replace(args[1] + " ", "");
						} catch (Exception e){
							if(e instanceof NumberFormatException){
								event.getChannel().sendMessage(EmoteReference.ERROR + "Not a valid number of items to buy.").queue();
							}
						}
					}
					if (args[0].equals("sell")) {
						if (user.money >= Integer.MAX_VALUE) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "You have too many credits. " +
								"Maybe you should spend some before getting more.").queue();
							return;
						}

						if (args[1].equals("all")) {
							long all = user.getInventory().asList().stream()
								.mapToLong(value -> (long) (value.getItem().getValue() * value.getAmount() * 0.9d))
								.sum();

							user.getInventory().clear();

							if (user.addMoney(all)) {
								event.getChannel().sendMessage(EmoteReference.MONEY + "You sold all your inventory items and gained " + all + " credits!").queue();
							} else {
								event.getChannel().sendMessage(EmoteReference.MONEY + "You sold all your inventory items and gained " + all + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java long (how??). Here's a buggy money bag for you.").queue();
							}
							return;
						}

						Item toSell = Items.fromAny(itemName).orElse(null);

						if(!toSell.isSellable()){
							event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot sell an item that cannot be sold.").queue();
							return;
						}

						if (user.getInventory().asMap().getOrDefault(toSell, null) == null) {
							event.getChannel().sendMessage(EmoteReference.STOP + "You cannot sell an item you don't have.").queue();
							return;
						}

						int many = itemNumber * - 1;
						long amount = Math.round((toSell.getValue() * 0.9)) * Math.abs(many);
						user.getInventory().process(new ItemStack(toSell, many));

						if (user.addMoney(amount)) {
							event.getChannel().sendMessage(EmoteReference.CORRECT + "You sold " + Math.abs(many) + " **" + toSell.getName() +
								"** and gained " + amount + " credits!").queue();
						} else {
							event.getChannel().sendMessage(EmoteReference.CORRECT + "You sold **" + toSell.getName() +
								"** and gained" + amount + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java long (how??). Here's a buggy money bag for you.").queue();
						}

						return;
					}

					if (args[0].equals("buy")) {
						Item itemToBuy = Items.fromAny(itemName).orElse(null);

						if (itemToBuy == null) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot buy an unexistant item.").queue();
							return;
						}

						if(!itemToBuy.isBuyable()){
							event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot buy an item that cannot be bought.").queue();
							return;
						}

						if (user.removeMoney(itemToBuy.getValue() * itemNumber)) {
							user.getInventory().process(new ItemStack(itemToBuy, itemNumber));
							event.getChannel().sendMessage(EmoteReference.OK + "Bought " + itemNumber + " " + itemToBuy.getEmoji() +
								" successfully. You now have " + user.money + " credits.").queue();
						} else {
							event.getChannel().sendMessage(EmoteReference.STOP + "You don't have enough money to buy this item.").queue();
						}

						return;
					}
				}

				EmbedBuilder embed = baseEmbed(event, EmoteReference.MARKET + "Mantaro Market");

				Stream.of(Items.ALL).forEach(item -> {
					String buyValue = item.isBuyable() ? EmoteReference.BUY + String.valueOf(Math.floor(item.getValue() * 1.1)) + "c " : "";
					String sellValue = item.isSellable() ? EmoteReference.SELL + String.valueOf(Math.floor(item.getValue() * 0.9)) + "c" : "";
					embed.addField(item.getEmoji() + " " + item.getName(), buyValue + sellValue, true);
				});

				event.getChannel().sendMessage(embed.build()).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Mantaro's market")
					.setDescription("List current items for buying and selling.")
					.addField("Buying and selling", "To buy do ~>market buy <item emoji>. It will substract the value from your money and give you the item.\n" +
						"To sell do ~>market sell all to sell all your items or ~>market sell <item emoji> to sell the specified item. " +
						"You'll get the sell value of the item on coins to spend.", false)
					.addField("To know", "If you don't have enough money you cannot buy the items.", false)
					.build();
			}
		});
	}

	private void mine() {
		RateLimiter rateLimiter = new RateLimiter(1000);
		Random r = new Random();

		super.register("mine", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				String id = event.getAuthor().getId();

				if (!rateLimiter.process(id)) {
					event.getChannel().sendMessage(EmoteReference.STOPWATCH +
						"Cooldown a lil bit, you're mining so fast that I can't print enough money!").queue();
					return;
				}

				UserData userData = MantaroData.getData().get().getUser(event, true);

				int picks = userData.getInventory().asMap().getOrDefault(Items.BROM_PICKAXE, new ItemStack(Items.BROM_PICKAXE, 0)).getAmount();
				if (picks == 0) {
					event.getChannel().sendMessage(":octagonal_sign: You don't have any pickaxe to mine with." + (TextChannelGround.of(event).dropItemWithChance(Items.BROM_PICKAXE, 5) ? " I think I saw a pickaxe somewhere, though. " + EmoteReference.PICK : "")).queue();
					return;
				}

				long moneyFound = (long) (r.nextInt(250) * (1.0d + picks * 0.5d));
				boolean dropped = TextChannelGround.of(event).dropItemWithChance(Items.BROM_PICKAXE, 10);
				String toSend = "";

				//Little chance, but chance.
				if (Math.random() * 100 > 90) {
					userData.getInventory().process(new ItemStack(Items.BROM_PICKAXE, -1));
					toSend = "\n" + EmoteReference.SAD + "Sadly, one of your pickaxes broke while mining. You still can use your others, though.";
				}

				if (userData.money >= Integer.MAX_VALUE) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You have too many credits. Maybe you should spend some before getting more.").queue();
					return;
				}

				if (userData.addMoney(moneyFound)) {
					event.getChannel().sendMessage(EmoteReference.POPPER + "Mining through messages, you found " + moneyFound + " credits!" + (dropped ? " :pick:" : "") + toSend).queue();
				} else {
					event.getChannel().sendMessage(EmoteReference.POPPER + "Mining through messages, you found " + moneyFound + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java long. Here's a buggy money bag for you." + (dropped ? " :pick:" : "") + toSend).queue();
				}

				MantaroData.getData().save();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Mine command")
					.setDescription("Mines money. Just like the good ol' gold days")
					.addField("Usage", "~>mine", false)
					.addField("Note", "More pickaxes make you mine faster.", false)
					.build();
			}
		});
	}

	private void profile() {
		super.register("profile", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (event.getMessage().getMentionedUsers().isEmpty()) {
					UserData data = MantaroData.getData().get().getUser(event, false);
					GlobalUserData user = MantaroData.getData().get().getUser(event.getAuthor(), false);

					event.getChannel().sendMessage(baseEmbed(event, event.getMember().getEffectiveName() + "'s Profile", event.getAuthor().getEffectiveAvatarUrl())
						.addField(":credit_card: Credits", "$ " + data.money, false)
						.addField(":pouch: Inventory", ItemStack.toString(data.getInventory().asList()), false)
						.addField(":tada: Birthday", user.birthdayDate != null ? user.birthdayDate.substring(0, 5) : "Not specified.", false)
						.build()
					).queue();
					return;
				} else if (!event.getMessage().getMentionedUsers().isEmpty()) {
					User user = event.getMessage().getMentionedUsers().get(0);
					UserData data = MantaroData.getData().get().getUser(event, false);
					event.getChannel().sendMessage(baseEmbed(event, user.getName() + "'s Profile", user.getEffectiveAvatarUrl())
						.addField(":credit_card: Credits", "$ " + data.money, false)
						.addField(":pouch: Inventory", ItemStack.toString(data.getInventory().asList()), false)
						.build()
					).queue();
					return;
				}
				TextChannelGround.of(event).dropItemWithChance(Items.BROM_PICKAXE, 10);
				event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention a valid user.").queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Profile command.")
					.setDescription("Retrieves your current user profile.")
					.build();
			}
		});
	}

	private void richest() {
		super.register("richest", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				boolean global = !MantaroData.getData().get().getGuild(event.getGuild(), false).localMode && !content.equals("guild");

				AtomicInteger integer = new AtomicInteger(1);

				event.getChannel().sendMessage(baseEmbed(event, global ? "Global richest Users" : event.getGuild().getName() + "'s richest Members", global ? event.getJDA().getSelfUser().getEffectiveAvatarUrl() : event.getGuild().getIconUrl())
					.setDescription(
						(global ? event.getJDA().getUsers().stream() : event.getGuild().getMembers().stream().map(Member::getUser))
							.filter(user -> !user.isBot())
							.sorted(Comparator.comparingLong(user -> Long.MAX_VALUE - MantaroData.getData().get().getUser(event.getGuild(), user, false).money))
							.limit(15)
							.map(user -> String.format("%d. **`%s#%s`** - **%d** Credits", integer.getAndIncrement(), user.getName(), user.getDiscriminator(), MantaroData.getData().get().getUser(event.getGuild(), user, false).money))
							.collect(Collectors.joining("\n"))
					)
					.build()
				).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Money list")
					.setDescription("Returns the global richest users, or the guild ones if you want.")
					.addField("Usage", "~>richest <global/guild>", false)
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
				TextChannelGround.of(event).dropItems(toDrop);
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
