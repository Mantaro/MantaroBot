package net.kodehawa.mantarobot.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.guild.GenericGuildEvent;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class I18n {
    //TODO maybe automate this
    private static final String[] LANGUAGES = {"en_US"};

    private static final ThreadLocal<String> ROOT = new ThreadLocal<>();

    private static final Map<String, I18n> LANGUAGE_MAP;

    private final Map<String, ?> map;

    static {
        Map<String, I18n> m = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        for(String s : LANGUAGES) {
            InputStream is = I18n.class.getResourceAsStream("/assets/languages/" + s + ".json");
            try {
                @SuppressWarnings("unchecked")
                Map<String, ?> map = (Map<String, ?>)mapper.readValue(is, Map.class);
                m.put(s, new I18n(map));
            } catch(Exception e) {
                throw new Error("Unable to initialize I18n", e);
            }
        }
        LANGUAGE_MAP = Collections.unmodifiableMap(m);
    }

    private I18n(Map<String, ?> map) {
        this.map = map;
    }

    private String get(Map<String, ?> map, String[] parts, int index) {
        if(parts.length - 1 == index) {
            Object o = map.get(parts[parts.length - 1]);
            if(o instanceof String) return (String)o;
            throw new IllegalArgumentException("Key " + Arrays.stream(parts).collect(Collectors.joining(".")) + " is not a string");
        }
        Object maybeMap = map.get(parts[index]);
        if(maybeMap instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, ?> map2 = (Map<String, ?>) maybeMap;
            return get(map2, parts, index + 1);
        }
        throw new IllegalArgumentException("Key " + Arrays.stream(parts).collect(Collectors.joining(".")) + " is not a map");
    }

    public String get(String query) {
        String root = ROOT.get();
        String actualQuery;
        if(root == null) {
            actualQuery = query;
        } else {
            actualQuery = root + "." + query;
        }
        return get(map, actualQuery.split("\\."), 0);
    }

    public String withRoot(String root, String query) {
        String s = ROOT.get();
        ROOT.set(root);
        try {
            return get(query);
        } finally {
            ROOT.set(s);
        }
    }

    public static I18n of(String guildId) {
        String lang = MantaroData.db().getGuild(guildId).getData().getLang();
        return getForLanguage(lang);
    }

    public static I18n of(Guild guild) {
        return of(guild.getId());
    }

    public static I18n of(GenericGuildEvent event) {
        return of(event.getGuild().getId());
    }

    public static I18n getForLanguage(String language) {
        I18n i = LANGUAGE_MAP.get(language);
        if(i == null) return LANGUAGE_MAP.get("en_US");
        return i;
    }

    public static boolean isValidLanguage(String lang) {
        return LANGUAGE_MAP.containsKey(lang);
    }

    public static void root(String newRoot) {
        ROOT.set(newRoot);
    }
}
