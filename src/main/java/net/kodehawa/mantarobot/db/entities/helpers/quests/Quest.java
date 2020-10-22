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

package net.kodehawa.mantarobot.db.entities.helpers.quests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.beans.ConstructorProperties;
import java.util.concurrent.TimeUnit;

public class Quest {
    private final QuestType type;
    private final int amount;
    private int currentAmount;
    private long questTakenAt;

    @JsonCreator
    @ConstructorProperties({"type", "amount"})
    public Quest(QuestType type, int amount) {
        this.type = type;
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    public QuestType getType() {
        return type;
    }

    public int getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(int currentAmount) {
        this.currentAmount = currentAmount;
    }

    public long getQuestTakenAt() {
        return questTakenAt;
    }

    public void setQuestTakenAt(long questTakenAt) {
        this.questTakenAt = questTakenAt;
    }

    @JsonIgnore
    public boolean isDone() {
        return currentAmount >= amount;
    }

    @JsonIgnore
    public void increaseAmount() {
        this.currentAmount += 1;
    }

    @JsonIgnore
    public boolean isActive() {
        return getQuestTakenAt() + TimeUnit.DAYS.toMillis(1) >= System.currentTimeMillis();
    }

    @JsonIgnore
    public String getProgress() {
        return String.format("[ %s / %s ]", currentAmount, amount);
    }

    public static enum QuestType {
        MINE("commands.profile.quests.mine"),
        FISH("commands.profile.quests.fish"),
        CHOP("commands.profile.quests.chop"),
        PET("commands.profile.quests.pet"),
        CRATE("commands.profile.quests.crate");

        final String i18n;

        QuestType(String i18n) {
            this.i18n = i18n;
        }

        public String getI18n() {
            return i18n;
        }
    }
}
