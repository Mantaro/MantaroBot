package net.kodehawa.mantarobot.commands.custom;

import br.com.brjdevs.java.utils.texts.MatcherUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.regex.Pattern;

public class ConditionalCustoms {
	private static final Pattern GETTER_MODIFIER = Pattern.compile("@[a-z]+\\{.*}"),
		FUNCNAME = Pattern.compile("\\{"),
		SPLITTER = Pattern.compile(";", Pattern.LITERAL);
	private static final Map<String, Function<String[], String>> functions = new HashMap<>();

	static {
		Map<String, BiPredicate<String, String>> comparators = new HashMap<>();

		comparators.put("eq", String::equals);
		comparators.put("ieq", String::equalsIgnoreCase);
		comparators.put("gt", (s1, s2) -> s1.compareTo(s2) > 1);
		comparators.put("lt", (s1, s2) -> s1.compareTo(s2) < 1);
		comparators.put("igt", (s1, s2) -> s1.compareToIgnoreCase(s2) > 1);
		comparators.put("ilt", (s1, s2) -> s1.compareToIgnoreCase(s2) < 1);
		comparators.put("neq", comparators.get("eq").negate());
		comparators.put("nieq", comparators.get("ieq").negate());
		comparators.put("ngt", comparators.get("gt").negate());
		comparators.put("nlt", comparators.get("lt").negate());
		comparators.put("nigt", comparators.get("igt").negate());
		comparators.put("nilt", comparators.get("ilt").negate());

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
