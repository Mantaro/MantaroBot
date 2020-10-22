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

package net.kodehawa.mantarobot.commands.currency;

public class Waifu {
    private long moneyValue;
    private long badgeValue;
    private long experienceValue;
    private double reputationMultiplier;
    private long claimValue;
    private long finalValue;
    private long performance;

    public Waifu(long moneyValue, long badgeValue, long experienceValue,
                 double reputationMultiplier, long claimValue, long finalValue, long performance) {
        this.moneyValue = moneyValue;
        this.badgeValue = badgeValue;
        this.experienceValue = experienceValue;
        this.reputationMultiplier = reputationMultiplier;
        this.claimValue = claimValue;
        this.finalValue = finalValue;
        this.performance = performance;
    }

    public Waifu() {
    }

    public long getMoneyValue() {
        return this.moneyValue;
    }

    public void setMoneyValue(long moneyValue) {
        this.moneyValue = moneyValue;
    }

    public long getBadgeValue() {
        return this.badgeValue;
    }

    public void setBadgeValue(long badgeValue) {
        this.badgeValue = badgeValue;
    }

    public long getExperienceValue() {
        return this.experienceValue;
    }

    public void setExperienceValue(long experienceValue) {
        this.experienceValue = experienceValue;
    }

    public double getReputationMultiplier() {
        return this.reputationMultiplier;
    }

    public void setReputationMultiplier(double reputationMultiplier) {
        this.reputationMultiplier = reputationMultiplier;
    }

    public long getClaimValue() {
        return this.claimValue;
    }

    public void setClaimValue(long claimValue) {
        this.claimValue = claimValue;
    }

    public long getFinalValue() {
        return this.finalValue;
    }

    public void setFinalValue(long finalValue) {
        this.finalValue = finalValue;
    }

    public long getPerformance() {
        return this.performance;
    }

    public void setPerformance(long performance) {
        this.performance = performance;
    }
}
