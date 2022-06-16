/*
 * Copyright (C) 2016-2021 David Rubio Escares / Kodehawa
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
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.kodehawa.mantarobot.commands.anime.AnimeData;
import net.kodehawa.mantarobot.core.command.slash.IContext;
import net.kodehawa.mantarobot.core.listeners.operations.ButtonOperations;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.ReactionOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.ButtonOperation;
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

// TODO: rewrite this cursedness! We don't need anything but buttons now, probably?
public class DiscordUtils {
    private static final Config config = MantaroData.config().get();

    private static final Button[] DEFAULT_COMPONENTS_FIRST = {
            Button.primary("button_first", Emoji.fromUnicode("⏪")).asDisabled(),
            Button.primary("button_right", Emoji.fromUnicode("◀️")).asDisabled(),
            Button.primary("button_left", Emoji.fromUnicode("▶️")),
            Button.primary("button_last", Emoji.fromUnicode("⏩"))
    };

    private static final Button[] DEFAULT_COMPONENTS_LAST = {
            Button.primary("button_first", Emoji.fromUnicode("⏪")),
            Button.primary("button_right", Emoji.fromUnicode("◀️")),
            Button.primary("button_left", Emoji.fromUnicode("▶️")).asDisabled(),
            Button.primary("button_last", Emoji.fromUnicode("⏩")).asDisabled()
    };

    private static final Button[] DEFAULT_COMPONENTS_ALL = {
            Button.primary("button_first", Emoji.fromUnicode("⏪")),
            Button.primary("button_right", Emoji.fromUnicode("◀️")),
            Button.primary("button_left", Emoji.fromUnicode("▶️")),
            Button.primary("button_last", Emoji.fromUnicode("⏩"))
    };

    private static final Button[] DEFAULT_COMPONENTS_DISABLED = {
            Button.primary("button_first", Emoji.fromUnicode("⏪")).asDisabled(),
            Button.primary("button_right", Emoji.fromUnicode("◀️")).asDisabled(),
            Button.primary("button_left", Emoji.fromUnicode("▶️")).asDisabled(),
            Button.primary("button_last", Emoji.fromUnicode("⏩")).asDisabled()
    };

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

    public static Future<Void> selectIntButton(IContext ctx, Message message, int max,
                                               IntConsumer valueConsumer, Consumer<Void> cancelConsumer) {
        int count = 0;
        List<ActionRow> buttons = new ArrayList<>();
        List<Button> temp = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            count++;
            if (count > 5) {
                buttons.add(ActionRow.of(temp));
                temp.clear();
                count = 0;
            }

            temp.add(Button.primary(String.valueOf(i + 1), String.valueOf(i + 1)));
        }

        buttons.add(ActionRow.of(temp));
        buttons.add(ActionRow.of(Button.danger("cancel", ctx.getLanguageContext().get("buttons.cancel"))));

        return ButtonOperations.createRows(message, 30L, e -> {
            if (e.getUser().getIdLong() != ctx.getAuthor().getIdLong()) {
                return Operation.IGNORED;
            }

            var button = e.getButton();
            if (button == null || button.getId() == null) {
                return Operation.IGNORED;
            }

            if (button.getId().equals("cancel")) {
                e.getHook().editOriginal(ctx.getLanguageContext().get("commands.profile.unequip.cancelled").formatted(EmoteReference.OK))
                        .setEmbeds()
                        .setActionRows()
                        .queue();

                return Operation.COMPLETED;
            }

            try {
                valueConsumer.accept(Integer.parseInt(button.getId()));
                return Operation.COMPLETED;
            } catch (Exception ignored) { }

            return Operation.IGNORED;
        }, buttons);
    }

    public static Future<Void> selectInt(GuildMessageReceivedEvent event, int max, IntConsumer valueConsumer) {
        return selectInt(event, max, valueConsumer, (o) -> { });
    }

    public static <T> Future<Void> selectList(GuildMessageReceivedEvent event, List<T> list,
                                              Function<T, String> toString, Function<String, MessageEmbed> toEmbed,
                                              Consumer<T> valueConsumer, Consumer<Void> cancelConsumer) {
        var r = embedList(list, toString);
        event.getChannel().sendMessageEmbeds(toEmbed.apply(r.getLeft())).queue();

        return selectInt(event, r.getRight() + 1, i -> valueConsumer.accept(list.get(i - 1)), cancelConsumer);
    }

    public static <T> Future<Void> selectList(GuildMessageReceivedEvent event, T[] list,
                                              Function<T, String> toString, Function<String, MessageEmbed> toEmbed,
                                              Consumer<T> valueConsumer, Consumer<Void> cancelConsumer) {
        var r = embedList(Arrays.asList(list), toString);
        event.getChannel().sendMessageEmbeds(toEmbed.apply(r.getLeft())).queue();

        return selectInt(event, r.getRight() + 1, i -> valueConsumer.accept(list[i - 1]), cancelConsumer);
    }

    public static <T> Future<Void> selectList(GuildMessageReceivedEvent event, List<T> list,
                                              Function<T, String> toString,
                                              Function<String, MessageEmbed> toEmbed, Consumer<T> valueConsumer) {
        return selectList(event, list, toString, toEmbed, valueConsumer, (o) -> { });
    }

    public static <T> Future<Void> selectListButton(IContext ctx, List<T> list,
                                                    Function<T, String> toString, Function<String, MessageEmbed> toEmbed,
                                                    Consumer<T> valueConsumer) {
        return selectListButton(ctx, list, toString, toEmbed, valueConsumer, (o) -> { });
    }

    public static <T> Future<Void> selectList(GuildMessageReceivedEvent event, T[] list,
                                              Function<T, String> toString, Function<String, MessageEmbed> toEmbed,
                                              Consumer<T> valueConsumer) {
        return selectList(event, list, toString, toEmbed, valueConsumer, (o) -> { });
    }

    public static <T> Future<Void> selectListButton(IContext ctx, List<T> list,
                                              Function<T, String> toString, Function<String, MessageEmbed> toEmbed,
                                              Consumer<T> valueConsumer, Consumer<Void> cancelConsumer) {
        var r = embedList(list, toString);
        var m = ctx.sendResult(toEmbed.apply(r.getLeft()));

        if (list.size() > 20) {
            throw new IllegalArgumentException("Too many options on ActionRow, attempted " + list.size() + ". Max: 20.");
        }

        return selectIntButton(ctx, m, r.getRight(), i -> valueConsumer.accept(list.get(i - 1)), cancelConsumer);
    }

    public static Future<Void> list(GuildMessageReceivedEvent event, int timeoutSeconds, boolean canEveryoneUse, int length,
                                    IntIntObjectFunction<EmbedBuilder> supplier, String... parts) {
        if (parts.length == 0) {
            return null;
        }

        List<MessageEmbed> embeds = buildSplitEmbed(supplier, length, parts);
        if (embeds.size() == 1) {
            event.getChannel().sendMessageEmbeds(embeds.get(0)).queue();
            return null;
        }

        var index = new AtomicInteger();
        var message = event.getChannel().sendMessageEmbeds(embeds.get(0)).complete();

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

                    message.editMessageEmbeds(embeds.get(index.decrementAndGet())).queue();
                }
                //right arrow
                case "\u27a1" -> {
                    if (index.get() + 1 >= embeds.size()) {
                        break;
                    }

                    message.editMessageEmbeds(embeds.get(index.incrementAndGet())).queue();
                }
                default -> { } // Do nothing, but make codefactor happy lol
            }

            if (e.getGuild().getSelfMember().hasPermission(e.getTextChannel(), Permission.MESSAGE_MANAGE) && e.getUser() != null) {
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
            event.getChannel().sendMessageEmbeds(base.build()).queue();
            return;
        }

        var index = new AtomicInteger();
        var m = event.getChannel().sendMessageEmbeds(base.build()).complete();

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

                m.editMessageEmbeds(toSend.build()).queue();
            } else if (contentRaw.equals("&p >>") || contentRaw.equals("&page >>")) {
                if (index.get() + 1 >= parts.size()) {
                    return Operation.IGNORED;
                }

                var toSend = addAllFields(base, parts.get(index.incrementAndGet()));

                toSend.setFooter("Current page: %,d | Total Pages: %,d".formatted((index.get() + 1), parts.size()),
                        event.getAuthor().getEffectiveAvatarUrl()
                );

                m.editMessageEmbeds(toSend.build()).queue();
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
            event.getChannel().sendMessageEmbeds(embeds.get(0)).queue();
            return;
        }

        var index = new AtomicInteger();
        var m = event.getChannel().sendMessageEmbeds(embeds.get(0)).complete();

        InteractiveOperations.create(event.getChannel(), event.getAuthor().getIdLong(), timeoutSeconds, e -> {
            if (!canEveryoneUse && e.getAuthor().getIdLong() != event.getAuthor().getIdLong()) {
                return Operation.IGNORED;
            }

            if (e.getMessage().getContentRaw().equals("&p <<") || e.getMessage().getContentRaw().equals("&page <<")) {
                if (index.get() == 0) {
                    return Operation.IGNORED;
                }

                m.editMessageEmbeds(embeds.get(index.decrementAndGet())).queue();
            } else if (e.getMessage().getContentRaw().equals("&p >>") || e.getMessage().getContentRaw().equals("&page >>")) {
                if (index.get() + 1 >= embeds.size()) {
                    return Operation.IGNORED;
                }

                m.editMessageEmbeds(embeds.get(index.incrementAndGet())).queue();
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

            if (e.getGuild().getSelfMember().hasPermission(e.getTextChannel(), Permission.MESSAGE_MANAGE) && e.getUser() != null) {
                e.getReaction().removeReaction(e.getUser()).queue();
            }

            return Operation.IGNORED;
        }, "\u2b05", "\u27a1", "\u274c");
    }

    public static Future<Void> listButtons(UtilsContext ctx, int timeoutSeconds, int length,
                                    IntIntObjectFunction<EmbedBuilder> supplier, String... parts) {
        if (parts.length == 0) {
            return null;
        }

        List<MessageEmbed> embeds = buildSplitEmbed(supplier, length, parts);
        if (embeds.size() == 1) {
            ctx.send(embeds.get(0));
            return null;
        }

        var index = new AtomicInteger();
        var message = ctx.send(embeds.get(0));
        return ButtonOperations.create(message, timeoutSeconds, new ButtonOperation() {
            @Override
            public int click(ButtonClickEvent e) {
                if (e.getUser().getIdLong() != ctx.getAuthor().getIdLong())
                    return Operation.IGNORED;

                var button = e.getButton();
                if (button == null)
                    return Operation.IGNORED;

                var hook = e.getHook();
                switch (button.getId()) {
                    case "button_first" -> {
                        index.set(0);
                        hook.editOriginalEmbeds(embeds.get(0)).queue();
                        hook.editOriginalComponents(ActionRow.of(DEFAULT_COMPONENTS_FIRST)).queue();
                    }
                    case "button_last" -> {
                        index.set(embeds.size() - 1);
                        hook.editOriginalEmbeds(embeds.get(embeds.size() - 1)).queue();
                        hook.editOriginalComponents(ActionRow.of(DEFAULT_COMPONENTS_LAST)).queue();
                    }

                    case "button_right" -> {
                        if (index.get() == 0) {
                            break;
                        }

                        if (index.get() == 0) {
                            hook.editOriginalComponents(ActionRow.of(DEFAULT_COMPONENTS_FIRST)).queue();
                        } else {
                            hook.editOriginalComponents(ActionRow.of(DEFAULT_COMPONENTS_ALL)).queue();
                        }

                        hook.editOriginalEmbeds(embeds.get(index.decrementAndGet())).queue();
                    }

                    case "button_left" -> {
                        if (index.get() + 1 >= embeds.size()) {
                            break;
                        }

                        if (index.get() == embeds.size() - 1) {
                            hook.editOriginalComponents(ActionRow.of(DEFAULT_COMPONENTS_LAST)).queue();
                        } else {
                            hook.editOriginalComponents(ActionRow.of(DEFAULT_COMPONENTS_ALL)).queue();
                        }

                        hook.editOriginalEmbeds(embeds.get(index.incrementAndGet())).queue();
                    }
                    default -> {
                        return Operation.IGNORED;
                    }
                }

                return Operation.IGNORED;
            }
            @Override
            public void onExpire() {
                message.editMessageComponents(ActionRow.of(DEFAULT_COMPONENTS_DISABLED)).queue();
            }
        }, DEFAULT_COMPONENTS_FIRST);
    }

    public static Future<Void> listButtons(UtilsContext ctx, int timeoutSeconds, List<String> parts) {
        if (parts.size() == 0) {
            return null;
        }

        if (parts.size() == 1) {
            ctx.send(parts.get(0));
            return null;
        }

        var index = new AtomicInteger();
        var m = ctx.send(parts.get(0));
        return ButtonOperations.create(m, timeoutSeconds, new ButtonOperation() {
            @Override
            public int click(ButtonClickEvent e) {
                if (e.getUser().getIdLong() != ctx.getAuthor().getIdLong())
                    return Operation.IGNORED;

                var hook = e.getHook();
                var button = e.getButton();
                if (button == null)
                    return Operation.IGNORED;

                switch (button.getId()) {
                    case "button_first" -> {
                        index.set(0);
                        hook.editOriginal(String.format("%s\n**Page: %d**", parts.get(index.get()), 1)).queue();
                        hook.editOriginalComponents(ActionRow.of(DEFAULT_COMPONENTS_FIRST)).queue();
                    }
                    case "button_last" -> {
                        index.set(parts.size() - 1);
                        hook.editOriginal(String.format("%s\n**Page: %d**", parts.get(parts.size() - 1), parts.size())).queue();
                        hook.editOriginalComponents(ActionRow.of(DEFAULT_COMPONENTS_LAST)).queue();
                    }
                    case "button_right" -> {
                        if (index.get() == 0) {
                            break;
                        }

                        if (index.get() == 0) {
                            hook.editOriginalComponents(ActionRow.of(DEFAULT_COMPONENTS_FIRST)).queue();
                        } else {
                            hook.editOriginalComponents(ActionRow.of(DEFAULT_COMPONENTS_ALL)).queue();
                        }

                        hook.editOriginal(String.format("%s\n**Page: %d**", parts.get(index.decrementAndGet()), index.get() + 1)).queue();
                    }

                    case "button_left" -> {
                        if (index.get() + 1 >= parts.size()) {
                            break;
                        }

                        if (index.get() == parts.size() - 1) {
                            hook.editOriginalComponents(ActionRow.of(DEFAULT_COMPONENTS_LAST)).queue();
                        } else {
                            hook.editOriginalComponents(ActionRow.of(DEFAULT_COMPONENTS_ALL)).queue();
                        }

                        hook.editOriginal(String.format("%s\n**Page: %d**", parts.get(index.incrementAndGet()), index.get() + 1)).queue();
                    }

                    default -> {
                        return Operation.IGNORED;
                    }
                }

                return Operation.IGNORED;
            }

            @Override
            public void onExpire() {
                m.editMessageComponents(ActionRow.of(DEFAULT_COMPONENTS_DISABLED)).queue();
            }
        }, DEFAULT_COMPONENTS_FIRST);
    }

    public static Future<Void> listButtons(UtilsContext ctx, int timeoutSeconds, int length,
                                    IntIntObjectFunction<EmbedBuilder> supplier, List<String> parts) {
        return listButtons(ctx, timeoutSeconds, length, supplier, parts.toArray(StringUtils.EMPTY_ARRAY));
    }

    public static Future<Void> listButtons(UtilsContext ctx, int timeoutSeconds,
                                    IntIntObjectFunction<EmbedBuilder> supplier, List<String> parts) {
        // Passing an empty String[] array to List#toArray makes it convert to a array of strings, god knows why.
        // Javadoc below just so I don't forget:
        // (...) If the list fits in the specified array, it is returned therein.
        // Otherwise, a new array is allocated with the runtime type of the specified array and the size of this list.
        return listButtons(ctx, timeoutSeconds, MessageEmbed.TEXT_MAX_LENGTH, supplier, parts.toArray(StringUtils.EMPTY_ARRAY));
    }

    public static Future<Void> listButtons(UtilsContext ctx, int timeoutSeconds, EmbedBuilder base, List<List<MessageEmbed.Field>> parts) {
        if (parts.size() == 0) {
            return null;
        }

        for (MessageEmbed.Field f : parts.get(0)) {
            base.addField(f);
        }

        if (parts.size() == 1) {
            ctx.send(base.build());
            return null;
        }

        base.setFooter("Total Pages: %s | Thanks for using Mantaro ❤️".formatted(parts.size()), ctx.getAuthor().getEffectiveAvatarUrl());
        var index = new AtomicInteger();
        var message = ctx.send(base.build());
        return ButtonOperations.create(message, timeoutSeconds, new ButtonOperation() {
            @Override
            public int click(ButtonClickEvent e) {
                if (e.getUser().getIdLong() != ctx.getAuthor().getIdLong()) {
                    return Operation.IGNORED;
                }

                var button = e.getButton();
                if (button == null)
                    return Operation.IGNORED;

                var hook = e.getHook();
                switch (button.getId()) {
                    case "button_first" -> {
                        index.set(0);
                        var toSend = addAllFields(base, parts.get(index.get()));
                        toSend.setFooter("Current page: %,d | Total Pages: %,d".formatted((index.get() + 1), parts.size()),
                                ctx.getAuthor().getEffectiveAvatarUrl()
                        );

                        hook.editOriginalEmbeds(toSend.build()).queue();
                        hook.editOriginalComponents(ActionRow.of(DEFAULT_COMPONENTS_FIRST)).queue();
                    }
                    case "button_last" -> {
                        index.set(parts.size() - 1);
                        var toSend = addAllFields(base, parts.get(index.get()));
                        toSend.setFooter("Current page: %,d | Total Pages: %,d".formatted((index.get() + 1), parts.size()),
                                ctx.getAuthor().getEffectiveAvatarUrl()
                        );

                        hook.editOriginalEmbeds(toSend.build()).queue();
                        hook.editOriginalComponents(ActionRow.of(DEFAULT_COMPONENTS_LAST)).queue();
                    }

                    case "button_right" -> {
                        if (index.get() == 0) {
                            break;
                        }

                        var toSend = addAllFields(base, parts.get(index.decrementAndGet()));
                        toSend.setFooter("Current page: %,d | Total Pages: %,d".formatted((index.get() + 1), parts.size()),
                                ctx.getAuthor().getEffectiveAvatarUrl()
                        );

                        if (index.get() == 0) {
                            hook.editOriginalComponents(ActionRow.of(DEFAULT_COMPONENTS_FIRST)).queue();
                        } else {
                            hook.editOriginalComponents(ActionRow.of(DEFAULT_COMPONENTS_ALL)).queue();
                        }

                        hook.editOriginalEmbeds(toSend.build()).queue();
                    }

                    case "button_left" -> {
                        if (index.get() + 1 >= parts.size()) {
                            break;
                        }

                        var toSend1 = addAllFields(base, parts.get(index.incrementAndGet()));
                        toSend1.setFooter("Current page: %,d | Total Pages: %,d".formatted((index.get() + 1), parts.size()),
                                ctx.getAuthor().getEffectiveAvatarUrl()
                        );

                        if (index.get() == parts.size() - 1) {
                            hook.editOriginalComponents(ActionRow.of(DEFAULT_COMPONENTS_LAST)).queue();
                        } else {
                            hook.editOriginalComponents(ActionRow.of(DEFAULT_COMPONENTS_ALL)).queue();
                        }

                        hook.editOriginalEmbeds(toSend1.build()).queue();
                    }
                    default -> {
                        return Operation.IGNORED;
                    }
                }

                return Operation.IGNORED;
            }

            @Override
            public void onExpire() {
                message.editMessageComponents(ActionRow.of(DEFAULT_COMPONENTS_DISABLED)).queue();
            }
        }, DEFAULT_COMPONENTS_FIRST);
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
            event.getChannel().sendMessageEmbeds(base.build()).queue();
            return null;
        }

        base.setFooter("Total Pages: %s | Thanks for using Mantaro ❤️".formatted(parts.size()), event.getAuthor().getEffectiveAvatarUrl());

        var index = new AtomicInteger();
        var message = event.getChannel().sendMessageEmbeds(base.build()).complete();
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

                    message.editMessageEmbeds(toSend.build()).queue();
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
                    message.editMessageEmbeds(toSend1.build()).queue();
                }
                default -> { } // Do nothing, but make codefactor happy lol
            }

            if (e.getGuild().getSelfMember().hasPermission(e.getTextChannel(), Permission.MESSAGE_MANAGE) && e.getUser() != null) {
                e.getReaction().removeReaction(e.getUser()).queue();
            }

            return Operation.IGNORED;
        }, "\u2b05", "\u27a1");
    }

    public static List<String> divideString(int max, char splitOn, StringBuilder builder) {
        List<String> list = new LinkedList<>();
        var str = builder.toString().trim();
        var stringBuilder = new StringBuilder();

        // Since we remove data from the string, loop until there's nothing left.
        while (str.length() > 0) {
            // We're gonna split on the given split character. Most commonly newline.
            var index = str.indexOf(splitOn);
            // Split the string on the first occurrence of the split character
            var line = index == -1 ? str : str.substring(0, index + 1);

            str = str.substring(line.length());
            // Remove new lines at the end of a split.
            if (str.equals("\n")) {
                str = "";
            }

            // If the length of this line is more than the maximum, add another split
            // and reset the StringBuilder, start all over again.
            if (stringBuilder.length() + line.length() > max) {
                list.add(stringBuilder.toString().trim());
                stringBuilder = new StringBuilder();
            }

            // Append the current line to the StringBuilder
            stringBuilder.append(line);
        }

        // We have a dangling StringBuilder with actual content, add it to a new page.
        if (stringBuilder.length() != 0) {
            list.add(stringBuilder.toString().trim());
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
        return divideString(max, '\n', new StringBuilder(s));
    }

    public static List<String> divideString(int max, char splitOn, String s) {
        return divideString(max, splitOn, new StringBuilder(s));
    }

    public static List<String> divideString(char splitOn, StringBuilder builder) {
        return divideString(1750, splitOn, builder);
    }

    public static List<String> divideString(StringBuilder builder) {
        return divideString(1750, '\n', builder);
    }

    public static void sendPaginatedEmbed(final UtilsContext ctx, EmbedBuilder builder,
                                          List<List<MessageEmbed.Field>> splitFields, final String str) {
        final var languageContext = ctx.getLanguageContext();
        final var show =  str.isEmpty() ? "" : EmoteReference.TALKING.toHeaderString() + str + "\n";
        final var newLine = builder.getDescriptionBuilder().length() > 0 ? "\n" : "";

        if (splitFields.size() > 1) {
            builder.appendDescription(
                    newLine + String.format(languageContext.get("general.buy_sell_paged_react"), show + "\n" +
                            EmoteReference.STOPWATCH + languageContext.get("general.reaction_timeout").formatted(120))
            );
        }

        listButtons(ctx, 120, builder, splitFields);
    }

    public static void sendPaginatedEmbed(final UtilsContext ctx, EmbedBuilder builder,
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
