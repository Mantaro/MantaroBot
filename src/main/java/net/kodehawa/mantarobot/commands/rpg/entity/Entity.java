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

	/**
	 * Adds x amount of health to the entity. Used in recovery and potion process.
	 *
	 * @param amount How much?
	 * @return Did it pass through? Please? (aka, did it not overflow?)
	 */
	boolean addHealth(int amount);

	/**
	 * Adds x amount of stamina to the entity. Used in recovery and potion process.
	 *
	 * @param amount How much?
	 * @return Did it pass through? Please? (aka, did it not overflow?)
	 */
	boolean addStamina(int amount);

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
}