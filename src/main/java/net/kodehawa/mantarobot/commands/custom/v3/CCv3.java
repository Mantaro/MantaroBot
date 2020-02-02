/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
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
        predicates.put("is-empty", String::isEmpty);
        predicates.put("is-not-empty", s -> !s.isEmpty());
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
        comparators.put("starts-with", String::startsWith);
        comparators.put("ignorecase-starts-with", (s1, s2) -> s1.toLowerCase().startsWith(s2.toLowerCase()));
        comparators.put("ends-with", String::endsWith);
        comparators.put("ignorecase-ends-with", (s1, s2) -> s1.toLowerCase().endsWith(s2.toLowerCase()));
        
        DEFAULT_OPERATIONS.put("if", (__, args) -> {
            if(args.size() < 1) {
                return "{If: missing required parameter <lhs>}";
            }
            if(args.size() < 2) {
                return "{If: missing required parameter <condition>}";
            }
            if(args.size() < 3) {
                return "{If: missing required parameter <rhs/iftrue>}";
            }
            String input1 = args.get(0).evaluate();
            String compare = args.get(1).evaluate();
            
            int resultIdx;
            BiPredicate<String, String> comparator = comparators.get(compare);
            Predicate<String> predicate = predicates.get(compare);
            if(comparator != null) {
                String input2 = args.get(2).evaluate();
                resultIdx = comparator.test(input1, input2) ? 3 : 4;
            } else if(predicate != null) {
                if(predicate.test(input1)) {
                    return args.get(2).evaluate();
                }
                resultIdx = 3;
            } else {
                if(compare.equals("true") || compare.equals("false")) {
                    resultIdx = compare.equals("true") ? 0 : 2;
                } else {
                    return "{If: operand " + compare + " is not a comparator, predicate nor boolean}";
                }
            }
            
            if(args.size() > resultIdx) {
                return args.get(resultIdx).evaluate();
            } else {
                return "";
            }
        });
        
        DEFAULT_OPERATIONS.put("compare", (__, args) -> {
            if(args.size() < 1) {
                return "{Compare: missing required parameter <lhs>}";
            }
            if(args.size() < 2) {
                return "{Compare: missing required parameter <comparator>}";
            }
            if(args.size() < 3) {
                return "{Compare: missing required parameter <rhs>}";
            }
            String lhs = args.get(0).evaluate();
            String cmp = args.get(1).evaluate();
            String rhs = args.get(2).evaluate();
            BiPredicate<String, String> comparator = comparators.get(cmp);
            if(comparator == null) {
                return "{Compare: unknown comparator " + cmp + "}";
            }
            return Boolean.toString(comparator.test(lhs, rhs));
        });
        
        DEFAULT_OPERATIONS.put("test", (__, args) -> {
            if(args.size() < 1) {
                return "{Test: missing required parameter <predicate>}";
            }
            if(args.size() < 2) {
                return "{Test: missing required parameter <operand>}";
            }
            String predicate = args.get(0).evaluate();
            String operand = args.get(1).evaluate();
            Predicate<String> p = predicates.get(predicate);
            if(p == null) {
                return "{Test: unknown predicate " + predicate + "}";
            }
            return Boolean.toString(p.test(operand));
        });
        
        DEFAULT_OPERATIONS.put("and", (__, args) -> {
            boolean res = true;
            int i = 1;
            for(Operation.Argument arg : args) {
                String v = arg.evaluate();
                if(v.equals("true") || v.equals("false")) {
                    res &= Boolean.parseBoolean(v);
                } else {
                    return "{And: value " + v + " at index " + i + " is not a boolean}";
                }
                i++;
            }
            return Boolean.toString(res);
        });
        
        DEFAULT_OPERATIONS.put("or", (__, args) -> {
            boolean res = false;
            int i = 1;
            for(Operation.Argument arg : args) {
                String v = arg.evaluate();
                if(v.equals("true") || v.equals("false")) {
                    res |= Boolean.parseBoolean(v);
                } else {
                    return "{Or: value " + v + " at index " + i + " is not a boolean}";
                }
                i++;
            }
            return Boolean.toString(res);
        });
        
        DEFAULT_OPERATIONS.put("not", (__, args) -> {
            if(args.size() < 1) {
                return "{Not: missing required parameter <value>}";
            }
            String s = args.get(0).evaluate();
            switch(s) {
                case "true":
                    return "false";
                case "false":
                    return "true";
                default:
                    return "{Not: value " + s + " is not a boolean}";
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
            if(args.size() < 1) {
                return "{Iam: missing required argument <role>}";
            }
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
            if(args.size() < 1) {
                return "{Iamnot: missing required argument <role>}";
            }
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
            return formatter.format(OffsetDateTime.now(zone));
        });
        
        DEFAULT_OPERATIONS.put("lower", (__, args) -> args.stream()
                                                              .map(Operation.Argument::evaluate)
                                                              .map(String::toLowerCase)
                                                              .collect(Collectors.joining(";")));
        
        DEFAULT_OPERATIONS.put("upper", (__, args) -> args.stream()
                                                              .map(Operation.Argument::evaluate)
                                                              .map(String::toUpperCase)
                                                              .collect(Collectors.joining(";")));
        
        DEFAULT_OPERATIONS.put("replace", (__, args) -> {
            if(args.size() < 1) {
                return "{Replace: missing required parameter <search>}";
            }
            if(args.size() < 2) {
                return "{Replace: missing required parameter <replacement>}";
            }
            if(args.size() < 3) {
                return "{Replace: missing required parameter <text>}";
            }
            String search = args.get(0).evaluate();
            String replace = args.get(1).evaluate();
            return args.stream().skip(2).map(Operation.Argument::evaluate)
                           .map(s -> s.replace(search, replace))
                           .collect(Collectors.joining(";"));
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
