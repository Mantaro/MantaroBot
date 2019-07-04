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

package net.kodehawa.mantarobot.db.entities.helpers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.item.PotionEffect;
import net.kodehawa.mantarobot.commands.currency.pets.Pet;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.profile.ProfileComponent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Data
public class PlayerData {
    public long experience = 0;
    private List<Badge> badges = new ArrayList<>();
    //Fix massive misspelling fuck up.
    @JsonProperty("dailyStrike")
    private long dailyStreak;
    private String description = null;
    private long gamesWon = 0;
    private long lastDailyAt;
    private long lockedUntil = 0;
    private Long marriedSince = null;
    private String marriedWith = null;
    private long moneyOnBank = 0;
    //null = most important badge shows.
    private Badge mainBadge = null;
    private long marketUsed;
    private boolean showBadge = true;
    private PotionEffect activePotion;
    private PotionEffect activeBuff;
    private long waifuCachedValue;
    private List<ProfileComponent> profileComponents = new LinkedList<>();
    private List<Pet> profilePets = new LinkedList<>();

    @JsonIgnore
    //LEGACY SUPPORT
    //Marriage UUID data is on UserData now!
    public boolean isMarried() {
        return marriedWith != null && MantaroBot.getInstance().getUserById(marriedWith) != null;
    }

    @JsonIgnore
    public boolean hasBadge(Badge b) {
        return badges.contains(b);
    }

    @JsonIgnore
    public boolean addBadgeIfAbsent(Badge b) {
        if(hasBadge(b)) {
            return false;
        }

        badges.add(b);
        return true;
    }

    @JsonIgnore
    public boolean removeBadge(Badge b) {
        if(!hasBadge(b)) {
            return false;
        }

        badges.remove(b);
        return true;
    }
}
