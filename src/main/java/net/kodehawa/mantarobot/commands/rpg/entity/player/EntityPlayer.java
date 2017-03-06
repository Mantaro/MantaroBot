package net.kodehawa.mantarobot.commands.rpg.entity.player;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.rpg.entity.EntityTickable;
import net.kodehawa.mantarobot.commands.rpg.game.core.GameReference;
import net.kodehawa.mantarobot.commands.rpg.inventory.Inventory;
import net.kodehawa.mantarobot.commands.rpg.item.ItemStack;
import net.kodehawa.mantarobot.commands.rpg.world.TextChannelWorld;
import net.kodehawa.mantarobot.data.MantaroData;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Single Guild {@link net.kodehawa.mantarobot.commands.rpg.entity.Entity} wrapper.
 * This contains all the functions necessary to make the Player interact with the {@link TextChannelWorld} (World).
 * <p>
 * This is extended on {@link net.kodehawa.mantarobot.commands.rpg.entity.player.EntityPlayerMP} (Global), so it also contains those objects.
 * When returned, it will return a {@link java.lang.String}  representation of all the objects here.
 * <p>
 * The user will see a representation of this if the guild is in local mode, else it will be a representation of EntityPlayerMP
 *
 * @author Kodehawa
 * @see net.kodehawa.mantarobot.commands.rpg.entity.Entity
 */
public class EntityPlayer extends EntityTickable {
	public Map<Integer, Integer> inventory = new HashMap<>();
	private int health = 250;
	private int stamina = 100;
	private long money = 0;
	private UUID uniqueId;

	//Don't serialize this.
	private static transient TextChannelWorld world;
	private transient Coordinates coordinates = new Coordinates(0, 0, 0, world);
	private transient static String entity;
	private transient GameReference currentGame;
	private transient boolean processing;

	/**
	 * Default constructor for this player. Won't do much, tbh.
	 */
	public EntityPlayer() {}

	/**
	 * Ticks this entity on every message received on the world.
	 * @param world The world where this entity is located at.
	 * @param event The received event.
	 */
	@Override
	public void tick(TextChannelWorld world, GuildMessageReceivedEvent event) {
		//this is a test pls.
		behaviour(world);
	}

	/**
	 * (INTERNAL)
	 * Gets the specified EntityPlayer which needs to be seeked.
	 *
	 * @param m The user to seek for.
	 * @return The EntityPlayer instance.
	 */
	public static EntityPlayer getPlayer(GuildMessageReceivedEvent m) {
		Objects.requireNonNull(m.getMember(), "Player user cannot be null!");
		entity = m.getMember().toString();
		world = TextChannelWorld.of(m.getChannel());
		return MantaroData.getData().get().getUser(m.getMember(), true);
	}

	/**
	 * Gets the specified EntityPlayer which needs to be seeked.
	 *
	 * @param entityId The user id to seek for.
	 * @return The EntityPlayer instance.
	 */
	public static EntityPlayer getPlayer(String entityId) {
		Objects.requireNonNull(entityId, "Player id cannot be null!");
		entity = entityId;
		return MantaroData.getData().get().users.getOrDefault(entityId, new EntityPlayerMP());
	}

	@Override
	public void setHealth(int amount) {
		health = amount;
	}

	@Override
	public void setStamina(int amount) {
		stamina = amount;
	}

	@Override
	public TextChannelWorld getWorld() {
		return world;
	}

	public int getHealth() {
		return health;
	}

	@Override
	public Inventory getInventory() {
		return new Inventory(this);
	}

	@Override
	public int getMaxHealth() {
		return 250;
	}

	@Override
	public int getMaxStamina() {
		return 100;
	}

	public int getStamina() {
		return stamina;
	}

	@Override
	public void behaviour(TextChannelWorld world) {
		Random r = new Random();
		int i = r.nextInt(350);
		int shift = r.nextInt(2);
		setCoordinates(new Coordinates(i / shift, 0, i / shift, world));
	}

	@Override
	public Coordinates getCoordinates() {
		return coordinates;
	}

	@Override
	public void setCoordinates(Coordinates coordinates) {
		this.coordinates = coordinates;
	}

	@Override
	public Type getType() {
		return Type.PLAYER;
	}

	@Override
	public UUID getId() {
		return uniqueId == null ?
			uniqueId = new UUID(money * new Random().nextInt(15), System.currentTimeMillis()) : uniqueId;
	}

	@Override
	public String toString() {
		return String.format(this.getClass().getSimpleName() +
				"({type: %s, id: %s, entity: %s, money: %s, health: %s, stamina: %s, processing: %s, inventory: %s, game: %s, coordinates: %s)}",
			getType(), getId(), entity, getMoney(), getHealth(), getStamina(), isProcessing(), getInventory().asList(), getGame(), getCoordinates());
	}

	/**
	 * Adds x amount of money from the player.
	 *
	 * @param money How much?
	 * @return pls dont overflow.
	 */
	public boolean addMoney(long money) {
		try {
			this.money = Math.addExact(this.money, money);
			return true;
		} catch (ArithmeticException ignored) {
			this.money = 0;
			this.getInventory().process(new ItemStack(9, 1));
			return false;
		}
	}

	public boolean consumeHealth(int amount) {
		return this.health - amount >= 0 && addHealth(-amount);
	}

	public boolean consumeStamina(int amount) {
		return this.stamina - amount >= 0 && addStamina(-amount);
	}

	/**
	 * What game am I playing?
	 *
	 * @return The game this EntityPlayer is currently involved in. This is used later on various game checks.
	 */
	public GameReference getGame() {
		return currentGame;
	}

	public long getMoney() {
		return money;
	}

	public void setMoney(long amount) {
		money = amount;
	}

	/**
	 * Returns the {@link TextChannelWorld} where the player is located.
	 *
	 * @param channel TextChannel to get the World from.
	 * @return The current World.
	 */
	public TextChannelWorld getWorld(TextChannel channel) {
		return TextChannelWorld.of(channel);
	}

	/**
	 * Is it receiving dynamic data?
	 *
	 * @return is it?
	 */
	public boolean isProcessing() {
		return processing;
	}

	/**
	 * Set the preparation for receive data.
	 * This is done to prevent it to receive data twice and also to prevent duplication of data.
	 *
	 * @param processing is it receiving data?
	 */
	public void setProcessing(boolean processing) {
		this.processing = processing;
	}

	/**
	 * Removes x amount of money from the player. Only goes though if money removed sums more than zero (avoids negative values).
	 *
	 * @param money How much?
	 * @return well if the sum negative it won't pass through, you little fucker.
	 */
	public boolean removeMoney(long money) {
		if (this.money - money < 0) return false;
		this.money -= money;
		return true;
	}

	/**
	 * Sets a game. Normally done on a instance of {@link net.kodehawa.mantarobot.commands.rpg.game.core.Game}
	 *
	 * @param game    The game you're gonna set. If set to null, it's taken as "no game" and normally done on Game close operations.
	 * @param channel The channel this was set on.
	 */
	public void setCurrentGame(@Nullable GameReference game, TextChannel channel) {
		currentGame = game;
		if (game != null){
			TextChannelWorld.of(channel).addGame(game);
			TextChannelWorld.of(channel).addEntity(this);
		}
		else{
			TextChannelWorld.of(channel).addGame(null);
			TextChannelWorld.of(channel).removeEntity(this);
		}
	}
}
