package net.kodehawa.mantarobot.db.entities.helpers;

import lombok.extern.slf4j.Slf4j;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.Items;

import java.util.*;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.db.entities.helpers.Inventory.Resolver.serialize;
import static net.kodehawa.mantarobot.db.entities.helpers.Inventory.Resolver.unserialize;

@Slf4j
public class Inventory {
    private final Map<Integer, Integer> inventory;

    public Inventory() {
        this(new HashMap<>());
    }

    public Inventory(Map<Integer, Integer> raw) {
        inventory = raw;
    }

    public List<ItemStack> asList() {
        return unserialize(inventory);
    }

    public Map<Item, ItemStack> asMap() {
        return ItemStack.mapped(asList());
    }

    public void clear() {
        inventory.clear();
    }

    public void clearOnlySellables() {
        List<ItemStack> ns = asList().stream().filter(item -> !item.getItem().isSellable()).collect(Collectors.toList());
        replaceWith(ns);
    }

    public boolean containsItem(Item item) {
        return asMap().containsKey(item);
    }

    public int getAmount(Item item) {
        return asMap().getOrDefault(item, new ItemStack(item, 0)).getAmount();
    }

    public ItemStack getStackOf(Item item) {
        if(containsItem(item)) {
            return asMap().get(item);
        } else {
            return null;
        }
    }

    public boolean merge(List<ItemStack> inv) {
        Map<Integer, Integer> map = new HashMap<>(inventory);
        Map<Integer, Integer> toAdd = serialize(inv);
        boolean[] hadOverflow = {false};
        toAdd.forEach((id, amount) -> {
            int currentAmount = map.getOrDefault(id, 0);
            if(currentAmount + amount > 5000) {
                currentAmount = 5000;
                hadOverflow[0] = true;
            } else {
                currentAmount += amount;
            }
            map.put(id, currentAmount);
        });
        replaceWith(unserialize(map));
        return hadOverflow[0];
    }

    public void process(List<ItemStack> is) {
        if(merge(is)) {
            log.error("Unhandled 'overflow' at " + Thread.currentThread().getStackTrace()[2] + ". <@182245310024777728> check it out");
        }
    }

    public void process(ItemStack... stacks) {
        merge(Arrays.asList(stacks));
    }

    public void replaceWith(List<ItemStack> inv) {
        inventory.clear();
        inventory.putAll(serialize(inv));
    }

    public static class Resolver {
        public static Map<Integer, Integer> serialize(List<ItemStack> list) {
            Map<Integer, Integer> collect = list.stream().filter(stack -> stack.getAmount() != 0).collect(Collectors.toMap(stack -> Items.idOf(stack.getItem()), ItemStack::getAmount, Integer::sum));
            collect.values().remove(0);
            return collect;
        }

        public static List<ItemStack> unserialize(Map<Integer, Integer> map) {
            if(map == null) return new ArrayList<>();
            return map.entrySet().stream().filter(e -> e.getValue() != 0).map(entry -> new ItemStack(Items.fromId(entry.getKey()), Math.max(Math.min(entry.getValue(), 5000), 0))).collect(Collectors.toList());
        }
    }
}
