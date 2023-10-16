package net.kodehawa.mantarobot.commands.currency.item.drops;

import net.kodehawa.mantarobot.commands.currency.item.Item;

public class ItemDrop {
    private final Item item;
    private final int weight;
    private final int min;
    private final int max;
    private final int reqToolLevel;

    public ItemDrop(Item item, int weight, int min, int max) {
       this(item, weight, min, max, 0);
    }

    public ItemDrop(Item item, int weight, int min, int max, int reqToolLevel) {
        this.item = item;
        this.weight = weight;
        this.min = min;
        this.max = max;
        this.reqToolLevel = reqToolLevel;
    }

    public Item item() {
        return item;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    public int reqToolLevel() {
        return reqToolLevel;
    }

    public int weight() {
        return weight;
    }
}
