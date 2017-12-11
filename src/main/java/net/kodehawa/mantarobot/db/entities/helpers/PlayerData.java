/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;

import java.beans.Transient;
import java.text.SimpleDateFormat;
import java.util.*;

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

    @Transient
    public boolean isMarried() {
        return marriedWith != null && MantaroBot.getInstance().getUserById(marriedWith) != null;
    }

    @Transient
    public String marryDate() {
        if(getMarriedSince() == null) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        final Date date = new Date(getMarriedSince());
        return sdf.format(date);
    }

    @Transient
    public String anniversary() {
        if(getMarriedSince() == null) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        Calendar cal = new GregorianCalendar();
        cal.setTime(new Date(getMarriedSince()));
        cal.add(Calendar.YEAR, 1);
        return sdf.format(cal.getTime());
    }

    @Transient
    public boolean hasBadge(Badge b) {
        return badges.contains(b);
    }

    @Transient
    public boolean addBadge(Badge b) {
        if(hasBadge(b)) {
            return false;
        }

        badges.add(b);
        return true;
    }

    @Transient
    public boolean removeBadge(Badge b) {
        if(!hasBadge(b)) {
            return false;
        }

        badges.remove(b);
        return true;
    }
}
