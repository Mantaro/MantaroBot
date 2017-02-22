package net.kodehawa.mantarobot.commands.currency.inventory;

import net.kodehawa.mantarobot.data.Data.UserData;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.commands.currency.inventory.Inventory.Resolver.serialize;
import static net.kodehawa.mantarobot.commands.currency.inventory.Inventory.Resolver.unserialize;

public class Inventory {
	static class Resolver {
		public static List<ItemStack> unserialize(Map<Integer, Integer> map) {
			return map.entrySet().stream().map(entry -> new ItemStack(Items.fromId(entry.getKey()), entry.getValue())).collect(Collectors.toList());
		}

		public static Map<Integer, Integer> serialize(List<ItemStack> inventory) {
			return ItemStack.serialized(inventory);
		}
	}

	private UserData userData;

	public Inventory(UserData userData) {
		this.userData = userData;
	}

	public List<ItemStack> asList() {
		return unserialize(userData.inventory);
	}

	public void replaceWith(List<ItemStack> inv) {
		userData.inventory = serialize(inv);
	}

	public void merge(List<ItemStack> inv) {
		List<ItemStack> cur = asList();
		cur.addAll(inv);
		replaceWith(ItemStack.reduce(cur));
	}
}
