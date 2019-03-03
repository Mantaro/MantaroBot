package net.kodehawa.mantarobot.commands.currency.item.special;

import lombok.Getter;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemType;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Castable;

public class FishRod extends Item implements Castable {
    @Getter
    private int level;
    @Getter
    private int castLevelRequired;
    @Getter
    private int maximumCastAmount;

    public FishRod(ItemType type, int level, int castLevelRequired, int maximumCastAmount, String emoji, String name, String translatedName, String desc, long value, String recipe, int... recipeTypes) {
        super(type, emoji, name, translatedName, desc, value, true, false, recipe, recipeTypes);
        this.level = level;
        this.castLevelRequired = castLevelRequired;
        this.maximumCastAmount = maximumCastAmount;
    }

    public FishRod(ItemType type, int level, int castLevelRequired, int maximumCastAmount, String emoji, String name, String translatedName, String desc, long value, boolean buyable, String recipe, int... recipeTypes) {
        super(type, emoji, name, translatedName, desc, value, true, buyable, recipe, recipeTypes);
        this.level = level;
        this.castLevelRequired = castLevelRequired;
        this.maximumCastAmount = maximumCastAmount;
    }

    public int getBreakRatio() {
        return 73 + (level + 4);
    }
}
