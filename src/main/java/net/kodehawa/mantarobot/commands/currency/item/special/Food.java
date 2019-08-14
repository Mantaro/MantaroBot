package net.kodehawa.mantarobot.commands.currency.item.special;

import lombok.Getter;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemType;

public class Food extends Item {
    @Getter
    private float saturation;
    @Getter
    private int hungerLevel;

    public Food(ItemType type, float saturation, int hungerLevel, String emoji, String name, String translatedName, String desc, long value, boolean buyable) {
        super(type, emoji, name, translatedName, desc, value, true, buyable);
        this.saturation = saturation;
        this.hungerLevel = hungerLevel;
    }
}
