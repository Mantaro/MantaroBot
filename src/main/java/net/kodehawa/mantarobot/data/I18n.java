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

package net.kodehawa.mantarobot.data;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.kodehawa.mantarobot.core.listeners.MantaroListener;
import net.kodehawa.mantarobot.utils.LanguageKeyNotFoundException;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.data.JsonDataManager;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class I18n {
    private static final Logger log = LoggerFactory.getLogger(I18n.class);
    public static final List<String> LANGUAGES = new ArrayList<>();
    private static final ThreadLocal<String> ROOT = new ThreadLocal<>();
    private static final Map<String, I18n> LANGUAGE_MAP;

    static {
        Map<String, I18n> m = new HashMap<>();
        try (var is = I18n.class.getResourceAsStream("/assets/languages/list.txt")) {
            for (var lang : IOUtils.toString(is, StandardCharsets.UTF_8).trim().split("\n")) {
                var language = lang.trim();
                LANGUAGES.add(language);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }

        for (String lang : LANGUAGES) {
            var is = I18n.class.getResourceAsStream("/assets/languages/" + lang);
            try {
                @SuppressWarnings("unchecked")
                Map<String, ?> map = (Map<String, ?>) JsonDataManager.fromJson(is, Map.class);

                var name = lang.replace(".json", "");
                m.put(name, new I18n(map, lang));

                log.debug("Initialized I18n for: {}", name);
            } catch (Exception e) {
                throw new Error("Unable to initialize I18n", e);
            }
        }

        LANGUAGE_MAP = Collections.unmodifiableMap(m);
    }

    private final Map<String, ?> map;
    private final String language;

    private I18n(Map<String, ?> map, String language) {
        this.map = map;
        this.language = language;
    }

    public static I18n of(String guildId) {
        var lang = MantaroData.db().getGuild(guildId).getLang();
        return getForLanguage(lang);
    }

    public static I18n ofUser(String userId) {
        var lang = MantaroData.db().getUser(userId).getLang();
        return getForLanguage(lang);
    }

    public static I18n of(Guild guild) {
        return of(guild.getId());
    }

    public static I18n ofUser(User user) {
        return of(user.getId());
    }

    public static I18n of(GenericGuildEvent event) {
        return of(event.getGuild().getId());
    }

    public static I18n getForLanguage(String language) {
        var lang = LANGUAGE_MAP.get(language);
        if (lang == null) {
            return LANGUAGE_MAP.get("en_US");
        }

        return lang;
    }

    public static boolean isValidLanguage(String lang) {
        return LANGUAGE_MAP.containsKey(lang);
    }

    public String getLanguage() {
        return language;
    }

    public static void root(String newRoot) {
        ROOT.set(newRoot);
    }

    @SuppressWarnings("unchecked")
    private String get(Map<String, ?> map, String[] parts, boolean recursion) {
        var index = 0;
        while (index != parts.length - 1) {
            Object maybeMap = map.get(parts[index]);
            if (maybeMap instanceof Map) {
                map = (Map<String, ?>) maybeMap;
                index++;
            } else {
                if (language.equals("en_US") || recursion) {
                    throw new LanguageKeyNotFoundException("Missing i18n key " + String.join(".", parts));
                }

                return get(LANGUAGE_MAP.get("en_US").map, parts, true);
            }
        }

        Object maybeString = map.get(parts[index]);
        if (maybeString instanceof String) {
            return (String) maybeString;
        }

        if (maybeString instanceof Collection) {
            Collection<String> c = ((Collection<String>) maybeString);
            return c.stream()
                    .skip(ThreadLocalRandom.current().nextInt(c.size()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
        }

        if (language.equals("en_US") || recursion) {
            throw new LanguageKeyNotFoundException("Missing i18n key " + String.join(".", parts));
        }

        return get(LANGUAGE_MAP.get("en_US").map, parts, true);
    }

    public String get(String query) {
        var root = ROOT.get();
        String actualQuery;

        if (root == null) {
            actualQuery = query;
        } else {
            actualQuery = root + "." + query;
        }

        return Utils.fixInlineCodeblockDirection(get(map, actualQuery.split("\\."), false));
    }

    public String withRoot(String root, String query) {
        var s = ROOT.get();
        ROOT.set(root);

        try {
            return get(query);
        } finally {
            ROOT.set(s);
        }
    }
}
