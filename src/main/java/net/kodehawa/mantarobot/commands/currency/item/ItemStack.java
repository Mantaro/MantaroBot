package net.kodehawa.mantarobot.commands.currency.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.Utils.randomOrder;

public class ItemStack {
    private final int amount;
    private final Item item;

    public ItemStack(Item item, int amount) {
        this.item = item;
        this.amount = amount;
    }

    public ItemStack(int i, int amount) {
        this(Items.fromId(i), amount);
    }

    public static Map<Item, ItemStack> mapped(List<ItemStack> list) {
        return list.stream().filter(stack -> stack.getAmount() != 0).collect(Collectors.toMap(ItemStack::getItem, UnaryOperator.identity(), ItemStack::join));
    }

    public static List<ItemStack> reduce(List<ItemStack> list) {
        return new ArrayList<>(mapped(list).values());
    }

    public static String toString(List<ItemStack> list) {
        if(list.isEmpty()) return "There's only dust.";
        return list.stream().filter(stack -> stack.getAmount() != 0).map(Object::toString).sorted(randomOrder()).collect(Collectors.joining(", "));
    }

    public static String toString(ItemStack... stacks) {
        return toString(Arrays.asList(stacks));
    }

    @Override
    public String toString() {
        return getItem().getEmoji() + " x " + getAmount();
    }

    public int getAmount() {
        return amount;
    }

    public Item getItem() {
        return item;
    }

    public ItemStack join(ItemStack stack) {
        if(!stack.getItem().equals(this.getItem())) throw new UnsupportedOperationException("Not the same Items");
        if(!canJoin(stack)) throw new UnsupportedOperationException("Add a check for canJoin before this");
        return new ItemStack(this.getItem(), this.getAmount() + stack.getAmount());
    }

    public boolean canJoin(ItemStack other) {
        int amountAfter = amount + other.amount;
        return amountAfter >= 0 && amountAfter <= 5000;
    }
}
