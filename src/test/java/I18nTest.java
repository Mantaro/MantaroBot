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

import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.LanguageKeyNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class I18nTest {
    @Test
    public void testI18n() throws LanguageKeyNotFoundException {
        //Should default to en_US
        I18nContext context = new I18nContext(new GuildData(), null);
        Assertions.assertEquals("en_US", context.getContextLanguage());

        String localized = context.get("test.inherited");
        Assertions.assertNotNull(localized);
        Assertions.assertEquals("owo", localized);
    }
}
