package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.info.CommandStatsManager;
import net.kodehawa.mantarobot.commands.rpg.RateLimiter;
import net.kodehawa.mantarobot.commands.rpg.entity.Entity;
import net.kodehawa.mantarobot.commands.rpg.entity.player.EntityPlayer;
import net.kodehawa.mantarobot.commands.rpg.entity.player.EntityPlayerMP;
import net.kodehawa.mantarobot.commands.rpg.entity.world.EntityTree;
import net.kodehawa.mantarobot.commands.rpg.item.Item;
import net.kodehawa.mantarobot.commands.rpg.item.ItemStack;
import net.kodehawa.mantarobot.commands.rpg.item.Items;
import net.kodehawa.mantarobot.commands.rpg.world.TextChannelWorld;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.Async;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RPGCmds extends Module {

	public RPGCmds() {
		super(Category.RPG);

		profile();
		loot();
		gamble();
		mine();
		richest();
		inventory();
		market();
		market();
		rep();
		transfer();
		item();
		chop();

		/*
		TODO NEXT:"
		 - cross-bot transfer command
		 */

		Async.startAsyncTask("RPG Thread", () -> {
			MantaroData.getData().get().users.values().forEach(player -> player.setMoney((long) Math.max(0, 0.999d + (player.getMoney() * 0.99562d))));
			MantaroData.getData().get().guilds.values().stream()
				.filter(guildData -> guildData.localMode && guildData.devaluation)
				.flatMap(guildData -> guildData.users.values().stream())
				.forEach(player -> player.setMoney((long) Math.floor(Math.max(0, 0.999d + (player.getMoney() * 0.99562d)))));
		}, 3600);
	}

	private void item(){
		super.register("item", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if(args[0].equals("potion")){
					switch (args[1]){
						case "health":
							Item healthPotion = Items.POTION_HEALTH;
							EntityPlayer player = EntityPlayer.getPlayer(event);
							if(!player.getInventory().containsItem(healthPotion)){
								event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot drink a potion you don't have.").queue();
								return;
							}

							player.getInventory().process(new ItemStack(healthPotion, -1));
							player.addHealth(player.getMaxHealth() - player.getHealth()); //Recover all health.
							player.save();
							event.getChannel().sendMessage(EmoteReference.CORRECT + "You recovered all your health.").queue();
							break;
						case "stamina":
							Item staminaPotion = Items.POTION_STAMINA;
							EntityPlayer player1 = EntityPlayer.getPlayer(event);
							if(!player1.getInventory().containsItem(staminaPotion)){
								event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot drink a potion you don't have.").queue();
								return;
							}

							player1.getInventory().process(new ItemStack(staminaPotion, -1));
							player1.addStamina(player1.getMaxStamina() - player1.getStamina()); //Recover all stamina.
							event.getChannel().sendMessage(EmoteReference.CORRECT + "You recovered all your stamina.").queue();
							player1.save();
							break;
						default:
							onHelp(event);
					}
					return;
				}

				if(args[0].equals("trash")){
					Item trash = Items.fromAny(content.replace(args[0] + " ", "")).orElse(null);

					if(trash == null){
						event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot trash a non existant item").queue();
						return;
					}
					EntityPlayer player = EntityPlayer.getPlayer(event);

					if(!player.getInventory().containsItem(trash)){
						event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot trash an item you don't have.").queue();
						return;
					}

					player.getInventory().process(new ItemStack(trash, -1));
					event.getChannel().sendMessage(EmoteReference.CORRECT + "Trashed " + trash.getEmoji()).queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Item command")
						.setDescription("Does actions with items.")
						.addField("Usage", "~>item potion <health/stamina>: uses an avaliable potion to recover your health or stamina.", false)
						.addField("Important", "You cannot use a potion you don't have", false)
						.build();
			}
		});
	}

	private void gamble() {
		RateLimiter rateLimiter = new RateLimiter(3500);
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


				EntityPlayer player = EntityPlayer.getPlayer(event);

				if (player.getMoney() <= 0) {
					event.getChannel().sendMessage(EmoteReference.ERROR2 + "You're broke. Search for some credits first!").queue();
					return;
				}

				if(!check(player, event)) return;

				double multiplier;
				long i;
				int luck;
				try {
					switch (content) {
						case "all":
						case "everything":
							i = player.getMoney();
							multiplier = 1.5d + (r.nextInt(1500) / 1000d);
							luck = 42 + (int) (multiplier * 10) + r.nextInt(20);
							break;
						case "half":
							i = player.getMoney() == 1 ? 1 : player.getMoney() / 2;
							multiplier = 1d + (r.nextInt(1500) / 1000d);
							luck = 35 + (int) (multiplier * 15) + r.nextInt(20);
							break;
						case "quarter":
							i = player.getMoney() == 1 ? 1 : player.getMoney() / 4;
							multiplier = 1d + (r.nextInt(1000) / 1000d);
							luck = 40 + (int) (multiplier * 15) + r.nextInt(20);
							break;
						default:
							i = Integer.parseInt(content);
							if (i > player.getMoney() || i < 0) throw new UnsupportedOperationException();
							multiplier = 1.2d + (i / player.getMoney() * r.nextInt(1300) / 1000d);
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
				EntityPlayer user = EntityPlayer.getPlayer(event);

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

				EntityPlayer player = EntityPlayer.getPlayer(event);
				if(!check(player, event)) return;
				TextChannelWorld ground = TextChannelWorld.of(event);
				List<ItemStack> loot = ground.collectItems();
				int moneyFound = ground.collectMoney() + Math.max(0, r.nextInt(400) - 300);

				if (!loot.isEmpty()) {
					String s = ItemStack.toString(ItemStack.reduce(loot));
					player.getInventory().merge(loot);
					if (moneyFound != 0) {
						if (player.addMoney(moneyFound)) {
							event.getChannel().sendMessage(EmoteReference.POPPER + "Digging through messages, you found " + s + ", along with " + moneyFound + " credits!").queue();
						} else {
							event.getChannel().sendMessage(EmoteReference.POPPER + "Digging through messages, you found " + s + ", along with " + moneyFound + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java long. Here's a buggy money bag for you.").queue();
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

				TextChannelWorld.of(event).dropItemWithChance(Items.BROM_PICKAXE, 10);
				EntityPlayer player = EntityPlayer.getPlayer(event);

				if(!check(player, event)) return;

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

						if (args[1].equals("all")) {
							long all = player.getInventory().asList().stream()
								.mapToLong(value -> (long) (value.getItem().getValue() * value.getAmount() * 0.9d))
								.sum();

							player.getInventory().clear();

							if (player.addMoney(all)) {
								event.getChannel().sendMessage(EmoteReference.MONEY + "You sold all your inventory items and gained " + all + " credits!").queue();
							} else {
								event.getChannel().sendMessage(EmoteReference.MONEY + "You sold all your inventory items and gained " + all + " credits. But you already had too many credits. Your bag overflowed.\nCongratulations, you exploded a Java long (how??). Here's a buggy money bag for you.").queue();
							}
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

						return;
					}

					if (args[0].equals("buy")) {
						Item itemToBuy = Items.fromAny(itemName).orElse(null);

						if (itemToBuy == null) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot buy an unexistant item.").queue();
							return;
						}

						if (!itemToBuy.isBuyable()) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot buy an item that cannot be bought.").queue();
							return;
						}

						if (player.removeMoney(itemToBuy.getValue() * itemNumber)) {
							player.getInventory().process(new ItemStack(itemToBuy, itemNumber));
							event.getChannel().sendMessage(EmoteReference.OK + "Bought " + itemNumber + " " + itemToBuy.getEmoji() +
								" successfully. You now have " + player.getMoney() + " credits.").queue();
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
		RateLimiter rateLimiter = new RateLimiter(2000);
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

				EntityPlayer player = EntityPlayer.getPlayer(event);

				if(!check(player, event)) return;

				if (!player.getInventory().containsItem(Items.BROM_PICKAXE)) {
					event.getChannel().sendMessage(":octagonal_sign: You don't have any pickaxe to mine with." + (TextChannelWorld.of(event).dropItemWithChance(Items.BROM_PICKAXE, 5) ? " I think I saw a pickaxe somewhere, though. " + EmoteReference.PICK : "")).queue();
					return;
				}

				int picks = player.getInventory().getAmount(Items.BROM_PICKAXE);
				player.consumeStamina(10);
				long moneyFound = (long) (r.nextInt(250) * (1.0d + picks * 0.5d));
				boolean dropped = TextChannelWorld.of(event).dropItemWithChance(Items.BROM_PICKAXE, 10);
				String toSend = "";

				//Little chance, but chance.
				if (Math.random() * 100 > 90) {
					player.getInventory().process(new ItemStack(Items.BROM_PICKAXE, -1));
					toSend = "\n" + EmoteReference.SAD + "Sadly, one of your pickaxes broke while mining. You still can use your others, though.";
				}

				//Even less chance, but chance.
				if (Math.random() * 100 > 95) {
					player.consumeHealth(10);
					toSend = "\n" + EmoteReference.SAD + "Sadly, you caught a sickness while mining. You lost 10 health.";
				}

				if (player.getMoney() >= Integer.MAX_VALUE) {
					event.getChannel().sendMessage(EmoteReference.ERROR + "You have too many credits. Maybe you should spend some before getting more.").queue();
					return;
				}

				if (player.addMoney(moneyFound)) {
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
				EntityPlayer player = MantaroData.getData().get().getUser(event, false);
				User author = event.getAuthor();
				EntityPlayerMP user = MantaroData.getData().get().getUser(author, false);
				Member member = event.getMember();

				if (!event.getMessage().getMentionedUsers().isEmpty()) {
					author = event.getMessage().getMentionedUsers().get(0);
					member = event.getGuild().getMember(author);

					user = EntityPlayerMP.getPlayer(author);
					player = EntityPlayer.getPlayer(member);
				}

				event.getChannel().sendMessage(baseEmbed(event, member.getEffectiveName() + "'s Profile", author.getEffectiveAvatarUrl())
					.addField(EmoteReference.HEART + "Health", "**" + player.getHealth() + "** " + CommandStatsManager.bar((int) (((double) player.getHealth() / (double) player.getMaxHealth()) * 100), 15), false)
					.addField(EmoteReference.RUNNER + "Stamina", "**" + player.getStamina() + "** " + CommandStatsManager.bar((int) (((double) player.getStamina() / (double) player.getMaxStamina()) * 100), 15), false)
					.addField(EmoteReference.DOLLAR + "Credits", "$ " + player.getMoney(), false)
					.addField(EmoteReference.REP + "Reputation", String.valueOf(player.getReputation()), false)
					.addField(EmoteReference.POUCH + "Inventory", ItemStack.toString(player.getInventory().asList()), false)
					.addField(EmoteReference.POPPER + "Birthday", user.birthdayDate != null ? user.birthdayDate.substring(0, 5) : "Not specified.", false)
					.setFooter(EmoteReference.ZAP + "In	 treatment/regeneration: " + player.isProcessing(), author.getEffectiveAvatarUrl())
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

	private void rep(){
		super.register("rep", new SimpleCommand() {
			RateLimiter rateLimiter = new RateLimiter(86400000);

			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if(event.getMessage().getMentionedUsers().get(0).equals(event.getAuthor())){
					event.getChannel().sendMessage(EmoteReference.THINKING + "You cannot rep yourself.").queue();
					return;
				}

				if(!rateLimiter.process(event.getMember())){
					event.getChannel().sendMessage(EmoteReference.ERROR + "You can only rep once every 24 hours.").queue();
					return;
				}

				if(event.getMessage().getMentionedUsers().isEmpty()){
					event.getChannel().sendMessage(EmoteReference.THINKING + "You need to mention one user.").queue();
					return;
				}

				User mentioned = event.getMessage().getMentionedUsers().get(0);
				EntityPlayerMP player = EntityPlayerMP.getPlayer(mentioned);
				if(player.addReputation(1)){
					event.getChannel().sendMessage(EmoteReference.CORRECT + "Added reputation to **" + mentioned.getName() + "**").queue();
				} else {
					event.getChannel().sendMessage(EmoteReference.CONFUSED + "You have more than 4000 reputation, congrats, you're popular.").queue();
				}
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

	private void transfer(){
		//for now, local transfer.
		super.register("transfer", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if(event.getMessage().getMentionedUsers().get(0).equals(event.getAuthor())){
					event.getChannel().sendMessage(EmoteReference.THINKING + "You cannot transfer money to yourself.").queue();
					return;
				}

				if(event.getMessage().getMentionedUsers().isEmpty()){
					event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention one user.").queue();
					return;
				}

				int toSend = Integer.parseInt(args[1]);
				EntityPlayer transferPlayer = EntityPlayer.getPlayer(event);
				if(transferPlayer.getMoney() < toSend){
					event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer money you don't have.").queue();
					return;
				}

				EntityPlayer toTransfer = EntityPlayer.getPlayer(event.getGuild().getMember(event.getMessage().getMentionedUsers().get(0)));
				transferPlayer.removeMoney(toSend);
				toTransfer.addMoney(toSend);
				transferPlayer.save(); //this'll save both.

				event.getChannel().sendMessage(EmoteReference.CORRECT + "Transferred **" + toSend + "** to *" + event.getMessage().getMentionedUsers().get(0).getName() + "* successfully.").queue();
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
							.sorted(Comparator.comparingLong(user -> Long.MAX_VALUE - MantaroData.getData().get().getUser(event.getGuild(), user, false).getMoney()))
							.limit(15)
							.map(user -> String.format("%d. **`%s#%s`** - **%d** Credits", integer.getAndIncrement(), user.getName(), user.getDiscriminator(), MantaroData.getData().get().getUser(event.getGuild(), user, false).getMoney()))
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

	private void chop(){
		RateLimiter rateLimiter = new RateLimiter(3500);

		super.register("chop", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				TextChannelWorld world = TextChannelWorld.of(event);
				EntityPlayer player = EntityPlayer.getPlayer(event);

				EntityTree tree = (EntityTree) world.getActiveEntities().stream().filter
						(entity -> (entity instanceof EntityTree)).findFirst().orElse(null);

				if(!rateLimiter.process(event.getMember())){
					event.getChannel().sendMessage(EmoteReference.ERROR + "You're chopping too fast, I cannot create enough wood!").queue();
					return;
				}

				if(!check(player, event)) return;

				if (!player.getInventory().containsItem(Items.AXE)) {
					event.getChannel().sendMessage(":octagonal_sign: You don't have any axe to chop with."
							+ (TextChannelWorld.of(event).dropItemWithChance(Items.AXE, 5) ?
							" I think I saw an axe somewhere, though. " + EmoteReference.AXE : "")).queue();
					return;
				}

				player.consumeStamina(10);

				if(tree == null){
					event.getChannel().sendMessage(EmoteReference.ERROR + "There are no trees in this world").queue();
					return;
				}

				int axes = player.getInventory().getAmount(Items.AXE);
				tree.setHealth(0);
				//if ticks aren't enough kek
				tree.onDeath();
				int give = (int) Math.max((axes * 0.5), 1);
				player.getInventory().process(new ItemStack(Items.WOOD, give));
				event.getChannel().sendMessage(String.format("%sChopping in %s got you %d wood.", EmoteReference.CORRECT, event.getChannel().getAsMention(), give)).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Chop command")
						.setDescription("Chops a tree.")
						.addField("Usage", "~>chop", false)
						.addField("Important", "Trees will be taken off the world and respawned later", false)
						.build();
			}
		});
	}

	private boolean check(EntityPlayer player, GuildMessageReceivedEvent event) {
		if (player.getStamina() < 10) {
			if (player.isProcessing()) {
				event.getChannel().sendMessage(EmoteReference.WARNING + "You don't have enough stamina and haven't been regenerated yet").queue();
				return false;
			}

			player.setProcessing(true);
			player.add(TextChannelWorld.of(event));
			event.getChannel().sendMessage(EmoteReference.ERROR + "You don't have enough stamina to do this. You need to rest for a bit. Wait a minute for it to be completely regenerated.").queue();
			Async.startAsyncTask("Stamina Task (Process) [" + player + "]", s -> {
				if (!player.addStamina(10)) {
					player.setProcessing(false);
					s.shutdown();
				}
			}, 10);
			return false;
		}

		if (player.getHealth() < 10) {
			if (player.isProcessing()) {
				event.getChannel().sendMessage(EmoteReference.WARNING + "You're still on the hospital.").queue();
				return false;
			}

			player.setProcessing(true);
			player.add(TextChannelWorld.of(event));
			event.getChannel().sendMessage(EmoteReference.ERROR + "You're too sick, so you were transferred to the hospital. In 15 minutes you should be okay.").queue();
			Async.asyncSleepThen(900000, () -> {
				player.addHealth(player.getMaxHealth() - 10);
				player.setProcessing(false);
			}).run();
			return false;
		}

		return true;
	}
}
