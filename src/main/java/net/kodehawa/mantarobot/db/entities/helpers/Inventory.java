/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.db.entities.helpers;

import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.db.entities.helpers.Inventory.Resolver.serialize;
import static net.kodehawa.mantarobot.db.entities.helpers.Inventory.Resolver.unserialize;

public class Inventory {
    private static final Logger LOGGER = LoggerFactory.getLogger("Inventory");
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
    
    public void clearOnlySellables() {
        List<ItemStack> ns = asList().stream().filter(item -> !item.getItem().isSellable()).collect(Collectors.toList());
        replaceWith(ns);
    }
    
    public boolean containsItem(Item item) {
        return asMap().containsKey(item);
    }
    
    public ItemStack getStackOf(Item item) {
        if(containsItem(item)) {
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
            LOGGER.error("Unhandled 'overflow' at " + Thread.currentThread().getStackTrace()[2] + ". <@182245310024777728> check it out");
        }
    }
    
    public void process(ItemStack... stacks) {
        merge(Arrays.asList(stacks));
    }
    
    public void replaceWith(List<ItemStack> inv) {
        inventory = serialize(inv);
    }
    
    
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
}
