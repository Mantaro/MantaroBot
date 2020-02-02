/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
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
    
    public Waifu(long moneyValue, long badgeValue, long experienceValue, double reputationMultiplier, long claimValue, long finalValue, long performance) {
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
    
    protected boolean canEqual(final Object other) {
        return other instanceof Waifu;
    }
    
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $moneyValue = this.moneyValue;
        result = result * PRIME + (int) ($moneyValue >>> 32 ^ $moneyValue);
        final long $badgeValue = this.badgeValue;
        result = result * PRIME + (int) ($badgeValue >>> 32 ^ $badgeValue);
        final long $experienceValue = this.experienceValue;
        result = result * PRIME + (int) ($experienceValue >>> 32 ^ $experienceValue);
        final long $reputationMultiplier = Double.doubleToLongBits(this.reputationMultiplier);
        result = result * PRIME + (int) ($reputationMultiplier >>> 32 ^ $reputationMultiplier);
        final long $claimValue = this.claimValue;
        result = result * PRIME + (int) ($claimValue >>> 32 ^ $claimValue);
        final long $finalValue = this.finalValue;
        result = result * PRIME + (int) ($finalValue >>> 32 ^ $finalValue);
        final long $performance = this.performance;
        result = result * PRIME + (int) ($performance >>> 32 ^ $performance);
        return result;
    }
    
    public boolean equals(final Object o) {
        if(o == this) return true;
        if(!(o instanceof Waifu)) return false;
        final Waifu other = (Waifu) o;
        if(!other.canEqual(this)) return false;
        if(this.moneyValue != other.moneyValue) return false;
        if(this.badgeValue != other.badgeValue) return false;
        if(this.experienceValue != other.experienceValue) return false;
        if(Double.compare(this.reputationMultiplier, other.reputationMultiplier) != 0) return false;
        if(this.claimValue != other.claimValue) return false;
        if(this.finalValue != other.finalValue) return false;
        return this.performance == other.performance;
    }
    
    public String toString() {
        return "Waifu(moneyValue=" + this.moneyValue + ", badgeValue=" + this.badgeValue + ", experienceValue=" + this.experienceValue + ", reputationMultiplier=" + this.reputationMultiplier + ", claimValue=" + this.claimValue + ", finalValue=" + this.finalValue + ", performance=" + this.performance + ")";
    }
}
