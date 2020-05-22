/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.currency;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.Items;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TextChannelGround {
    //TODO: Move to redis as channel id -> int, int (item id, value)
    private static final Map<String, List<ItemStack>> DROPPED_ITEMS = new HashMap<>();
    private static final Map<String, AtomicInteger> DROPPED_MONEY = new HashMap<>();
    private static final Random r = new Random(System.currentTimeMillis());
    private final AtomicInteger money;
    private final List<ItemStack> stacks;

    private TextChannelGround(List<ItemStack> stacks, AtomicInteger money) {
        this.stacks = stacks;
        this.money = money;
    }

    public static TextChannelGround of(String id) {
        return new TextChannelGround(
                DROPPED_ITEMS.computeIfAbsent(id, k -> new ArrayList<>()),
                DROPPED_MONEY.computeIfAbsent(id, k -> new AtomicInteger(0))
        );
    }

    public static TextChannelGround of(TextChannel ch) {
        return of(ch.getId());
    }

    public static TextChannelGround of(GuildMessageReceivedEvent event) {
        return of(event.getChannel());
    }

    public List<ItemStack> collectItems() {
        List<ItemStack> finalStacks = new ArrayList<>();
        for (ItemStack stack : stacks) {
            finalStacks.add(new ItemStack(stack.getItem(), Math.min(stack.getAmount(), 25)));
        }

        stacks.clear();
        return finalStacks;
    }

    public int collectMoney() {
        return money.getAndSet(0);
    }

    public TextChannelGround dropItem(Item item) {
        return dropItems(new ItemStack(item, 1));
    }

    public TextChannelGround dropItem(int item) {
        return dropItem(Items.ALL[item]);
    }

    public boolean dropItemWithChance(Item item, int weight) {
        boolean doDrop = r.nextInt(weight) == 0;
        if (doDrop)
            dropItem(item);

        return doDrop;
    }

    public boolean dropItemWithChance(int item, int weight) {
        return dropItemWithChance(Items.fromId(item), weight);
    }

    public TextChannelGround dropItems(List<ItemStack> stacks) {
        List<ItemStack> finalStacks = new ArrayList<>(stacks);
        this.stacks.addAll(finalStacks);
        return this;
    }

    public TextChannelGround dropItems(ItemStack... stacks) {
        return dropItems(Arrays.asList(stacks));
    }

    public void dropMoney(int money) {
        this.money.addAndGet(money);
    }

    public boolean dropMoneyWithChance(int money, int weight) {
        boolean doDrop = r.nextInt(weight) == 0;
        if (doDrop)
            dropMoney(money);

        return doDrop;
    }
}
