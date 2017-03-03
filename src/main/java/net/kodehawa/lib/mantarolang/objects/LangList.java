package net.kodehawa.lib.mantarolang.objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LangList implements LangCallable, LangContainer {
	private final List<LangObject> list = new ArrayList<>();

	public LangList(List<LangObject> objs) {
		list.addAll(objs);
	}

	public LangList() {

	}

	@Override
	public List<LangObject> call(List<LangObject> args) {
		return Collections.singletonList(get(list, cast(get(args, 0, new LangInteger(0)), LangInteger.class).get().intValue()));
	}

	@Override
	public LangObject get(String name) {
		switch (name) {
			case "remove":
				return LangCallable.of(args -> Collections.singletonList(list.remove(cast(get(args, 0, new LangInteger(0)), LangInteger.class).get().intValue())));
			case "add":
				return LangCallable.of(args -> {
					list.add(args.get(0));
					return Collections.singletonList(this);
				});
			case "unpack":
				return LangCallable.of(args -> list);
			case "collect":
				return LangCallable.of(args -> {
					LangCallable callable = cast(get(args, 0, (LangCallable) self -> self), LangCallable.class);
					return Collections.singletonList(new LangList(
						list.stream()
							.flatMap(o -> callable.call(o).stream())
							.collect(Collectors.toList()))
					);
				});
			case "concat":
				return LangCallable.of(args -> {
					String join = cast(get(args,0, new LangString("")),LangString.class).asString();
					LangCallable callable = cast(get(args, 1, (LangCallable) self -> self), LangCallable.class);
					return Collections.singletonList(new LangString(
						list.stream()
							.flatMap(o -> callable.call(o).stream())
							.map(LangObject::asString)
							.collect(Collectors.joining(join))
					));
				});
		}

		return invalidProperty(name);
	}

	@Override
	public String toString() {
		return "LList" + list;
	}
}
