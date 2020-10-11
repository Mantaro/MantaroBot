/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *  
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.currency.pets.global;

import net.kodehawa.mantarobot.commands.currency.item.PotionEffect;

public class PetStats {
    private boolean inBattle = false;

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
    private long currentStamina = getStamina(); //Unless this changes on battle, should remain equal.
    private long currentHP = getHp(); //Unless this changes in battle, should remain equal.
    private boolean elementAffinity;
    private boolean elementBoost;
    private boolean elementQualification;

    //Current recovery stats
    private PotionEffect currentEffect;
    private long regenStat;
    private long staminaRegenCoef;

    private long epochLastBattle;
    private long epochLastIdle;

    public boolean isInBattle() {
        return this.inBattle;
    }

    public void setInBattle(boolean inBattle) {
        this.inBattle = inBattle;
    }

    public long getStamina() {
        return this.stamina;
    }

    public void setStamina(long stamina) {
        this.stamina = stamina;
    }

    public long getHp() {
        return this.hp;
    }

    public void setHp(long hp) {
        this.hp = hp;
    }

    public boolean isFly() {
        return this.fly;
    }

    public void setFly(boolean fly) {
        this.fly = fly;
    }

    public boolean isVenom() {
        return this.venom;
    }

    public void setVenom(boolean venom) {
        this.venom = venom;
    }

    public long getAffection() {
        return this.affection;
    }

    public void setAffection(long affection) {
        this.affection = affection;
    }

    public double getIdleRecoveryCoef() {
        return this.idleRecoveryCoef;
    }

    public void setIdleRecoveryCoef(double idleRecoveryCoef) {
        this.idleRecoveryCoef = idleRecoveryCoef;
    }

    public double getIdleStaminaRecoveryCoef() {
        return this.idleStaminaRecoveryCoef;
    }

    public void setIdleStaminaRecoveryCoef(double idleStaminaRecoveryCoef) {
        this.idleStaminaRecoveryCoef = idleStaminaRecoveryCoef;
    }

    public double getBattleRecoveryCoef() {
        return this.battleRecoveryCoef;
    }

    public void setBattleRecoveryCoef(double battleRecoveryCoef) {
        this.battleRecoveryCoef = battleRecoveryCoef;
    }

    public double getBattleStaminaRecoveryCoef() {
        return this.battleStaminaRecoveryCoef;
    }

    public void setBattleStaminaRecoveryCoef(double battleStaminaRecoveryCoef) {
        this.battleStaminaRecoveryCoef = battleStaminaRecoveryCoef;
    }

    public double getRecoveryMult() {
        return this.recoveryMult;
    }

    public void setRecoveryMult(double recoveryMult) {
        this.recoveryMult = recoveryMult;
    }

    public double getStaminaRecoveryMult() {
        return this.staminaRecoveryMult;
    }

    public void setStaminaRecoveryMult(double staminaRecoveryMult) {
        this.staminaRecoveryMult = staminaRecoveryMult;
    }

    public long getCurrentStamina() {
        return this.currentStamina;
    }

    public void setCurrentStamina(long currentStamina) {
        this.currentStamina = currentStamina;
    }

    public long getCurrentHP() {
        return this.currentHP;
    }

    public void setCurrentHP(long currentHP) {
        this.currentHP = currentHP;
    }

    public boolean isElementAffinity() {
        return this.elementAffinity;
    }

    public void setElementAffinity(boolean elementAffinity) {
        this.elementAffinity = elementAffinity;
    }

    public boolean isElementBoost() {
        return this.elementBoost;
    }

    public void setElementBoost(boolean elementBoost) {
        this.elementBoost = elementBoost;
    }

    public boolean isElementQualification() {
        return this.elementQualification;
    }

    public void setElementQualification(boolean elementQualification) {
        this.elementQualification = elementQualification;
    }

    public PotionEffect getCurrentEffect() {
        return this.currentEffect;
    }

    public void setCurrentEffect(PotionEffect currentEffect) {
        this.currentEffect = currentEffect;
    }

    public long getRegenStat() {
        return this.regenStat;
    }

    public void setRegenStat(long regenStat) {
        this.regenStat = regenStat;
    }

    public long getStaminaRegenCoef() {
        return this.staminaRegenCoef;
    }

    public void setStaminaRegenCoef(long staminaRegenCoef) {
        this.staminaRegenCoef = staminaRegenCoef;
    }

    public long getEpochLastBattle() {
        return this.epochLastBattle;
    }

    public void setEpochLastBattle(long epochLastBattle) {
        this.epochLastBattle = epochLastBattle;
    }

    public long getEpochLastIdle() {
        return this.epochLastIdle;
    }

    public void setEpochLastIdle(long epochLastIdle) {
        this.epochLastIdle = epochLastIdle;
    }
}
