package net.kodehawa.mantarobot.commands.rpg.entity.player;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.rpg.entity.Entity;
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
 * This contains all the functions necessary to make the Player interact with the {@link TextChannelWorld} (World) and the necessary functions to make
 * this {@link Entity} interact with the rest of the players and with itself.
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
	private int reputation = 0;

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
	 * @param m The user to seek for (from event).
	 * @return The EntityPlayer instance.
	 */
	public static EntityPlayer getPlayer(GuildMessageReceivedEvent m) {
		Objects.requireNonNull(m.getMember(), "Player user cannot be null!");
		entity = m.getMember().toString();
		world = TextChannelWorld.of(m.getChannel());
		return MantaroData.getData().get().getUser(m.getMember(), true);
	}

	/**
	 * (INTERNAL)
	 * Gets the specified EntityPlayer which needs to be seeked.
	 *
	 * @param m The user to seek for.
	 * @return The EntityPlayer instance.
	 */
	public static EntityPlayer getPlayer(Member m) {
		Objects.requireNonNull(m, "Player user cannot be null!");
		entity = m.toString();
		return MantaroData.getData().get().getUser(m, true);
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

	/**
	 * Sets a player reputation, ignoring the value already set.
	 * @param amount How much?
	 */
	public void setReputation(int amount){
		reputation = amount;
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

	/**
	 * Gets a player's reputation. Normally a result of community interaction, it's more like a merit than an actual RPG statistic.
	 * @return How much reputation do I have.
	 */
	public int getReputation(){
		return reputation;
	}

	/**
	 * @return How much stamina do I have to spare?
	 */
	@Override
	public int getMaxStamina() {
		return 100;
	}

	/**
	 * @return How much stamina do I have?
	 */
	public int getStamina() {
		return stamina;
	}

	/**
	 * Normally what to do on each tick.
	 * @param world The {@link TextChannelWorld} this entity is in.
	 */
	@Override
	public void behaviour(TextChannelWorld world) {
		Random r = new Random();
		int i = r.nextInt(350);
		int shift = r.nextInt(2) + 1;
		setCoordinates(new Coordinates(i / shift, 0, i / shift, world));
	}

	/**
	 * @return Where am I?
	 */
	@Override
	public Coordinates getCoordinates() {
		return coordinates;
	}

	/**
	 * Where I will be?.
	 */
	@Override
	public void setCoordinates(Coordinates coordinates) {
		this.coordinates = coordinates;
	}

	/**
	 * @return What am I?
	 */
	@Override
	public Type getType() {
		return Type.PLAYER;
	}

	/**
	 * UUID identifier. It's unique and gets saved to the DB when it's generated. Used for various checks.
	 * @return the UUID.
	 */
	@Override
	public UUID getId() {
		return uniqueId == null ?
			uniqueId = new UUID(money * new Random().nextInt(15), System.currentTimeMillis()) : uniqueId;
	}

	@Override
	public String toString() {
		return String.format(this.getClass().getSimpleName() +
				"({type: %s, id: %s, entity: %s, reputation: %s, money: %s, health: %s, stamina: %s, processing: %s, inventory: %s, game: %s, coordinates: %s)}",
			getType(), getId(), entity, getReputation(), getMoney(), getHealth(), getStamina(), isProcessing(), getInventory().asList(), getGame(), getCoordinates());
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

	/**
	 * Adds x amount of reputation to a player. Normally 1.
	 * @param rep how much?
	 * @return are you less than 400?
	 */
	public boolean addReputation(long rep) {
		if (this.reputation + rep > 4000) return false;
		this.reputation += rep;
		return true;
	}

	/**
	 * Makes a player a little bit sicker. Normally the result of sick-inducing activities like mining.
	 * @param amount how much am I gonna consume?
	 * @return if it's more than zero.
	 */
	public boolean consumeHealth(int amount) {
		return this.health - amount >= 0 && addHealth(-amount);
	}

	/**
	 * Makes a player tired. If stamina reaches a critical point, you cannot do much action in the RPG.
	 * @param amount how much am I gonna consume?
	 * @return if it's more than zero.
	 */
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

	/**
	 * @return How much money do I have to spare?
	 */
	public long getMoney() {
		return money;
	}

	/**
	 * Set a specific amount of money to the player, overwritting previous values.
	 */
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
	 * Adds one reputation point.
	 * @return this.
	 */
	public EntityPlayer addReputation() {
		this.reputation = reputation++;
		return this;
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
