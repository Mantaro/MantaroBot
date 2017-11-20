/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
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
 */

package net.kodehawa.mantarobot.utils;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.ReactionOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

public class DiscordUtils {
    public static <T> Pair<String, Integer> embedList(List<T> list, Function<T, String> toString) {
        StringBuilder b = new StringBuilder();
        for(int i = 0; i < list.size(); i++) {
            String s = toString.apply(list.get(i));
            if(b.length() + s.length() + 5 > MessageEmbed.TEXT_MAX_LENGTH) return Pair.of(b.toString(), i);
            b.append("**").append(i + 1).append(".** ");
            b.append(s);
            b.append("\n");
        }

        return Pair.of(b.toString(), list.size());
    }


    public static Future<Void> selectInt(GuildMessageReceivedEvent event, int max, IntConsumer valueConsumer) {
        return InteractiveOperations.createOverriding(event.getChannel(), 30, (e) -> {
            if(!e.getAuthor().equals(event.getAuthor())) return Operation.IGNORED;

            try {
                int choose = Integer.parseInt(e.getMessage().getContent());
                if(choose < 1 || choose > max) return Operation.IGNORED;
                valueConsumer.accept(choose);
                return Operation.COMPLETED;
            } catch(Exception ignored) { }

            return Operation.IGNORED;
        });
    }

    public static <T> Future<Void> selectList(GuildMessageReceivedEvent event, List<T> list, Function<T, String> toString, Function<String, MessageEmbed> toEmbed, Consumer<T> valueConsumer) {
        Pair<String, Integer> r = embedList(list, toString);
        event.getChannel().sendMessage(toEmbed.apply(r.getLeft())).queue();
        return selectInt(event, r.getRight() + 1, i -> valueConsumer.accept(list.get(i - 1)));
    }

    public static <T> Future<Void> selectList(GuildMessageReceivedEvent event, T[] list, Function<T, String> toString, Function<String, MessageEmbed> toEmbed, Consumer<T> valueConsumer) {
        Pair<String, Integer> r = embedList(Arrays.asList(list), toString);
        event.getChannel().sendMessage(toEmbed.apply(r.getLeft())).queue();
        return selectInt(event, r.getRight() + 1, i -> valueConsumer.accept(list[i - 1]));
    }

    public static Future<Void> list(GuildMessageReceivedEvent event, int timeoutSeconds, boolean canEveryoneUse, IntIntObjectFunction<EmbedBuilder> supplier, String... parts) {
        if(parts.length == 0) return null;
        List<MessageEmbed> embeds = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        int total;
        {
            int t = 0;
            int c = 0;
            for(String s : parts) {
                if(s.length() + c + 1 > MessageEmbed.TEXT_MAX_LENGTH) {
                    t++;
                    c = 0;
                }
                c += s.length() + 1;
            }
            if(c > 0) t++;
            total = t;
        }

        for(String s : parts) {
            int l = s.length() + 1;
            if(l > MessageEmbed.TEXT_MAX_LENGTH)
                throw new IllegalArgumentException("Length for one of the pages is greater than the maximum");
            if(sb.length() + l > MessageEmbed.TEXT_MAX_LENGTH) {
                EmbedBuilder eb = supplier.apply(embeds.size() + 1, total);
                eb.setDescription(sb.toString());
                embeds.add(eb.build());
                sb = new StringBuilder();
            }
            sb.append(s).append('\n');
        }
        if(sb.length() > 0) {
            EmbedBuilder eb = supplier.apply(embeds.size() + 1, total);
            eb.setDescription(sb.toString());
            embeds.add(eb.build());
        }
        AtomicInteger index = new AtomicInteger();
        Message m = event.getChannel().sendMessage(embeds.get(0)).complete();

        return ReactionOperations.create(m, timeoutSeconds, (e) -> {
            if(!canEveryoneUse && e.getUser().getIdLong() != event.getAuthor().getIdLong()) return Operation.IGNORED;
            switch(e.getReactionEmote().getName()) {
                case "\u2b05": //left arrow
                    if(index.get() == 0) break;
                    if(e.getChannel().getMessageById(m.getIdLong()) == null) break;
                    m.editMessage(embeds.get(index.decrementAndGet())).queue();
                    break;
                case "\u27a1": //right arrow
                    if(index.get() + 1 >= embeds.size()) break;
                    if(e.getChannel().getMessageById(m.getIdLong()) == null) break;
                    m.editMessage(embeds.get(index.incrementAndGet())).queue();
                    break;
            }
            if(event.getGuild().getSelfMember().hasPermission(e.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                e.getReaction().removeReaction(e.getUser()).queue();
            }

            return Operation.IGNORED;
        }, "\u2b05", "\u27a1");
    }

    public static Future<Void> list(GuildMessageReceivedEvent event, int timeoutSeconds, boolean canEveryoneUse, List<String> parts) {
        if(parts.size() == 0) return null;

        if(parts.size() == 1){
            event.getChannel().sendMessage(parts.get(0)).queue();
            return null;
        }

        AtomicInteger index = new AtomicInteger();
        Message m = event.getChannel().sendMessage(parts.get(0)).complete();

        return ReactionOperations.create(m, timeoutSeconds, (e) -> {
            if(!canEveryoneUse && e.getUser().getIdLong() != event.getAuthor().getIdLong()) return Operation.IGNORED;

            switch(e.getReactionEmote().getName()) {
                case "\u2b05": //left arrow
                    if(index.get() == 0) break;
                    if(e.getChannel().getMessageById(m.getIdLong()) == null) break;
                    m.editMessage(String.format("%s\n**Page: %d**", parts.get(index.decrementAndGet()), index.get() + 1)).queue();
                    break;

                case "\u27a1": //right arrow
                    if(index.get() + 1 >= parts.size()) break;
                    if(e.getChannel().getMessageById(m.getIdLong()) == null) break;
                    m.editMessage(String.format("%s\n**Page: %d**", parts.get(index.incrementAndGet()), index.get() + 1)).queue();
                    break;

                case "\u274c":
                    m.delete().queue();
                    break;
            }

            if(event.getGuild().getSelfMember().hasPermission(e.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                e.getReaction().removeReaction(e.getUser()).queue();
            }

            return Operation.IGNORED;
        }, "\u2b05", "\u27a1", "\u274c");
    }

    public static Future<Void> listText(GuildMessageReceivedEvent event, int timeoutSeconds, boolean canEveryoneUse, List<String> parts) {
        if(parts.size() == 0) return null;

        if(parts.size() == 1){
            event.getChannel().sendMessage(parts.get(0)).queue();
            return null;
        }

        AtomicInteger index = new AtomicInteger();
        Message m = event.getChannel().sendMessage(parts.get(0)).complete();

        return InteractiveOperations.createOverriding(event.getChannel(), timeoutSeconds, e -> {
            if(!canEveryoneUse && e.getAuthor().getIdLong() != event.getAuthor().getIdLong())
                return Operation.IGNORED;

            if(e.getMessage().getContent().equals("&p <<") || e.getMessage().getContent().equals("&page <<")) {
                if(index.get() == 0) return Operation.IGNORED;
                if(e.getChannel().getMessageById(m.getIdLong()) == null) return Operation.IGNORED;

                m.editMessage(String.format("%s\n**Page: %d**", parts.get(index.decrementAndGet()), index.get() + 1)).queue();
            } else if (e.getMessage().getContent().equals("&p >>") || e.getMessage().getContent().equals("&page >>")) {
                if(index.get() + 1 >= parts.size()) return Operation.IGNORED;
                if(e.getChannel().getMessageById(m.getIdLong()) == null) return Operation.IGNORED;

                m.editMessage(String.format("%s\n**Page: %d**", parts.get(index.incrementAndGet()), index.get() + 1)).queue();
            }

            if(e.getMessage().getContent().equals("&cancel")) {
                if(e.getChannel().getMessageById(m.getIdLong()) == null) return Operation.COMPLETED;
                m.delete().queue();
                return Operation.COMPLETED;
            }

            return Operation.IGNORED;
        });
    }

    public static List<String> divideString(String s) {
        return divideString(1750, new StringBuilder(s));
    }

    public static List<String> divideString(StringBuilder builder) {
        return divideString(1750, builder);
    }

    public static List<String> divideString(int max, StringBuilder builder) {
        List<String> m = new LinkedList<>();
        String s = builder.toString().trim();
        StringBuilder sb = new StringBuilder();
        while(s.length() > 0) {
            int idx = s.indexOf('\n');
            String line = idx == -1 ? s : s.substring(0, idx + 1);
            s = s.substring(line.length());
            if(s.equals("\n")) s = "";
            if(sb.length() + line.length() > max) {
                m.add(sb.toString());
                sb = new StringBuilder();
            }
            sb.append(line);
        }
        if(sb.length() != 0) m.add(sb.toString());

        return m;
    }
}
