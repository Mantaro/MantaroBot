package net.kodehawa.mantarobot.commands.currency.entity;

import net.kodehawa.mantarobot.commands.currency.inventory.Inventory;

import java.util.UUID;

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

	public enum Type{
		PLAYER, MOB
	}
}