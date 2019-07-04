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
    private Type element;
    private boolean fly;
    private boolean venom;

    //Idle buffs
    private long idleRecoveryCoef;
    private long idleStaminaRecoveryCoef;

    //Battle buffs
    private long battleRecoveryCoef;
    private long battleStaminaRecoveryCoef;

    //Idle and battle multipliers
    private long recoveryMult;
    private long staminaRecoveryMult;

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

    protected enum Type {
        EARTH, WATER, FIRE
    }
}
