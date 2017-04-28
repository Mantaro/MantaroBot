package net.kodehawa.mantarobot.commands;

import com.rethinkdb.model.OptArgs;
import com.rethinkdb.net.Cursor;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroShard;
import net.kodehawa.mantarobot.commands.currency.RateLimiter;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.data.entities.helpers.UserData;
import net.kodehawa.mantarobot.modules.CommandRegistry;
import net.kodehawa.mantarobot.modules.Event;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;

@Module
public class CurrencyCmds {
	@Event
	public static void daily(CommandRegistry cr) {
		RateLimiter rateLimiter = new RateLimiter(TimeUnit.HOURS, 24);
		Random r = new Random();
		cr.register("daily", new SimpleCommand(Category.CURRENCY) {
			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {
				String id = event.getAuthor().getId();
				long money = 300L;
				User mentionedUser = null;
				try {
					mentionedUser = event.getMessage().getMentionedUsers().get(0);
				} catch (IndexOutOfBoundsException ignored) {}

				Player player;

				if (!rateLimiter.process(id)) {
					event.getChannel().sendMessage(EmoteReference.STOPWATCH +
						"Halt! You can only do this once every 24 hours.").queue();
					return;
				}

				if (mentionedUser != null && !mentionedUser.getId().equals(event.getAuthor().getId())) {
					money = money + r.nextInt(50);
					player = MantaroData.db().getPlayer(event.getGuild().getMember(mentionedUser));

					if(mentionedUser.getId().equals(player.getData().getMarriedWith()) && player.getData().getMarriedSince() != null &&
							Long.parseLong(player.getData().anniversary()) - player.getData().getMarriedSince() > TimeUnit.DAYS.toMillis(1)) {
						money = money + r.nextInt(120);
					}

					player.addMoney(money);
					player.save();

					event.getChannel().sendMessage(EmoteReference.CORRECT + "I gave your **$" + money + "** daily credits to " + mentionedUser.getName()).queue();
					return;
				}

				player = MantaroData.db().getPlayer(event.getMember());
				player.addMoney(money);
				player.save();
				event.getChannel().sendMessage(EmoteReference.CORRECT + "You got **$" + money + "** daily credits.").queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Daily command")
					.setDescription("Gives you $300 credits per day (or between 300 and 350 if you transfer it to another person).")
					.build();
			}
		});
	}

	@Event
	public static void gamble(CommandRegistry cr) {
		RateLimiter rateLimiter = new RateLimiter(TimeUnit.SECONDS, 10);
		Random r = new Random();

		cr.register("gamble", new SimpleCommand(Category.CURRENCY) {
			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {
				String id = event.getAuthor().getId();

				if (!MantaroData.db().getUser(event.getMember()).isPremium() && !rateLimiter.process(id)) {
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
							multiplier = 1d + (r.nextInt(1100) / 1000d);
							luck = 25 + (int) (multiplier * 10) + r.nextInt(18);
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

					if (player.getMoney() >= Integer.MAX_VALUE && gains > 1000000) {
						gains = gains / 5;
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

				player.save();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Gamble command")
					.setDescription("Gambles your money")
					.addField("Usage", "~>gamble <all/half/quarter> or ~>gamble <amount>", false)
					.build();
			}
		});
	}

	@Event
	public static void inventory(CommandRegistry cr) {
		cr.register("inventory", new SimpleCommand(Category.CURRENCY) {
			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {
				Player user = MantaroData.db().getPlayer(event.getMember());

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

	@Event
	public static void loot(CommandRegistry cr) {
		RateLimiter rateLimiter = new RateLimiter(TimeUnit.MINUTES, 5);
		Random r = new Random();

		cr.register("loot", new SimpleCommand(Category.CURRENCY) {
			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {

				String id = event.getAuthor().getId();

				if (!MantaroData.db().getUser(event.getMember()).isPremium() && !rateLimiter.process(id)) {
					event.getChannel().sendMessage(EmoteReference.STOPWATCH +
						"Cooldown a lil bit, you can only do this once every 5 minutes.").queue();
					return;
				}

				Player player = MantaroData.db().getPlayer(event.getMember());
				TextChannelGround ground = TextChannelGround.of(event);
				List<ItemStack> loot = ground.collectItems();
				int moneyFound = ground.collectMoney() + Math.max(0, r.nextInt(400) - 100);

				if (!loot.isEmpty()) {
					String s = ItemStack.toString(ItemStack.reduce(loot));
					player.getInventory().merge(loot);
					if (moneyFound != 0) {
						if (player.addMoney(moneyFound)) {
							event.getChannel().sendMessage(EmoteReference.POPPER + "Digging through messages, you found " + s + ", along with $" + moneyFound + " credits!").queue();
						} else {
							event.getChannel().sendMessage(EmoteReference.POPPER + "Digging through messages, you found " + s + ", along with $" + moneyFound + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java long. Here's a buggy money bag for you.").queue();
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
							event.getChannel().sendMessage(EmoteReference.POPPER + "Digging through messages, you found $" + moneyFound + " credits!").queue();
						} else {
							event.getChannel().sendMessage(EmoteReference.POPPER + "Digging through messages, you found $" + moneyFound + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java long. Here's a buggy money bag for you.").queue();
						}
					} else {
						event.getChannel().sendMessage(EmoteReference.SAD + "Digging through messages, you found nothing but dust").queue();
					}
				}

				player.save();
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

	@Event
	public static void market(CommandRegistry cr) {
		cr.register("market", new SimpleCommand(Category.CURRENCY) {
			RateLimiter rateLimiter = new RateLimiter(TimeUnit.SECONDS, 5);

			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (!MantaroData.db().getUser(event.getMember()).isPremium() && !rateLimiter.process(event.getAuthor().getId())) {
					event.getChannel().sendMessage(EmoteReference.STOPWATCH +
						"Cooldown a lil bit, you're calling me so fast that I can't get enough items!").queue();
					return;
				}

				Player player = MantaroData.db().getPlayer(event.getMember());

				//debug code
				Stream.of(Items.ALL).forEach(item -> TextChannelGround.of(event).dropItem(item));


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
								long all = player.getInventory().asList().stream()
										.filter(item -> item.getItem().isSellable())
										.mapToLong(value -> (long) (value.getItem().getValue() * value.getAmount() * 0.9d))
										.sum();

								if(args.length > 2 && args[2].equals("calculate")){
									event.getChannel().sendMessage(EmoteReference.THINKING + "You'll get **" + all + "** credits if you sell all of your items").queue();
									return;
								}

								player.getInventory().clearOnlySellables();

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

							if (player.getInventory().asMap().getOrDefault(toSell, null) == null) {
								event.getChannel().sendMessage(EmoteReference.STOP + "You cannot sell an item you don't have.").queue();
								return;
							}

							if (player.getInventory().getAmount(toSell) < itemNumber) {
								event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot sell more items than what you have.").queue();
								return;
							}

							int many = itemNumber * -1;
							long amount = Math.round((toSell.getValue() * 0.9)) * Math.abs(many);
							player.getInventory().process(new ItemStack(toSell, many));

							if (player.addMoney(amount)) {
								event.getChannel().sendMessage(EmoteReference.CORRECT + "You sold " + Math.abs(many) + " **" + toSell.getName() +
									"** and gained " + amount + " credits!").queue();
							} else {
								event.getChannel().sendMessage(EmoteReference.CORRECT + "You sold **" + toSell.getName() +
									"** and gained" + amount + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java long (how??). Here's a buggy money bag for you.").queue();
							}

							player.save();
							return;
						} catch (Exception e) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "Item doesn't exist or invalid syntax").queue();
							e.printStackTrace();
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
								player.getInventory().process(new ItemStack(itemToBuy, itemNumber));
								player.save();
								event.getChannel().sendMessage(EmoteReference.OK + "Bought " + itemNumber + " " + itemToBuy.getEmoji() +
									" successfully. You now have " + player.getMoney() + " credits.").queue();

							} else {
								event.getChannel().sendMessage(EmoteReference.STOP + "You don't have enough money to buy this item.").queue();
							}
							return;
						} catch (Exception e) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "Item doesn't exist or invalid syntax.").queue();
						}
						return;
					}
				}

				EmbedBuilder embed = baseEmbed(event, EmoteReference.MARKET + "Mantaro Market");

				Stream.of(Items.ALL).forEach(item -> {
					if(!item.isHidden()){
						String buyValue = item.isBuyable() ? EmoteReference.BUY + String.valueOf(Math.floor(item.getValue() * 1.1)) + "c " : "";
						String sellValue = item.isSellable() ? EmoteReference.SELL + String.valueOf(Math.floor(item.getValue() * 0.9)) + "c" : "";
						embed.addField(item.getEmoji() + " " + item.getName(), buyValue + sellValue, true);
					}
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

	@Event
	public static void profile(CommandRegistry cr) {
		cr.register("profile", new SimpleCommand(Category.CURRENCY) {
			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {
				Player player = MantaroData.db().getPlayer(event.getMember());
				User author = event.getAuthor();

				if (args.length > 0 && args[0].equals("description")) {
					if (args.length == 1) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "You need to provide an argument! (set or remove)\n" +
							"for example, ~>profile description set Hi there!").queue();
						return;
					}

					if (args[1].equals("set")) {
						int MAX_LENGTH = 300;
						if (MantaroData.db().getUser(author).isPremium()) MAX_LENGTH = 500;
						String content1 = SPLIT_PATTERN.split(content, 3)[2];

						if (content1.length() > MAX_LENGTH) {
							event.getChannel().sendMessage(EmoteReference.ERROR +
								"The description is too long! `(Limit of 300 characters for everyone and 500 for premium users)`").queue();
							return;
						}

						player.getData().setDescription(content1);
						event.getChannel().sendMessage(EmoteReference.POPPER + "Set description to: **" + content1 + "**\n" +
							"Check your shiny new profile with `~>profile`").queue();
						player.saveAsync();
						return;
					}

					if (args[1].equals("reset")) {
						player.getData().setDescription(null);
						event.getChannel().sendMessage(EmoteReference.CORRECT + "Successfully reset description.").queue();
						player.saveAsync();
						return;
					}
				}

				UserData user = MantaroData.db().getUser(event.getMember()).getData();
				Member member = event.getMember();

				if (!event.getMessage().getMentionedUsers().isEmpty()) {
					author = event.getMessage().getMentionedUsers().get(0);
					member = event.getGuild().getMember(author);

					if (author.isBot()) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "Bots have no profiles.").queue();
						return;
					}

					user = MantaroData.db().getUser(author).getData();
					player = MantaroData.db().getPlayer(member);
				}

				User user1 = getUserById(player.getData().getMarriedWith());
				String marriedSince = player.getData().marryDate();
				String anniversary = player.getData().anniversary();

				if(args.length > 0 && args[0].equals("anniversary")){
					if(anniversary == null){
						event.getChannel().sendMessage(EmoteReference.ERROR + "I don't see any anniversary here :(. Maybe you were married before this change was implemented, in that case do ~>marry anniversarystart").queue();
						return;
					}
					event.getChannel().sendMessage(String.format("%sYour anniversary with **%s** is on %s. You married on **%s**", EmoteReference.POPPER, user1.getName(), anniversary, marriedSince)).queue();
					return;
				}

				event.getChannel().sendMessage(baseEmbed(event, (user1 == null || !player.getInventory().containsItem(Items.RING) ? "" : EmoteReference.RING) + member.getEffectiveName() + "'s Profile", author.getEffectiveAvatarUrl())
					.setThumbnail(author.getEffectiveAvatarUrl())
					.setDescription(player.getData().getDescription() == null ? "No description set" : player.getData().getDescription())
					.addField(EmoteReference.DOLLAR + "Credits", "$ " + player.getMoney(), false)
					.addField(EmoteReference.ZAP + "Level", player.getLevel() + " (Experience: " + player.getData().getExperience() + ")", true)
					.addField(EmoteReference.REP + "Reputation", String.valueOf(player.getReputation()), true)
					.addField(EmoteReference.POUCH + "Inventory", ItemStack.toString(player.getInventory().asList()), false)
					.addField(EmoteReference.POPPER + "Birthday", user.getBirthday() != null ? user.getBirthday().substring(0, 5) : "Not specified.", true)
					.addField(EmoteReference.HEART + "Married with", user1 == null ? "Nobody." : user1.getName() + "#" + user1.getDiscriminator(), true)
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

	@Event
	public static void rep(CommandRegistry cr) {
		cr.register("rep", new SimpleCommand(Category.CURRENCY) {
			RateLimiter rateLimiter = new RateLimiter(TimeUnit.HOURS, 1);

			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (event.getMessage().getMentionedUsers().isEmpty()) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention at least one user.").queue();
					return;
				}

				if (event.getMessage().getMentionedUsers().get(0).equals(event.getAuthor())) {
					event.getChannel().sendMessage(EmoteReference.THINKING + "You cannot rep yourself.").queue();
					return;
				}

				if (event.getMessage().getMentionedUsers().isEmpty()) {
					event.getChannel().sendMessage(EmoteReference.THINKING + "You need to mention one user.").queue();
					return;
				}

				if (!rateLimiter.process(event.getMember())) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You can only rep once every 24 hours.").queue();
					return;
				}
				User mentioned = event.getMessage().getMentionedUsers().get(0);
				Player player = MantaroData.db().getPlayer(event.getGuild().getMember(mentioned));
				player.addReputation(1L);
				player.save();
				event.getChannel().sendMessage(EmoteReference.CORRECT + "Added reputation to **" + mentioned.getName() + "**").queue();
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

	@Event
	public static void richest(CommandRegistry cr) {
		cr.register("richest", new SimpleCommand(Category.CURRENCY) {
			RateLimiter rateLimiter = new RateLimiter(TimeUnit.SECONDS, 10);

			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {

				if (!rateLimiter.process(event.getMember())) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "Dang! Don't you think you're going a bit too fast?").queue();
					return;
				}

				boolean local = MantaroData.db().getGuild(event).getData().isRpgLocalMode();
				String pattern = ':' + (local ? event.getGuild().getId() : "g") + '$';
				boolean global = !local && !content.equals("guild") && !content.equals("local");

				AtomicInteger i = new AtomicInteger();

				Cursor<Map> c1 = r.table("players")
						.orderBy()
						.optArg("index", r.desc("money"))
						.filter(player -> player.g("id").match(pattern))
						.map(player -> player.pluck("id", "money"))
						.limit(15)
						.run(MantaroData.conn(), OptArgs.of("read_mode", "outdated"));
				List<Map> c = c1.toList();

				event.getChannel().sendMessage(
					baseEmbed(event,
						(global ? "Global" : event.getGuild().getName() + "'s") + " richest Users",
						global ? event.getJDA().getSelfUser().getEffectiveAvatarUrl() : event.getGuild().getIconUrl()
					).setDescription(c.stream()
						.map(map -> Pair.of(getUserById(map.get("id").toString().split(":")[0]), map.get("money").toString()))
						.filter(p -> Objects.nonNull(p.getKey()))
						.filter(p -> global || event.getGuild().isMember(p.getKey()))
						.map(p -> String.format("%d - **%s#%s** - Credits: $%s", i.incrementAndGet(), p.getKey().getName(), p.getKey().getDiscriminator(), p.getValue()))
						.collect(Collectors.joining("\n"))
					).build()
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

	@Event
	public static void transfer(CommandRegistry cr) {
		cr.register("transfer", new SimpleCommand(Category.CURRENCY) {
			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (event.getMessage().getMentionedUsers().isEmpty()) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention one user.").queue();
					return;
				}

				if (event.getMessage().getMentionedUsers().get(0).equals(event.getAuthor())) {
					event.getChannel().sendMessage(EmoteReference.THINKING + "You cannot transfer money to yourself.").queue();
					return;
				}

				int toSend;
				try {
					toSend = Math.abs(Integer.parseInt(args[1]));
				} catch (Exception e) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify the amount.").queue();
					return;
				}

				Player transferPlayer = MantaroData.db().getPlayer(event.getMember());
				if (transferPlayer.getMoney() < toSend) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer money you don't have.").queue();
					return;
				}

				User user = event.getMessage().getMentionedUsers().get(0);
				if (user.isBot()) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer money to a bot.").queue();
					return;
				}
				Player toTransfer = MantaroData.db().getPlayer(event.getGuild().getMember(user));
				if (toTransfer.addMoney(toSend)) {
					transferPlayer.removeMoney(toSend);
					transferPlayer.save();
					toTransfer.save();
					event.getChannel().sendMessage(EmoteReference.CORRECT + "Transferred **" + toSend + "** to *" + event.getMessage().getMentionedUsers().get(0).getName() + "* successfully.").queue();
				} else {
					event.getChannel().sendMessage(EmoteReference.ERROR + "Don't do that.").queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Transfer command")
					.setDescription("Transfers money from you to another player.")
					.addField("Usage", "~>transfer <@user> <money>", false)
					.addField("Parameters", "@user: user to send money to\n" +
						"money: money to transfer.", false)
					.addField("Important", "You cannot send more money than what you already have", false)
					.build();
			}
		});
	}

	private static User getUserById(String id) {
		if (id == null) return null;
		MantaroShard shard1 = MantaroBot.getInstance().getShardList().stream().filter(shard ->
			shard.getJDA().getUserById(id) != null).findFirst().orElse(null);
		return shard1 == null ? null : shard1.getUserById(id);
	}
}
