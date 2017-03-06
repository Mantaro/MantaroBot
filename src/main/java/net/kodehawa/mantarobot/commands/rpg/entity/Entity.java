package net.kodehawa.mantarobot.commands.rpg.entity;

import net.kodehawa.mantarobot.commands.rpg.inventory.Inventory;
import net.kodehawa.mantarobot.commands.rpg.world.TextChannelWorld;
import net.kodehawa.mantarobot.data.MantaroData;

import java.util.UUID;

/**
 * Interface for all Entities.
 * All entities have a UUID (Unique Identifier), a {@link net.kodehawa.mantarobot.commands.rpg.inventory.Inventory}, health and stamina by default.
 * More types of variables can and might be added to the specific entity.
 * All entities have a {@link Type}, which determines its behaviour with the surrounding {@link TextChannelWorld}
 * Data is saved to a serialized JSON-type file for concurrent usage.
 */
public interface Entity {

	/**
	 * The possible {@link Entity} types possible.
	 */
	enum Type {
		PLAYER("player"), MOB("mob"), SPECIAL("entity");

		String type;

		Type(String s) {
			type = s;
		}

		@Override
		public String toString() {
			return type;
		}
	}

	void setHealth(int amount);

	void setStamina(int amount);

	TextChannelWorld getWorld();

	/**
	 * Adds x amount of health to the entity. Used in recovery and potion process.
	 *
	 * @param amount How much?
	 * @return Did it pass through? Please? (aka, did it not overflow?)
	 */
	default boolean addHealth(int amount) {
		if (getHealth() + amount < 0 || getHealth() + amount > getMaxHealth()) return false;
		setHealth(getHealth() + amount);
		return true;
	}

	/**
	 * Adds x amount of stamina to the entity. Used in recovery and potion process.
	 *
	 * @param amount How much?
	 * @return Did it pass through? Please? (aka, did it not overflow?)
	 */
	default boolean addStamina(int amount) {
		if (getStamina() + amount < 0 || getStamina() + amount > getMaxStamina()) return false;
		setStamina(getStamina() + amount);
		return true;
	}

	/**
	 * @return How much health do I have left?
	 */
	int getHealth();

	/**
	 * A wrapper for Inventory.
	 *
	 * @return The inventory for this entity.
	 */
	Inventory getInventory();

	/**
	 * @return How much health do I have to spare?
	 */
	int getMaxHealth();

	/**
	 * @return How much stamina do I have to spare?
	 */
	int getMaxStamina();

	/**
	 * @return How much stamina do I have left?
	 */
	int getStamina();

	/**
	 * The behaviour of this entity with the surrending {@link TextChannelWorld}. A tree will never move, but a player might randomly change coords.
	 * Also a tree will check if their surrondings are empty and spawn more trees. That's behaviour for ya.
	 */
	void behaviour(TextChannelWorld world);

	/**
	 * Implementation.
	 * @return Where am I in the current {@link TextChannelWorld}?
	 */
	Coordinates getCoordinates();

	void setCoordinates(Coordinates coordinates);

	/**
	 * Normally do nothing. But some entities will do special things on spawn.
	 */
	default void onSpawn(){}

	/**
	 * Normally do nothing. But some entities will do special things on death.
	 */
	default void onDeath(){}

	/**
	 * Normally do nothing. For future uses.
	 */
	default void onRespawn(){}

	/**
	 * The specified {@link Entity} type. Used for {@link TextChannelWorld} interactions.
	 *
	 * @return What am I?
	 */
	Type getType();

	/**
	 * @return A string representation of this {@link Entity}
	 */
	default String debug() {
		return String.format(this.getClass().getSimpleName() +
				"({type: %s, id: %s, entity: %s, health: %s, stamina: %s, processing: %s, inventory: %s})",
			getType(), getId(), 0, getHealth(), getStamina(), false, getInventory().asList());
	}

	/**
	 * @return The ID of this Entity.
	 */
	default UUID getId() {
		return UUID.randomUUID();
	}

	/**
	 * Saves the current Entity data, if needed. Only used for players.
	 *
	 * @return like, idk why it even returns.
	 */
	default String save() {
		MantaroData.getData().save();
		return "Saved data";
	}

	/**
	 * Where am I?
	 */
	class Coordinates {
		int x;
		int y;
		int z;
		TextChannelWorld entityWorld;

		public Coordinates(int x, int y, int z, TextChannelWorld world){
			this.x = x;
			this.y = y;
			this.z = z;
			this.entityWorld = world;
		}

		public String toString(){
			return String.format("[Coordinates(%d, %d, %d), World(%s)]", x, y, z, entityWorld);
		}
	}
}