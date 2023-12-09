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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.SplitUtil;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.TextCommand;
import net.kodehawa.mantarobot.core.command.TextContext;
import net.kodehawa.mantarobot.core.command.helpers.CommandCategory;
import net.kodehawa.mantarobot.core.command.helpers.CommandPermission;
import net.kodehawa.mantarobot.core.command.helpers.HelpContent;
import net.kodehawa.mantarobot.core.command.i18n.I18nContext;
import net.kodehawa.mantarobot.core.command.meta.Alias;
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.meta.Help;
import net.kodehawa.mantarobot.core.command.meta.Module;
import net.kodehawa.mantarobot.core.command.meta.Name;
import net.kodehawa.mantarobot.core.command.meta.Options;
import net.kodehawa.mantarobot.core.command.processor.CommandProcessor;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.commands.info.HelpUtils.forType;
import static net.kodehawa.mantarobot.commands.info.HelpUtils.forTypeSlash;
import static net.kodehawa.mantarobot.commands.info.HelpUtils.getCommandMention;
import static net.kodehawa.mantarobot.utils.commands.EmoteReference.BLUE_SMALL_MARKER;

@Module
public class HelpCmd {
    private static final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(2)
            .cooldown(2, TimeUnit.SECONDS)
            .maxCooldown(3, TimeUnit.SECONDS)
            .randomIncrement(true)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("help")
            .build();

    private static final Random random = new Random();

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(HelpCommand.class);
        cr.register(HelpText.class);
        cr.register(Slash.class);
    }

    @Name("help")
    @Description("The usual help command helping you.")
    @Category(CommandCategory.INFO)
    @Options({
            @Options.Option(type = OptionType.STRING, name = "command", description = "The command to check help for.")
    })
    @Help(
            description = "The command you're using right now. Shows a list of commands or the command usage.",
            usage = "`/help command:[command path]`",
            parameters = {
                    @Help.Parameter(
                            name = "command",
                            description = """
                                    The command to check help for. You can use sub-commands too.
                                    For example, you can use `/help profile show` to see the help for `/profile show`.
                                    """,
                            optional = true
                    )
            }
    )
    public static class HelpCommand extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            if (!RatelimitUtils.ratelimit(rateLimiter, ctx, false)) {
                return;
            }

            var command = ctx.getOptionAsString("command", "").toLowerCase();
            if (command.isBlank()) {
                buildHelpSlash(ctx);
            } else {
                var cmd = CommandProcessor.REGISTRY.getCommandManager().slashCommands().get(command);
                var parent = "";
                // Cursed sub-command detection.
                if (command.contains(" ")) {
                    var split = command.trim().split("\\s+");
                    if (split.length > 0) {
                        parent = split[0];
                        var parentCmd = CommandProcessor.REGISTRY.getCommandManager().slashCommands().get(parent);
                        if (parentCmd != null && !parentCmd.getSubCommands().isEmpty()) {
                            var sub = split[1];
                            var subCmd = parentCmd.getSubCommands().get(sub);
                            if (subCmd != null) {
                                cmd = subCmd;
                            }
                        }
                    }
                }

                if (cmd == null) {
                    ctx.sendLocalized("commands.help.extended.not_found", EmoteReference.ERROR);
                    return;
                }

                var help = cmd.getHelp();

                if (cmd.getDescription() != null && (help == null || help.description() == null)) {
                    help = new HelpContent.Builder()
                            .setDescription(cmd.getDescription())
                            .build();
                }

                if (help == null || help.description() == null) {
                    ctx.sendLocalized("commands.help.extended.no_help", EmoteReference.ERROR);
                    return;
                }

                var languageContext = ctx.getLanguageContext();
                var hasSubs = !cmd.getSubCommands().isEmpty();

                String cmdMentionText;
                if (hasSubs) {
                    cmdMentionText =  languageContext.get("commands.help.mention_tip_subs");
                } else {
                    cmdMentionText = getCommandMention(
                            parent.isBlank() ? cmd.getName() : parent,
                            parent.isBlank() ? "" : cmd.getName()
                    ) + "\n" + languageContext.get("commands.help.mention_tip");
                }

                var desc = new StringBuilder();
                if (random.nextBoolean()) {
                    desc.append(languageContext.get("commands.help.patreon")).append("\n");
                }

                desc.append(help.description());
                desc.append("\n").append(languageContext.get("commands.help.include_warning"));

                EmbedBuilder builder = new EmbedBuilder()
                        .setColor(Color.PINK)
                        .setAuthor(languageContext.get("commands.help.help_header").formatted(command), null,
                                ctx.getAuthor().getEffectiveAvatarUrl()
                        ).setDescription(desc)
                        .addField(EmoteReference.MEGA.toHeaderString() + languageContext.get("commands.help.mention"), cmdMentionText, false);

                var options = cmd.getOptions();
                var parameters = cmd.getHelp().parameters();
                var usage = cmd.getHelp().usage();
                if (usage != null && !usage.isBlank()) {
                    builder.addField(EmoteReference.PENCIL.toHeaderString() + languageContext.get("commands.help.usage"), usage, false);
                }

                // Assume parameters is better explained.
                if (options != null && !options.isEmpty() && parameters.isEmpty()) {
                    var optionString = options.stream()
                            .map(optionData -> {
                                var name = optionData.getName();
                                var description = optionData.getDescription();
                                var str = "`%s` - %s".formatted(name, description);
                                if (!optionData.isRequired()) {
                                    str += " " + languageContext.get("commands.help.optional");
                                }

                                return str;
                            }).collect(Collectors.joining("\n"));

                    builder.addField(EmoteReference.ZAP.toHeaderString() + languageContext.get("commands.help.options"), optionString, false);
                }

                if (!parameters.isEmpty()) {
                    var paramString = parameters.stream()
                            .map(parameter -> {
                                var str = "`%s` - %s".formatted(parameter.name(), parameter.description());
                                if (parameter.optional()) {
                                    str += " " + languageContext.get("commands.help.optional");
                                }

                                return str;
                            }).collect(Collectors.joining("\n"));

                    builder.addField(EmoteReference.ZAP.toHeaderString() + languageContext.get("commands.help.options"), paramString, false);
                }


                if (hasSubs) {
                    var subs =
                            cmd.getSubCommands()
                                    .entrySet()
                                    .stream()
                                    .sorted(Comparator.comparingInt(a ->
                                            a.getValue().getDescription() == null ? 0 : a.getValue().getDescription().length())
                                    ).collect(
                                            Collectors.toMap(
                                                    Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new
                                            )
                                    );

                    var stringBuilder = new StringBuilder();
                    for (var inners : subs.entrySet()) {
                        var name = inners.getKey();
                        var inner = inners.getValue();

                        if (inner.getDescription() != null) {
                            stringBuilder.append("""
                                        %s %s - %s
                                        """.formatted(BLUE_SMALL_MARKER, getCommandMention(cmd.getName(), name), inner.getDescription())
                            );
                        }
                    }
                    if (!stringBuilder.isEmpty()) {
                        var split = SplitUtil.split(stringBuilder.toString(), MessageEmbed.VALUE_MAX_LENGTH, SplitUtil.Strategy.NEWLINE);
                        for (var s : split) {
                            builder.addField(EmoteReference.ZAP.toHeaderString() + "Sub-commands", s, false);
                        }
                        builder.addField(EmoteReference.ZAP.toHeaderString() + "Sub-command help", languageContext.get("commands.help.subcommand_help"), false);
                    }
                }

                ctx.send(builder.build(),
                        ActionRow.of(
                                Button.link("https://www.mantaro.site/mantaro-wiki", "Check the wiki!"),
                                Button.link("https://support.mantaro.site", "Get support here")
                        )
                );
            }
        }
    }

    @Name("help")
    @Alias("halp")
    @Alias("commands")
    @Category(CommandCategory.INFO)
    @Help(
            description = "The command you're using right now. Shows a list of commands or the command usage.",
            usage = "`~>help command`",
            parameters = {
                    @Help.Parameter(
                            name = "command",
                            description = "The command to check help for. You can use sub-commands too.",
                            optional = true
                    )
            }
    )
        public static class HelpText extends TextCommand {
        @Override
        protected void process(TextContext ctx) {
            if (!RatelimitUtils.ratelimit(rateLimiter, ctx, false)) {
                return;
            }

            if (!ctx.getSelfMember().hasPermission(ctx.getChannel(), Permission.MESSAGE_EMBED_LINKS)) {
                ctx.sendLocalized("general.missing_embed_permissions");
                return;
            }

            var content = ctx.takeAllString();
            var commandCategory = CommandCategory.lookupFromString(content);

            if (content.isEmpty()) {
                buildHelp(ctx, null);
            } else if (commandCategory != null) {
                buildHelp(ctx, commandCategory);
            } else {
                var member = ctx.getMember();
                var command = CommandProcessor.REGISTRY.commands().get(content);

                if (command == null) {
                    ctx.sendLocalized("commands.help.extended.not_found", EmoteReference.ERROR);
                    return;
                }

                if (command.isOwnerCommand() && !CommandPermission.OWNER.test(member)) {
                    ctx.sendLocalized("commands.help.extended.not_found", EmoteReference.ERROR);
                    return;
                }

                var help = command.help();
                if (help == null || help.description() == null) {
                    ctx.sendLocalized("commands.help.extended.no_help", EmoteReference.ERROR);
                    return;
                }

                var descriptionList = help.descriptionList();
                var languageContext = ctx.getLanguageContext();

                var desc = new StringBuilder();
                if (random.nextBoolean()) {
                    desc.append(languageContext.get("commands.help.patreon"))
                            .append("\n");
                }

                if (descriptionList.isEmpty()) {
                    desc.append(help.description());
                }
                else {
                    desc.append(descriptionList.get(random.nextInt(descriptionList.size())));
                }

                desc.append("\n").append("**Don't include <> or [] on the command itself.**");

                EmbedBuilder builder = new EmbedBuilder()
                        .setColor(Color.PINK)
                        .setAuthor("Command help for " + content, null,
                                ctx.getAuthor().getEffectiveAvatarUrl()
                        ).setDescription(desc);

                if (help.usage() != null) {
                    builder.addField(EmoteReference.PENCIL.toHeaderString() + "Usage", help.usage(), false);
                }

                if (!help.parameters().isEmpty()) {
                    builder.addField(EmoteReference.SLIDER.toHeaderString() + "Parameters", help.parameters().stream()
                            .map(entry -> "`%s` - *%s*".formatted(entry.name(), entry.description()))
                            .collect(Collectors.joining("\n")), false
                    );
                }

                //Known command aliases.
                var commandAliases = command.getAliases();
                if (!commandAliases.isEmpty()) {
                    String aliases = commandAliases
                            .stream()
                            .filter(alias -> !alias.equalsIgnoreCase(content))
                            .map("`%s`"::formatted)
                            .collect(Collectors.joining(" "));

                    if (!aliases.trim().isEmpty()) {
                        builder.addField(EmoteReference.FORK.toHeaderString() + "Aliases", aliases, false);
                    }
                }

                ctx.send(builder.build(),
                        ActionRow.of(
                                Button.link("https://www.mantaro.site/mantaro-wiki", "Check the wiki!"),
                                Button.link("https://support.mantaro.site", "Get support here")
                        )
                );
            }
        }
    }

    private static void buildHelpSlash(SlashContext ctx) {
        var dbGuild = ctx.getDBGuild();
        var dbUser = ctx.getDBUser();
        var languageContext = ctx.getLanguageContext();

        // Start building the help description.
        var description = new StringBuilder();
        description.append(languageContext.get("commands.help.base"));

        if (!dbUser.isPremium() && !dbGuild.isPremium()) {
            description.append(languageContext.get("commands.help.patreon"));
        }

        var disabledCommands = dbGuild.getDisabledCommands();
        if (!disabledCommands.isEmpty()) {
            description.append(languageContext.get("commands.help.disabled_commands").formatted(disabledCommands.size()));
        }

        var channelSpecificDisabledCommands = dbGuild.getChannelSpecificDisabledCommands();
        var disabledChannelCommands = channelSpecificDisabledCommands.get(ctx.getChannel().getId());
        if (disabledChannelCommands != null && !disabledChannelCommands.isEmpty()) {
            description.append("\n");
            description.append(
                    languageContext.get("commands.help.channel_specific_disabled_commands")
                            .formatted(disabledChannelCommands.size())
            );
        }
        // End of help description.

        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor(languageContext.get("commands.help.title"), null, ctx.getGuild().getIconUrl())
                .setColor(Color.PINK)
                .setDescription(description.toString())
                .setFooter(languageContext.get("commands.help.footer").formatted(
                        "❤️", CommandProcessor.REGISTRY.getCommandManager().slashCommands()
                                .values()
                                .stream()
                                .filter(c -> c.getCategory() != null)
                                .count()
                ), ctx.getGuild().getIconUrl());

        Arrays.stream(CommandCategory.values())
                .filter(c -> c != CommandCategory.HIDDEN)
                .filter(c -> c != CommandCategory.OWNER || CommandPermission.OWNER.test(ctx.getMember()))
                .filter(c -> !CommandProcessor.REGISTRY.getSlashCommandsForCategory(c).isEmpty())
                .forEach(c ->
                        embed.addField(
                                languageContext.get(c.toString()) + " " + languageContext.get("commands.help.commands") + ":",
                                forTypeSlash(ctx.getChannel(), dbGuild, c), false
                        )
                );

        ctx.send(embed.build(),
                ActionRow.of(
                        Button.link("https://patreon.com/mantaro", "Patreon"),
                        Button.link("https://www.mantaro.site/mantaro-wiki", "More Help"),
                        Button.link("https://support.mantaro.site", "Support Server"),
                        Button.link("https://twitter.com/mantarodiscord", "Twitter")
                )
        );
    }

    private static void buildHelp(TextContext ctx, CommandCategory category) {
        var dbGuild = ctx.getDBGuild();
        var dbUser = ctx.getDBUser();
        var languageContext = ctx.getLanguageContext();

        // Start building the help description.
        var description = new StringBuilder();
        if (category == null) {
            description.append(languageContext.get("commands.help.base_prefix"));
        } else {
            description.append(languageContext.get("commands.help.base_category")
                    .formatted(languageContext.get(category.toString()))
            );
        }

        if (!dbUser.isPremium() && !dbGuild.isPremium()) {
            description.append(languageContext.get("commands.help.patreon"));
        }

        var disabledCommands = dbGuild.getDisabledCommands();
        if (!disabledCommands.isEmpty()) {
            description.append(languageContext.get("commands.help.disabled_commands").formatted(disabledCommands.size()));
        }

        var channelSpecificDisabledCommands = dbGuild.getChannelSpecificDisabledCommands();
        var disabledChannelCommands = channelSpecificDisabledCommands.get(ctx.getChannel().getId());
        if (disabledChannelCommands != null && !disabledChannelCommands.isEmpty()) {
            description.append("\n");
            description.append(
                    languageContext.get("commands.help.channel_specific_disabled_commands")
                            .formatted(disabledChannelCommands.size())
            );
        }
        // End of help description.

        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor(languageContext.get("commands.help.title"), null, ctx.getGuild().getIconUrl())
                .setColor(Color.PINK)
                .setDescription(description.toString())
                .setFooter(languageContext.get("commands.help.footer").formatted(
                        "❤️", CommandProcessor.REGISTRY.commands()
                                .values()
                                .stream()
                                .filter(c -> c.category() != null)
                                .count()
                ), ctx.getGuild().getIconUrl());

        Arrays.stream(CommandCategory.values())
                .filter(c -> {
                    if (category != null) {
                        return c == category;
                    } else {
                        return true;
                    }
                })
                .filter(c -> c != CommandCategory.HIDDEN)
                .filter(c -> c != CommandCategory.OWNER || CommandPermission.OWNER.test(ctx.getMember()))
                .filter(c -> !CommandProcessor.REGISTRY.getCommandsForCategory(c).isEmpty())
                .forEach(c ->
                        embed.addField(
                                languageContext.get(c.toString()) + " " + languageContext.get("commands.help.commands") + ":",
                                forType(ctx.getChannel(), dbGuild, c), false
                        )
                );

        ctx.send(embed.build(),
                ActionRow.of(
                        Button.link("https://patreon.com/mantaro", "Patreon"),
                        Button.link("https://www.mantaro.site/mantaro-wiki", "More Help"),
                        Button.link("https://support.mantaro.site", "Support Server"),
                        Button.link("https://twitter.com/mantarodiscord", "Twitter")
                )
        );
    }

    // This looks hilarious.
    // Maybe just remove it.
    @Alias("info") @Alias("status") @Alias("shard")
    @Alias("shardinfo") @Alias("ping") @Alias("time")
    @Alias("prune") @Alias("ban") @Alias("kick")
    @Alias("softban") @Alias("userinfo") @Alias("serverinfo")
    @Alias("avatar") @Alias("roleinfo") @Alias("support")
    @Alias("donate") @Alias("language") @Alias("invite")
    @Alias("danbooru") @Alias("e621") @Alias("e926")
    @Alias("yandere") @Alias("konachan") @Alias("gelbooru")
    @Alias("safebooru") @Alias("rule34") @Alias("iam")
    @Alias("iamnot") @Alias("8ball") @Alias("createpoll")
    @Alias("anime") @Alias("character") @Alias("poll")
    @Alias("coinflip") @Alias("ratewaifu") @Alias("roll")
    @Alias("birthday") @Alias("profile") @Alias("reputation")
    @Alias("equip") @Alias("unequip") @Alias("badges")
    @Alias("activatekey") @Alias("premium") @Alias("transfer")
    @Alias("itemtransfer") @Alias("pet") @Alias("waifu")
    @Alias("play") @Alias("shuffle") @Alias("np")
    @Alias("skip") @Alias("stop") @Alias("ns")
    @Alias("volume") @Alias("forceplay") @Alias("lyrics")
    @Alias("playnow") @Alias("rewind") @Alias("forward")
    @Alias("restartsong") @Alias("removetrack") @Alias("move")
    @Alias("cast") @Alias("salvage") @Alias("repeat")
    @Alias("repair") @Alias("marry") @Alias("divorce")
    @Alias("mute") @Alias("unmute") @Alias("remindme")
    @Alias("game") @Alias("trivia") @Alias("love")
    @Category(CommandCategory.HIDDEN)
    public static class Slash extends TextCommand {
        // old, squished
        String[][] squishPairs = {
                {"bloodsuck", "action bloodsuck"},
                {"teehee", "action teehee"},
                {"nom", "action nom"},
                {"smile", "action smile"},
                {"facedesk", "action facedesk"},
                {"lewd", "action lewd"},
                {"bite", "action bite"},
                {"blush", "action blush"},
                {"stare", "action stare"},
                {"holdhands", "action holdhands"},
                {"nuzzle", "action nuzzle"},
                {"cat", "image cat"},
                {"dog", "image dog"},
                {"catgirl", "image catgirl"},
                {"cast", "cast item"},
                {"lang", "mantaro language"},
                {"invite", "mantaro invite"},
                {"support", "mantaro support"},
                {"donate", "mantaro donate"},
                {"shard", "mantaro shard"},
                {"shardinfo", "mantaro shardlist"},
                {"userinfo", "info user"},
                {"serverinfo", "info server"},
                {"roleinfo", "info role"},
                {"activatekey", "premium activate"},
                {"info", "stats"}
        };
        // Some commands had to be squished into subcommands.
        Map<String, String> squish = Utils.toMap(squishPairs);

        @Override
        protected void process(TextContext ctx) {
            I18nContext i18nContext = ctx.getLanguageContext();
            var builder = new EmbedBuilder();
            var squished = squish.get(name);
            builder.setAuthor(i18nContext.get("commands.slash.title"))
                    .setDescription(i18nContext.get("commands.slash.description")
                            .formatted(EmoteReference.WARNING, squished != null ? squished : name) + "\n" +
                            i18nContext.get("commands.slash.description_2")
                    )
                    .setColor(Color.PINK)
                    .setImage("https://apiv2.mantaro.site/image/common/slash-example.png")
                    .setFooter(i18nContext.get("commands.pet.status.footer"), ctx.getMember().getEffectiveAvatarUrl());

            ctx.send(builder.build());
        }
    }
}
