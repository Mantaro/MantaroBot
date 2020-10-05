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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class QuestTracker {
    private List<Quest> currentActive = new ArrayList<>();

    public List<Quest> getCurrentActiveQuests() {
        return currentActive;
    }

    public void setCurrentActive(List<Quest> currentActive) {
        this.currentActive = currentActive;
    }

    @JsonIgnore
    public void addQuest(Quest quest) {
        currentActive.add(quest);
    }

    @JsonIgnore
    public void removeQuest(Quest quest) {
        currentActive.remove(quest);
    }

    @JsonIgnore
    public Quest startRandomQuest(SecureRandom random) {
        Quest.QuestType[] available = Quest.QuestType.values();
        Quest.QuestType toAssign = available[random.nextInt(available.length)];
        Quest quest = new Quest(toAssign, random.nextInt(40));
        addQuest(quest);

        return quest;
    }
}
