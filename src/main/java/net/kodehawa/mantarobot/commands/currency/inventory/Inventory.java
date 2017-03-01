package net.kodehawa.mantarobot.commands.currency.inventory;

import net.kodehawa.mantarobot.data.Data.UserData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.commands.currency.inventory.Inventory.Resolver.serialize;
import static net.kodehawa.mantarobot.commands.currency.inventory.Inventory.Resolver.unserialize;

public class Inventory {
	static class Resolver {
		public static List<ItemStack> unserialize(Map<Integer, Integer> map) {
			return map.entrySet().stream().filter(e -> e.getValue() != 0).map(entry -> new ItemStack(Items.fromId(entry.getKey()), entry.getValue())).collect(Collectors.toList());
		}

		public static Map<Integer, Integer> serialize(List<ItemStack> list) {
			Map<Integer, Integer> collect = list.stream().collect(Collectors.toMap(stack -> Items.idOf(stack.getItem()), ItemStack::getAmount, Integer::sum));
			collect.values().remove(0);
			return collect;
		}
	}

	private UserData userData;

	public Inventory(UserData userData) {
		this.userData = userData;
	}

	public List<ItemStack> asList() {
		return unserialize(userData.inventory);
	}

	public Map<Item,ItemStack> asMap() {
		return ItemStack.mapped(asList());
	}

	public void clear() {
		replaceWith(new ArrayList<>());
	}

	public void replaceWith(List<ItemStack> inv) {
		userData.inventory = serialize(inv);
	}

	public void process(ItemStack stack) {
		merge(Collections.singletonList(stack));
	}

	public void merge(List<ItemStack> inv) {
		List<ItemStack> cur = asList();
		cur.addAll(inv);
		replaceWith(ItemStack.reduce(cur));
	}
}
