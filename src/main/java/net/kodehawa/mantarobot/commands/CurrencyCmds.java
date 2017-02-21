package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.InventoryResolver;
import net.kodehawa.mantarobot.commands.currency.InventoryResolver.*;
import net.kodehawa.mantarobot.data.Data.UserData;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.Expirator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.commands.currency.InventoryResolver.*;

public class CurrencyCmds extends Module {
	public final Expirator expirator = new Expirator();
	public final List<String> usersRatelimited = new ArrayList<>();

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
		 */
	}

	private void loot() {
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

				List<Item> items = InventoryResolver.loot(event.getChannel());

				if (items.isEmpty()) {
					event.getChannel().sendMessage("Digging through messages, you found nothing but dust").queue();
				} else {
					UserData user = MantaroData.getData().get().getUser(event.getAuthor(), true);
					Map<Item, Integer> inventory = InventoryResolver.resolve(user.inventory);
					Map<Item, Integer> loot = InventoryResolver.organize(items);
					loot.forEach((k, v) -> inventory.merge(k, v, Integer::sum));
					event.getChannel().sendMessage("Digging through messages, you found: " + InventoryResolver.print(loot)).queue();
					user.inventory = InventoryResolver.serialize(inventory);
					MantaroData.getData().update();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Loot command")
						.setDescription("Loots the current chat for items, for usage in Mantaro's currency system.\n"
								+ "Currently, there are ``" + InventoryResolver.ITEMS.size() + "`` items avaliable in chance," +
								"for which you have a random chance of getting one or more.")
						.addField("Usage", "~>loot", false)
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
					.addField(":pouch: Inventory", print(resolve(data.inventory)), false)
					.build()
				).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Profile command.")
						.setDescription("Retrieves your current user profile.")
						.build()	;
			}
		});
	}

	private void summon() {
		super.register("summon", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				Random r = new Random(System.currentTimeMillis());
				List<Item> toDrop = InventoryResolver.ITEMS.stream().filter(item -> r.nextBoolean()).collect(Collectors.toList());
				toDrop.forEach(item -> InventoryResolver.drop(event.getChannel(), item));
				event.getChannel().sendMessage("Dropped " + print(organize(toDrop)) + " in the channel").queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return null;
			}
		});
	}
}
