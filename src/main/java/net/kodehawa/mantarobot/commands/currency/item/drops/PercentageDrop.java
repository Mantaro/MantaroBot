package net.kodehawa.mantarobot.commands.currency.item.drops;

import net.kodehawa.mantarobot.commands.currency.item.Item;

public class PercentageDrop extends ItemDrop {
    private final int bound;
    private final String msg;

    @SuppressWarnings("unused")
    public PercentageDrop(Item item, int bound, int weight, int min, int max, int reqToolLevel) {
        this(item, bound, weight, min, max, reqToolLevel, null);
    }

    @SuppressWarnings("unused")
    public PercentageDrop(Item item, int bound, int weight, int min, int max) {
        this(item, bound, weight, min, max, null);
    }

    public PercentageDrop(Item item, int bound, int threshold, int min, int max, String msg) {
        this(item, bound, threshold, min, max, 0, msg);
    }

    public PercentageDrop(Item item, int bound, int threshold, int min, int max, int reqToolLevel, String msg) {
        super(item, threshold, min, max, reqToolLevel);
        this.bound = bound;
        this.msg = msg;
    }

    public int bound() {
        return bound;
    }

    public String msg() {
        return msg;
    }
}
