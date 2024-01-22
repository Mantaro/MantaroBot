package net.kodehawa.mantarobot.commands.currency.item;

public enum PotionEffectType {
    /**
     * Drop chance bonus. Generally applicable to all, one, or any combination of commands
     */
    DROP_CHANCE_OTHER,
    /**
     * Money based bonus
     */
    MONEY,
    /**
     * Durability reducing/affecting
     */
    DURABILITY,
    /**
     * Drop chance bonus. Fish command only.
     */
    DROP_CHANCE_FISH,
    /**
     * Applicable to any buff that affects the players pet
     */
    PET,
    /**
     * Daily buff (granted for multiples of x)
     */
    DAILY;
}
