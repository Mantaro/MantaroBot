package net.kodehawa.mantarobot.data.data;

import net.kodehawa.mantarobot.commands.currency.inventory.Inventory;
import net.kodehawa.mantarobot.commands.currency.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class UserData {
	public Map<Integer, Integer> inventory = new HashMap<>();
	public long money = 0;
	public int health = 250, stamina = 100;

	public boolean addStamina(int amount) {
		if (stamina + amount < 0 || stamina + amount > 100) return false;
		stamina += amount;
		return true;
	}

	public boolean consumeStamina(int amount) {
		return addStamina(-amount);
	}

	public boolean addHealth(int amount) {
		if (health - amount < 0 || health + amount > 250) return false;
		health -= amount;
		return true;
	}

	public boolean consumeHealth(int amount) {
		return addHealth(-amount);
	}

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

	public Inventory getInventory() {
		return new Inventory(this);
	}

	public boolean removeMoney(long money) {
		if (this.money - money < 0) return false;
		this.money -= money;
		return true;
	}
}
