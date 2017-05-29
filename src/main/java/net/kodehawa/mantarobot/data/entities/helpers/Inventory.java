package net.kodehawa.mantarobot.data.entities.helpers;

import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.data.entities.helpers.Inventory.Resolver.serialize;
import static net.kodehawa.mantarobot.data.entities.helpers.Inventory.Resolver.unserialize;

public class Inventory {
    //this can be removed if no itemstack errors are showing up
    private static final Logger LOGGER = LoggerFactory.getLogger("Inventory");

	public static class Resolver {
		public static Map<Integer, Integer> serialize(List<ItemStack> list) {
			Map<Integer, Integer> collect = list.stream().filter(stack -> stack.getAmount() != 0).collect(Collectors.toMap(stack -> Items.idOf(stack.getItem()), ItemStack::getAmount, Integer::sum));
			collect.values().remove(0);
			return collect;
		}

		public static List<ItemStack> unserialize(Map<Integer, Integer> map) {
			return map.entrySet().stream().filter(e -> e.getValue() != 0).map(entry -> new ItemStack(Items.fromId(entry.getKey()), Math.max(Math.min(entry.getValue(), 5000), 0))).collect(Collectors.toList());
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

	public void clearOnlySellables(){
		List<ItemStack> ns = asList().stream().filter(item -> !item.getItem().isSellable()).collect(Collectors.toList());
		replaceWith(ns);
	}

	public boolean containsItem(Item item) {
		return asMap().containsKey(item);
	}

	public ItemStack getStackOf(Item item) {
		if(containsItem(item)){
			return asMap().get(item);
		} else {
			return null;
		}
	}

	public int getAmount(Item item) {
		return asMap().getOrDefault(item, new ItemStack(item, 0)).getAmount();
	}

	public boolean merge(List<ItemStack> inv) {
		Map<Integer, Integer> map = new HashMap<>(inventory);
		Map<Integer, Integer> toAdd = serialize(inv);
		boolean[] hadOverflow = {false};
		toAdd.forEach((id, amount)->{
		    int currentAmount = map.getOrDefault(id, 0);
		    if(currentAmount + amount > 5000) {
		        currentAmount = 5000;
		        hadOverflow[0] = true;
            }
		    else {
		        currentAmount += amount;
            }
		    map.put(id, currentAmount);
        });
		replaceWith(unserialize(map));
		return hadOverflow[0];
	}

	public void process(List<ItemStack> is) {
		if(merge(is)) {
		    LOGGER.error("Unhandled 'overflow' at " + Thread.currentThread().getStackTrace()[2] + ". <@182245310024777728> check it out");
        }
	}

	public void process(ItemStack... stacks) {
		merge(Arrays.asList(stacks));
	}

	public void replaceWith(List<ItemStack> inv) {
		inventory = serialize(inv);
	}
}
