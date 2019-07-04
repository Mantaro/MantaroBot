/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.currency.pets;

import lombok.Getter;
import lombok.Setter;
import net.kodehawa.mantarobot.commands.currency.item.PotionEffect;

@Getter
@Setter
public class PetStats {
    //Global statistics
    private long stamina;
    private long hp;
    private boolean fly;
    private boolean venom;

    private long affection;

    //Idle buffs
    private double idleRecoveryCoef = 0.1;
    private double idleStaminaRecoveryCoef = 0.2;

    //Battle buffs
    private double battleRecoveryCoef = 0.18;
    private double battleStaminaRecoveryCoef = 0.25;

    //Idle and battle multipliers
    private double recoveryMult = 1.1;
    private double staminaRecoveryMult = 1.14;

    //Current battle stats
    private long currentStamina;
    private long currentHP;
    private boolean elementAffinity;
    private boolean elementBoost;
    private boolean elementQualification;

    //Current recovery stats
    private PotionEffect currentEffect;
    private long regenStat;
    private long staminaRegenCoef;

    @Getter
    public enum Type {
        EARTH("Earth", "commands.pet.types.earth"), WATER("Water", "commands.pet.types.water"), FIRE("Fire", "commands.pet.types.fire");

        String readable;
        String translatable;
        Type(String readable, String translatable) {
            this.readable = readable;
            this.translatable = translatable;
        }
    }
}
