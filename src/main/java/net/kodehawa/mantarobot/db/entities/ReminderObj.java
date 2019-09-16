/*
 * Copyright (C) 2016-2019 David Alejandro Rubio Escares / Kodehawa
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
 *
 */

package net.kodehawa.mantarobot.db.entities;

import lombok.Data;

@Data
public class ReminderObj {

    public final String id;
    public final String reminder;
    public final long time;
    private final long scheduledAtMillis;
    private final String userId;
    private final long offset;

    public ReminderObj(String id, String userId, String reminder, long scheduledAt, long time, long offset) {
        this.id = id;
        this.userId = userId;
        this.reminder = reminder;
        this.time = time;
        this.scheduledAtMillis = scheduledAt;
        this.offset = offset;
    }
}
