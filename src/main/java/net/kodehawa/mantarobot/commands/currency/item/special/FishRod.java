package net.kodehawa.mantarobot.commands.currency.item.special;

import lombok.Getter;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemType;

public class FishRod extends Item {
    @Getter
    private int level;

    public FishRod(ItemType type, int level, String emoji, String name, String translatedName, String desc, long value, String recipe, int... recipeTypes) {
        super(type, emoji, name, translatedName, desc, value, true, false, recipe, recipeTypes);
        this.level = level;
    }
}
