package net.kodehawa.mantarobot.commands.currency.entity.player;

import net.kodehawa.mantarobot.commands.currency.entity.Entity;
import net.kodehawa.mantarobot.commands.currency.inventory.Inventory;
import net.kodehawa.mantarobot.commands.currency.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class EntityPlayer implements Entity {
	public Map<Integer, Integer> inventory = new HashMap<>();
	private long money = 0;
	private int health = 250, stamina = 100;
	private boolean processing;

	@Override
	public int getMaxHealth(){
		return 250;
	}

	@Override
	public int getMaxStamina(){
		return 100;
	}

	@Override
	public boolean addStamina(int amount) {
		if (stamina + amount < 0 || stamina + amount > getMaxStamina()) return false;
		stamina += amount;
		return true;
	}

	@Override
	public boolean addHealth(int amount) {
		if (health - amount < 0 || health + amount > getMaxHealth()) return false;
		health += amount;
		return true;
	}

	public boolean consumeStamina(int amount) {
		if (this.stamina - amount < 0) return false;
		return addStamina(-amount);
	}

	public boolean consumeHealth(int amount) {
		if (this.health - amount < 0) return false;
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

	@Override
	public UUID getId() {
		return new UUID(money * new Random().nextInt(15), System.currentTimeMillis());
	}

	@Override
	public Inventory getInventory() {
		return new Inventory(this);
	}

	public boolean removeMoney(long money) {
		if (this.money - money < 0) return false;
		this.money -= money;
		return true;
	}

	public long getMoney() {
		return money;
	}

	public void setMoney(long amount){
		money = amount;
	}

	public int getHealth() {
		return health;
	}

	public int getStamina() {
		return stamina;
	}

	public void setProcessing(boolean processing) {
		this.processing = processing;
	}

	public boolean isProcessing() {
		return processing;
	}
}
