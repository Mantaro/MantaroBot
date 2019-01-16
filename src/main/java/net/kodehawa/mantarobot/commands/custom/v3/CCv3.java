package net.kodehawa.mantarobot.commands.custom.v3;

import com.google.gson.JsonPrimitive;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.custom.EmbedJSON;
import net.kodehawa.mantarobot.commands.custom.legacy.DynamicModifiers;
import net.kodehawa.mantarobot.commands.custom.v3.ast.Node;
import net.kodehawa.mantarobot.commands.custom.v3.interpreter.InterpreterContext;
import net.kodehawa.mantarobot.commands.custom.v3.interpreter.InterpreterVisitor;
import net.kodehawa.mantarobot.commands.custom.v3.interpreter.Operation;
import net.kodehawa.mantarobot.utils.URLEncoding;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")
public class CCv3 {
    private static final Pattern USER_MENTION_PATTERN = Pattern.compile("(?:<@!?)?(\\d{1,20})>?");
    private static final Map<String, Operation> DEFAULT_OPERATIONS = new HashMap<>();
    private static final Pattern FILTER = Pattern.compile("([a-zA-Z0-9]{24}\\.[a-zA-Z0-9]{6}\\.[a-zA-Z0-9_\\-])\\w+");

    static {
        Map<String, BiPredicate<String, String>> comparators = new HashMap<>();
        Map<String, Predicate<String>> predicates = new HashMap<>();

        predicates.put("usermention", s -> USER_MENTION_PATTERN.matcher(s).find());

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

        DEFAULT_OPERATIONS.put("if", (__, args) -> {
            if(args.size() < 4)
                return "`if requires at least 4 parameters`";
            String input1 = args.get(0), compare = args.get(1),
                    input2 = args.get(2), outputTrue = args.get(3);

            BiPredicate<String, String> comparator = comparators.get(compare);
            Predicate<String> predicate = predicates.get(compare);

            if(comparator == null) {
                if(predicate == null) {
                    return "`'The " + compare + "' comparator doesn't exist`";
                }
            }

            if(predicate != null) {
                if(predicate.test(input1))
                    return outputTrue;
            }

            if(comparator != null) {
                if(comparator.test(input1, input2))
                    return outputTrue;
            }

            if(args.size() >= 5)
                return args.get(4);

            return "";
        });

        //@{not-empty[;arg]+?}
        DEFAULT_OPERATIONS.put("not-empty", (__, args) -> {
            for(String arg : args)
                if(!arg.isEmpty())
                    return arg;

            return "";
        });

        //@{not-empty-strict[;arg]+?}
        DEFAULT_OPERATIONS.put("not-empty-strict", (__, args) -> {
            for(String arg : args)
                if(!arg.trim().isEmpty())
                    return arg;

            return "";
        });

        //@url
        DEFAULT_OPERATIONS.put("url", (__, args) -> URLEncoding.encode(String.join(";", args)));

        //@jsonescape
        DEFAULT_OPERATIONS.put("jsonescape", (__, args) -> {
            String s = new JsonPrimitive(String.join(";", args)).toString();
            return s.substring(1, s.length() - 1);
        });

        DEFAULT_OPERATIONS.put("embed", (interpreter, args) -> {
            try {
                EmbedJSON embed = GsonDataManager.gson(false)
                        .fromJson('{' + String.join(";", args) + '}', EmbedJSON.class);
                interpreter.set("embed", embed);
            } catch(Exception e) {
                return e.toString();
            }
            return "";
        });

        DEFAULT_OPERATIONS.put("set", (context, args) -> {
            if(args.size() < 2) {
                return "Usage: set <name> <value>";
            }
            String value = args.stream().skip(1).collect(Collectors.joining(";"));
            context.vars().put(args.get(0), value);
            return "";
        });
    }

    public static void process(GuildMessageReceivedEvent event, Node ast, boolean preview) {
        InterpreterContext context = new InterpreterContext(new DynamicModifiers()
                .mapEvent("event", event), DEFAULT_OPERATIONS);

        String result = ast.accept(new InterpreterVisitor(), context);
        EmbedJSON embed = context.get("embed");

        if(embed == null && result.isEmpty()) {
            event.getChannel().sendMessageFormat("Command response is empty.").queue();
            return;
        }

        MessageBuilder builder = new MessageBuilder().setContent(FILTER.matcher(result).replaceAll("-filtered regex-"));

        if(preview) {
            builder.append("\n\n")
                    .append(EmoteReference.WARNING)
                    .append("**This is a preview of how a CC with this content would look like, ALL MENTIONS ARE DISABLED ON THIS MODE.**\n")
                    .append("`Command Preview Requested By: ")
                    .append(event.getAuthor().getName())
                    .append("#")
                    .append(event.getAuthor().getDiscriminator())
                    .append("`")
                    .stripMentions(event.getJDA());
        }

        builder.setEmbed(embed == null ? null : embed.gen(event.getMember()))
                .stripMentions(event.getJDA(), Message.MentionType.HERE, Message.MentionType.EVERYONE)
                .sendTo(event.getChannel()).queue();
    }
}
