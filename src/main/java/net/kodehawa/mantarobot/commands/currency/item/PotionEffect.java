/*
 * Copyright (C) 2016 Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.currency.item;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.kodehawa.mantarobot.commands.currency.item.special.Potion;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@SuppressWarnings("unused")
public class PotionEffect {
    private String uuid;
    private int potion; //item id
    private long until;
    private ItemType.PotionType type;
    private long timesUsed;
    private long amountEquipped = 1;

    @BsonCreator
    public PotionEffect(@BsonProperty("potion") int potionId, @BsonProperty("until") long until, @BsonProperty("type") ItemType.PotionType type) {
        uuid = UUID.randomUUID().toString();
        this.potion = potionId;
        this.until = until;
        this.type = type;
    }

    @BsonIgnore
    public boolean use() {
        long newAmount = amountEquipped - 1;
        if (newAmount < 1) {
            return false;
        } else {
            setAmountEquipped(newAmount);
            setTimesUsed(0);
            return true;
        }
    }

    @BsonIgnore
    public void equip(int amount) {
        long newAmount = amountEquipped + amount;
        if (newAmount > 15) {
            setAmountEquipped(15);
        } else {
            setAmountEquipped(newAmount);
        }
    }

    @BsonIgnore
    public void equip() {
        equip(1);
    }

    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getPotion() {
        return this.potion;
    }

    public void setPotion(int potion) {
        this.potion = potion;
    }

    public long getUntil() {
        return this.until;
    }

    public void setUntil(long until) {
        this.until = until;
    }

    public ItemType.PotionType getType() {
        return this.type;
    }

    public void setType(ItemType.PotionType type) {
        this.type = type;
    }

    public long getTimesUsed() {
        return this.timesUsed;
    }

    public void setTimesUsed(long timesUsed) {
        this.timesUsed = timesUsed;
    }

    public long getAmountEquipped() {
        return this.amountEquipped;
    }

    public void setAmountEquipped(long amountEquipped) {
        this.amountEquipped = amountEquipped;
    }

    public static MessageEmbed.Field toDisplayField(SlashContext ctx, PotionEffect effect, PlayerEquipment equippedItems) {
        var potion = (Potion) ItemHelper.fromId(effect.getPotion());
        var potionEquipped = 0L;
        if (potion != null) {
            var effectActive = equippedItems.isEffectActive(potion.getEffectType(), potion.getMaxUses()) || effect.getAmountEquipped() > 1;
            potionEquipped = effectActive ? effect.getAmountEquipped() : effect.getAmountEquipped() - 1;
            var languageContext = ctx.getI18nContext();
            return new MessageEmbed.Field(
                    "%s\u2009\u2009\u2009%s".formatted(potion.getEmoji(), potion.getName()) +
                            (languageContext.getContextLanguage().equals("en_US") ? "" :
                                    " (" + languageContext.get(potion.getTranslatedName()) + ")")
                            + " (" + potionEquipped + "x)",
                    "%s: %,d %s%n%s: %,d %s%n%s: %s".formatted(
                            languageContext.get("commands.profile.stats.times_used"),
                            effect.getTimesUsed(),
                            languageContext.get("commands.profile.stats.times"),
                            languageContext.get("commands.profile.stats.uses"),
                            potion.getMaxUses(),
                            languageContext.get("commands.profile.stats.times"),
                            languageContext.get("commands.profile.stats." + (potion.getEffectType().isPotion() ? "potion_type" : "buff_type")),
                            languageContext.get("items.effect_types." + potion.getEffectType().name().toLowerCase())
                    ), true);
        }
        return null;
    }
}
