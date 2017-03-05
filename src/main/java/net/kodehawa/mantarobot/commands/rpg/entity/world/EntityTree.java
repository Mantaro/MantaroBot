package net.kodehawa.mantarobot.commands.rpg.entity.world;

import net.kodehawa.mantarobot.commands.rpg.entity.Entity;
import net.kodehawa.mantarobot.commands.rpg.inventory.Inventory;

public class EntityTree implements Entity {

	@Override
	public boolean addHealth(int amount) {
		return false;
	}

	@Override
	public boolean addStamina(int amount) {
		return false;
	}

	@Override
	public int getHealth() {
		return 0;
	}

	@Override
	public Inventory getInventory() {
		return null;
	}

	@Override
	public int getMaxHealth() {
		return 20;
	}

	@Override
	public int getMaxStamina() {
		return 0;
	}

	@Override
	public int getStamina() {
		return 0;
	}

	@Override
	public void behaviour() {

	}

	@Override
	public void onDeath() {

	}

	@Override
	public Type getType() {
		return Type.SPECIAL;
	}
}
