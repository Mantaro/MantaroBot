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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemHelper;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.utils.data.JsonDataManager;

import java.beans.ConstructorProperties;
import java.security.SecureRandom;
import java.util.*;

import static net.kodehawa.mantarobot.db.entities.helpers.Inventory.Resolver.serialize;
import static net.kodehawa.mantarobot.db.entities.helpers.Inventory.Resolver.unserialize;

public class TextChannelGround {
    private static final SecureRandom random = new SecureRandom();

    public static Ground of(String id) {
        final var identifier =  "textchannelground:" + id;
        try (var jedis = MantaroData.getDefaultJedisPool().getResource()) {
            final var json = jedis.get(identifier);
            if (json == null) {
                // No ground found, create new one.
                var ground = new Ground(new HashMap<>(), 0, id);
                var newJson = JsonDataManager.toJson(ground);
                jedis.set(identifier, newJson);

                return ground;
            } else {
                return JsonDataManager.fromJson(json, Ground.class);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new Ground(new HashMap<>(), 0, id);
        }
    }

    public static Ground of(TextChannel ch) {
        return of(ch.getId());
    }

    public static void delete(TextChannel ch) {
        final var identifier = "textchannelground:" + ch.getId();
        try (var jedis = MantaroData.getDefaultJedisPool().getResource()) {
            // We don't need to check whether it exists or not
            // Redis will happily run it anyway, so we can save one query.
            jedis.del(identifier);
        }
    }

    public static Ground of(GuildMessageReceivedEvent event) {
        return of(event.getChannel());
    }

    public static class Ground {
        @JsonProperty("groundItems")
        final Inventory groundItems = new Inventory();
        int money;
        String channel;

        @JsonCreator
        @ConstructorProperties({"groundItems", "money", "channel"})
        public Ground(Map<Integer, Integer> inventory, int money, String channel) {
            this.money = money;
            this.channel = channel;
            this.groundItems.replaceWith(unserialize(inventory));
        }

        @JsonProperty("groundItems")
        public Map<Integer, Integer> rawGround() {
            return serialize(groundItems.asList());
        }

        @JsonIgnore
        public List<ItemStack> getGroundItems() {
            return groundItems.asList();
        }

        public void setGroundItems(List<ItemStack> groundItems) {
            this.groundItems.replaceWith(groundItems);
        }

        public int getMoney() {
            return money;
        }

        public void setMoney(int money) {
            this.money = money;
        }

        @JsonIgnore
        public int dropMoney(int amount) {
            int m = money += amount;
            save();
            return m;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        @JsonIgnore
        public ArrayList<ItemStack> collectItems() {
            ArrayList<ItemStack> finalStacks = new ArrayList<>();
            for (var stack : groundItems.asList()) {
                finalStacks.add(new ItemStack(stack.getItem(), Math.min(stack.getAmount(), 25)));
            }

            groundItems.clear();
            save();
            return finalStacks;
        }

        @JsonIgnore
        public int collectMoney() {
            var oldMoney = money;
            setMoney(0);
            save();
            return oldMoney;
        }

        @JsonIgnore
        public void dropItem(Item item) {
            if (groundItems.getAmount(item) >= ItemStack.MAX_STACK_SIZE) {
                return;
            }

            dropItems(new ItemStack(item, 1));
            save();
        }

        @JsonIgnore
        public void dropItemWithChance(Item item, int weight) {
            var doDrop = random.nextInt(weight) == 0;
            if (doDrop) {
                if (groundItems.getAmount(item) >= ItemStack.MAX_STACK_SIZE) {
                    return;
                }

                dropItem(item);
                save();
            }
        }

        @JsonIgnore
        public void dropItemWithChance(int item, int weight) {
            if (groundItems.getAmount(ItemHelper.fromId(item)) >= ItemStack.MAX_STACK_SIZE) {
                return;
            }

            dropItemWithChance(ItemHelper.fromId(item), weight);
            save();
        }

        @JsonIgnore
        public void dropItems(List<ItemStack> stacks) {
            this.groundItems.process(stacks);
            save();
        }

        @JsonIgnore
        public void dropItems(ItemStack... stacks) {
            dropItems(Arrays.asList(stacks));
            save();
        }

        @JsonIgnore
        public void save() {
            final var identifier =  "textchannelground:" + channel;
            try (var jedis = MantaroData.getDefaultJedisPool().getResource()) {
                jedis.set(identifier, JsonDataManager.toJson(this));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }
}
