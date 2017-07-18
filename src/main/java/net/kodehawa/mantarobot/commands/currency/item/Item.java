package net.kodehawa.mantarobot.commands.currency.item;

import lombok.Getter;

import java.util.Random;

public class Item {
    protected final long value;
    private final boolean staticPrice;
    private final String emoji, name, desc;
    private long price, maxSize;
    private boolean sellable, buyable;
    @Getter
    private boolean hidden;

    public Item(String emoji, String name, String desc, long value) {
        this(emoji, name, desc, value, false, true, true, false, 100);
    }

    public Item(String emoji, String name, String desc, long value, boolean staticPrice, boolean sellable, boolean buyable, boolean hidden, long maxSize) {
        this.emoji = emoji;
        this.name = name;
        this.desc = desc;
        this.value = value;
        this.price = value;
        this.staticPrice = staticPrice;
        this.sellable = sellable;
        this.buyable = buyable;
        this.maxSize = maxSize;
        this.hidden = hidden;
    }

    public Item(String emoji, String name, String desc, long value, boolean sellable, boolean buyable) {
        this(emoji, name, desc, value, false, sellable, buyable, false, 100);
    }

    public Item(String emoji, String name, String desc, long value, boolean buyable) {
        this(emoji, name, desc, value, false, true, buyable, false, 100);
    }

    public Item(String emoji, String name, String desc, long value, boolean sellable, boolean buyable, boolean hidden) {
        this(emoji, name, desc, value, false, sellable, buyable, hidden, 100);
    }

    /**
     * Constructor specifically meant for special items. Assuming it will be hidden, with a market price of 0 and neither buyables or sellables
     * So market price really doesn't matter. The hidden attribute means it won't appear on market.
     *
     * @param emoji The emoji it should it display on market.
     * @param name  Display name.
     * @param desc  A short description, normally used in inventory.
     */
    public Item(String emoji, String name, String desc) {
        this(emoji, name, desc, 0, false, false, false, true, 100);
    }

    @Override
    public String toString() {
        return "**" + name + "** ($" + value + ")";
    }

    void changePrices(Random r) {
        if(staticPrice) return;
        long min = (long) (value * 0.9), max = (long) (value * 1.1), dif = max - min;
        price = min + r.nextInt((int) dif);
    }

    public String getDesc() {
        return desc;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getName() {
        return name;
    }

    public long getValue() {
        return price;
    }

    public boolean isBuyable() {
        return buyable;
    }

    public boolean isSellable() {
        return sellable;
    }

    public long maxSize() {
        return maxSize;
    }
}
