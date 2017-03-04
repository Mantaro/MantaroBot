package net.kodehawa.mantarobot.commands.currency.entity.player;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.commands.currency.entity.Entity;
import net.kodehawa.mantarobot.commands.currency.game.core.GameReference;
import net.kodehawa.mantarobot.commands.currency.inventory.Inventory;
import net.kodehawa.mantarobot.commands.currency.inventory.ItemStack;
import net.kodehawa.mantarobot.commands.currency.world.TextChannelWorld;
import net.kodehawa.mantarobot.data.MantaroData;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Single Guild {@link net.kodehawa.mantarobot.commands.currency.entity.Entity} wrapper.
 * This contains all the functions necessary to make the Player interact with the {@link TextChannelWorld} (World).
 * <p>
 * This is extended on {@link net.kodehawa.mantarobot.commands.currency.entity.player.EntityPlayerMP} (Global), so it also contains those objects.
 * When returned, it will return a {@link java.lang.String}  representation of all the objects here.
 * <p>
 * The user will see a representation of this if the guild is in local mode, else it will be a representation of EntityPlayerMP
 *
 * @author Kodehawa
 * @see net.kodehawa.mantarobot.commands.currency.entity.Entity
 */
public class EntityPlayer implements Entity {
	private transient static String entity;

	/**
	 * (INTERNAL)
	 * Gets the specified EntityPlayer which needs to be seeked.
	 *
	 * @param e The user to seek for.
	 * @return The EntityPlayer instance.
	 */
	public static EntityPlayer getPlayer(User e) {
		Objects.requireNonNull(e, "Player user cannot be null!");
		entity = e.toString();
		return MantaroData.getData().get().getUser(e, true);
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

	public Map<Integer, Integer> inventory = new HashMap<>();
	private transient GameReference currentGame;
	private int health = 250, stamina = 100;
	private long money = 0;
	//Don't serialize this.
	private transient boolean processing;
	private UUID uniqueId;

	public EntityPlayer() {
	}

	@Override
	public boolean addHealth(int amount) {
		if (health - amount < 0 || health + amount > getMaxHealth()) return false;
		health += amount;
		return true;
	}

	@Override
	public boolean addStamina(int amount) {
		if (stamina + amount < 0 || stamina + amount > getMaxStamina()) return false;
		stamina += amount;
		return true;
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
				"({type: %s, id: %s, entity: %s, money: %s, health: %s, stamina: %s, processing: %s, inventory: %s, game: %s)",
			getType(), getId(), entity, getMoney(), getHealth(), getStamina(), isProcessing(), getInventory().asList(), getGame());
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
	 * Sets a game. Normally done on a instance of {@link net.kodehawa.mantarobot.commands.currency.game.core.Game}
	 *
	 * @param game    The game you're gonna set. If set to null, it's taken as "no game" and normally done on Game close operations.
	 * @param channel The channel this was set on.
	 */
	public void setCurrentGame(@Nullable GameReference game, TextChannel channel) {
		currentGame = game;
		if (game != null) TextChannelWorld.of(channel).addEntity(this, game);
		else TextChannelWorld.of(channel).removeEntity(this);
	}
}
