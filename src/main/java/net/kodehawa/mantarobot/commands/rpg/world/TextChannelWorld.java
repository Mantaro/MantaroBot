package net.kodehawa.mantarobot.commands.rpg.world;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.rpg.entity.Entity;
import net.kodehawa.mantarobot.commands.rpg.entity.EntityTickable;
import net.kodehawa.mantarobot.commands.rpg.entity.player.EntityPlayer;
import net.kodehawa.mantarobot.commands.rpg.entity.world.EntityTree;
import net.kodehawa.mantarobot.commands.rpg.game.core.Game;
import net.kodehawa.mantarobot.commands.rpg.game.core.GameReference;
import net.kodehawa.mantarobot.commands.rpg.item.Item;
import net.kodehawa.mantarobot.commands.rpg.item.ItemStack;
import net.kodehawa.mantarobot.commands.rpg.item.Items;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class TextChannelWorld {
	private static final Map<String, List<EntityTickable>> ACTIVE_ENTITIES = new HashMap<>();
	private static final Map<String, Map<EntityPlayer, Game>> ACTIVE_GAMES = new HashMap<>();
	private static final Map<String, List<Entity>> ACTIVE_STATIC_ENTITIES = new HashMap<>(); //non-tickable
	private static final Map<String, List<ItemStack>> DROPPED_ITEMS = new HashMap<>();
	private static final Map<String, AtomicInteger> DROPPED_MONEY = new HashMap<>();
	private static TextChannel channel;
	private static Random r = new Random(System.currentTimeMillis());

	public static TextChannelWorld of(String id) {
		return new TextChannelWorld(
			DROPPED_ITEMS.computeIfAbsent(id, k -> new ArrayList<>()),
			DROPPED_MONEY.computeIfAbsent(id, k -> new AtomicInteger(0)),
			ACTIVE_ENTITIES.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()),
			ACTIVE_STATIC_ENTITIES.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()),
			ACTIVE_GAMES.computeIfAbsent(id, k -> new HashMap<>()));
	}

	public static TextChannelWorld of(TextChannel ch) {
		channel = ch;
		return of(ch.getId());
	}

	public static TextChannelWorld of(GuildMessageReceivedEvent event) {
		channel = event.getChannel();
		return of(event.getChannel());
	}

	private final List<Entity> entities;
	private final List<EntityTickable> entityTickables;
	private final Map<EntityPlayer, Game> games;
	private final AtomicInteger money;
	private final List<ItemStack> stacks;

	private TextChannelWorld(List<ItemStack> stacks, AtomicInteger money, List<EntityTickable> entityTickables,
							 List<Entity> entities, Map<EntityPlayer, Game> games) {
		this.stacks = stacks;
		this.money = money;
		this.entityTickables = entityTickables;
		this.entities = entities;
		this.games = games;
	}

	public String toString() {
		return String.format("{World(%s, %s)}", channel, getActiveEntities().size());
	}

	public TextChannelWorld addEntity(EntityTickable entity) {
		entityTickables.add(entity);
		return this;
	}

	public TextChannelWorld addGame(EntityPlayer player, Game game) {
		games.put(player, game);

		return this;
	}

	public TextChannelWorld addStaticEntity(Entity entity) {
		entities.add(entity);
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

	public List<EntityTickable> getActiveEntities() {
		return entityTickables;
	}

	public String getRepresentation() {
		return "";
	}

	public Map<EntityPlayer,Game> getRunningGames() {
		return games;
	}

	public List<Entity> getStaticEntities() {
		return entities;
	}

	public TextChannelWorld removeEntity(EntityTickable entity) {
		entityTickables.remove(entity);
		return this;
	}

	public TextChannelWorld removeGame(EntityPlayer player) {
		games.remove(player);
		return this;
	}

	public TextChannelWorld removeStaticEntity(Entity entity) {
		entities.remove(entity);
		return this;
	}

	public void tick(GuildMessageReceivedEvent event) {
		if (entityTickables.isEmpty()) {
			EntityTree tree = new EntityTree();
			tree.onSpawn(TextChannelWorld.of(event));
		}

		entityTickables.forEach(entityTickable -> {
			if (entityTickable instanceof EntityPlayer) {
				if (((EntityPlayer) entityTickable).getGame() == null && !((EntityPlayer) entityTickable).isProcessing())
					entityTickables.remove(entityTickable);
			}

			if (!entityTickable.check(event)) {
				entityTickable.tick(this, event);
			}
		});
	}
}
