package net.kodehawa.mantarobot.data.entities.helpers;

import net.kodehawa.mantarobot.commands.rpg.item.Item;
import net.kodehawa.mantarobot.commands.rpg.item.ItemStack;
import net.kodehawa.mantarobot.commands.rpg.item.Items;

import java.util.*;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.data.entities.helpers.Inventory.Resolver.serialize;
import static net.kodehawa.mantarobot.data.entities.helpers.Inventory.Resolver.unserialize;

public class Inventory {
	public static class Resolver {
		public static Map<Integer, Integer> serialize(List<ItemStack> list) {
			Map<Integer, Integer> collect = list.stream().filter(stack -> stack.getAmount() != 0).collect(Collectors.toMap(stack -> Items.idOf(stack.getItem()), ItemStack::getAmount, Integer::sum));
			collect.values().remove(0);
			return collect;
		}

		public static List<ItemStack> unserialize(Map<Integer, Integer> map) {
			return map.entrySet().stream().filter(e -> e.getValue() != 0).map(entry -> new ItemStack(Items.fromId(entry.getKey()), entry.getValue())).collect(Collectors.toList());
		}
	}

	private Map<Integer, Integer> inventory = new HashMap<>();

	public List<ItemStack> asList() {
		return unserialize(inventory);
	}

	public Map<Item, ItemStack> asMap() {
		return ItemStack.mapped(asList());
	}

	public void clear() {
		replaceWith(new ArrayList<>());
	}

	public boolean containsItem(Item item) {
		return asMap().containsKey(item);
	}

	public int getAmount(Item item) {
		return asMap().getOrDefault(item, new ItemStack(item, 0)).getAmount();
	}

	public void merge(List<ItemStack> inv) {
		List<ItemStack> cur = asList();
		cur.addAll(inv);
		replaceWith(ItemStack.reduce(cur));
	}

	public void process(ItemStack stack) {
		merge(Collections.singletonList(stack));
	}

	public void replaceWith(List<ItemStack> inv) {
		inventory = serialize(inv);
	}
}
