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
}