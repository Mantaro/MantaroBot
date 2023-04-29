/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.core.modules.commands.i18n;

import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.db.entities.MongoGuild;
import net.kodehawa.mantarobot.db.entities.MongoUser;

public class I18nContext {
    private MongoGuild guildData;
    private MongoUser userData;
    private I18n i18n = null;

    public I18nContext(MongoGuild guildData, MongoUser userData) {
        this.guildData = guildData;
        this.userData = userData;
    }

    public I18nContext(I18n i18n) {
        this.i18n = i18n;
    }

    public I18nContext() { }


    public String get(String s) {
        I18n context = I18n.getForLanguage(getContextLanguage());
        return context.get(s);
    }

    public String withRoot(String root, String s) {
        I18n context = I18n.getForLanguage(getContextLanguage());
        return context.withRoot(root, s);
    }

    public String getContextLanguage() {
        if (i18n != null) {
            return i18n.getLanguage();
        }

        if (guildData == null && userData == null) {
            return "en_US";
        }

        String lang;
        if (userData == null)
            lang = guildData.getLang();
        else
            lang = userData.getLang() == null || userData.getLang().isEmpty() ? guildData.getLang() : userData.getLang();

        I18n context = I18n.getForLanguage(lang);
        return context == null ? "en_US" : lang;
    }
}
