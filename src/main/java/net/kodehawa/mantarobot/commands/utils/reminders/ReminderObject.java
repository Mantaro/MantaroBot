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

package net.kodehawa.mantarobot.commands.utils.reminders;

//This just exists for the sake of serializing (!)
public class ReminderObject {
    public String id;
    public String reminder;
    public long time;
    private long scheduledAtMillis;
    private String userId;
    private String guildId;
    
    ReminderObject(String id, String reminder, long time, long scheduledAtMillis, String userId, String guildId) {
        this.id = id;
        this.reminder = reminder;
        this.time = time;
        this.scheduledAtMillis = scheduledAtMillis;
        this.userId = userId;
        this.guildId = guildId;
    }
    
    public static ReminderObjectBuilder builder() {
        return new ReminderObjectBuilder();
    }
    
    public String getId() {
        return this.id;
    }
    
    public String getReminder() {
        return this.reminder;
    }
    
    public long getTime() {
        return this.time;
    }
    
    public long getScheduledAtMillis() {
        return this.scheduledAtMillis;
    }
    
    public String getUserId() {
        return this.userId;
    }
    
    public String getGuildId() {
        return this.guildId;
    }
    
    public static class ReminderObjectBuilder {
        private String id;
        private String reminder;
        private long time;
        private long scheduledAtMillis;
        private String userId;
        private String guildId;
        
        ReminderObjectBuilder() {
        }
        
        public ReminderObject.ReminderObjectBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        public ReminderObject.ReminderObjectBuilder reminder(String reminder) {
            this.reminder = reminder;
            return this;
        }
        
        public ReminderObject.ReminderObjectBuilder time(long time) {
            this.time = time;
            return this;
        }
        
        public ReminderObject.ReminderObjectBuilder scheduledAtMillis(long scheduledAtMillis) {
            this.scheduledAtMillis = scheduledAtMillis;
            return this;
        }
        
        public ReminderObject.ReminderObjectBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public ReminderObject.ReminderObjectBuilder guildId(String guildId) {
            this.guildId = guildId;
            return this;
        }
        
        public ReminderObject build() {
            return new ReminderObject(id, reminder, time, scheduledAtMillis, userId, guildId);
        }
        
        public String toString() {
            return "ReminderObject.ReminderObjectBuilder(id=" + this.id + ", reminder=" + this.reminder + ", time=" + this.time + ", scheduledAtMillis=" + this.scheduledAtMillis + ", userId=" + this.userId + ", guildId=" + this.guildId + ")";
        }
    }
}
