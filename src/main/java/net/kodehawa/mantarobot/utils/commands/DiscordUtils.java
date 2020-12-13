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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.utils.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.ReactionOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.IntIntObjectFunction;
import net.kodehawa.mantarobot.utils.StringUtils;
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
    private static final Config config = MantaroData.config().get();

    public static <T> Pair<String, Integer> embedList(List<T> list, Function<T, String> toString) {
        var builder = new StringBuilder();
        for (var i = 0; i < list.size(); i++) {
            var str = toString.apply(list.get(i));

            if (builder.length() + str.length() + 5 > MessageEmbed.TEXT_MAX_LENGTH) {
                return Pair.of(builder.toString(), i);
            }

            builder.append("**").append(i + 1).append(".** ");
            builder.append(str);
            builder.append("\n");
        }

        return Pair.of(builder.toString(), list.size());
    }

    public static Future<Void> selectInt(GuildMessageReceivedEvent event, int max,
                                         IntConsumer valueConsumer, Consumer<Void> cancelConsumer) {
        return InteractiveOperations.create(event.getChannel(), event.getAuthor().getIdLong(), 30, (e) -> {
            if (!e.getAuthor().equals(event.getAuthor())) {
                return Operation.IGNORED;
            }

            //Replace prefix because people seem to think you have to add the prefix before literally everything.
            var dbGuild = MantaroData.db().getGuild(event.getGuild());
            var message = e.getMessage().getContentRaw();

            if (message.equalsIgnoreCase("&cancel")) {
                e.getChannel().sendMessage(EmoteReference.CORRECT + "Cancelled operation.").queue();
                cancelConsumer.accept(null);
                return Operation.COMPLETED;
            }

            for (var s : config.prefix) {
                if (message.toLowerCase().startsWith(s)) {
                    message = message.substring(s.length());
                }
            }

            var guildCustomPrefix = dbGuild.getData().getGuildCustomPrefix();
            if (guildCustomPrefix != null && !guildCustomPrefix.isEmpty() && message.toLowerCase().startsWith(guildCustomPrefix)) {
                message = message.substring(guildCustomPrefix.length());
            } //End of prefix replacing

            try {
                var choose = Integer.parseInt(message);
                if (choose < 1 || choose > max) {
                    return Operation.IGNORED;
                }

                valueConsumer.accept(choose);

                return Operation.COMPLETED;
            } catch (Exception ignored) { }

            return Operation.IGNORED;
        });
    }

    public static Future<Void> selectInt(GuildMessageReceivedEvent event, int max, IntConsumer valueConsumer) {
        return selectInt(event, max, valueConsumer, (o) -> { });
    }

    public static <T> Future<Void> selectList(GuildMessageReceivedEvent event, List<T> list,
                                              Function<T, String> toString, Function<String, MessageEmbed> toEmbed,
                                              Consumer<T> valueConsumer, Consumer<Void> cancelConsumer) {
        var r = embedList(list, toString);
        event.getChannel().sendMessage(toEmbed.apply(r.getLeft())).queue();

        return selectInt(event, r.getRight() + 1, i -> valueConsumer.accept(list.get(i - 1)), cancelConsumer);
    }

    public static <T> Future<Void> selectList(GuildMessageReceivedEvent event, T[] list,
                                              Function<T, String> toString, Function<String, MessageEmbed> toEmbed,
                                              Consumer<T> valueConsumer, Consumer<Void> cancelConsumer) {
        var r = embedList(Arrays.asList(list), toString);
        event.getChannel().sendMessage(toEmbed.apply(r.getLeft())).queue();

        return selectInt(event, r.getRight() + 1, i -> valueConsumer.accept(list[i - 1]), cancelConsumer);
    }

    public static <T> Future<Void> selectList(GuildMessageReceivedEvent event, List<T> list,
                                              Function<T, String> toString,
                                              Function<String, MessageEmbed> toEmbed, Consumer<T> valueConsumer) {
        return selectList(event, list, toString, toEmbed, valueConsumer, (o) -> { });
    }

    public static <T> Future<Void> selectList(GuildMessageReceivedEvent event, T[] list,
                                              Function<T, String> toString, Function<String, MessageEmbed> toEmbed,
                                              Consumer<T> valueConsumer) {
        return selectList(event, list, toString, toEmbed, valueConsumer, (o) -> { });
    }

    public static Future<Void> list(GuildMessageReceivedEvent event, int timeoutSeconds, boolean canEveryoneUse, int length,
                                    IntIntObjectFunction<EmbedBuilder> supplier, String... parts) {
        if (parts.length == 0) {
            return null;
        }

        List<MessageEmbed> embeds = buildSplitEmbed(supplier, length, parts);
        var index = new AtomicInteger();
        var message = event.getChannel().sendMessage(embeds.get(0)).complete();

        return ReactionOperations.create(message, timeoutSeconds, (e) -> {
            if (!canEveryoneUse && e.getUser().getIdLong() != event.getAuthor().getIdLong()) {
                return Operation.IGNORED;
            }

            switch (e.getReactionEmote().getName()) {
                //left arrow
                case "\u2b05" -> {
                    if (index.get() == 0) {
                        break;
                    }

                    message.editMessage(embeds.get(index.decrementAndGet())).queue();
                }
                //right arrow
                case "\u27a1" -> {
                    if (index.get() + 1 >= embeds.size()) {
                        break;
                    }

                    message.editMessage(embeds.get(index.incrementAndGet())).queue();
                }
                default -> { } // Do nothing, but make codefactor happy lol
            }

            if (event.getGuild().getSelfMember().hasPermission(e.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                e.getReaction().removeReaction(e.getUser()).queue();
            }

            return Operation.IGNORED;
        }, "\u2b05", "\u27a1");
    }

    public static void listText(GuildMessageReceivedEvent event, int timeoutSeconds, boolean canEveryoneUse,
                                EmbedBuilder base, List<List<MessageEmbed.Field>> parts) {
        if (parts.size() == 0) {
            return;
        }

        for (MessageEmbed.Field f : parts.get(0)) {
            base.addField(f);
        }

        if (parts.size() == 1) {
            event.getChannel().sendMessage(base.build()).queue();
            return;
        }

        var index = new AtomicInteger();
        var m = event.getChannel().sendMessage(base.build()).complete();

        InteractiveOperations.create(event.getChannel(), event.getAuthor().getIdLong(), timeoutSeconds, e -> {
            if (!canEveryoneUse && e.getAuthor().getIdLong() != event.getAuthor().getIdLong()) {
                return Operation.IGNORED;
            }

            var contentRaw = e.getMessage().getContentRaw();
            if (contentRaw.equals("&p <<") || contentRaw.equals("&page <<")) {
                if (index.get() == 0) {
                    return Operation.IGNORED;
                }

                var toSend = addAllFields(base, parts.get(index.decrementAndGet()));
                toSend.setFooter("Current page: %,d | Total Pages: %,d".formatted((index.get() + 1), parts.size()),
                        event.getAuthor().getEffectiveAvatarUrl()
                );

                m.editMessage(toSend.build()).queue();
            } else if (contentRaw.equals("&p >>") || contentRaw.equals("&page >>")) {
                if (index.get() + 1 >= parts.size()) {
                    return Operation.IGNORED;
                }

                var toSend = addAllFields(base, parts.get(index.incrementAndGet()));

                toSend.setFooter("Current page: %,d | Total Pages: %,d".formatted((index.get() + 1), parts.size()),
                        event.getAuthor().getEffectiveAvatarUrl()
                );

                m.editMessage(toSend.build()).queue();
            }

            if (contentRaw.equals("&cancel")) {
                m.delete().queue();
                return Operation.COMPLETED;
            }

            return Operation.IGNORED;
        });
    }

    public static void listText(GuildMessageReceivedEvent event, int timeoutSeconds, boolean canEveryoneUse, List<String> parts) {
        if (parts.size() == 0) {
            return;
        }

        if (parts.size() == 1) {
            event.getChannel().sendMessage(parts.get(0)).queue();
            return;
        }

        var index = new AtomicInteger();
        var m = event.getChannel().sendMessage(parts.get(0)).complete();

        InteractiveOperations.create(event.getChannel(), event.getAuthor().getIdLong(), timeoutSeconds, e -> {
            if (!canEveryoneUse && e.getAuthor().getIdLong() != event.getAuthor().getIdLong()) {
                return Operation.IGNORED;
            }

            var contentRaw = e.getMessage().getContentRaw();

            if (contentRaw.equals("&p <<") || contentRaw.equals("&page <<")) {
                if (index.get() == 0) {
                    return Operation.IGNORED;
                }

                m.editMessage(String.format("%s\n**Page: %d** | Total: %d**", parts.get(index.decrementAndGet()), index.get() + 1, parts.size())).queue();
            } else if (contentRaw.equals("&p >>") || contentRaw.equals("&page >>")) {
                if (index.get() + 1 >= parts.size()) {
                    return Operation.IGNORED;
                }

                m.editMessage(String.format("%s\n**Page: %d | Total: %d**", parts.get(index.incrementAndGet()), index.get() + 1, parts.size())).queue();
            }

            if (contentRaw.equals("&cancel")) {
                m.delete().queue();
                return Operation.COMPLETED;
            }

            return Operation.IGNORED;
        });
    }


    public static void listText(GuildMessageReceivedEvent event, int timeoutSeconds, boolean canEveryoneUse, int length,
                                IntIntObjectFunction<EmbedBuilder> supplier, String... parts) {
        if (parts.length == 0) {
            return;
        }

        List<MessageEmbed> embeds = buildSplitEmbed(supplier, length, parts);
        if (embeds.size() == 1) {
            event.getChannel().sendMessage(embeds.get(0)).queue();
            return;
        }

        var index = new AtomicInteger();
        var m = event.getChannel().sendMessage(embeds.get(0)).complete();

        InteractiveOperations.create(event.getChannel(), event.getAuthor().getIdLong(), timeoutSeconds, e -> {
            if (!canEveryoneUse && e.getAuthor().getIdLong() != event.getAuthor().getIdLong()) {
                return Operation.IGNORED;
            }

            if (e.getMessage().getContentRaw().equals("&p <<") || e.getMessage().getContentRaw().equals("&page <<")) {
                if (index.get() == 0) {
                    return Operation.IGNORED;
                }

                m.editMessage(embeds.get(index.decrementAndGet())).queue();
            } else if (e.getMessage().getContentRaw().equals("&p >>") || e.getMessage().getContentRaw().equals("&page >>")) {
                if (index.get() + 1 >= embeds.size()) {
                    return Operation.IGNORED;
                }

                m.editMessage(embeds.get(index.incrementAndGet())).queue();
            }

            if (e.getMessage().getContentRaw().equals("&cancel")) {
                m.delete().queue();
                return Operation.COMPLETED;
            }

            return Operation.IGNORED;
        });
    }

    public static void listText(GuildMessageReceivedEvent event, int timeoutSeconds, boolean canEveryoneUse, int length,
                                        IntIntObjectFunction<EmbedBuilder> supplier, List<String> parts) {
        listText(event, timeoutSeconds, canEveryoneUse, length, supplier, parts.toArray(StringUtils.EMPTY_ARRAY));
    }

    public static void listText(GuildMessageReceivedEvent event, int timeoutSeconds, boolean canEveryoneUse,
                                IntIntObjectFunction<EmbedBuilder> supplier, List<String> parts) {
        listText(event, timeoutSeconds, canEveryoneUse, MessageEmbed.TEXT_MAX_LENGTH, supplier, parts.toArray(StringUtils.EMPTY_ARRAY));
    }

    public static Future<Void> list(GuildMessageReceivedEvent event, int timeoutSeconds, boolean canEveryoneUse, List<String> parts) {
        if (parts.size() == 0) {
            return null;
        }

        if (parts.size() == 1) {
            event.getChannel().sendMessage(parts.get(0)).queue();
            return null;
        }

        var index = new AtomicInteger();
        var m = event.getChannel().sendMessage(parts.get(0)).complete();

        return ReactionOperations.create(m, timeoutSeconds, (e) -> {
            if (!canEveryoneUse && e.getUser().getIdLong() != event.getAuthor().getIdLong())
                return Operation.IGNORED;

            switch (e.getReactionEmote().getName()) {
                //left arrow
                case "\u2b05" -> {
                    if (index.get() == 0) {
                        break;
                    }

                    m.editMessage(String.format("%s\n**Page: %d**", parts.get(index.decrementAndGet()), index.get() + 1)).queue();
                }
                //right arrow
                case "\u27a1" -> {
                    if (index.get() + 1 >= parts.size()) {
                        break;
                    }

                    m.editMessage(String.format("%s\n**Page: %d**", parts.get(index.incrementAndGet()), index.get() + 1)).queue();
                }
                case "\u274c" -> {
                    m.delete().queue();
                    return Operation.COMPLETED;
                }
                default -> { } // Do nothing, but make codefactor happy lol
            }

            if (event.getGuild().getSelfMember().hasPermission(e.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                e.getReaction().removeReaction(e.getUser()).queue();
            }

            return Operation.IGNORED;
        }, "\u2b05", "\u27a1", "\u274c");
    }

    public static Future<Void> list(GuildMessageReceivedEvent event, int timeoutSeconds, boolean canEveryoneUse, int length,
                                    IntIntObjectFunction<EmbedBuilder> supplier, List<String> parts) {
        return list(event, timeoutSeconds, canEveryoneUse, length, supplier, parts.toArray(StringUtils.EMPTY_ARRAY));
    }

    public static Future<Void> list(GuildMessageReceivedEvent event, int timeoutSeconds, boolean canEveryoneUse,
                                    IntIntObjectFunction<EmbedBuilder> supplier, List<String> parts) {
        // Passing an empty String[] array to List#toArray makes it convert to a array of strings, god knows why.
        // Javadoc below just so I don't forget:
        // (...) If the list fits in the specified array, it is returned therein.
        // Otherwise, a new array is allocated with the runtime type of the specified array and the size of this list.
        return list(event, timeoutSeconds, canEveryoneUse, MessageEmbed.TEXT_MAX_LENGTH, supplier, parts.toArray(StringUtils.EMPTY_ARRAY));
    }

    public static Future<Void> list(GuildMessageReceivedEvent event, int timeoutSeconds, boolean canEveryoneUse,
                                    EmbedBuilder base, List<List<MessageEmbed.Field>> parts) {
        if (parts.size() == 0) {
            return null;
        }

        for (MessageEmbed.Field f : parts.get(0)) {
            base.addField(f);
        }

        if (parts.size() == 1) {
            event.getChannel().sendMessage(base.build()).queue();
            return null;
        }

        base.setFooter("Total Pages: %s | Thanks for using Mantaro ❤️".formatted(parts.size()), event.getAuthor().getEffectiveAvatarUrl());

        var index = new AtomicInteger();
        var message = event.getChannel().sendMessage(base.build()).complete();
        return ReactionOperations.create(message, timeoutSeconds, (e) -> {
            if (!canEveryoneUse && e.getUser().getIdLong() != event.getAuthor().getIdLong()) {
                return Operation.IGNORED;
            }

            switch (e.getReactionEmote().getName()) {
                //left arrow
                case "\u2b05" -> {
                    if (index.get() == 0) {
                        break;
                    }

                    var toSend = addAllFields(base, parts.get(index.decrementAndGet()));
                    toSend.setFooter("Current page: %,d | Total Pages: %,d".formatted((index.get() + 1), parts.size()),
                            event.getAuthor().getEffectiveAvatarUrl()
                    );

                    message.editMessage(toSend.build()).queue();
                }
                //right arrow
                case "\u27a1" -> {
                    if (index.get() + 1 >= parts.size()) {
                        break;
                    }

                    var toSend1 = addAllFields(base, parts.get(index.incrementAndGet()));
                    toSend1.setFooter("Current page: %,d | Total Pages: %,d".formatted((index.get() + 1), parts.size()),
                            event.getAuthor().getEffectiveAvatarUrl()
                    );
                    message.editMessage(toSend1.build()).queue();
                }
                default -> { } // Do nothing, but make codefactor happy lol
            }

            if (event.getGuild().getSelfMember().hasPermission(e.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                e.getReaction().removeReaction(e.getUser()).queue();
            }

            return Operation.IGNORED;
        }, "\u2b05", "\u27a1");
    }

    public static List<String> divideString(int max, StringBuilder builder) {
        List<String> list = new LinkedList<>();
        var str = builder.toString().trim();
        var stringBuilder = new StringBuilder();

        while (str.length() > 0) {
            var index = str.indexOf('\n');

            var line = index == -1 ?
                    str : str.substring(0, index + 1);

            str = str.substring(line.length());
            // Split on newline, if possible.
            if (str.equals("\n")) {
                str = "";
            }

            if (stringBuilder.length() + line.length() > max) {
                list.add(stringBuilder.toString());
                stringBuilder = new StringBuilder();
            }

            stringBuilder.append(line);
        }

        if (stringBuilder.length() != 0) {
            list.add(stringBuilder.toString());
        }

        return list;
    }

    private static List<MessageEmbed> buildSplitEmbed(IntIntObjectFunction<EmbedBuilder> supplier, long length, String... parts) {
        List<MessageEmbed> embeds = new ArrayList<>();
        var stringBuilder = new StringBuilder();

        // Get the amount of embeds we need to create.
        int total;
        {
            int totalAmount = 0;
            int chars = 0;

            // Iterate through the list of parts.
            for (var part : parts) {
                // If the length of the part + chars + 1
                // is more than the desired length, we split this one.
                if (part.length() + chars + 1 > length) {
                    // Update the total embed amount.
                    totalAmount++;

                    // Reset the char amount to 0, as we're splitting.
                    chars = 0;
                }

                // Update the character count.
                chars += part.length() + 1;
            }

            // Update the text embed amount if the final character count > 1.
            if (chars > 0) {
                totalAmount++;
            }

            // The total amount of embeds to part.
            total = totalAmount;
        }

        // Build the split embeds
        for (var part : parts) {
            var finalLength = part.length() + 1;

            // Can't go through if a page size is bigger than the maximum allowed.
            if (finalLength > MessageEmbed.TEXT_MAX_LENGTH) {
                throw new IllegalArgumentException("Length for one of the pages is greater than the maximum");
            }

            // Create the embeds. Split if the final length is more than the allowed.
            if (stringBuilder.length() + finalLength > length) {
                var embedBuilder = supplier.apply(embeds.size() + 1, total);

                embedBuilder.setDescription(stringBuilder.toString());
                embeds.add(embedBuilder.build());

                // Reset the string builder to build a new embed.
                stringBuilder = new StringBuilder();
            }

            stringBuilder.append(part).append('\n');
        }


        // If we have a dangling builder, it means we didn't get to reset the builder
        // when building a new embed, and there's a dangling one:
        // Add it to the total.
        if (stringBuilder.length() > 0) {
            var embedBuilder = supplier.apply(embeds.size() + 1, total);
            embedBuilder.setDescription(stringBuilder.toString());

            embeds.add(embedBuilder.build());
        }

        return embeds;
    }

    public static List<String> divideString(int max, String s) {
        return divideString(max, new StringBuilder(s));
    }

    public static List<String> divideString(StringBuilder builder) {
        return divideString(1750, builder);
    }

    public static void sendPaginatedEmbed(final Context ctx, EmbedBuilder builder,
                                          List<List<MessageEmbed.Field>> splitFields, final String str) {
        final var languageContext = ctx.getLanguageContext();
        final var show = "\n" + (str.isEmpty() ? "" : EmoteReference.TALKING + str);
        final var newLine = builder.getDescriptionBuilder().length() > 0 ? "\n" : "";

        if (ctx.hasReactionPerms()) {
            builder.appendDescription(
                    newLine + String.format(languageContext.get("general.buy_sell_paged_react"), show)
            );

            list(ctx.getEvent(), 120, false, builder, splitFields);
        } else {
            builder.appendDescription(
                    newLine + String.format(languageContext.get("general.buy_sell_paged_text"), show)
            );

            listText(ctx.getEvent(), 120, false, builder, splitFields);
        }
    }

    public static void sendPaginatedEmbed(final Context ctx, EmbedBuilder builder,
                                          List<List<MessageEmbed.Field>> splitFields) {
        sendPaginatedEmbed(ctx, builder, splitFields, "");
    }

    public static List<List<MessageEmbed.Field>> divideFields(int max, List<MessageEmbed.Field> fields) {
        List<MessageEmbed.Field> temp = new LinkedList<>();
        List<List<MessageEmbed.Field>> m = new LinkedList<>();

        while (fields.size() > 0) {
            if (temp.size() < max) {
                temp.add(fields.get(0));
                fields.remove(0);
            } else {
                m.add(temp);
                temp = new LinkedList<>();
            }
        }

        if (temp.size() != 0) {
            m.add(temp);
        }

        return m;
    }

    private static EmbedBuilder addAllFields(EmbedBuilder builder, List<MessageEmbed.Field> fields) {
        builder.clearFields();

        for (MessageEmbed.Field f : fields) {
            builder.addField(f);
        }

        return builder;
    }
}
