package net.kodehawa.mantarobot.commands.currency.entity;

import net.kodehawa.mantarobot.commands.currency.inventory.Inventory;
import net.kodehawa.mantarobot.data.MantaroData;

import java.util.UUID;

/**
 * Interface for all Entities.
 * All entities have a UUID (Unique Identifier), a {@link net.kodehawa.mantarobot.commands.currency.inventory.Inventory}, health and stamina by default.
 * More types of variables can and might be added to the specific entity.
 * All entities have a {@link Type}, which determines its behaviour with the surrounding {@link net.kodehawa.mantarobot.commands.currency.inventory.TextChannelGround}
 * Data is saved to a serialized JSON-type file for concurrent usage.
 */
public interface Entity {

	default UUID getId() {
		return UUID.randomUUID();
	}

	Inventory getInventory();

	boolean addHealth(int amount);

	boolean addStamina(int amount);

	int getMaxHealth();

	int getMaxStamina();

	int getHealth();

	int getStamina();

	Type getType();

	default String debug(){
		return String.format(this.getClass().getSimpleName() +
						"({type: %s, id: %s, entity: %s, health: %s, stamina: %s, processing: %s, inventory: %s})",
				getType(), getId(), 0, getHealth(), getStamina(), false, getInventory().asList());
	}

	default String save(){
		MantaroData.getData().save();
		return "Saved data";
	}

	enum Type{
		PLAYER("player"), MOB("mob"), SPECIAL("entity");

		String type;

		Type(String s){
			type = s;
		}

		@Override
		public String toString() {
			return type;
		}
	}
}