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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.custom.CustomCommandHandler;
import net.kodehawa.mantarobot.commands.custom.v3.Parser;
import net.kodehawa.mantarobot.commands.custom.v3.SyntaxException;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.*;
import net.kodehawa.mantarobot.core.command.processor.CommandProcessor;
import net.kodehawa.mantarobot.core.command.slash.IContext;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.listeners.operations.ModalOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.ModalOperation;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleTreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.CustomCommand;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.data.MantaroData.db;

@Module
public class CustomCmds {
    public final static Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");
    public final static Pattern INVALID_CHARACTERS_PATTERN = Pattern.compile("[^a-zA-Z0-9_]");

    private static final Map<String, CustomCommand> customCommands = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(CustomCmds.class);
    private static final SecureRandom random = new SecureRandom();
    //People spamming crap... we cant have nice things owo
    private static final IncreasingRateLimiter customRatelimiter = new IncreasingRateLimiter.Builder()
            .spamTolerance(2)
            .limit(1)
            .cooldown(4, TimeUnit.SECONDS)
            .cooldownPenaltyIncrease(4, TimeUnit.SECONDS)
            .maxCooldown(2, TimeUnit.MINUTES)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("custom")
            .build();
    //Just so this is in english.
    private static final I18nContext i18nTemp = new I18nContext();
    private static final Predicate<IContext> adminPredicate = (ctx) -> {
        if (db().getGuild(ctx.getGuild()).getData().isCustomAdminLockNew() && !CommandPermission.ADMIN.test(ctx.getMember())) {
            ctx.getChannel().sendMessage(i18nTemp.get("commands.custom.admin_only")).queue();
            return false;
        }

        return true;
    };

    public static void handle(String prefix, String cmdName, Context ctx, GuildData guildData, String args) {
        CustomCommand customCommand = getCustomCommand(ctx.getGuild().getId(), cmdName);
        if (customCommand == null) {
            return;
        }

        // !! CCS disable check start.
        if (guildData.getDisabledCommands().contains(cmdName)) {
            return;
        }

        List<String> channelDisabledCommands = guildData.getChannelSpecificDisabledCommands().get(ctx.getChannel().getId());
        if (channelDisabledCommands != null && channelDisabledCommands.contains(cmdName)) {
            return;
        }

        HashMap<String, List<String>> roleSpecificDisabledCommands = guildData.getRoleSpecificDisabledCommands();
        if (ctx.getMember().getRoles().stream().anyMatch(r -> roleSpecificDisabledCommands.computeIfAbsent(r.getId(), s -> new ArrayList<>()).contains(cmdName)) && !CommandPermission.ADMIN.test(ctx.getMember())) {
            return;
        }
        // !! CCS disable check end.

        // Create a new language context only if the command goes through.
        // This avoids getting a user everytime a command is ran, even if the command is not valid.
        ctx.setLanguageContext(new I18nContext(guildData, db().getUser(ctx.getAuthor()).getData()));

        // Run the actual custom command.
        List<String> values = customCommand.getValues();

        // what
        if (values.isEmpty()) {
            return;
        }

        if (customCommand.getData().isNsfw() && !ctx.isChannelNSFW()) {
            ctx.sendLocalized("commands.custom.nsfw_not_nsfw", EmoteReference.ERROR);
            return;
        }

        String response = values.get(random.nextInt(values.size()));
        try {
            new CustomCommandHandler(prefix, ctx, response, args).handle();
        } catch (SyntaxException e) {
            ctx.sendStrippedLocalized("commands.custom.error_running_new", EmoteReference.ERROR, e.getMessage());
        } catch (Exception e) {
            ctx.sendLocalized("commands.custom.error_running", EmoteReference.ERROR);
            e.printStackTrace();
        }
    }

    //Lazy-load custom commands into cache.
    public static CustomCommand getCustomCommand(String id, String name) {
        if (CommandProcessor.REGISTRY.commands().containsKey(name)) {
            return null;
        }

        if (customCommands.containsKey(id + ":" + name)) {
            return customCommands.get(id + ":" + name);
        }

        CustomCommand custom = db().getCustomCommand(id, name);
        if (custom == null)
            return null;

        if (!NAME_PATTERN.matcher(name).matches()) {
            String newName = INVALID_CHARACTERS_PATTERN.matcher(custom.getName()).replaceAll("_");
            log.info("Custom Command with Invalid Characters {} found. Replacing with '_'", custom.getName());

            custom.delete();
            custom = CustomCommand.of(custom.getGuildId(), newName, custom.getValues());
            custom.save();
        }

        if (CommandProcessor.REGISTRY.commands().containsKey(custom.getName())) {
            custom.delete();
            custom = CustomCommand.of(custom.getGuildId(), "_" + custom.getName(), custom.getValues());
            custom.save();
        }

        //add to registry
        customCommands.put(custom.getId(), custom);

        return custom;
    }

    @Subscribe
    public void registry(CommandRegistry cr) {
        cr.registerSlash(Custom.class);
    }

    @Description("Add, modify or list custom commands / tags.")
    @Category(CommandCategory.UTILS)
    @Help(description = """
            Manages the Custom Commands of the current server.
            If you wish to allow normal people to make custom commands, run `~>opts admincustom false`.
            Running the above isn't exactly recommended, but works for small servers.
            See subcommands for more commands, or refer to the [wiki](https://www.mantaro.site/mantaro-wiki/guides/custom-commands)
            """, usage = "`/custom [sub command]`")
    public static class Custom extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {}

        @Override
        public Predicate<SlashContext> getPredicate() {
            return ctx -> RatelimitUtils.ratelimit(customRatelimiter, ctx, null);
        }

        @Name("list")
        @Description("List all custom commands")
        public static class ListCustom extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                listCustoms(ctx);
            }
        }

        @Description("Views the response of a custom command.")
        @Options({
                @Options.Option(type = OptionType.STRING, name = "name", description = "The custom command to view", required = true),
                @Options.Option(type = OptionType.INTEGER, name = "response", description = "The response to view", required = true)
        })
        @Help(
                description = "Views the response of a custom command.",
                usage = "/custom view name:<name> response:[response num]",
                parameters = {
                        @Help.Parameter(name = "name", description = "The name of the custom command to view."),
                        @Help.Parameter(name = "response", description = "The response number of the response to view (shown on /custom raw)."),
                }
        )
        public static class View extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                viewCommands(ctx, ctx.getOptionAsString("name"), ctx.getOptionAsInteger("response") - 1);
            }
        }

        @Description("View a custom command in raw form.")
        @Options({
                @Options.Option(type = OptionType.STRING, name = "name", description = "The custom command to view.", required = true)
        })
        @Help(
                description = "View a custom command in raw form.",
                usage = "/custom raw name:<name>",
                parameters = {
                        @Help.Parameter(name = "name", description = "The name of the custom command to view.")
                }
        )
        public static class Raw extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                rawCommand(ctx, ctx.getOptionAsString("name"));
            }
        }

        @Description("Shows information about a custom command.")
        @Options({
                @Options.Option(type = OptionType.STRING, name = "name", description = "The custom command to view.", required = true)
        })
        @Help(
                description = "Shows information about a custom command.",
                usage = "/custom info name:<name>",
                parameters = {
                        @Help.Parameter(name = "name", description = "The name of the custom command to view info of.")
                }
        )
        public static class Info extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                infoCommand(ctx, ctx.getOptionAsString("name"));
            }
        }

        @Description("Rename a custom command.")
        @Options({
                @Options.Option(type = OptionType.STRING, name = "name", description = "The custom command to rename.", required = true),
                @Options.Option(type = OptionType.STRING, name = "new", description = "What to rename it to.", required = true)
        })
        @Help(
                description = "Shows information about a custom command.",
                usage = "/custom rename name:<name> new:<new name>",
                parameters = {
                        @Help.Parameter(name = "name", description = "The name of the custom command to rename."),
                        @Help.Parameter(name = "new", description = "The new name to use")
                }
        )
        public static class Rename extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                renameCmd(ctx, ctx.getOptionAsString("name"), ctx.getOptionAsString("new"));
            }
        }

        @Description("Lock a custom command.")
        @Options({
                @Options.Option(type = OptionType.STRING, name = "name", description = "The custom command to lock.", required = true)
        })
        public static class LockCommand extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                lockCmd(ctx, ctx.getOptionAsString("name"));
            }
        }

        @Description("Unlock a custom command.")
        @Options({
                @Options.Option(type = OptionType.STRING, name = "name", description = "The custom command to unlock.", required = true)
        })
        public static class UnlockCommand extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                unlockCmd(ctx, ctx.getOptionAsString("name"));
            }
        }

        @Description("Deletes a response from a custom command.")
        @Options({
                @Options.Option(type = OptionType.STRING, name = "name", description = "The custom command to remove a response from.", required = true),
                @Options.Option(type = OptionType.INTEGER, name = "response", description = "The response to remove.", required = true)
        })
        @Help(
                description = "Deletes a response from a custom command.",
                usage = "/custom deleteresponse name:<name> response:[response num]",
                parameters = {
                        @Help.Parameter(name = "name", description = "The name of the custom command to delete a response of."),
                        @Help.Parameter(name = "response", description = "The response number of the response to delete (shown on /custom raw)."),
                }
        )
        public static class DeleteResponse extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                deleteResponseCmd(ctx, ctx.getOptionAsString("name"), ctx.getOptionAsInteger("response"));
            }
        }

        @Description("Remove a custom command")
        @Options({
                @Options.Option(type = OptionType.STRING, name = "name", description = "The custom command to remove.", required = true)
        })
        public static class Remove extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                removeCmd(ctx, ctx.getOptionAsString("name"));
            }
        }

        @Description("Add a custom command. This will open a pop-up.")
        @Options(@Options.Option(type = OptionType.BOOLEAN, name = "nsfw", description = "Whether the command is NSFW or not.", required = true))
        @Help(description = "Add a custom command. This will open a pop-up. The pop-up will time out in 5 minutes.")
        public static class Add extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                if (!adminPredicate.test(ctx)) {
                    return;
                }

                var lang = ctx.getLanguageContext();
                var subject = TextInput.create("content", lang.get("commands.custom.add.content_slash"), TextInputStyle.PARAGRAPH)
                        .setPlaceholder(lang.get("commands.custom.add.content_placeholder"))
                        .setRequiredRange(5, 3900)
                        .build();

                var nameInput = TextInput.create("name", lang.get("commands.custom.add.name_slash"), TextInputStyle.SHORT)
                        .setPlaceholder(lang.get("commands.custom.add.custom_name_placeholder"))
                        .setRequiredRange(2, 49)
                        .build();

                var nsfw = ctx.getOptionAsBoolean("nsfw");
                var id = "%s/%s".formatted(ctx.getAuthor().getId(), ctx.getChannel().getId());
                var modal = Modal.create(id, lang.get("commands.custom.add.header_slash")).addActionRows(ActionRow.of(nameInput), ActionRow.of(subject)).build();
                ctx.replyModal(modal);

                ModalOperations.create(id, 300, new ModalOperation() {
                    @Override
                    public int modal(ModalInteractionEvent event) {
                        // This might not be possible here, as we send only events based on the id.
                        if (!event.getModalId().equalsIgnoreCase(id)) {
                            return Operation.IGNORED;
                        }

                        if (event.getUser().getIdLong() != ctx.getAuthor().getIdLong()) {
                            return Operation.IGNORED;
                        }

                        var contentRaw = event.getValue("content");
                        if (contentRaw == null) {
                            event.reply(lang.get("commands.custom.add.empty_content").formatted(EmoteReference.ERROR))
                                    .setEphemeral(true)
                                    .queue();
                            return Operation.COMPLETED;
                        }

                        var content = contentRaw.getAsString().trim();
                        if (content.isBlank()) {
                            event.reply(lang.get("commands.custom.add.empty_content").formatted(EmoteReference.ERROR))
                                    .setEphemeral(true)
                                    .queue();
                            return Operation.COMPLETED;
                        }

                        if (content.length() > 3900) {
                            event.reply(lang.get("commands.custom.add.too_long").formatted(EmoteReference.ERROR, 3900))
                                    .setEphemeral(true)
                                    .queue();
                            return Operation.COMPLETED;
                        }

                        var nameRaw = event.getValue("name");
                        if (nameRaw == null) {
                            event.reply(lang.get("commands.custom.add.not_enough_args").formatted(EmoteReference.ERROR))
                                    .setEphemeral(true)
                                    .queue();
                            return Operation.COMPLETED;
                        }

                        var name = nameRaw.getAsString().trim();
                        if (name.isBlank()) {
                            event.reply(lang.get("commands.custom.add.not_enough_args").formatted(EmoteReference.ERROR))
                                    .setEphemeral(true)
                                    .queue();
                            return Operation.COMPLETED;
                        }

                        if (!NAME_PATTERN.matcher(name).matches()) {
                            event.reply(lang.get("commands.custom.character_not_allowed").formatted(EmoteReference.ERROR))
                                    .setEphemeral(true)
                                    .queue();
                            return Operation.COMPLETED;
                        }

                        if (name.length() >= 50) {
                            event.reply(lang.get("commands.custom.name_too_long").formatted(EmoteReference.ERROR))
                                    .setEphemeral(true)
                                    .queue();
                            return Operation.COMPLETED;
                        }

                        if (CommandProcessor.REGISTRY.commands().containsKey(name)) {
                            event.reply(lang.get("commands.custom.already_exists").formatted(EmoteReference.ERROR, name))
                                    .setEphemeral(true)
                                    .queue();
                            return Operation.COMPLETED;
                        }

                        content = content.replace("@everyone", "[nice meme]").replace("@here", "[you tried]");
                        if (content.contains("v3:")) {
                            try {
                                new Parser(content).parse();
                            } catch (SyntaxException e) {
                                event.reply(lang.get("commands.custom.new_error").formatted(EmoteReference.ERROR, e.getMessage()))
                                        .setEphemeral(true)
                                        .queue();
                                return Operation.COMPLETED;
                            }
                        }

                        var custom = CustomCommand.of(ctx.getGuild().getId(), name, Collections.singletonList(content));
                        var c = ctx.db().getCustomCommand(ctx.getGuild(), name);
                        if (c != null) {
                            if (custom.getData().isLocked()) {
                                event.reply(lang.get("commands.custom.locked_command").formatted(EmoteReference.ERROR2))
                                        .setEphemeral(true)
                                        .queue();
                                return Operation.COMPLETED;
                            }

                            final var values = c.getValues();
                            var customLimit = 50;
                            if (ctx.getConfig().isPremiumBot() || ctx.getDBGuild().isPremium()) {
                                customLimit = 100;
                            }

                            if (values.size() > customLimit) {
                                event.reply(lang.get("commands.custom.add.too_many_responses").formatted(EmoteReference.ERROR2, values.size()))
                                        .setEphemeral(true)
                                        .queue();
                                return Operation.COMPLETED;
                            }

                            custom.getValues().addAll(values);
                        } else {
                            // Are the first two checks redundant?
                            if (!ctx.getConfig().isPremiumBot() && !ctx.getDBGuild().isPremium() && ctx.db().getCustomCommands(ctx.getGuild()).size() > 100) {
                                event.reply(lang.get("commands.custom.add.too_many_commands").formatted(EmoteReference.ERROR2))
                                        .setEphemeral(true)
                                        .queue();
                                return Operation.COMPLETED;
                            }
                        }

                        custom.getData().setOwner(ctx.getAuthor().getId());
                        if (nsfw) {
                            custom.getData().setNsfw(true);
                        }

                        // save at DB
                        custom.save();
                        // reflect at local
                        customCommands.put(custom.getId(), custom);

                        event.reply(lang.get("commands.custom.add.success").formatted(EmoteReference.CORRECT, name))
                                .queue();

                        return Operation.COMPLETED;
                    }

                    @Override
                    public void onExpire() {
                        ModalOperation.super.onExpire();
                        ctx.getEvent().getHook()
                                .sendMessage(lang.get("commands.custom.add.time_out").formatted(EmoteReference.ERROR2))
                                .setEphemeral(true)
                                .queue();
                    }
                });
            }
        }

        @Description("Edits a custom command. This will open a pop-up for content.")
        @Options({
                @Options.Option(type = OptionType.STRING, name = "name", description = "The custom command to edit.", required = true),
                @Options.Option(type = OptionType.INTEGER, name = "response", description = "The response number to edit.", required = true),
                @Options.Option(type = OptionType.BOOLEAN, name = "nsfw", description = "Whether the command is NSFW or not.", required = true)
        })
        @Help(
                description = "Deletes a response from a custom command.",
                usage = "/custom edit name:<name> response:[response num] content:[new response content] nsfw:[true/false]",
                parameters = {
                        @Help.Parameter(name = "name", description = "The name of the custom command to edit."),
                        @Help.Parameter(name = "response", description = "The response number of the response to edit."),
                        @Help.Parameter(name = "nsfw", description = "Whether the entire command should be marked as nsfw."),
                }
        )
        public static class Edit extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                if (!adminPredicate.test(ctx)) {
                    return;
                }

                var lang = ctx.getLanguageContext();
                var subject = TextInput.create("content", lang.get("commands.custom.edit.content_slash"), TextInputStyle.PARAGRAPH)
                        .setPlaceholder(lang.get("commands.custom.edit.content_slash_placeholder"))
                        .setRequiredRange(5, 3900)
                        .build();

                var name = ctx.getOptionAsString("name");
                var where = ctx.getOptionAsInteger("response");
                var nsfw = ctx.getOptionAsBoolean("nsfw");
                var id = "%s/%s".formatted(ctx.getAuthor().getId(), ctx.getChannel().getId());
                var modal = Modal.create(id, lang.get("commands.custom.edit.header_slash")).addActionRows(ActionRow.of(subject)).build();
                ctx.replyModal(modal);

                ModalOperations.create(id, 180, new ModalOperation() {
                    @Override
                    public int modal(ModalInteractionEvent event) {
                        // This might not be possible here, as we send only events based on the id.
                        if (!event.getModalId().equalsIgnoreCase(id)) {
                            return Operation.IGNORED;
                        }

                        if (event.getUser().getIdLong() != ctx.getAuthor().getIdLong()) {
                            return Operation.IGNORED;
                        }

                        var contentRaw = event.getValue("content");
                        if (contentRaw == null) {
                            event.reply(lang.get("commands.custom.edit.empty_response").formatted(EmoteReference.ERROR))
                                    .setEphemeral(true)
                                    .queue();
                            return Operation.COMPLETED;
                        }

                        var commandContent = contentRaw.getAsString().trim();
                        if (commandContent.isBlank()) {
                            event.reply(lang.get("commands.custom.edit.empty_response").formatted(EmoteReference.ERROR))
                                    .setEphemeral(true)
                                    .queue();
                            return Operation.COMPLETED;
                        }

                        var custom = ctx.db().getCustomCommand(ctx.getGuild(), name);
                        if (custom == null) {
                            event.reply(lang.get("commands.custom.not_found").formatted(EmoteReference.ERROR2, name))
                                    .setEphemeral(true)
                                    .queue();
                            return Operation.COMPLETED;
                        }

                        if (custom.getData().isLocked()) {
                            event.reply(lang.get("commands.custom.locked_command").formatted(EmoteReference.ERROR))
                                    .setEphemeral(true)
                                    .queue();
                            return Operation.COMPLETED;
                        }

                        var values = custom.getValues();
                        if (where - 1 > values.size()) {
                            event.reply(lang.get("commands.custom.edit.no_index").formatted(EmoteReference.ERROR))
                                    .setEphemeral(true)
                                    .queue();
                            return Operation.COMPLETED;
                        }

                        if (commandContent.isEmpty()) {
                            event.reply(lang.get("commands.custom.edit.empty_response").formatted(EmoteReference.ERROR))
                                    .setEphemeral(true)
                                    .queue();
                            return Operation.COMPLETED;
                        }

                        if (commandContent.length() > 3900) {
                            event.reply(lang.get("commands.custom.add.too_long").formatted(EmoteReference.ERROR, 3900))
                                    .setEphemeral(true)
                                    .queue();
                            return Operation.COMPLETED;
                        }

                        if (nsfw) {
                            custom.getData().setNsfw(true);
                        }

                        custom.getValues().set(where - 1, commandContent);
                        custom.saveAsync();
                        customCommands.put(custom.getId(), custom);
                        event.reply(lang.get("commands.custom.edit.success").formatted(EmoteReference.CORRECT, where, custom.getName())).queue();
                        return Operation.COMPLETED;
                    }

                    @Override
                    public void onExpire() {
                        ModalOperation.super.onExpire();
                        ctx.getEvent().getHook()
                                .sendMessage(lang.get("commands.custom.add.time_out").formatted(EmoteReference.ERROR2))
                                .setEphemeral(true)
                                .queue();
                    }
                });
            }
        }

        // TODO: either port fully or add warning to use eval on prefix commands only for now.
        @Description("Eval the result of a custom command.")
        public static class Eval extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                ctx.reply("commands.custom.eval.slash_notice", EmoteReference.CORRECT);
            }
        }
    }

    @Subscribe
    public void custom(CommandRegistry cr) {
        SimpleTreeCommand customCommand = cr.register("custom", new SimpleTreeCommand(CommandCategory.UTILS) {
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription(
                                """
                                Manages the Custom Commands of the current server.
                                If you wish to allow normal people to make custom commands, run `~>opts admincustom false`.
                                Running the above isn't exactly recommended, but works for small servers.
                                See subcommands for more commands, or refer to the [wiki](https://www.mantaro.site/mantaro-wiki/guides/custom-commands)
                                """
                        )
                        .setUsage("`~>custom <sub command>`")
                        .build();
            }
        });

        customCommand.setPredicate(ctx ->
                RatelimitUtils.ratelimit(customRatelimiter, ctx, null)
        );

        customCommand.addSubCommand("list", new SubCommand() {
            @Override
            public String description() {
                return "Lists all the current commands on this server.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                listCustoms(ctx);
            }
        }).createSubCommandAlias("list", "ls");

        customCommand.addSubCommand("view", new SubCommand() {
            @Override
            public String description() {
                return "Views the response of an specific command.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var args = StringUtils.splitArgs(content, 2);
                if (args.length < 2) {
                    ctx.sendLocalized("commands.custom.view.not_found", EmoteReference.ERROR);
                    return;
                }

                var cmd = args[0];
                int number;
                try {
                    number = Integer.parseInt(args[1]) - 1;
                } catch (NumberFormatException e) {
                    ctx.sendLocalized("general.invalid_number", EmoteReference.ERROR);
                    return;
                }

                viewCommands(ctx, cmd, number);
            }
        }).createSubCommandAlias("view", "vw");

        customCommand.addSubCommand("raw", new SubCommand() {
            @Override
            public String description() {
                return "Show all the raw responses of the specified command.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                String command = content.trim();
                if (command.isEmpty()) {
                    ctx.sendLocalized("commands.custom.raw.no_command", EmoteReference.ERROR);
                    return;
                }

                rawCommand(ctx, command);
            }
        }).createSubCommandAlias("raw", "rw");

        customCommand.addSubCommand("clear", new SubCommand() {
            @Override
            public String description() {
                return "Clear all custom commands.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                clearCommand(ctx);
            }
        }).createSubCommandAlias("clear", "clr");

        customCommand.addSubCommand("eval", new SubCommand() {
            @Override
            public String description() {
                return "Evaluates the result of a custom command.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                if (!adminPredicate.test(ctx)) {
                    return;
                }

                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.custom.eval.not_specified", EmoteReference.ERROR);
                    return;
                }

                try {
                    var ctn = content;
                    ctn = Utils.DISCORD_INVITE.matcher(ctn).replaceAll("-invite link-");
                    ctn = Utils.DISCORD_INVITE_2.matcher(ctn).replaceAll("-invite link-");

                    //Sadly no way to get the prefix used, so eval will have the old bug still.
                    // TODO: CANNOT PORT TO SLASH: somehow requires event from the old Context to function.
                    // THIS HAS TO CHANGE.
                    new CustomCommandHandler("", ctx, ctn).handle(true);
                } catch (SyntaxException e) {
                    ctx.sendStrippedLocalized("commands.custom.eval.new_error", EmoteReference.ERROR, e.getMessage());
                } catch (Exception e) {
                    ctx.sendStrippedLocalized("commands.custom.eval.error",
                            EmoteReference.ERROR, e.getMessage() == null ? "" : " (E: " + e.getMessage() + ")"
                    );
                }
            }
        }).createSubCommandAlias("eval", "evl");

        customCommand.addSubCommand("remove", new SubCommand() {
            @Override
            public String description() {
                return "Removes a custom command.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.custom.remove.no_command", EmoteReference.ERROR);
                    return;
                }

                removeCmd(ctx, content);
            }
        }).createSubCommandAlias("remove", "rm");

        customCommand.addSubCommand("info", new SubCommand() {
            @Override
            public String description() {
                return "Shows the information about an specific command.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.custom.raw.no_command", EmoteReference.ERROR);
                    return;
                }

                infoCommand(ctx, content);
            }
        });

        customCommand.addSubCommand("edit", new SubCommand() {
            @Override
            public String description() {
                return "Edits the response of a command.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.custom.edit.no_command", EmoteReference.ERROR);
                    return;
                }

                var opts = ctx.getOptionalArguments();
                var ctn = Utils.replaceArguments(opts, content, "nsfw");
                var nsfw = opts.containsKey("nsfw");
                var args = StringUtils.splitArgs(ctn, -1);
                if (args.length < 2) {
                    ctx.sendLocalized("commands.custom.edit.not_enough_args", EmoteReference.ERROR);
                    return;
                }

                var cmd = args[0];
                int where;
                var index = args[1];
                //replace first occurrence and second argument: custom command and index.
                var commandContent = ctn.replaceFirst(cmd, "").replaceFirst(index, "").trim();
                try {
                    where = Math.abs(Integer.parseInt(index));
                } catch (NumberFormatException e) {
                    ctx.sendLocalized("commands.custom.edit.invalid_number", EmoteReference.ERROR);
                    return;
                }

                editCmd(ctx, cmd, where, commandContent, nsfw);
            }
        });

        customCommand.addSubCommand("deleteresponse", new SubCommand() {
            @Override
            public String description() {
                return "Deletes a response of a command.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.custom.deleteresponse.no_command", EmoteReference.ERROR);
                    return;
                }

                var args = StringUtils.splitArgs(content, -1);
                if (args.length < 2) {
                    ctx.sendLocalized("commands.custom.deleteresponse.not_enough_args", EmoteReference.ERROR);
                    return;
                }

                int where;
                var index = args[1];
                try {
                    where = Math.abs(Integer.parseInt(index));
                } catch (NumberFormatException e) {
                    ctx.sendLocalized("commands.custom.deleteresponse.invalid_number", EmoteReference.ERROR);
                    return;
                }

                deleteResponseCmd(ctx, args[0], where);
            }
        }).createSubCommandAlias("deleteresponse", "dlr");

        customCommand.addSubCommand("lockcommand", new SubCommand() {
            @Override
            public String description() {
                return "Looks a command for further edits until unlocked.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                lockCmd(ctx, content);
            }
        });

        customCommand.addSubCommand("unlockcommand", new SubCommand() {
            @Override
            public String description() {
                return "Unlocks a command to do further edits.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                unlockCmd(ctx, content);
            }
        });

        customCommand.addSubCommand("rename", new SubCommand() {
            @Override
            public String description() {
                return "Renames a custom command.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                if (!adminPredicate.test(ctx)) {
                    return;
                }

                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.custom.rename.no_command", EmoteReference.ERROR);
                    return;
                }

                var args = ctx.getArguments();
                if (args.length < 2) {
                    ctx.sendLocalized("commands.custom.rename.not_enough_args", EmoteReference.ERROR);
                    return;
                }

                var cmd = args[0];
                var value = args[1];
                renameCmd(ctx, cmd, value);
            }
        }).createSubCommandAlias("rename", "rn");

        customCommand.addSubCommand("add", new SubCommand() {
            @Override
            public String description() {
                return "Adds a new custom commands or adds a response to an existing command.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.custom.add.no_command", EmoteReference.ERROR);
                    return;
                }

                var args = StringUtils.splitArgs(content, -1);

                if (args.length < 2) {
                    ctx.sendLocalized("commands.custom.add.not_enough_args", EmoteReference.ERROR);
                    return;
                }

                var cmd = args[0];
                if (!NAME_PATTERN.matcher(cmd).matches()) {
                    ctx.sendLocalized("commands.custom.character_not_allowed", EmoteReference.ERROR);
                    return;
                }

                var value = content.replaceFirst(args[0], "").trim();
                var opts = ctx.getOptionalArguments();
                var cmdSource = Utils.replaceArguments(opts, value, "nsfw");
                var nsfw = opts.containsKey("nsfw");
                addCmd(ctx, cmd, cmdSource, nsfw);
            }
        }).createSubCommandAlias("add", "new");
    }

    private static void listCustoms(IContext ctx) {
        if (!ctx.getGuild().getSelfMember().hasPermission(ctx.getChannel(), Permission.MESSAGE_EMBED_LINKS) && ctx instanceof Context) {
            ctx.sendLocalized("general.missing_embed_permissions");
            return;
        }

        var commands = ctx.db().getCustomCommands(ctx.getGuild())
                .stream()
                .map(CustomCommand::getName)
                .toList();

        var description = ctx.getLanguageContext().get("general.dust");
        if (!commands.isEmpty()) {
            description = ctx.getLanguageContext().get("commands.custom.ls.description") + "\n";
            description += commands.stream().map(cc -> "*`" + cc + "`*").collect(Collectors.joining(", "));
        }

        var cmds = DiscordUtils.divideString(900, ',', description);
        var builder = new EmbedBuilder()
                .setAuthor(ctx.getLanguageContext().get("commands.custom.ls.header"), null, ctx.getGuild().getIconUrl())
                .setColor(ctx.getMember().getColor())
                .setThumbnail("https://i.imgur.com/glP3VKI.png")
                .setFooter(ctx.getLanguageContext().get("commands.custom.ls.footer").formatted(commands.size()),
                        ctx.getAuthor().getEffectiveAvatarUrl()
                );

        DiscordUtils.listButtons(ctx.getUtilsContext(), 120, 900,
                (p, total) -> builder.setFooter(String.format("Commands: %,d | Total Pages: %s | Current: %s", commands.size(), total, p)), cmds
        );
    }

    private static void viewCommands(IContext ctx, String cmd, int number) {
        var command = ctx.db().getCustomCommand(ctx.getGuild(), cmd);
        if (command == null) {
            ctx.sendLocalized("commands.custom.view.not_found", EmoteReference.ERROR);
            return;
        }

        if (command.getValues().size() < number) {
            ctx.sendLocalized("commands.custom.view.less_than_specified", EmoteReference.ERROR);
            return;
        }

        ctx.sendLocalized("commands.custom.view.success", (number + 1), command.getName(), command.getValues().get(number));
    }

    private static void rawCommand(IContext ctx, String command) {
        var custom = ctx.db().getCustomCommand(ctx.getGuild(), command);
        if (custom == null) {
            ctx.sendLocalized("commands.custom.not_found", EmoteReference.ERROR2, command);
            return;
        }

        List<MessageEmbed.Field> fields = new ArrayList<>();
        var count = new AtomicInteger();
        for (var value : custom.getValues()) {
            var val = value;
            var current = count.incrementAndGet();
            if (value.length() > 900) {
                val = ctx.getLanguageContext().get("commands.custom.raw.too_large_view").formatted(custom.getName(), current);
            }

            fields.add(new MessageEmbed.Field("Response N° " + current, val, false));
        }

        var embed = ctx.baseEmbed(
                    ctx, ctx.getLanguageContext().get("commands.custom.raw.header").formatted(command), ctx.getAuthor().getEffectiveAvatarUrl()
                ).setDescription(ctx.getLanguageContext().get("commands.custom.raw.description"))
                .setFooter(ctx.getLanguageContext().get("commands.custom.raw.amount")
                                .formatted(count.get(), custom.getValues().size()),
                        ctx.getGuild().getIconUrl()
                );

        DiscordUtils.sendPaginatedEmbed(ctx.getUtilsContext(), embed, DiscordUtils.divideFields(6, fields));
    }

    private static void infoCommand(IContext ctx, String cmd) {
        var command = ctx.db().getCustomCommand(ctx.getGuild(), cmd);
        if (command == null) {
            ctx.sendLocalized("commands.custom.raw.not_found", EmoteReference.ERROR);
            return;
        }

        var owner = command.getData().getOwner();
        var member = owner.isEmpty() ? null : ctx.getGuild().retrieveMemberById(owner).useCache(true).complete();
        ctx.send(new EmbedBuilder()
                .setAuthor("Custom Command Information for " + cmd, null, ctx.getAuthor().getEffectiveAvatarUrl())
                .setDescription(
                        EmoteReference.BLUE_SMALL_MARKER +
                                "**Owner:** " + (member == null ? "Nobody" : member.getUser().getAsTag()) + "\n" +
                                EmoteReference.BLUE_SMALL_MARKER +
                                "**Owner ID:** " + (member == null ? "None" : member.getId()) + "\n" +
                                EmoteReference.BLUE_SMALL_MARKER +
                                "**NSFW:** " + command.getData().isNsfw() + "\n" +
                                EmoteReference.BLUE_SMALL_MARKER +
                                "**Responses:** " + command.getValues().size() + "\n"
                )
                .setThumbnail("https://i.imgur.com/jPL5Lof.png")
                .build()
        );
    }

    private static void clearCommand(IContext ctx) {
        if (!adminPredicate.test(ctx)) {
            return;
        }

        // TODO: add confirmation dialog
        if (!ctx.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            return;
        }

        var customCommands = ctx.db().getCustomCommands(ctx.getGuild());
        if (customCommands.isEmpty()) {
            ctx.sendLocalized("commands.custom.no_cc", EmoteReference.ERROR);
            return;
        }

        int size = customCommands.size();
        customCommands.stream().filter(cmd -> !cmd.getData().isLocked()).forEach(CustomCommand::deleteAsync);
        customCommands.forEach(c -> CustomCmds.customCommands.remove(c.getId()));
        ctx.sendLocalized("commands.custom.clear.success", EmoteReference.PENCIL, size);
    }

    public static void renameCmd(IContext ctx, String cmd, String value) {
        if (!adminPredicate.test(ctx)) {
            return;
        }

        if (!NAME_PATTERN.matcher(cmd).matches() || !NAME_PATTERN.matcher(value).matches()) {
            ctx.sendLocalized("commands.custom.character_not_allowed", EmoteReference.ERROR);
            return;
        }

        if (cmd.length() >= 50) {
            ctx.sendLocalized("commands.custom.name_too_long", EmoteReference.ERROR);
            return;
        }

        if (CommandProcessor.REGISTRY.commands().containsKey(value)) {
            ctx.sendLocalized("commands.custom.already_exists", EmoteReference.ERROR);
            return;
        }

        var oldCustom = ctx.db().getCustomCommand(ctx.getGuild(), cmd);
        if (oldCustom == null) {
            ctx.sendLocalized("commands.custom.not_found", EmoteReference.ERROR2, cmd);
            return;
        }

        final var newCustom = CustomCommand.of(ctx.getGuild().getId(), value, oldCustom.getValues());
        final var oldCustomData = oldCustom.getData();
        newCustom.getData().setNsfw(oldCustomData.isNsfw());
        newCustom.getData().setOwner(oldCustomData.getOwner());

        //change at DB
        oldCustom.deleteAsync();
        newCustom.saveAsync();

        //reflect at local
        customCommands.remove(oldCustom.getId());
        customCommands.put(newCustom.getId(), newCustom);

        //clear commands if none
        if (customCommands.keySet().stream().noneMatch(s -> s.endsWith(":" + cmd)))
            customCommands.remove(cmd);

        ctx.sendLocalized("commands.custom.rename.success", EmoteReference.CORRECT, cmd, value);

        //easter egg :D
        TextChannelGround.of(ctx.getChannel()).dropItemWithChance(8, 2);
    }

    public static void lockCmd(IContext ctx, String name) {
        if (!adminPredicate.test(ctx)) {
            return;
        }

        if (!ctx.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            ctx.sendLocalized("commands.custom.lockcommand.no_permission", EmoteReference.ERROR);
            return;
        }

        if (name.isEmpty()) {
            ctx.sendLocalized("commands.custom.lockcommand.no_command", EmoteReference.ERROR);
            return;
        }

        var cmd = ctx.db().getCustomCommand(ctx.getGuild(), name);
        if (cmd == null) {
            ctx.sendLocalized("commands.custom.not_found", EmoteReference.ERROR, name);
            return;
        }

        cmd.getData().setLocked(true);
        cmd.saveUpdating();

        ctx.sendLocalized("commands.custom.lockcommand.success", EmoteReference.CORRECT, name);
    }

    public static void unlockCmd(IContext ctx, String name) {
        if (!adminPredicate.test(ctx)) {
            return;
        }

        if (!ctx.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            ctx.sendLocalized("commands.custom.lockcommand.no_permission", EmoteReference.ERROR);
            return;
        }

        if (name.isEmpty()) {
            ctx.sendLocalized("commands.custom.lockcommand.no_command", EmoteReference.ERROR);
            return;
        }

        var cmd = ctx.db().getCustomCommand(ctx.getGuild(), name);
        if (cmd == null) {
            ctx.sendLocalized("commands.custom.not_found", EmoteReference.ERROR, name);
            return;
        }

        var data = cmd.getData();
        if (!data.isLocked()) {
            ctx.sendLocalized("commands.custom.unlockcommand.not_locked", EmoteReference.ERROR, name);
            return;
        }

        data.setLocked(false);
        cmd.saveUpdating();

        ctx.sendLocalized("commands.custom.unlockcommand.success", EmoteReference.CORRECT, name);
    }

    public static void deleteResponseCmd(IContext ctx, String name, int where) {
        if (!adminPredicate.test(ctx)) {
            return;
        }

        var custom = ctx.db().getCustomCommand(ctx.getGuild(), name);
        if (custom == null) {
            ctx.sendLocalized("commands.custom.not_found", EmoteReference.ERROR2, name);
            return;
        }

        if (custom.getData().isLocked()) {
            ctx.sendLocalized("commands.custom.locked_command", EmoteReference.ERROR2);
            return;
        }


        var values = custom.getValues();
        if (where - 1 > values.size()) {
            ctx.sendLocalized("commands.custom.deleteresponse.no_index", EmoteReference.ERROR);
            return;
        }

        custom.getValues().remove(where - 1);
        if (custom.getValues().isEmpty()) {
            custom.delete();
            ctx.sendLocalized("commands.custom.deleteresponse.no_responses_left", EmoteReference.CORRECT);
            return;
        }

        custom.saveAsync();
        customCommands.put(custom.getId(), custom);
        ctx.sendLocalized("commands.custom.deleteresponse.success", EmoteReference.CORRECT, where, custom.getName());
    }

    public static void removeCmd(IContext ctx, String content) {
        if (!adminPredicate.test(ctx)) {
            return;
        }

        if (!NAME_PATTERN.matcher(content).matches()) {
            ctx.sendLocalized("commands.custom.character_not_allowed", EmoteReference.ERROR);
            return;
        }

        //hint: always check for this
        if (CommandProcessor.REGISTRY.commands().containsKey(content)) {
            ctx.sendLocalized("commands.custom.already_exists", EmoteReference.ERROR, content);
            return;
        }

        CustomCommand custom = getCustomCommand(ctx.getGuild().getId(), content);
        if (custom == null) {
            ctx.sendLocalized("commands.custom.not_found", EmoteReference.ERROR2, content);
            return;
        }

        if (custom.getData().isLocked()) {
            ctx.sendLocalized("commands.custom.locked_command", EmoteReference.ERROR2);
            return;
        }

        //delete at DB
        custom.deleteAsync();

        //reflect at local
        customCommands.remove(custom.getId());

        //clear commands if none
        if (customCommands.keySet().stream().noneMatch(s -> s.endsWith(":" + content))) {
            customCommands.remove(content);
        }

        ctx.sendLocalized("commands.custom.remove.success", EmoteReference.PENCIL, content);
    }

    public static void addCmd(IContext ctx, String name, String content, boolean nsfw) {
        if (!adminPredicate.test(ctx)) {
            return;
        }

        if (content.isEmpty()) {
            ctx.sendLocalized("commands.custom.add.empty_content", EmoteReference.ERROR);
            return;
        }

        if (content.length() > Message.MAX_CONTENT_LENGTH - 100) {
            ctx.sendLocalized("commands.custom.add.too_long", EmoteReference.ERROR, Message.MAX_CONTENT_LENGTH - 100);
            return;
        }

        if (name.length() >= 50) {
            ctx.sendLocalized("commands.custom.name_too_long", EmoteReference.ERROR);
            return;
        }

        if (CommandProcessor.REGISTRY.commands().containsKey(name)) {
            ctx.sendLocalized("commands.custom.already_exists", EmoteReference.ERROR, name);
            return;
        }

        content = content.replace("@everyone", "[nice meme]").replace("@here", "[you tried]");
        if (content.contains("v3:")) {
            try {
                new Parser(content).parse();
            } catch (SyntaxException e) {
                ctx.sendLocalized("commands.custom.new_error", EmoteReference.ERROR, e.getMessage());
                return;
            }
        }

        var custom = CustomCommand.of(ctx.getGuild().getId(), name, Collections.singletonList(content));
        var c = ctx.db().getCustomCommand(ctx.getGuild(), name);
        if (c != null) {
            if (custom.getData().isLocked()) {
                ctx.sendLocalized("commands.custom.locked_command", EmoteReference.ERROR2);
                return;
            }

            final var values = c.getValues();
            var customLimit = 50;
            if (ctx.getConfig().isPremiumBot() || ctx.getDBGuild().isPremium()) {
                customLimit = 100;
            }

            if (values.size() > customLimit) {
                ctx.sendLocalized("commands.custom.add.too_many_responses", EmoteReference.ERROR, values.size());
                return;
            }

            custom.getValues().addAll(values);
        } else {
            // Are the first two checks redundant?
            if (!ctx.getConfig().isPremiumBot() && !ctx.getDBGuild().isPremium() && ctx.db().getCustomCommands(ctx.getGuild()).size() > 100) {
                ctx.sendLocalized("commands.custom.add.too_many_commands", EmoteReference.ERROR);
                return;
            }
        }


        custom.getData().setOwner(ctx.getAuthor().getId());
        if (nsfw) {
            custom.getData().setNsfw(true);
        }

        //save at DB
        custom.save();
        //reflect at local
        customCommands.put(custom.getId(), custom);
        ctx.sendLocalized("commands.custom.add.success", EmoteReference.CORRECT, name);

        //easter egg :D
        TextChannelGround.of(ctx.getChannel()).dropItemWithChance(8, 2);
    }

    public static void editCmd(IContext ctx, String name, int where, String commandContent, boolean nsfw) {
        if (!adminPredicate.test(ctx)) {
            return;
        }

        var custom = ctx.db().getCustomCommand(ctx.getGuild(), name);
        if (custom == null) {
            ctx.sendLocalized("commands.custom.not_found", EmoteReference.ERROR2, name);
            return;
        }

        if (custom.getData().isLocked()) {
            ctx.sendLocalized("commands.custom.locked_command", EmoteReference.ERROR2);
            return;
        }

        var values = custom.getValues();
        if (where - 1 > values.size()) {
            ctx.sendLocalized("commands.custom.edit.no_index", EmoteReference.ERROR);
            return;
        }

        if (commandContent.isEmpty()) {
            ctx.sendLocalized("commands.custom.edit.empty_response", EmoteReference.ERROR);
            return;
        }

        if (commandContent.length() > Message.MAX_CONTENT_LENGTH - 100) {
            ctx.sendLocalized("commands.custom.add.too_long", EmoteReference.ERROR, Message.MAX_CONTENT_LENGTH - 100);
            return;
        }

        if (nsfw) {
            custom.getData().setNsfw(true);
        }

        custom.getValues().set(where - 1, commandContent);
        custom.saveAsync();
        customCommands.put(custom.getId(), custom);
        ctx.sendLocalized("commands.custom.edit.success", EmoteReference.CORRECT, where, custom.getName());
    }
}
