package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.rpg.RateLimiter;
import net.kodehawa.mantarobot.commands.rpg.TextChannelGround;
import net.kodehawa.mantarobot.commands.rpg.item.Item;
import net.kodehawa.mantarobot.commands.rpg.item.ItemStack;
import net.kodehawa.mantarobot.commands.rpg.item.Items;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.data.entities.helpers.UserData;
import net.kodehawa.mantarobot.modules.Category;
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
		super(Category.RPG);

		profile();
		loot();
		gamble();
		richest();
		inventory();
		market();
		rep();
		transfer();
		daily();
		create();

		/*
		TODO NEXT:"
		 - cross-bot transfer command
		 */

		//TODO fix @AdrianTodt
		/*Async.task("RPG Thread", () -> {
			MantaroData.getData().get().users.values().forEach(player -> player.setMoney((long) Math.max(0, 0.999d + (player.getMoney() * 0.99562d))));
			MantaroData.getData().get().guilds.values().stream()
				.filter(guildData -> guildData.localMode && guildData.devaluation)
				.flatMap(guildData -> guildData.users.values().stream())
				.forEach(player -> player.setMoney((long) Math.floor(Math.max(0, 0.999d + (player.getMoney() * 0.99562d)))));
		}, 3600);*/
	}

	private void gamble() {
		RateLimiter rateLimiter = new RateLimiter(5000);
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

				Player player = MantaroData.db().getPlayer(event.getMember());

				if (player.getMoney() <= 0) {
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
							i = player.getMoney();
							multiplier = 1.5d + (r.nextInt(1500) / 1000d);
							luck = 30 + (int) (multiplier * 10) + r.nextInt(20);
							break;
						case "half":
							i = player.getMoney() == 1 ? 1 : player.getMoney() / 2;
							multiplier = 1d + (r.nextInt(1500) / 1000d);
							luck = 20 + (int) (multiplier * 15) + r.nextInt(20);
							break;
						case "quarter":
							i = player.getMoney() == 1 ? 1 : player.getMoney() / 4;
							multiplier = 1d + (r.nextInt(1000) / 1000d);
							luck = 35 + (int) (multiplier * 15) + r.nextInt(20);
							break;
						default:
							i = Integer.parseInt(content);
							if (i > player.getMoney() || i < 0) throw new UnsupportedOperationException();
							multiplier = 1.2d + (i / player.getMoney() * r.nextInt(1300) / 1000d);
							luck = 15 + (int) (multiplier * 15) + r.nextInt(10);
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

					if (player.getMoney() >= Integer.MAX_VALUE) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "You have too many credits. Maybe you should spend some before getting more.").queue();
						return;
					}

					if (player.addMoney(gains)) {
						event.getChannel().sendMessage(EmoteReference.DICE + "Congrats, you won " + gains + " credits and got to keep what you had!").queue();
					} else {
						event.getChannel().sendMessage(EmoteReference.DICE + "Congrats, you won " + gains + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java long. Here's a buggy money bag for you.").queue();
					}
				} else {
					player.setMoney(Math.max(0, player.getMoney() - i));
					event.getChannel().sendMessage("\uD83C\uDFB2 Sadly, you lost " + (player.getMoney() == 0 ? "all your" : i) + " credits! \uD83D\uDE26").queue();
				}

				player.saveAsync();
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
				Player user = MantaroData.db().getPlayer(event.getMember());

				EmbedBuilder builder = baseEmbed(event, event.getMember().getEffectiveName() + "'s Inventory", event.getAuthor().getEffectiveAvatarUrl());

				List<ItemStack> list = user.inventory().asList();
				if (list.isEmpty()) builder.setDescription("There is only dust.");
				else
					user.inventory().asList().forEach(stack -> {
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

	private void daily(){
		RateLimiter rateLimiter = new RateLimiter(86400000); //24 hours
		Random r = new Random();
		super.register("daily", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				String id = event.getAuthor().getId();
				long money = 300L;
				User mentionedUser = null;
				try{
					mentionedUser = event.getMessage().getMentionedUsers().get(0);
				} catch (IndexOutOfBoundsException ignored){}

				Player player;


				if (!rateLimiter.process(id)) {
					event.getChannel().sendMessage(EmoteReference.STOPWATCH +
							"Cooldown a lil bit, you can only do this once every 24 hours.").queue();
					return;
				}

				if(mentionedUser != null){
					money = money + r.nextInt(50);
					player = Player.of(mentionedUser);
					player.addMoney(money);
					player.saveAsync();
					event.getChannel().sendMessage(EmoteReference.CORRECT + "You gave your **$" + money + "** daily credits to " + mentionedUser.getName()).queue();
					return;
				}

				player = Player.of(event.getMember());
				player.addMoney(money);
				player.saveAsync();
				event.getChannel().sendMessage(EmoteReference.CORRECT + "You received **$" + money + "** daily credits.").queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Daily command")
						.setDescription("Gives you $300 credits per day (or between 300 and 350 if you transfer it to another person).")
						.build();
			}
		});
	}

	private void loot() {
		RateLimiter rateLimiter = new RateLimiter(1200000);
		Random r = new Random();

		super.register("loot", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {

				String id = event.getAuthor().getId();

				if (!rateLimiter.process(id)) {
					event.getChannel().sendMessage(EmoteReference.STOPWATCH +
						"Cooldown a lil bit, you can only do this once every 20 minutes.").queue();
					return;
				}

				Player player = MantaroData.db().getPlayer(event.getMember());
				TextChannelGround ground = TextChannelGround.of(event);
				List<ItemStack> loot = ground.collectItems();
				int moneyFound = ground.collectMoney() + Math.max(0, r.nextInt(400) - 100);

				if (!loot.isEmpty()) {
					String s = ItemStack.toString(ItemStack.reduce(loot));
					player.inventory().merge(loot);
					if (moneyFound != 0) {
						if (player.addMoney(moneyFound)) {
							event.getChannel().sendMessage(EmoteReference.POPPER + "Digging through messages, you found $" + s + ", along with " + moneyFound + " credits!").queue();
						} else {
							event.getChannel().sendMessage(EmoteReference.POPPER + "Digging through messages, you found $" + s + ", along with " + moneyFound + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java long. Here's a buggy money bag for you.").queue();
						}
					} else {
						event.getChannel().sendMessage(EmoteReference.MEGA + "Digging through messages, you found " + s).queue();
					}
				} else {
					if (moneyFound != 0) {
						if (player.getMoney() >= Integer.MAX_VALUE) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "You have too many credits. Maybe you should spend some before getting more.").queue();
							return;
						}

						if (player.addMoney(moneyFound)) {
							event.getChannel().sendMessage(EmoteReference.POPPER + "Digging through messages, you found " + moneyFound + " credits!").queue();
						} else {
							event.getChannel().sendMessage(EmoteReference.POPPER + "Digging through messages, you found " + moneyFound + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java long. Here's a buggy money bag for you.").queue();
						}
					} else {
						event.getChannel().sendMessage(EmoteReference.SAD + "Digging through messages, you found nothing but dust").queue();
					}
				}

				player.saveAsync();
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
				Player player = MantaroData.db().getPlayer(event.getMember());

				if (args.length > 0) {
					int itemNumber = 1;
					String itemName = content.replace(args[0] + " ", "");
					boolean isMassive = !itemName.isEmpty() && itemName.split(" ")[0].matches("^[0-9]*$");
					if (isMassive) {
						try {
							itemNumber = Math.abs(Integer.valueOf(itemName.split(" ")[0]));
							itemName = itemName.replace(args[1] + " ", "");
						} catch (Exception e) {
							if (e instanceof NumberFormatException) {
								event.getChannel().sendMessage(EmoteReference.ERROR + "Not a valid number of items to buy.").queue();
							}
						}
					}

					if (args[0].equals("sell")) {
						if (player.getMoney() >= Integer.MAX_VALUE) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "You have too many credits. " +
								"Maybe you should spend some before getting more.").queue();
							return;
						}
						try {
							if (args[1].equals("all")) {
								long all = player.inventory().asList().stream()
									.mapToLong(value -> (long) (value.getItem().getValue() * value.getAmount() * 0.9d))
									.sum();

								player.inventory().clear();

								if (player.addMoney(all)) {
									event.getChannel().sendMessage(EmoteReference.MONEY + "You sold all your inventory items and gained " + all + " credits!").queue();
								} else {
									event.getChannel().sendMessage(EmoteReference.MONEY + "You sold all your inventory items and gained " + all + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java long (how??). Here's a buggy money bag for you.").queue();
								}

								player.saveAsync();
								return;
							}

							Item toSell = Items.fromAny(itemName).orElse(null);

							if (!toSell.isSellable()) {
								event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot sell an item that cannot be sold.").queue();
								return;
							}

							if (player.inventory().asMap().getOrDefault(toSell, null) == null) {
								event.getChannel().sendMessage(EmoteReference.STOP + "You cannot sell an item you don't have.").queue();
								return;
							}

							if(player.inventory().getAmount(toSell) < itemNumber){
								event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot sell more items than what you have.").queue();
								return;
							}

							int many = itemNumber * -1;
							long amount = Math.round((toSell.getValue() * 0.9)) * Math.abs(many);
							player.inventory().process(new ItemStack(toSell, many));

							if (player.addMoney(amount)) {
								event.getChannel().sendMessage(EmoteReference.CORRECT + "You sold " + Math.abs(many) + " **" + toSell.getName() +
									"** and gained " + amount + " credits!").queue();
							} else {
								event.getChannel().sendMessage(EmoteReference.CORRECT + "You sold **" + toSell.getName() +
									"** and gained" + amount + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java long (how??). Here's a buggy money bag for you.").queue();
							}

							player.saveAsync();
							return;
						} catch (NullPointerException e) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "Item doesn't exist or invalid syntax").queue();
						}
						return;
					}

					if (args[0].equals("buy")) {
						Item itemToBuy = Items.fromAny(itemName).orElse(null);

						if (itemToBuy == null) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot buy an unexistant item.").queue();
							return;
						}

						try {
							if (!itemToBuy.isBuyable()) {
								event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot buy an item that cannot be bought.").queue();
								return;
							}

							if (player.removeMoney(itemToBuy.getValue() * itemNumber)) {
								player.inventory().process(new ItemStack(itemToBuy, itemNumber));
								event.getChannel().sendMessage(EmoteReference.OK + "Bought " + itemNumber + " " + itemToBuy.getEmoji() +
									" successfully. You now have " + player.getMoney() + " credits.").queue();

								player.saveAsync();
							} else {
								event.getChannel().sendMessage(EmoteReference.STOP + "You don't have enough money to buy this item.").queue();
							}
							return;
						} catch (NullPointerException e) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "Item doesn't exist or invalid syntax.").queue();
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
					.addField("Information", "To buy and sell multiple items you need to do ~>market <buy/sell> <amount> <item>", false)
					.build();
			}
		});
	}

	private void create(){
		super.register("createprofile", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				Player player = MantaroData.db().getPlayer(event.getMember());
				player.addMoney(1);
				player.saveAsync();
				event.getChannel().sendMessage(EmoteReference.CORRECT + "Created your profile.").queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Create profile")
						.setDescription("Builds your profile.")
						.build();
			}
		});
	}

	private void profile() {
		super.register("profile", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				Player player = MantaroData.db().getPlayer(event.getMember());
				User author = event.getAuthor();
				UserData user = MantaroData.db().getUser(event.getMember()).getData();
				Member member = event.getMember();

				if (!event.getMessage().getMentionedUsers().isEmpty()) {
					author = event.getMessage().getMentionedUsers().get(0);
					member = event.getGuild().getMember(author);

					if(author.isBot()){
						event.getChannel().sendMessage(EmoteReference.ERROR + "Bots have no profiles.").queue();
						return;
					}

					user = MantaroData.db().getUser(author).getData();
					player = MantaroData.db().getPlayer(member);
				}

				event.getChannel().sendMessage(baseEmbed(event, member.getEffectiveName() + "'s Profile", author.getEffectiveAvatarUrl())
					.addField(EmoteReference.DOLLAR + "Credits", "$ " + player.getMoney(), false)
					.addField(EmoteReference.ZAP + "Level", player.getLevel() + " (Experience: " + player.getData().getExperience() + ")", false)
					.addField(EmoteReference.REP + "Reputation", String.valueOf(player.getReputation()), false)
					.addField(EmoteReference.POUCH + "Inventory", ItemStack.toString(player.inventory().asList()), false)
					.addField(EmoteReference.POPPER + "Birthday", user.getBirthday() != null ? user.getBirthday().substring(0, 5) : "Not specified.", false)
					.setFooter(EmoteReference.ZAP + "In treatment/regeneration: " + player.isProcessing(), author.getEffectiveAvatarUrl())
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

	private void rep() {
		super.register("rep", new SimpleCommand() {
			RateLimiter rateLimiter = new RateLimiter(86400000);

			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (event.getMessage().getMentionedUsers().get(0).equals(event.getAuthor())) {
					event.getChannel().sendMessage(EmoteReference.THINKING + "You cannot rep yourself.").queue();
					return;
				}

				if (!rateLimiter.process(event.getMember())) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You can only rep once every 24 hours.").queue();
					return;
				}

				if (event.getMessage().getMentionedUsers().isEmpty()) {
					event.getChannel().sendMessage(EmoteReference.THINKING + "You need to mention one user.").queue();
					return;
				}

				User mentioned = event.getMessage().getMentionedUsers().get(0);
				Player player = MantaroData.db().getPlayer(event.getMember());
				if (player.addReputation(1)) {
					event.getChannel().sendMessage(EmoteReference.CORRECT + "Added reputation to **" + mentioned.getName() + "**").queue();
				} else {
					event.getChannel().sendMessage(EmoteReference.CONFUSED + "You have more than 4000 reputation, congrats, you're popular.").queue();
				}
				player.saveAsync();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Reputation command")
					.setDescription("Reps an user.")
					.addField("Usage", "~>rep <@user>", false)
					.addField("Parameters", "@user: user to mention", false)
					.addField("Important", "Only usable every 24 hours.", false)
					.build();
			}
		});
	}

	private void richest() {
		super.register("richest", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				boolean global = !MantaroData.db().getGuild(event.getGuild()).getData().getRpgLocalMode() && !content.equals("guild");
				AtomicInteger integer = new AtomicInteger(1);
				event.getChannel().sendMessage(baseEmbed(event, global ? "Global richest Users" : event.getGuild().getName() + "'s richest Members", global ? event.getJDA().getSelfUser().getEffectiveAvatarUrl() : event.getGuild().getIconUrl())
					.setDescription(
						(global ? MantaroBot.getInstance().getUsers().stream() : event.getGuild().getMembers().stream().map(Member::getUser))
							.filter(user -> user != null && !user.isBot())
							.sorted(Comparator.comparingLong(user -> Long.MAX_VALUE - MantaroData.db().getPlayer(user, user.getMutualGuilds().get(0)).getMoney()))
							.limit(15)
							.map(user -> String.format("%d. **`%s#%s`** - **%d** Credits", integer.getAndIncrement(), user.getName(), user.getDiscriminator(), MantaroData.db().getPlayer(user, user.getMutualGuilds().get(0)).getMoney()))
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

	private void transfer() {
		//for now, local transfer.
		super.register("transfer", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (event.getMessage().getMentionedUsers().get(0).equals(event.getAuthor())) {
					event.getChannel().sendMessage(EmoteReference.THINKING + "You cannot transfer money to yourself.").queue();
					return;
				}

				if (event.getMessage().getMentionedUsers().isEmpty()) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention one user.").queue();
					return;
				}

				int toSend = Math.abs(Integer.parseInt(args[1]));
				Player transferPlayer = MantaroData.db().getPlayer(event.getMember());
				if (transferPlayer.getMoney() < toSend) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer money you don't have.").queue();
					return;
				}

				User user = event.getMessage().getMentionedUsers().get(0);
				if(user.isBot()){
					event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer money to a bot.").queue();
					return;
				}
				Player toTransfer = MantaroData.db().getPlayer(event.getGuild().getMember(user));
				if (toTransfer.addMoney(toSend)) {
					transferPlayer.removeMoney(toSend);
					transferPlayer.saveAsync(); //this'll.saveAsync both.
					event.getChannel().sendMessage(EmoteReference.CORRECT + "Transferred **" + toSend + "** to *" + event.getMessage().getMentionedUsers().get(0).getName() + "* successfully.").queue();
				} else {
					event.getChannel().sendMessage(EmoteReference.ERROR + "Don't do that.").queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Transfer command")
					.setDescription("Transfers money from one you to another player.")
					.addField("Usage", "~>transfer <@user> <money>", false)
					.addField("Parameters", "@user: user to send money to\n" +
						"money: money to transfer.", false)
					.addField("Important", "You cannot send more money than what you already have", false)
					.build();
			}
		});
	}
}
