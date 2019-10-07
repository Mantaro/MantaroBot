/*
 * Copyright (C) 2016-2019 David Alejandro Rubio Escares / Kodehawa
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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.custom.v3;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.MiscCmds;
import net.kodehawa.mantarobot.commands.custom.EmbedJSON;
import net.kodehawa.mantarobot.commands.custom.legacy.DynamicModifiers;
import net.kodehawa.mantarobot.commands.custom.v3.ast.Node;
import net.kodehawa.mantarobot.commands.custom.v3.interpreter.InterpreterContext;
import net.kodehawa.mantarobot.commands.custom.v3.interpreter.InterpreterVisitor;
import net.kodehawa.mantarobot.commands.custom.v3.interpreter.Operation;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

@SuppressWarnings("Duplicates")
public class CCv3 {
    private static final Pattern USER_MENTION_PATTERN = Pattern.compile("(?:<@!?)?(\\d{1,20})>?");
    private static final Map<String, Operation> DEFAULT_OPERATIONS = new HashMap<>();
    private static final Pattern FILTER = Pattern.compile("([a-zA-Z0-9]{24}\\.[a-zA-Z0-9]{6}\\.[a-zA-Z0-9_\\-])\\w+");
    private static final DateTimeFormatter DEFAULT_TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(ISO_LOCAL_TIME)
            .toFormatter();

    static {
        Map<String, BiPredicate<String, String>> comparators = new HashMap<>();
        Map<String, Predicate<String>> predicates = new HashMap<>();

        predicates.put("usermention", s -> USER_MENTION_PATTERN.matcher(s).find());
        comparators.put("equals", String::equals);
        comparators.put("ignorecase-equals", String::equalsIgnoreCase);
        comparators.put("greater-than", (s1, s2) -> s1.compareTo(s2) > 0);
        comparators.put("less-than", (s1, s2) -> s1.compareTo(s2) < 0);
        comparators.put("ignorecase-greater-than", (s1, s2) -> s1.compareToIgnoreCase(s2) > 0);
        comparators.put("ignorecase-less-than", (s1, s2) -> s1.compareToIgnoreCase(s2) < 0);
        comparators.put("not-equals", comparators.get("equals").negate());
        comparators.put("ignorecase-not-equals", comparators.get("ignorecase-equals").negate());
        comparators.put("not-greater-than", comparators.get("greater-than").negate());
        comparators.put("not-less-than", comparators.get("less-than").negate());
        comparators.put("ignorecase-not-greater-than", comparators.get("ignorecase-greater-than").negate());
        comparators.put("ignorecase-not-less-than", comparators.get("ignorecase-less-than").negate());
        comparators.put("contains", String::contains);
        comparators.put("ignorecase-contains", (s1, s2) -> s1.toLowerCase().contains(s2.toLowerCase()));

        DEFAULT_OPERATIONS.put("if", (__, args) -> {
            if(args.size() < 1) {
                return "{If: missing required parameter <lhs>}";
            }
            if(args.size() < 2) {
                return "{If: missing required parameter <condition>}";
            }
            if(args.size() < 3) {
                return "{If: missing required parameter <rhs>}";
            }
            if(args.size() < 4) {
                return "{If: missing required parameter <iftrue>}";
            }
            String input1 = args.get(0).evaluate();
            String compare = args.get(1).evaluate();
            String input2 = args.get(2).evaluate();

            BiPredicate<String, String> comparator = comparators.get(compare);
            Predicate<String> predicate = predicates.get(compare);

            if(comparator == null) {
                if(predicate == null) {
                    return "{If: comparator " + compare + " does not exist}";
                }
            }

            if(predicate != null) {
                if(predicate.test(input1)) {
                    return args.get(3).evaluate();
                }
            }

            if(comparator != null) {
                if(comparator.test(input1, input2)) {
                    return args.get(3).evaluate();
                }
            }

            if(args.size() >= 5) {
                return args.get(4).evaluate();
            }

            return "";
        });

        DEFAULT_OPERATIONS.put("and", (__, args) -> {
            if(args.size() < 1) {
                return "{And: missing required parameter <first>}";
            }
            if(args.size() < 2) {
                return "{And: missing required parameter <second>}";
            }
            //evaluate argument 1 and 2
            String i1 = args.get(0).evaluate();
            String i2 = args.get(1).evaluate();

            //if argument 1 is the same as argument 2. They both return -string-, so they gotta be the same, basically.
            if(i1.equals(i2)) {
                return args.size() > 2 ? args.get(2).evaluate() : "";
            } else {
                return args.size() > 3 ? args.get(3).evaluate() : "";
            }
        });

        //Same as above, but ignores case.
        DEFAULT_OPERATIONS.put("andic", (__, args) -> {
            //evaluate argument 1 and 2
            String i1 = args.get(0).evaluate();
            String i2 = args.get(1).evaluate();

            //if argument 1 is the same as argument 2. They both return -string-, so they gotta be the same, basically.
            if(i1.equalsIgnoreCase(i2)) {
                return args.size() > 2 ? args.get(2).evaluate() : "";
            } else {
                return args.size() > 3 ? args.get(3).evaluate() : "";
            }
        });

        DEFAULT_OPERATIONS.put("or", (__, args) -> {
            String c1 = args.get(1).evaluate();
            String c2 = args.get(4).evaluate();

            BiPredicate<String, String> comparator1 = comparators.get(c1);
            BiPredicate<String, String> comparator2 = comparators.get(c2);

            if(comparator1 != null && comparator2 != null) {
                String input1 = args.get(0).evaluate();
                String input2 = args.get(2).evaluate();
                String input3 = args.get(3).evaluate();
                String input4 = args.get(5).evaluate();

                if(comparator1.test(input1, input2) || comparator2.test(input3, input4)) {
                    return args.get(6).evaluate();
                }
                else
                    return args.get(7).evaluate();
            } else {
                return "You need two comparators to check if an OR operation is correct.";
            }
        });

        //@{not-empty[;arg]+?}
        DEFAULT_OPERATIONS.put("not-empty", (__, args) -> {
            for(Operation.Argument arg : args) {
                String value = arg.evaluate();
                if(!value.isEmpty())
                    return value;
            }

            return "";
        });

        //@{not-empty-strict[;arg]+?}
        DEFAULT_OPERATIONS.put("not-empty-strict", (__, args) -> {
            for(Operation.Argument arg : args) {
                String value = arg.evaluate();
                if(!value.trim().isEmpty())
                    return value;
            }

            return "";
        });

        DEFAULT_OPERATIONS.put("embed", (interpreter, args) -> {
            try {
                EmbedJSON embed = GsonDataManager.gson(false)
                        .fromJson('{' +
                                args.stream()
                                        .map(Operation.Argument::evaluate)
                                        .collect(Collectors.joining(";"))
                                + '}', EmbedJSON.class);
                interpreter.set("embed", embed);
            } catch(Exception e) {
                return e.toString();
            }
            return "";
        });

        DEFAULT_OPERATIONS.put("set", (context, args) -> {
            if(args.size() < 1) {
                return "{Set: missing required parameter <name>}";
            }
            if(args.size() < 2) {
                return "{Set: missing required parameter <value>}";
            }
            String key = args.get(0).evaluate();
            String value = args.stream().skip(1)
                    .map(Operation.Argument::evaluate).collect(Collectors.joining(";"));
            context.vars().put(key, value);
            return "";
        });

        DEFAULT_OPERATIONS.put("iam", (context, args) -> {
            String iam = args.get(0).evaluate();
            String ctn = args.stream().skip(1).map(Operation.Argument::evaluate).collect(Collectors.joining(" "));
            GuildMessageReceivedEvent event = context.event();

            if(ctn.isEmpty())
                MiscCmds.iamFunction(iam, event, null);
            else
                MiscCmds.iamFunction(iam, event, null, ctn);

            return "";
        });

        DEFAULT_OPERATIONS.put("iamnot", (context, args) -> {
            String iam = args.get(0).evaluate();
            String ctn = args.stream().skip(1).map(Operation.Argument::evaluate).collect(Collectors.joining(" "));

            GuildMessageReceivedEvent event = context.event();

            if(ctn.isEmpty())
                MiscCmds.iamnotFunction(iam, event, null);
            else
                MiscCmds.iamnotFunction(iam, event, null, ctn);

            return "";
        });

        DEFAULT_OPERATIONS.put("timestamp", (context, args) -> {
            DateTimeFormatter formatter = DEFAULT_TIMESTAMP_FORMATTER;
            ZoneId zone = ZoneId.of("UTC");
            if(args.size() > 0) {
                String pattern = args.get(0).evaluate();
                try {
                    formatter = DateTimeFormatter.ofPattern(pattern);
                } catch(IllegalArgumentException e) {
                    return "{Timestamp: provided format " + pattern + " is invalid: " + e.getMessage() + "}";
                }
            }
            if(args.size() > 1) {
                String z = args.get(1).evaluate();
                try {
                    zone = ZoneId.of(z);
                } catch(DateTimeException e) {
                    return "{Timestamp: provided zone " + z + " is invalid: " + e.getMessage() + "}";
                }
            }
            if(args.size() > 2) {
                args.subList(2, args.size() - 1).forEach(Operation.Argument::evaluate);
            }
            return formatter.format(OffsetDateTime.now(zone));
        });
    }

    public static void process(String prefix, GuildMessageReceivedEvent event, Node ast, boolean preview) {
        InterpreterContext context = new InterpreterContext(new DynamicModifiers()
                .mapEvent(prefix, "event", event), DEFAULT_OPERATIONS, event);

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
