package net.kodehawa.mantarobot.commands.rpg.world;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.rpg.entity.Entity;
import net.kodehawa.mantarobot.commands.rpg.game.core.GameReference;
import net.kodehawa.mantarobot.commands.rpg.inventory.Item;
import net.kodehawa.mantarobot.commands.rpg.inventory.ItemStack;
import net.kodehawa.mantarobot.commands.rpg.inventory.Items;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TextChannelWorld {
	private static final Map<String, List<ItemStack>> DROPPED_ITEMS = new HashMap<>();
	private static final Map<String, AtomicInteger> DROPPED_MONEY = new HashMap<>();
	private static Map<Entity, GameReference> ACTIVE_ENTITIES = new HashMap<>();
	private static Map<GameReference, Integer> ACTIVE_GAMES = new HashMap<>();
	private static Random r = new Random(System.currentTimeMillis());

	public static TextChannelWorld of(String id) {
		return new TextChannelWorld(DROPPED_ITEMS.computeIfAbsent(id, k -> new ArrayList<>()), DROPPED_MONEY.computeIfAbsent(id, k -> new AtomicInteger(0)));
	}

	public static TextChannelWorld of(TextChannel channel) {
		return of(channel.getId());
	}

	public static TextChannelWorld of(GuildMessageReceivedEvent event) {
		return of(event.getChannel());
	}

	private final AtomicInteger money;
	private final List<ItemStack> stacks;

	private TextChannelWorld(List<ItemStack> stacks, AtomicInteger money) {
		this.stacks = stacks;
		this.money = money;
	}

	public TextChannelWorld addEntity(Entity entity, GameReference game) {
		ACTIVE_ENTITIES.put(entity, game);
		return this;
	}

	public TextChannelWorld addGame(GameReference game, int people) {
		//if it's running.
		removeGame(game);

		//add it with new quantity of people
		ACTIVE_GAMES.put(game, people);

		return this;
	}

	public List<ItemStack> collectItems() {
		List<ItemStack> finalStacks = new ArrayList<>(stacks);
		stacks.clear();
		return finalStacks;
	}

	public int collectMoney() {
		return money.getAndSet(0);
	}

	public TextChannelWorld dropItem(Item item) {
		return dropItems(new ItemStack(item, 1));
	}

	public TextChannelWorld dropItem(int item) {
		return dropItem(Items.ALL[item]);
	}

	public boolean dropItemWithChance(Item item, int weight) {
		boolean doDrop = r.nextInt(weight) == 0;
		if (doDrop) dropItem(item);
		return doDrop;
	}

	public boolean dropItemWithChance(int item, int weight) {
		return dropItemWithChance(Items.fromId(item), weight);
	}

	public TextChannelWorld dropItems(List<ItemStack> stacks) {
		List<ItemStack> finalStacks = new ArrayList<>(stacks);
		this.stacks.addAll(finalStacks);
		return this;
	}

	public TextChannelWorld dropItems(ItemStack... stacks) {
		return dropItems(Arrays.asList(stacks));
	}

	public void dropMoney(int money) {
		this.money.addAndGet(money);
	}

	public boolean dropMoneyWithChance(int money, int weight) {
		boolean doDrop = r.nextInt(weight) == 0;
		if (doDrop) dropMoney(money);
		return doDrop;
	}

	public Map<GameReference, Integer> getRunningGames() {
		return ACTIVE_GAMES;
	}

	public TextChannelWorld removeEntity(Entity entity) {
		ACTIVE_ENTITIES.remove(entity);
		return this;
	}

	public TextChannelWorld removeGame(GameReference game) {
		ACTIVE_GAMES.remove(game);
		return this;
	}
}
