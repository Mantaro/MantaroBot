package net.kodehawa.mantarobot.commands.info;

import net.kodehawa.mantarobot.core.CommandProcessor;
import net.kodehawa.mantarobot.modules.commands.base.Category;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HelpUtils {
	public static String forType(Category category) {
		return forType(CommandProcessor.REGISTRY.commands().entrySet().stream()
			.filter(entry -> entry.getValue().category() == category)
			.map(Entry::getKey));
	}

	public static String forType(Stream<String> values) {
		return "``" + values.sorted().collect(Collectors.joining("`` ``")) + "``";
	}

	public static String forType(Collection<String> collection) {
		return forType(collection.stream());
	}
}
