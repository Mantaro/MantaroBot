/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.TextCommand;
import net.kodehawa.mantarobot.core.command.TextContext;
import net.kodehawa.mantarobot.core.command.argument.Parsers;
import net.kodehawa.mantarobot.core.command.meta.Alias;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.meta.Help;
import net.kodehawa.mantarobot.core.command.meta.Name;
import net.kodehawa.mantarobot.core.command.meta.Module;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.options.core.Option;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Arrays;
import java.util.LinkedList;

@Module
public class OptsCmd {
    @Subscribe
    public void register(CommandRegistry cr) {
        cr.register(Options.class);
    }

    @Name("opts")
    @Help(
            description = """
                    This command allows you to change Mantaro settings for this server.
                    All values set are local and NOT global, meaning that they will only effect this server.
                    """,
            usage = "Check https://www.mantaro.site/mantaro-wiki/basics/server-configuration for a guide on how to use opts. Welcome to the jungle."
    )
    public static class Options extends TextCommand {
        @Override
        protected void process(TextContext ctx) {
            var lang = ctx.getLanguageContext();
            var content = ctx.argument(Parsers.remainingContent(),
                    lang.get("options.error_general").formatted(EmoteReference.ERROR),
                    lang.get("options.error_general").formatted(EmoteReference.ERROR)
            ).trim();
            var args = StringUtils.advancedSplitArgs(content, 0); // this is so hacky lol

            var optName = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (i > 15) break; // No option is so long.

                var str = args[i];
                if (!optName.isEmpty()) {
                    optName.append(":");
                }

                optName.append(str);
                var option = Option.getOptionMap().get(optName.toString());
                if (option != null) {
                    var callable = option.getEventConsumer();
                    try {
                        String[] a;
                        if (++i < args.length) {
                            a = Arrays.copyOfRange(args, i, args.length);
                        } else {
                            a = StringUtils.EMPTY_ARRAY;
                        }

                        // Pass raw content without parsing multi-argument stuff, we don't need it for *everything*
                        ctx.setCustomContent(content.substring(optName.length()).trim());
                        var player = MantaroData.db().getPlayer(ctx.getAuthor());
                        if (player.addBadgeIfAbsent(Badge.DID_THIS_WORK)) {
                            player.updateAllChanged();
                        }

                        callable.accept(ctx, a);
                    } catch (IndexOutOfBoundsException ignored) { }
                    return;
                }
            }

            ctx.sendLocalized("options.error_general", EmoteReference.WARNING);

        }

        @Alias("ls")
        @Description("Lists all the available options.")
        public static class List extends TextCommand {
            @Override
            protected void process(TextContext ctx) {
                var builder = new StringBuilder();

                for (var opt : Option.getAvaliableOptions()) {
                    builder.append(opt).append("\n");
                }

                var dividedMessages = DiscordUtils.divideString(builder);
                java.util.List<String> messages = new LinkedList<>();
                var languageContext = ctx.getLanguageContext();
                for (var msgs : dividedMessages) {
                    messages.add(
                            String.format(languageContext.get("commands.opts.list.header"),
                                    languageContext.get("general.button_react"),
                                    String.format("```prolog%n%s```", msgs))
                    );
                }

                DiscordUtils.listButtons(ctx.getUtilsContext(), 45, messages);
            }
        }

        @Name("help")
        @Description("Shows help for an specific option.")
        public static class OptsHelp extends TextCommand {
            @Override
            protected void process(TextContext ctx) {
                var args = ctx.takeMany(Parsers.string());
                var name = new StringBuilder();
                for (int i = 1; i < args.size(); i++) {
                    var s = args.get(i);
                    if (!name.isEmpty()) {
                        name.append(":");
                    }

                    name.append(s);
                    var option = Option.getOptionMap().get(name.toString());

                    if (option != null) {
                        try {
                            var builder = new EmbedBuilder()
                                    .setAuthor(option.getOptionName(), null, ctx.getAuthor().getEffectiveAvatarUrl())
                                    .setDescription(option.getDescription())
                                    .setThumbnail("https://apiv2.mantaro.site/image/common/help-icon.png")
                                    .addField(EmoteReference.PENCIL.toHeaderString() + "Type", option.getType().toString(), false);

                            ctx.send(builder.build());
                        } catch (IndexOutOfBoundsException ignored) { }
                        return;
                    }

                    ctx.sendLocalized("commands.opts.option_not_found", EmoteReference.ERROR);
                }
            }
        }
    }

}
