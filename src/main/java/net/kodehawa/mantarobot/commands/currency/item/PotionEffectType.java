package net.kodehawa.mantarobot.commands.currency.item;

public enum PotionEffectType {
    DROP_CHANCE_OTHER(true),
    WAIFU(true),
    DURABILITY(true),
    DROP_CHANCE_FISH(false);

    private final boolean isPotion;

    PotionEffectType(boolean isPotion) {
        this.isPotion = isPotion;
    }

    public boolean isPotion() {
        return isPotion;
    }
}
