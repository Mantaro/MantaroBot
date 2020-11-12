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

package net.kodehawa.mantarobot.commands.custom.legacy;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.URLEncoding;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class ConditionalCustoms {
    private static final Pattern GETTER_MODIFIER = Pattern.compile("@[a-z]+\\{.*?}", Pattern.MULTILINE),
            FUNCNAME = Pattern.compile("\\{", Pattern.MULTILINE),
            SPLITTER = Pattern.compile(";", Pattern.LITERAL | Pattern.MULTILINE);
    private static final Pattern userMentionPattern = Pattern.compile("(?:<@!?)?(\\d{1,20})>?");
    private static final Map<String, Function<String[], String>> functions = new HashMap<>();

    static {
        Map<String, BiPredicate<String, String>> comparators = new HashMap<>();
        Map<String, Predicate<String>> predicates = new HashMap<>(); //1 = this is silly, 2 = we need to remake this

        predicates.put("usermention", s -> userMentionPattern.matcher(s).find());

        comparators.put("equals", String::equals);
        comparators.put("ignorecase-equals", String::equalsIgnoreCase);
        comparators.put("greater-than", (s1, s2) -> s1.compareTo(s2) < 0);
        comparators.put("less-than", (s1, s2) -> s1.compareTo(s2) > 0);
        comparators.put("ignorecase-greater-than", (s1, s2) -> s1.compareToIgnoreCase(s2) < 0);
        comparators.put("ignorecase-less-than", (s1, s2) -> s1.compareToIgnoreCase(s2) > 0);
        comparators.put("not-equals", comparators.get("equals").negate());
        comparators.put("ignorecase-not-equals", comparators.get("ignorecase-equals").negate());
        comparators.put("not-greater-than", comparators.get("greater-than").negate());
        comparators.put("not-less-than", comparators.get("less-than").negate());
        comparators.put("ignorecase-not-greater-than", comparators.get("ignorecase-greater-than").negate());
        comparators.put("ignorecase-not-less-than", comparators.get("ignorecase-less-than").negate());
        comparators.put("contains", String::contains);
        comparators.put("ignorecase-contains", (s1, s2) -> s1.toLowerCase().contains(s2.toLowerCase()));

        //@{if;INPUT1;COMPARE;INPUT2;OUTPUT_TRUE[;OUTPUT_FALSE]}
        functions.put("if", args -> {
            if (args.length < 4)
                return "`if requires at least 6 parameters`";
            String input1 = args[0], compare = args[1], input2 = args[2], outputTrue = args[3];

            BiPredicate<String, String> comparator = comparators.get(compare);
            Predicate<String> predicate = predicates.get(compare);

            if (comparator == null) {
                if (predicate == null) {
                    return "`'The " + compare + "' comparator doesn't exist`";
                }
            }

            if (predicate != null) {
                if (predicate.test(input1))
                    return outputTrue;
            }

            if (comparator != null) {
                if (comparator.test(input1, input2))
                    return outputTrue;
            }

            if (args.length >= 5)
                return args[4];

            return "";
        });

        //@{ne[;arg]+?}
        functions.put("ne", args -> {
            for (String arg : args)
                if (!arg.isEmpty())
                    return arg;

            return "";
        });

        //@{nes[;arg]+?}
        functions.put("nes", args -> {
            for (String arg : args)
                if (!arg.trim().isEmpty())
                    return arg;

            return "";
        });

        //@url
        functions.put("url", args -> URLEncoding.encode(String.join(";", args)));

        //@jsonescape
        functions.put("jsonescape", args -> {
            String s = JsonStringEncoder.getInstance().quoteAsString(String.join(";", args)).toString();
            return s.substring(1, s.length() - 1);
        });
    }

    public static String resolve(String string, int depth) {
        if (!string.contains("@") || !string.contains("{") || !string.contains("}"))
            return string;
        if (depth > 4)
            return string;

        return GETTER_MODIFIER.matcher(string).replaceAll(r -> {
            String s = r.group();

            s = s.substring(1, s.length() - 1);

            if (GETTER_MODIFIER.matcher(s).find())
                s = resolve(s, depth + 1);

            String[] parts = FUNCNAME.split(s, 2);

            if (parts.length == 0)
                return "`function name is empty`";

            String name = parts[0].replace("\n", "");

            if (!functions.containsKey(name))
                return "`" + s + " isn't a function`";

            Function<String[], String> f = functions.get(name);

            if (parts.length == 1) {
                return f.apply(StringUtils.EMPTY_ARRAY);
            }

            return f.apply(SPLITTER.split(parts[1], -1));
        });
    }
}
