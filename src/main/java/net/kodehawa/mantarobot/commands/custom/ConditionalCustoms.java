package net.kodehawa.mantarobot.commands.custom;

import br.com.brjdevs.java.utils.texts.MatcherUtils;
import com.google.gson.JsonPrimitive;
import net.kodehawa.mantarobot.utils.URLEncoding;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.regex.Pattern;

public class ConditionalCustoms {
	private static final Pattern GETTER_MODIFIER = Pattern.compile("@[a-z]+\\{.*?}"),
		FUNCNAME = Pattern.compile("\\{"),
		SPLITTER = Pattern.compile(";", Pattern.LITERAL);
	private static final Map<String, Function<String[], String>> functions = new HashMap<>();

	static {
		Map<String, BiPredicate<String, String>> comparators = new HashMap<>();

		comparators.put("equals", String::equals);
		comparators.put("ignorecase-equals", String::equalsIgnoreCase);
		comparators.put("greater-than", (s1, s2) -> s1.compareTo(s2) < 1);
		comparators.put("less-than", (s1, s2) -> s1.compareTo(s2) > 1);
		comparators.put("ignorecase-greater-than", (s1, s2) -> s1.compareToIgnoreCase(s2) < 1);
		comparators.put("ignorecase-less-than", (s1, s2) -> s1.compareToIgnoreCase(s2) > 1);
		comparators.put("not-equals", comparators.get("equals").negate());
		comparators.put("ignorecase-not-equals", comparators.get("ignorecase-equals").negate());
		comparators.put("not-greater-than", comparators.get("greater-than").negate());
		comparators.put("not-less-than", comparators.get("less-than").negate());
		comparators.put("ignorecase-not-greater-than", comparators.get("ignorecase-greater-than").negate());
		comparators.put("ignorecase-not-less-than", comparators.get("ignorecase-less-than").negate());

		//@{if;INPUT1;COMPARE;INPUT2;OUTPUT_TRUE[;OUTPUT_FALSE]}
		functions.put("if", args -> {
			if (args.length < 4) return "`if requires at least 6 parameters`";
			String input1 = args[0], compare = args[1], input2 = args[2], outputTrue = args[3];
			BiPredicate<String, String> comparator = comparators.get(compare);
			if (comparator == null) return "`'" + compare + "' comparator doesn't exists`";
			if (comparator.test(input1, input2)) return outputTrue;
			if (args.length >= 5) return args[4];
			return "";
		});

		//@{ne[;arg]+?}
		functions.put("ne", args -> {
			for (String arg : args) if (!arg.isEmpty()) return arg;
			return "";
		});

		//@{nes[;arg]+?}
		functions.put("nes", args -> {
			for (String arg : args) if (!arg.trim().isEmpty()) return arg;
			return "";
		});

		//@url
		functions.put("url", args -> URLEncoding.encode(String.join(";", args)));

		//@jsonescape
		functions.put("jsonescape", args -> {
			String s = new JsonPrimitive(String.join(";", args)).toString();
			return s.substring(1, s.length() - 1);
		});
	}

	public static String resolve(String string, int depth) {
		if (!string.contains("@") || !string.contains("{") || !string.contains("}")) return string;
		if (depth > 4) return string;

		return MatcherUtils.replaceAll(GETTER_MODIFIER.matcher(string), s -> {
			s = s.substring(1, s.length() - 1);
			if (GETTER_MODIFIER.matcher(s).find()) s = resolve(s, depth + 1);
			String[] parts = FUNCNAME.split(s, 2);
			if (parts.length == 0) return "`function name is empty`";

			String name = parts[0];

			if (!functions.containsKey(name)) return "`" + s + " isn't a function`";
			Function<String[], String> f = functions.get(name);

			if (parts.length == 1) {
				return f.apply(new String[0]);
			}

			return f.apply(SPLITTER.split(parts[1], -1));
		});
	}
}
