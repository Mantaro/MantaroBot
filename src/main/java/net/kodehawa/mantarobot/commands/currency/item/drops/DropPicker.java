package net.kodehawa.mantarobot.commands.currency.item.drops;

import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.ItemType;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

public class DropPicker {
    private final int totalWeight;
    private final NavigableMap<Integer, ItemDrop> map = new TreeMap<>();
    private final Map<Item, Integer> ensured = new HashMap<>();
    private final List<PercentageDrop> percentages;

    private DropPicker(List<ItemDrop> drops, List<ItemDrop> ensured, List<PercentageDrop> percentages) {
        var totalWeight = 0;
        for (final ItemDrop drop : drops) {
            totalWeight += drop.weight();
            this.map.put(totalWeight, drop);
        }
        for (final ItemDrop drop : ensured) {
            totalWeight += drop.weight();
            this.map.put(totalWeight, drop);
            this.ensured.put(drop.item(), drop.min());
        }
        this.totalWeight = totalWeight;
        this.percentages = percentages;
    }


    /**
     * Retrieve a random amount and kinds of drops
     * @param rolls the amount of rolls (does not affect {@link PercentageDrop})
     * @param random the random instance
     * @param toolLevel tool level used by the player
     * @param context language context of the player
     * @param isPremium whether the player is premium
     * @param msg message to append any potential {@link PercentageDrop#msg()} calls to.
     * @return a list of Item stacks dropped (already squashed).
     */
    public List<ItemStack> getRandomDrops(int rolls, Random random, int toolLevel, I18nContext context, boolean isPremium, StringBuilder msg) {
        List<ItemStack> list = new ArrayList<>();
        // handle non percentage based drops
        for (int i = 0; i < rolls; i++) {
            var ran = map.higherKey(random.nextInt(totalWeight));
            var drop = map.get(ran);
            // tool level too low
            if (toolLevel < drop.reqToolLevel()) continue;
            var am = random.nextInt(drop.max() - drop.min() + 1) + drop.min();
            var existing = list.stream().filter(is -> is.getItem().equals(drop.item())).findFirst();
            if (existing.isPresent()) {
                var stack = existing.get();
                list.set(list.indexOf(stack), new ItemStack(stack.getItem(), stack.getAmount() + am));
            } else {
                list.add(new ItemStack(drop.item(), am));
            }
        }

        // handle ensuring
        for (var ensure : ensured.keySet()) {
            var optional = list.stream().filter(is -> is.getItem().equals(ensure)).findFirst();
            // item wasn't already found, add min of it
            if (optional.isEmpty()) {
                list.add(new ItemStack(ensure, ensured.get(ensure)));
            }
        }

        // handle percentage based drops
        for (var percentageDrop : percentages) {
            // tool level too low
            if (toolLevel < percentageDrop.reqToolLevel()) continue;
            // should drop?
            if (random.nextInt(percentageDrop.bound()) > percentageDrop.weight()) {
                var am = random.nextInt(percentageDrop.max() - percentageDrop.min() + 1) + percentageDrop.min();
                var actualItem = percentageDrop.item();
                // handle crate replacement
                if (actualItem.getItemType() == ItemType.CRATE) {
                    actualItem = getCrate(isPremium, actualItem);
                }
                list.add(new ItemStack(actualItem, am));
                if (percentageDrop.msg() != null) {
                    msg.append("\n").append(EmoteReference.MEGA).append(
                            context.get(percentageDrop.msg()).formatted(
                                    actualItem.getEmojiDisplay(),
                                    actualItem.getName()
                            )
                    );
                }
            }
        }
        return list;
    }


    private static Item getCrate (boolean isPremium, Item item) {
        if (!isPremium) return item;
        if (item == ItemReference.FISH_CRATE) return ItemReference.FISH_PREMIUM_CRATE;
        if (item == ItemReference.CHOP_CRATE) return ItemReference.CHOP_PREMIUM_CRATE;
        if (item == ItemReference.MINE_CRATE) return ItemReference.MINE_PREMIUM_CRATE;
        return item;
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        List<ItemDrop> variable = new ArrayList<>();
        List<ItemDrop> ensured = new ArrayList<>();
        List<PercentageDrop> percentages = new ArrayList<>();

        private Builder() {
        }

        /**
         * Add an item drop to the picker. Drops added
         * this way have a chance to not be dropped at all.
         * If you want to always drop the minimum use {@link #ensureMin(ItemDrop...)}.
         *
         * @param drops drops to ensure
         * @return this builder instance for chaining
         */
        public Builder random(ItemDrop... drops) {
            variable.addAll(Arrays.asList(drops));
            return this;
        }

        /**
         * Add an ensured item drop to the picker. Drops added
         * this way will be effectively guaranteed to drop their
         * minimum amount. Regardless of rolls.
         * If you do not want this behaviour use {@link #random(ItemDrop...)}.
         *
         * @param drops drops to ensure
         * @return this builder instance for chaining
         */
        public Builder ensureMin(ItemDrop... drops) {
            ensured.addAll(Arrays.asList(drops));
            return this;
        }

        /**
         * Add a percentage based item drop.
         * <p>
         *     Important: These drops are handled independent of roll
         *     count passed to {@link #getRandomDrops(int, Random, int, I18nContext, boolean, StringBuilder)}.
         *     Meaning even with a roll count of 0 they can still drop.
         * </p>
         *
         * @param drops drops to ensure
         * @return this builder instance for chaining
         */
        public Builder percentage(PercentageDrop ...drops) {
            percentages.addAll(Arrays.asList(drops));
            return this;
        }

        public DropPicker build() {
            return new DropPicker(variable, ensured, percentages);
        }
    }
}
