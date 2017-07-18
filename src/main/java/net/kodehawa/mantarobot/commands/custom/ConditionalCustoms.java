package net.kodehawa.mantarobot.commands.custom;

import br.com.brjdevs.java.utils.collections.CollectionUtils;
import com.google.gson.JsonPrimitive;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.lib.customfunc.CustomFuncException;
import net.kodehawa.lib.customfunc.CustomFunction;
import net.kodehawa.lib.customfunc.Environiment;
import net.kodehawa.mantarobot.utils.URLEncoding;
import org.apache.commons.collections4.iterators.ArrayIterator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

public class ConditionalCustoms {
    private static final Map<String, CustomFunction> functions = new ConcurrentHashMap<>();

    static {
        Map<String, BiPredicate<Object, Object>> comparators = new ConcurrentHashMap<>();

        comparators.put(
                "equals",
                Object::equals
        );

        comparators.put(
                "ignorecase_equals",
                wrapString(
                        String::equalsIgnoreCase
                )
        );

        comparators.put(
                "greater_than",
                wrapComp(
                        (s1, s2) -> s1.compareTo(s2) < 1
                )
        );

        comparators.put(
                "less_than",
                wrapComp(
                        (s1, s2) -> s1.compareTo(s2) > 1
                )
        );

        comparators.put(
                "ignorecase_greater_than",
                wrapString(
                        (s1, s2) -> s1.compareToIgnoreCase(s2) < 1
                )
        );

        comparators.put(
                "ignorecase_less_than",
                wrapString(
                        (s1, s2) -> s1.compareToIgnoreCase(s2) > 1
                )
        );

        comparators.put(
                "not_equals",
                negate(
                        Object::equals
                )
        );

        comparators.put(
                "ignorecase_not_equals",
                wrapString(
                        negate(
                                String::equalsIgnoreCase
                        )
                )
        );

        comparators.put(
                "not_greater_than",
                wrapComp(
                        negate(
                                (s1, s2) -> s1.compareTo(s2) < 1
                        )
                )
        );

        comparators.put(
                "not_less_than",
                wrapComp(
                        negate(
                                (s1, s2) -> s1.compareTo(s2) > 1
                        )
                )
        );

        comparators.put(
                "ignorecase_not_greater_than",
                wrapString(
                        negate(
                                (s1, s2) -> s1.compareToIgnoreCase(s2) < 1
                        )
                )
        );

        comparators.put(
                "ignorecase_not_less_than",
                wrapString(
                        negate(
                                (s1, s2) -> s1.compareToIgnoreCase(s2) > 1
                        )
                )
        );

        //$if{INPUT1,COMPARE,INPUT2,OUTPUT_TRUE[,OUTPUT_FALSE]}
        functions.put("if", args -> {
            if(args.length < 4) throw new CustomFuncException("`if requires at least 4 parameters`");
            Object input1 = args[0], input2 = args[2];
            String compare = args[1].toString();

            BiPredicate<Object, Object> comparator = comparators.get(compare.toLowerCase());
            if(comparator == null) return "`'" + compare + "' comparator doesn't exists`";
            return comparator.test(input1, input2) ? args[3] : args.length >= 5 ? args[4] : "";
        });

        //@ne{[,arg]+?}
        functions.put("ne", args -> {
            for(Object arg : args) {
                String s = arg.toString();
                if(!s.isEmpty()) return arg;
            }
            return "";
        });

        functions.put("not_empty", functions.get("ne"));

        functions.put("expand", ArrayIterator::new);

        functions.put("trim", args -> new IteratorAdapter<String, Object>(new ArrayIterator<>(args)) {
            @Override
            protected String wrap(Object o) {
                return String.valueOf(o).trim();
            }
        });

        functions.put("url", args -> new IteratorAdapter<String, Object>(new ArrayIterator<>(args)) {
            @Override
            protected String wrap(Object o) {
                return URLEncoding.encode(String.valueOf(o));
            }
        });

        functions.put("jsonescape", args -> new IteratorAdapter<String, Object>(new ArrayIterator<>(args)) {
            @Override
            protected String wrap(Object o) {
                String s = new JsonPrimitive(String.valueOf(o)).toString();
                return s.substring(1, s.length() - 1);
            }
        });

        functions.put("random", CollectionUtils::random);
    }

    public static Environiment genEnv(GuildMessageReceivedEvent event) {
        Map<String, Object> tokens = new LinkedHashMap<>();
        tokens.put("true", true);
        tokens.put("false", false);
        tokens.putAll(Mapifier.mapped("event", event));
        return Environiment.of(functions, tokens);
    }

    private static <T1, T2> BiPredicate<T1, T2> negate(BiPredicate<T1, T2> p) {
        return p.negate();
    }

    @SuppressWarnings("unchecked")
    private static <T> BiPredicate<Object, Object> wrapComp(BiPredicate<Comparable<T>, Comparable<T>> p) {
        return (o, o2) -> {
            if((!(o instanceof Comparable) || !(o2 instanceof Comparable))) return false;

            try {
                return p.test(((Comparable<T>) o), ((Comparable<T>) o2));
            } catch(ClassCastException ignored) {
            }
            return false;
        };
    }

    private static BiPredicate<Object, Object> wrapString(BiPredicate<String, String> p) {
        return (o, o2) -> !(!(o instanceof String) || !(o2 instanceof String)) && p.test(((String) o), ((String) o2));
    }
}
