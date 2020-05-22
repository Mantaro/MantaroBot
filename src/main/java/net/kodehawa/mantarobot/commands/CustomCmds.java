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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.custom.CustomCommandHandler;
import net.kodehawa.mantarobot.commands.custom.v3.Parser;
import net.kodehawa.mantarobot.commands.custom.v3.SyntaxException;
import net.kodehawa.mantarobot.commands.info.stats.manager.CommandStatsManager;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleTreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.core.processor.DefaultCommandProcessor;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.CustomCommand;
import net.kodehawa.mantarobot.db.entities.helpers.CustomCommandData;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;
import org.apache.commons.lang3.tuple.Pair;
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
@SuppressWarnings("unused")
public class CustomCmds {
    public final static Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+"),
            INVALID_CHARACTERS_PATTERN = Pattern.compile("[^a-zA-Z0-9_]"),
            NAME_WILDCARD_PATTERN = Pattern.compile("[a-zA-Z0-9_*]+");
    private static final Map<String, CustomCommand> customCommands = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(CustomCmds.class);
    private static final SecureRandom random = new SecureRandom();


    public static boolean handle(String prefix, String cmdName, Context ctx, String args) {
        CustomCommand customCommand = getCustomCommand(ctx.getGuild().getId(), cmdName);
        GuildData guildData = ctx.getDBGuild().getData();

        if (customCommand == null)
            return false;

        //CCS disable check start.
        if (guildData.getDisabledCommands().contains(cmdName)) {
            return false;
        }

        List<String> channelDisabledCommands = guildData.getChannelSpecificDisabledCommands().get(ctx.getChannel().getId());
        if (channelDisabledCommands != null && channelDisabledCommands.contains(cmdName)) {
            return false;
        }

        HashMap<String, List<String>> roleSpecificDisabledCommands = guildData.getRoleSpecificDisabledCommands();
        if (ctx.getMember().getRoles().stream().anyMatch(r -> roleSpecificDisabledCommands.computeIfAbsent(r.getId(),
                s -> new ArrayList<>()).contains(cmdName)) && !CommandPermission.ADMIN.test(ctx.getMember())) {
            return false;
        }
        //CCS disable check end.

        List<String> values = customCommand.getValues();
        if (customCommand.getData().isNsfw() && !ctx.getChannel().isNSFW()) {
            ctx.sendLocalized("commands.custom.nsfw_not_nsfw", EmoteReference.ERROR);
            return true;
        }

        CommandStatsManager.log("custom command");

        String response = values.get(random.nextInt(values.size()));
        try {
            new CustomCommandHandler(prefix, ctx, response, args).handle();
        } catch (SyntaxException e) {
            ctx.sendStrippedLocalized("commands.custom.error_running_new", EmoteReference.ERROR, e.getMessage());
        } catch (Exception e) {
            ctx.sendLocalized("commands.custom.error_running", EmoteReference.ERROR);
            e.printStackTrace();
        }

        return true;
    }

    //Lazy-load custom commands into cache.
    public static CustomCommand getCustomCommand(String id, String name) {
        //lol
        if (DefaultCommandProcessor.REGISTRY.commands().containsKey(name)) {
            return null;
        }

        if (customCommands.containsKey(id + ":" + name)) {
            return customCommands.get(id + ":" + name);
        }

        CustomCommand custom = db().getCustomCommand(id, name);
        //yes
        if (custom == null)
            return null;

        if (!NAME_PATTERN.matcher(name).matches()) {
            String newName = INVALID_CHARACTERS_PATTERN.matcher(custom.getName()).replaceAll("_");
            log.info("Custom Command with Invalid Characters {} found. Replacing with '_'", custom.getName());

            custom.deleteAsync();
            custom = CustomCommand.of(custom.getGuildId(), newName, custom.getValues());
            custom.saveAsync();
        }

        if (DefaultCommandProcessor.REGISTRY.commands().containsKey(custom.getName())) {
            custom.deleteAsync();
            custom = CustomCommand.of(custom.getGuildId(), "_" + custom.getName(), custom.getValues());
            custom.saveAsync();
        }

        //add to registry
        customCommands.put(custom.getId(), custom);

        return custom;
    }

    @Subscribe
    public void custom(CommandRegistry cr) {
        String any = "[\\d\\D]*?";
        final ManagedDatabase db = db();

        //People spamming crap... we cant have nice things owo
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                .spamTolerance(2)
                .limit(1)
                .cooldown(4, TimeUnit.SECONDS)
                .cooldownPenaltyIncrease(4, TimeUnit.SECONDS)
                .maxCooldown(2, TimeUnit.MINUTES)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("custom")
                .build();

        SimpleTreeCommand customCommand = (SimpleTreeCommand) cr.register("custom", new SimpleTreeCommand(Category.UTILS) {
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Manages the Custom Commands of the Guild. If you wish to allow normal people to make custom commands, run `~>opts admincustom false` (it's locked to admins by default)")
                        .setUsage("`~>custom <sub command>`")
                        .build();
            }
        });

        customCommand.setPredicate(ctx -> Utils.handleIncreasingRatelimit(rateLimiter, ctx.getAuthor(), ctx.getEvent(), null));

        //Just so this is in english.
        I18nContext i18nTemp = new I18nContext();
        Predicate<GuildMessageReceivedEvent> adminPredicate = (event) -> {
            if (db().getGuild(event.getGuild()).getData().isCustomAdminLockNew() && !CommandPermission.ADMIN.test(event.getMember())) {
                event.getChannel().sendMessage(i18nTemp.get("commands.custom.admin_only")).queue();
                return false;
            }

            return true;
        };

        customCommand.addSubCommand("list", new SubCommand() {
            @Override
            public String description() {
                return "Lists all the current commands on this server.";
            }

            @Override
            protected void call(Context ctx, String content) {
                String filter = ctx.getGuild().getId() + ":";
                List<String> commands = ctx.db().getCustomCommands(ctx.getGuild())
                        .stream()
                        .map(CustomCommand::getName)
                        .collect(Collectors.toList());
                I18nContext languageContext = ctx.getLanguageContext();

                EmbedBuilder builder = new EmbedBuilder()
                        .setAuthor(languageContext.get("commands.custom.ls.header"), null, ctx.getGuild().getIconUrl())
                        .setColor(ctx.getMember().getColor())
                        .setThumbnail("https://images.emojiterra.com/twitter/v11/512px/1f6e0.png")
                        .setDescription(languageContext.get("commands.custom.ls.description") + "\n" +
                                (commands.isEmpty() ? languageContext.get("general.dust") :
                                        checkString(commands.stream().map(cc -> "*`" + cc + "`*").collect(Collectors.joining(", "))
                                        ))
                        ).setFooter(String.format(languageContext.get("commands.custom.ls.footer"), commands.size()), ctx.getAuthor().getEffectiveAvatarUrl());

                ctx.send(builder.build());
            }
        }).createSubCommandAlias("list", "ls");

        customCommand.addSubCommand("view", new SubCommand() {
            @Override
            public String description() {
                return "Views the response of an specific command.";
            }

            @Override
            protected void call(Context ctx, String content) {
                String[] args = StringUtils.splitArgs(content, 2);
                if (args.length < 2) {
                    ctx.sendLocalized("commands.custom.view.not_found", EmoteReference.ERROR);
                    return;
                }

                String cmd = args[0];
                CustomCommand command = ctx.db().getCustomCommand(ctx.getGuild(), cmd);

                if (command == null) {
                    ctx.sendLocalized("commands.custom.view.not_found", EmoteReference.ERROR);
                    return;
                }

                int number;
                try {
                    number = Integer.parseInt(args[1]) - 1;
                } catch (NumberFormatException e) {
                    ctx.sendLocalized("general.invalid_number", EmoteReference.ERROR);
                    return;
                }

                if (command.getValues().size() < number) {
                    ctx.sendLocalized("commands.custom.view.less_than_specified", EmoteReference.ERROR);
                    return;
                }

                ctx.sendLocalized("commands.custom.view.success", (number + 1), command.getName(), command.getValues().get(number));

            }
        }).createSubCommandAlias("view", "vw");

        customCommand.addSubCommand("raw", new SubCommand() {
            @Override
            public String description() {
                return "Show all the raw responses of the specified command.";
            }

            @Override
            protected void call(Context ctx, String content) {
                String command = content.trim();
                if (command.isEmpty()) {
                    ctx.sendLocalized("commands.custom.raw.no_command", EmoteReference.ERROR);
                    return;
                }

                CustomCommand custom = ctx.db().getCustomCommand(ctx.getGuild(), command);
                if (custom == null) {
                    ctx.sendStrippedLocalized("commands.custom.not_found", EmoteReference.ERROR2, command);
                    return;
                }

                List<MessageEmbed.Field> fields = new ArrayList<>();
                AtomicInteger count = new AtomicInteger();
                for (String value : custom.getValues()) {
                    String val = value;
                    if (value.length() > 900)
                        val = Utils.paste(value);

                    fields.add(new MessageEmbed.Field("Response N° " + count.incrementAndGet(), val, true));
                }

                I18nContext languageContext = ctx.getLanguageContext();

                EmbedBuilder embed = baseEmbed(ctx.getEvent(), String.format(languageContext.get("commands.custom.raw.header"), command))
                        .setDescription(languageContext.get("commands.custom.raw.description"))
                        .setFooter(String.format(languageContext.get("commands.custom.raw.amount"), 6, custom.getValues().size()), null);

                List<List<MessageEmbed.Field>> splitFields = DiscordUtils.divideFields(6, fields);

                if (ctx.hasReactionPerms()) {
                    embed.appendDescription("\n" + String.format(languageContext.get("general.buy_sell_paged_react"), splitFields.size(), ""));
                    DiscordUtils.list(ctx.getEvent(), 100, false, embed, splitFields);
                } else {
                    embed.appendDescription("\n" + String.format(languageContext.get("general.buy_sell_paged_text"), splitFields.size(), ""));
                    DiscordUtils.listText(ctx.getEvent(), 100, false, embed, splitFields);
                }
            }
        }).createSubCommandAlias("raw", "rw");

        customCommand.addSubCommand("clear", new SubCommand() {
            @Override
            public String description() {
                return "Clear all custom commands.";
            }

            @Override
            protected void call(Context ctx, String content) {
                if (!ctx.getMember().hasPermission(Permission.MANAGE_SERVER)) {
                    return;
                }

                List<CustomCommand> customCommands = ctx.db().getCustomCommands(ctx.getGuild());

                if (customCommands.isEmpty()) {
                    ctx.sendLocalized("commands.custom.no_cc", EmoteReference.ERROR);
                    return;
                }

                int size = customCommands.size();
                customCommands.forEach(CustomCommand::deleteAsync);
                customCommands.forEach(c -> CustomCmds.customCommands.remove(c.getId()));
                ctx.sendLocalized("commands.custom.clear.success", EmoteReference.PENCIL, size);
            }
        }).createSubCommandAlias("clear", "clr");

        customCommand.addSubCommand("eval", new SubCommand() {
            @Override
            public String description() {
                return "Evaluates the result of a custom command.";
            }

            @Override
            protected void call(Context ctx, String content) {
                if (!adminPredicate.test(ctx.getEvent())) {
                    return;
                }

                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.custom.eval.not_specified", EmoteReference.ERROR);
                    return;
                }

                try {
                    String ctn = content;
                    ctn = Utils.DISCORD_INVITE.matcher(ctn).replaceAll("-invite link-");
                    ctn = Utils.DISCORD_INVITE_2.matcher(ctn).replaceAll("-invite link-");

                    //Sadly no way to get the prefix used, so eval will have the old bug still.
                    new CustomCommandHandler("", ctx, ctn).handle(true);
                } catch (SyntaxException e) {
                    ctx.sendStrippedLocalized("commands.custom.eval.new_error", EmoteReference.ERROR, e.getMessage());
                } catch (Exception e) {
                    ctx.sendStrippedLocalized("commands.custom.eval.error", EmoteReference.ERROR, e.getMessage() == null ? "" : " (E: " + e.getMessage() + ")");
                }
            }
        }).createSubCommandAlias("eval", "evl");

        customCommand.addSubCommand("remove", new SubCommand() {
            @Override
            public String description() {
                return "Removes a custom command.";
            }

            @Override
            protected void call(Context ctx, String content) {
                if (!adminPredicate.test(ctx.getEvent())) {
                    return;
                }

                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.custom.remove.no_command", EmoteReference.ERROR);
                    return;
                }

                if (!NAME_PATTERN.matcher(content).matches()) {
                    ctx.sendLocalized("commands.custom.character_not_allowed", EmoteReference.ERROR);
                    return;
                }

                //hint: always check for this
                if (DefaultCommandProcessor.REGISTRY.commands().containsKey(content)) {
                    ctx.sendLocalized("commands.custom.already_exists", EmoteReference.ERROR, content);
                    return;
                }

                CustomCommand custom = getCustomCommand(ctx.getGuild().getId(), content);
                if (custom == null) {
                    ctx.sendLocalized("commands.custom.not_found", EmoteReference.ERROR2, content);
                    return;
                }

                //delete at DB
                custom.deleteAsync();

                //reflect at local
                customCommands.remove(custom.getId());

                //clear commands if none
                if (customCommands.keySet().stream().noneMatch(s -> s.endsWith(":" + content)))
                    customCommands.remove(content);

                ctx.sendLocalized("commands.custom.remove.success", EmoteReference.PENCIL, content);
            }
        }).createSubCommandAlias("remove", "rm");

        customCommand.addSubCommand("import", new SubCommand() {
            @Override
            public String description() {
                return "Imports a custom command from another server you're in.";
            }

            @Override
            protected void call(Context ctx, String content) {
                if (!adminPredicate.test(ctx.getEvent())) {
                    return;
                }

                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.custom.import.no_command", EmoteReference.ERROR);
                    return;
                }

                if (!NAME_WILDCARD_PATTERN.matcher(content).matches()) {
                    ctx.sendLocalized("commands.custom.character_not_allowed", EmoteReference.ERROR);
                    return;
                }

                Map<String, Guild> mapped = ctx.getBot()
                        .getShardManager()
                        .getMutualGuilds(ctx.getAuthor()).stream()
                        .collect(Collectors.toMap(ISnowflake::getId, g -> g));

                List<Pair<Guild, CustomCommand>> filtered = db
                        .getCustomCommandsByName(("*" + content + "*").replace("*", any)).stream()
                        .map(customCommand -> {
                            Guild guild = mapped.get(customCommand.getGuildId());
                            return guild == null ? null : Pair.of(guild, customCommand);
                        })
                        .filter(Objects::nonNull)

                        .collect(Collectors.toList());

                if (filtered.size() == 0) {
                    ctx.sendLocalized("commands.custom.import.not_found", EmoteReference.ERROR);
                    return;
                }

                I18nContext languageContext = ctx.getLanguageContext();

                DiscordUtils.selectList(
                        ctx.getEvent(), filtered,
                        pair -> String.format(languageContext.get("commands.custom.import.header"), pair.getValue().getName(), pair.getRight().getValues().size(), pair.getKey()),
                        s -> baseEmbed(ctx.getEvent(), languageContext.get("commands.custom.import.selection")).setDescription(s)
                                .setFooter(
                                        languageContext.get("commands.custom.import.note"),
                                        null
                                ).build(),
                        pair -> {
                            CustomCommand custom = CustomCommand.transfer(ctx.getGuild().getId(), pair.getValue());
                            //save at DB
                            custom.saveAsync();

                            //reflect at local
                            customCommands.put(custom.getId(), custom);

                            ctx.sendLocalized("commands.custom.import.success", custom.getName(), pair.getKey().getName(), custom.getValues().size());
                            //easter egg :D
                            TextChannelGround.of(ctx.getEvent()).dropItemWithChance(8, 2);
                        }
                );
            }
        }).createSubCommandAlias("import", "ipt");

        customCommand.addSubCommand("info", new SubCommand() {
            @Override
            public String description() {
                return "Shows the information about an specific command.";
            }

            @Override
            protected void call(Context ctx, String content) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.custom.raw.no_command", EmoteReference.ERROR);
                    return;
                }

                CustomCommand command = ctx.db().getCustomCommand(ctx.getGuild(), content);
                String owner = command.getData().getOwner();
                User user = owner.isEmpty() ? null : ctx.getShardManager().getUserCache().getElementById(owner);
                
                ctx.send(new EmbedBuilder()
                        .setAuthor("Custom Command Information for " + content, null, ctx.getAuthor().getEffectiveAvatarUrl())
                        .setDescription(
                                EmoteReference.BLUE_SMALL_MARKER + "**Owner:** " + (user == null ? "Nobody" : user.getName() + "#" + user.getDiscriminator()) + "\n" +
                                        EmoteReference.BLUE_SMALL_MARKER + "**Owner ID:** " + (user == null ? "None" : user.getId()) + "\n" +
                                        EmoteReference.BLUE_SMALL_MARKER + "**NSFW:** " + command.getData().isNsfw() + "\n" +
                                        EmoteReference.BLUE_SMALL_MARKER + "**Responses:** " + command.getValues().size() + "\n"
                        )
                        .setThumbnail("https://i.imgur.com/jPL5Lof.png")
                        .build()
                );
            }
        });

        customCommand.addSubCommand("edit", new SubCommand() {
            @Override
            public String description() {
                return "Edits the response of a command.";
            }

            @Override
            protected void call(Context ctx, String content) {
                if (!adminPredicate.test(ctx.getEvent())) {
                    return;
                }
                
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.custom.edit.no_command", EmoteReference.ERROR);
                    return;
                }

                Map<String, String> opts = ctx.getOptionalArguments();
                String ctn = Utils.replaceArguments(opts, content, "nsfw");

                String[] args = StringUtils.splitArgs(ctn, -1);
                if (args.length < 2) {
                    ctx.sendLocalized("commands.custom.edit.not_enough_args", EmoteReference.ERROR);
                    return;
                }
                var cmd = args[0];
                CustomCommand custom = ctx.db().getCustomCommand(ctx.getGuild(), cmd);
                if (custom == null) {
                    ctx.sendLocalized("commands.custom.not_found", EmoteReference.ERROR2, args[0]);
                    return;
                }

                int where;
                String index = args[1];
                //replace first occurrence and second argument: custom command and index.
                String commandContent = ctn.replaceFirst(cmd, "").replaceFirst(index, "").trim();
                try {
                    where = Math.abs(Integer.parseInt(index));
                } catch (NumberFormatException e) {
                    ctx.sendLocalized("commands.custom.edit.invalid_number", EmoteReference.ERROR);
                    return;
                }

                List<String> values = custom.getValues();
                if (where - 1 > values.size()) {
                    ctx.sendLocalized("commands.custom.edit.no_index", EmoteReference.ERROR);
                    return;
                }

                if (commandContent.isEmpty()) {
                    ctx.sendLocalized("commands.custom.edit.empty_response", EmoteReference.ERROR);
                    return;
                }

                if (opts.containsKey("nsfw")) {
                    custom.getData().setNsfw(true);
                }

                custom.getValues().set(where - 1, commandContent);

                custom.saveAsync();
                customCommands.put(custom.getId(), custom);

                ctx.sendLocalized("commands.custom.edit.success", EmoteReference.CORRECT, where, custom.getName());
            }
        });

        customCommand.addSubCommand("deleteresponse", new SubCommand() {
            @Override
            public String description() {
                return "Deletes a response of a command.";
            }

            @Override
            protected void call(Context ctx, String content) {
                if (!adminPredicate.test(ctx.getEvent())) {
                    return;
                }

                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.custom.deleteresponse.no_command", EmoteReference.ERROR);
                    return;
                }

                String[] args = StringUtils.splitArgs(content, -1);
                if (args.length < 1) {
                    ctx.sendLocalized("commands.custom.deleteresponse.not_enough_args", EmoteReference.ERROR);
                    return;
                }

                CustomCommand custom = ctx.db().getCustomCommand(ctx.getGuild(), args[0]);
                if (custom == null) {
                    ctx.sendLocalized("commands.custom.not_found", EmoteReference.ERROR2, args[0]);
                    return;
                }

                int where;
                String index = args[1];
                try {
                    where = Math.abs(Integer.parseInt(index));
                } catch (NumberFormatException e) {
                    ctx.sendLocalized("commands.custom.deleteresponse.invalid_number", EmoteReference.ERROR);
                    return;
                }

                List<String> values = custom.getValues();
                if (where - 1 > values.size()) {
                    ctx.sendLocalized("commands.custom.deleteresponse.no_index", EmoteReference.ERROR);
                    return;
                }

                custom.getValues().remove(where - 1);

                custom.saveAsync();
                customCommands.put(custom.getId(), custom);

                ctx.sendLocalized("commands.custom.deleteresponse.success", EmoteReference.CORRECT, where, custom.getName());
            }
        }).createSubCommandAlias("deleteresponse", "dlr");

        customCommand.addSubCommand("rename", new SubCommand() {
            @Override
            public String description() {
                return "Renames a custom command.";
            }

            @Override
            protected void call(Context ctx, String content) {
                if (!adminPredicate.test(ctx.getEvent())) {
                    return;
                }

                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.custom.rename.no_command", EmoteReference.ERROR);
                    return;
                }

                String[] args = ctx.getArguments();

                if (args.length < 2) {
                    ctx.sendLocalized("commands.custom.rename.not_enough_args", EmoteReference.ERROR);
                    return;
                }

                String cmd = args[0];
                String value = args[1];

                if (!NAME_PATTERN.matcher(cmd).matches() || !NAME_PATTERN.matcher(value).matches()) {
                    ctx.sendLocalized("commands.custom.character_not_allowed", EmoteReference.ERROR);
                    return;
                }

                if (DefaultCommandProcessor.REGISTRY.commands().containsKey(value)) {
                    ctx.sendLocalized("commands.custom.already_exists", EmoteReference.ERROR);
                    return;
                }

                CustomCommand oldCustom = ctx.db().getCustomCommand(ctx.getGuild(), cmd);

                if (oldCustom == null) {
                    ctx.sendLocalized("commands.custom.not_found", EmoteReference.ERROR2, cmd);
                    return;
                }

                CustomCommand newCustom = CustomCommand.of(ctx.getGuild().getId(), value, oldCustom.getValues());

                final CustomCommandData oldCustomData = oldCustom.getData();
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
                TextChannelGround.of(ctx.getEvent()).dropItemWithChance(8, 2);

            }
        }).createSubCommandAlias("rename", "rn");

        customCommand.addSubCommand("add", new SubCommand() {
            @Override
            public String description() {
                return "Adds a new custom commands or adds a response to an existing command.";
            }

            @Override
            protected void call(Context ctx, String content) {
                if (!adminPredicate.test(ctx.getEvent())) {
                    return;
                }

                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.custom.add.no_command", EmoteReference.ERROR);
                    return;
                }

                String[] args = StringUtils.splitArgs(content, -1);

                if (args.length < 2) {
                    ctx.sendLocalized("commands.custom.add.not_enough_args", EmoteReference.ERROR);
                    return;
                }

                String cmd = args[0];
                String value = content.replaceFirst(args[0], "").trim();

                Map<String, String> opts = ctx.getOptionalArguments();
                String cmdSource = Utils.replaceArguments(opts, value, "nsfw");

                if (cmdSource.isEmpty()) {
                    ctx.sendLocalized("commands.custom.add.empty_content", EmoteReference.ERROR);
                    return;
                }

                if (!NAME_PATTERN.matcher(cmd).matches()) {
                    ctx.sendLocalized("commands.custom.character_not_allowed", EmoteReference.ERROR);
                    return;
                }

                if (cmd.length() >= 50) {
                    ctx.sendLocalized("commands.custom.name_too_long", EmoteReference.ERROR);
                    return;
                }

                if (DefaultCommandProcessor.REGISTRY.commands().containsKey(cmd)) {
                    ctx.sendLocalized("commands.custom.already_exists", EmoteReference.ERROR, cmd);
                    return;
                }

                cmdSource = cmdSource.replace("@everyone", "[nice meme]").replace("@here", "[you tried]");

                if (cmdSource.contains("v3:")) {
                    try {
                        new Parser(cmdSource).parse();
                    } catch (SyntaxException e) {
                        ctx.sendLocalized("commands.custom.new_error", EmoteReference.ERROR, e.getMessage());
                        return;
                    }
                }

                CustomCommand custom = CustomCommand.of(ctx.getGuild().getId(), cmd, Collections.singletonList(cmdSource));
                CustomCommand c = ctx.db().getCustomCommand(ctx.getEvent(), cmd);

                if (c != null) {
                    custom.getValues().addAll(c.getValues());
                } else {
                    //Are the first two checks redundant?
                    if (!ctx.getConfig().isPremiumBot() && !ctx.getDBGuild().isPremium() && db.getCustomCommands(ctx.getGuild()).size() > 100) {
                        ctx.sendLocalized("commands.custom.add.too_many_commands", EmoteReference.ERROR);
                        return;
                    }
                }


                custom.getData().setOwner(ctx.getAuthor().getId());
                if (opts.containsKey("nsfw")) {
                    custom.getData().setNsfw(true);
                }

                //save at DB
                custom.saveAsync();
                //reflect at local
                customCommands.put(custom.getId(), custom);

                ctx.sendLocalized("commands.custom.add.success", EmoteReference.CORRECT, cmd);

                //easter egg :D
                TextChannelGround.of(ctx.getEvent()).dropItemWithChance(8, 2);
            }
        }).createSubCommandAlias("add", "new");
    }
}
