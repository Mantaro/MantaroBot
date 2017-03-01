package net.kodehawa.mantarobot.data.data;

import net.kodehawa.mantarobot.commands.currency.inventory.Inventory;
import net.kodehawa.mantarobot.commands.currency.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class UserData {
	public Map<Integer, Integer> inventory = new HashMap<>();
	public long money = 0;

	public Inventory getInventory() {
		return new Inventory(this);
	}

	public boolean addMoney(long money) {
		try {
			this.money = Math.addExact(this.money, money);
			return true;
		} catch (ArithmeticException ignored) {
			this.money = 0;
			this.getInventory().process(new ItemStack(9,1));
			return false;
		}
	}

	public boolean removeMoney(long money){
		if(this.money - money < 0) return false;
		this.money = this.money - money;
		return true;
	}
}
