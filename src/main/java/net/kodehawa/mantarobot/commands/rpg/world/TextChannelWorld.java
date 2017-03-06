package net.kodehawa.mantarobot.commands.rpg.world;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.rpg.entity.Entity;
import net.kodehawa.mantarobot.commands.rpg.entity.EntityTickable;
import net.kodehawa.mantarobot.commands.rpg.entity.player.EntityPlayer;
import net.kodehawa.mantarobot.commands.rpg.entity.world.EntityTree;
import net.kodehawa.mantarobot.commands.rpg.game.core.GameReference;
import net.kodehawa.mantarobot.commands.rpg.item.Item;
import net.kodehawa.mantarobot.commands.rpg.item.ItemStack;
import net.kodehawa.mantarobot.commands.rpg.item.Items;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class TextChannelWorld {
	private static final Map<String, List<ItemStack>> DROPPED_ITEMS = new HashMap<>();
	private static final Map<String, AtomicInteger> DROPPED_MONEY = new HashMap<>();
	private static List<EntityTickable> ACTIVE_ENTITIES = new CopyOnWriteArrayList<>();
	private static List<Entity> ACTIVE_STATIC_ENTITIES = new CopyOnWriteArrayList<>(); //non-tickable
	private static List<GameReference> ACTIVE_GAMES = new ArrayList<>();
	private static Random r = new Random(System.currentTimeMillis());
	private static TextChannel channel;

	public static TextChannelWorld of(String id) {
		return new TextChannelWorld(DROPPED_ITEMS.computeIfAbsent(id, k -> new ArrayList<>()), DROPPED_MONEY.computeIfAbsent(id, k -> new AtomicInteger(0)));
	}

	public static TextChannelWorld of(TextChannel ch) {
		channel = ch;
		return of(ch.getId());
	}

	public static TextChannelWorld of(GuildMessageReceivedEvent event) {
		channel = event.getChannel();
		return of(event.getChannel());
	}

	private final AtomicInteger money;
	private final List<ItemStack> stacks;

	private TextChannelWorld(List<ItemStack> stacks, AtomicInteger money) {
		this.stacks = stacks;
		this.money = money;
	}

	public TextChannelWorld addStaticEntity(Entity entity) {
		ACTIVE_STATIC_ENTITIES.add(entity);
		return this;
	}


	public TextChannelWorld addEntity(EntityTickable entity) {
		ACTIVE_ENTITIES.add(entity);
		return this;
	}

	public TextChannelWorld addGame(GameReference game) {
		//if it's running.
		removeGame(game);

		//add it with new quantity of people
		ACTIVE_GAMES.add(game);

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

	public List<GameReference> getRunningGames() {
		return ACTIVE_GAMES;
	}

	public List<EntityTickable> getActiveEntities() {
		return ACTIVE_ENTITIES;
	}

	public TextChannelWorld removeEntity(EntityTickable entity) {
		ACTIVE_ENTITIES.remove(entity);
		return this;
	}

	public TextChannelWorld removeStaticEntity(Entity entity) {
		ACTIVE_STATIC_ENTITIES.remove(entity);
		return this;
	}

	public TextChannelWorld removeGame(GameReference game) {
		ACTIVE_GAMES.remove(game);
		return this;
	}

	public void tick(GuildMessageReceivedEvent event){
		if(ACTIVE_ENTITIES.isEmpty()){
			EntityTree tree = new EntityTree();
			tree.onSpawn(TextChannelWorld.of(event));
		}

		ACTIVE_ENTITIES.forEach(entityTickable -> {
			if(entityTickable instanceof EntityPlayer){
				if(((EntityPlayer) entityTickable).getGame() == null && !((EntityPlayer) entityTickable).isProcessing())
					ACTIVE_ENTITIES.remove(entityTickable);
			}

			if(!entityTickable.check(event)){
				System.out.println("Ticked entity");
				entityTickable.tick(thi	s, event);
			}
		});
		System.out.println("Ticked world.");
	}

	public String toString(){
		return String.format("{World(%s, %s)}", channel, getActiveEntities().size());
	}
}
